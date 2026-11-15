package com.example.server

import android.content.Context
import android.os.Build
import android.os.Environment
import com.example.controller.AppManagerController
import com.example.controller.ContentDataProvider
import com.example.controller.DeviceController
import com.example.controller.PermissionHelper
import com.example.controller.ScreenCastManager
import com.example.data.AuthManager
import com.example.data.CommandLogRepository
import com.example.data.McpManager
import com.example.mcp.McpEngine
import java.util.UUID
import com.example.data.NotificationRepository
import com.example.data.ServerStateManager
import com.example.service.PhoneDeckAccessibilityService
import com.example.service.RemoteServerService
import com.example.service.ScreenCastPermissionActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class HttpBridgeServer(
    private val context: Context,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        CommandLogRepository.log("ERROR", "bridge_scope", "local", 500, throwable.message ?: "Coroutine error")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    @Synchronized
    fun start() {
        if (serverJob != null && serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                val ss = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                serverSocket = ss
                val localIp = DeviceController.getLocalIpAddress()
                ServerStateManager.setServerRunning(true, port, localIp)
                CommandLogRepository.log("SYSTEM", "/server/start", "127.0.0.1", 200, "Server started on $localIp:$port")

                while (isActive) {
                    val client = try {
                        ss.accept()
                    } catch (e: Exception) {
                        if (!isActive || ss.isClosed) break
                        continue
                    }
                    launch {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                CommandLogRepository.log("ERROR", "/server", "127.0.0.1", 500, "Server error: ${e.message}")
            } finally {
                ServerStateManager.setServerRunning(false, port)
            }
        }
    }

    @Synchronized
    fun stop() {
        runCatching {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
            serverJob = null
            ServerStateManager.setServerRunning(false, port)
            CommandLogRepository.log("SYSTEM", "/server/stop", "127.0.0.1", 200, "Server stopped")
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        try {
            socket.soTimeout = 30000
            socket.tcpNoDelay = true
            val input = socket.getInputStream()
            val output = BufferedOutputStream(socket.getOutputStream(), 16384)

            val line = readLine(input) ?: return@withContext
            val parts = line.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val rawPath = parts[1]
            val path = if (rawPath.contains("?")) rawPath.substringBefore("?") else rawPath
            val queryStr = if (rawPath.contains("?")) rawPath.substringAfter("?") else ""
            val queryParams = parseQueryParams(queryStr)

            val headers = mutableMapOf<String, String>()
            var headerLine: String?
            while (readLine(input).also { headerLine = it } != null && !headerLine.isNullOrEmpty()) {
                val colonIdx = headerLine!!.indexOf(':')
                if (colonIdx > 0) {
                    val key = headerLine!!.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            if (headers["expect"]?.equals("100-continue", ignoreCase = true) == true) {
                output.write("HTTP/1.1 100 Continue\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                output.flush()
            }

            ServerStateManager.incrementRequests()

            if (method == "OPTIONS") {
                sendResponse(output, 204, "No Content", "text/plain", ByteArray(0))
                return@withContext
            }

            val isAuthenticated = AuthManager.isRequestAuthenticated(context, headers["cookie"])

            when {
                method == "GET" && (path == "/" || path == "/index.html") -> {
                    val html = if (isAuthenticated) DashboardHtml.get() else AuthHtml.get()
                    sendResponse(
                        output,
                        200,
                        "OK",
                        "text/html; charset=utf-8",
                        html.toByteArray(StandardCharsets.UTF_8),
                        mapOf("Cache-Control" to "no-store")
                    )
                }

                method == "GET" && path == "/api/auth/status" -> {
                    val json = JSONObject().apply {
                        put("authenticated", isAuthenticated)
                        put("isDefaultPassword", if (isAuthenticated) AuthManager.isUsingDefaultPassword(context) else false)
                    }
                    sendJson(output, json.toString(), mapOf("Cache-Control" to "no-store"))
                }

                method == "POST" && path == "/api/auth/login" -> {
                    val bodyStr = readBodyString(input, headers)
                    val password = runCatching { JSONObject(bodyStr).optString("password", "") }.getOrDefault("")
                    if (AuthManager.verifyPassword(context, password)) {
                        val cookieHeader = AuthManager.buildSessionCookieHeader(context)
                        val json = JSONObject()
                            .put("success", true)
                            .put("isDefaultPassword", AuthManager.isUsingDefaultPassword(context))
                            .toString()
                        sendResponse(
                            output,
                            200,
                            "OK",
                            "application/json; charset=utf-8",
                            json.toByteArray(StandardCharsets.UTF_8),
                            mapOf(
                                "Set-Cookie" to cookieHeader,
                                "Cache-Control" to "no-store"
                            )
                        )
                        CommandLogRepository.log("AUTH", "/api/auth/login", clientIp, 200, "Session unlocked")
                    } else {
                        val json = JSONObject()
                            .put("success", false)
                            .put("message", "Incorrect password")
                            .toString()
                        sendResponse(
                            output,
                            401,
                            "Unauthorized",
                            "application/json; charset=utf-8",
                            json.toByteArray(StandardCharsets.UTF_8),
                            mapOf("Cache-Control" to "no-store")
                        )
                        CommandLogRepository.log("AUTH", "/api/auth/login", clientIp, 401, "Failed login attempt")
                    }
                }

                // MCP Protocol Endpoints (Protected by Bearer Token)
                path == "/mcp" || path.startsWith("/mcp/") -> {
                    handleMcpRequest(method, path, queryParams, headers, input, output, socket, clientIp)
                }

                !isAuthenticated -> {
                    val json = JSONObject()
                        .put("success", false)
                        .put("error", "unauthorized")
                        .put("message", "Authentication required")
                        .toString()
                    sendResponse(
                        output,
                        401,
                        "Unauthorized",
                        "application/json; charset=utf-8",
                        json.toByteArray(StandardCharsets.UTF_8),
                        mapOf("Cache-Control" to "no-store")
                    )
                }

                method == "POST" && path == "/api/auth/change-password" -> {
                    val bodyStr = readBodyString(input, headers)
                    val jsonBody = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val currentPwd = jsonBody.optString("currentPassword", "")
                    val newPwd = jsonBody.optString("newPassword", "")
                    val (ok, msg) = AuthManager.changePassword(context, currentPwd, newPwd)
                    if (ok) {
                        val refreshedCookie = AuthManager.buildSessionCookieHeader(context)
                        val respJson = JSONObject()
                            .put("success", true)
                            .put("message", msg)
                            .put("isDefaultPassword", AuthManager.isUsingDefaultPassword(context))
                            .toString()
                        sendResponse(
                            output,
                            200,
                            "OK",
                            "application/json; charset=utf-8",
                            respJson.toByteArray(StandardCharsets.UTF_8),
                            mapOf(
                                "Set-Cookie" to refreshedCookie,
                                "Cache-Control" to "no-store"
                            )
                        )
                        CommandLogRepository.log("AUTH", "/api/auth/change-password", clientIp, 200, "Password updated")
                    } else {
                        val respJson = JSONObject()
                            .put("success", false)
                            .put("message", msg)
                            .toString()
                        sendResponse(
                            output,
                            400,
                            "Bad Request",
                            "application/json; charset=utf-8",
                            respJson.toByteArray(StandardCharsets.UTF_8),
                            mapOf("Cache-Control" to "no-store")
                        )
                    }
                }

                method == "POST" && path == "/api/auth/logout" -> {
                    val respJson = JSONObject().put("success", true).toString()
                    sendResponse(
                        output,
                        200,
                        "OK",
                        "application/json; charset=utf-8",
                        respJson.toByteArray(StandardCharsets.UTF_8),
                        mapOf(
                            "Set-Cookie" to AuthManager.buildClearCookieHeader(),
                            "Cache-Control" to "no-store"
                        )
                    )
                    CommandLogRepository.log("AUTH", "/api/auth/logout", clientIp, 200, "Session locked")
                }

                // Web UI MCP Management APIs
                method == "GET" && path == "/api/mcp/config" -> {
                    val cfg = McpManager.getConfigJson(context)
                    cfg.put("activeToolsCount", McpEngine.getActiveTools(context).size)
                    sendJson(output, cfg.toString())
                }

                method == "POST" && path == "/api/mcp/config" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    McpManager.updateConfigFromJson(context, json)
                    val updated = McpManager.getConfigJson(context)
                    updated.put("activeToolsCount", McpEngine.getActiveTools(context).size)
                    sendJson(output, updated.toString())
                }

                method == "POST" && path == "/api/mcp/regenerate-token" -> {
                    val newToken = McpManager.regenerateToken(context)
                    val resp = JSONObject().apply {
                        put("success", true)
                        put("token", newToken)
                        put("maskedToken", if (newToken.length > 10) newToken.take(7) + "••••••••" + newToken.takeLast(4) else "••••••••")
                    }
                    CommandLogRepository.log("MCP", "/api/mcp/regenerate-token", clientIp, 200, "MCP Bearer token regenerated")
                    sendJson(output, resp.toString())
                }

                method == "GET" && path == "/api/mcp/tools" -> {
                    val tools = McpEngine.getActiveTools(context)
                    val arr = JSONArray()
                    tools.forEach { t ->
                        arr.put(JSONObject().apply {
                            put("name", t.name)
                            put("description", t.description)
                            put("category", t.category)
                            put("inputSchema", t.inputSchema)
                        })
                    }
                    sendJson(output, JSONObject().put("tools", arr).toString())
                }

                method == "POST" && path == "/api/mcp/test" -> {
                    val bodyStr = readBodyString(input, headers)
                    val jsonReq = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val payload = if (jsonReq.has("jsonrpc")) {
                        bodyStr
                    } else {
                        val m = jsonReq.optString("method", "tools/list")
                        val p = jsonReq.optJSONObject("params") ?: JSONObject()
                        JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", m).put("params", p).toString()
                    }
                    val result = McpEngine.processJsonRpc(context, payload)
                    sendJson(output, result)
                }

                method == "GET" && path == "/api/permissions" -> {
                    val perms = PermissionHelper.getAllPermissionStates(context)
                    val array = JSONArray()
                    val mapObj = JSONObject()
                    perms.forEach { p ->
                        mapObj.put(p.id, p.isGranted)
                        array.put(JSONObject().apply {
                            put("id", p.id)
                            put("title", p.title)
                            put("webPageId", p.webPageId)
                            put("isGranted", p.isGranted)
                            put("isSpecial", p.isSpecial)
                        })
                    }
                    val resp = JSONObject().apply {
                        put("permissions", array)
                        put("map", mapObj)
                    }
                    sendJson(output, resp.toString())
                }

                method == "GET" && path == "/api/status" -> {
                    val status = DeviceController.getDeviceStatus(context)
                    val json = JSONObject().apply {
                        put("model", status.model)
                        put("manufacturer", status.manufacturer)
                        put("brand", status.brand)
                        put("androidVersion", status.androidVersion)
                        put("sdkInt", status.sdkInt)
                        put("batteryLevel", status.batteryLevel)
                        put("isCharging", status.isCharging)
                        put("pluggedType", status.pluggedType)
                        put("batteryTempC", status.batteryTempC.toDouble())
                        put("batteryVoltageMv", status.batteryVoltageMv)
                        put("batteryHealth", status.batteryHealth)
                        put("totalRamMb", status.totalRamMb)
                        put("availRamMb", status.availRamMb)
                        put("totalStorageGb", status.totalStorageGb.toDouble())
                        put("freeStorageGb", status.freeStorageGb.toDouble())
                        put("isScreenOn", status.isScreenOn)
                        put("mediaVolumePercent", status.mediaVolumePercent)
                        put("ringVolumePercent", status.ringVolumePercent)
                        put("alarmVolumePercent", status.alarmVolumePercent)
                        put("isMuted", status.isMuted)
                        put("wifiSsid", status.wifiSsid)
                        put("localIp", status.localIp)
                        put("serverPort", status.serverPort)
                        put("isWakeLockActive", status.isWakeLockActive)
                        put("isTorchActive", status.isTorchActive)
                        put("uptimeSeconds", status.uptimeSeconds)
                        put("totalRequests", status.totalRequests)
                        put("clipboardPreview", status.clipboardPreview)
                        put("isCasting", ScreenCastManager.isCasting.value)
                    }
                    sendJson(output, json.toString())
                }

                /* LIVE SCREEN CAST ENDPOINTS */
                method == "GET" && path == "/api/screen/status" -> {
                    val json = JSONObject().apply {
                        put("isCasting", ScreenCastManager.isCasting.value)
                        put("fps", ScreenCastManager.targetFps.get())
                        put("quality", ScreenCastManager.jpegQuality.get())
                        put("scale", ScreenCastManager.scalePercent.get())
                        put("width", ScreenCastManager.streamWidth.get())
                        put("height", ScreenCastManager.streamHeight.get())
                        put("frameCount", ScreenCastManager.frameCount.get())
                        put("lastError", ScreenCastManager.lastError.value)
                    }
                    sendJson(output, json.toString())
                }

                method == "POST" && path == "/api/screen/request" -> {
                    val ok = runCatching {
                        ScreenCastPermissionActivity.launch(context)
                        true
                    }.getOrDefault(false)
                    sendJson(
                        output,
                        JSONObject()
                            .put("success", ok)
                            .put("message", if (ok) "Screen cast consent dialog opened on phone" else "Could not open dialog")
                            .toString()
                    )
                }

                method == "POST" && path == "/api/screen/stop" -> {
                    RemoteServerService.stopScreenCast(context)
                    sendJson(output, JSONObject().put("success", true).put("message", "Screen cast stopped").toString())
                }

                method == "POST" && path == "/api/screen/config" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val fps = if (json.has("fps")) json.optInt("fps") else null
                    val quality = if (json.has("quality")) json.optInt("quality") else null
                    val scale = if (json.has("scale")) json.optInt("scale") else null
                    ScreenCastManager.updateConfig(fps, quality, scale)
                    sendJson(output, JSONObject().put("success", true).toString())
                }

                method == "GET" && path == "/api/screen/frame" -> {
                    val frame = ScreenCastManager.getLatestFrame()
                    if (frame == null) {
                        sendResponse(output, 404, "Not Found", "text/plain", "No frame available".toByteArray())
                    } else {
                        sendResponse(output, 200, "OK", "image/jpeg", frame, mapOf("Cache-Control" to "no-store"))
                    }
                }

                method == "GET" && path == "/api/screen/stream" -> {
                    queryParams["fps"]?.toIntOrNull()?.let { ScreenCastManager.updateConfig(it, null, null) }
                    queryParams["quality"]?.toIntOrNull()?.let { ScreenCastManager.updateConfig(null, it, null) }
                    queryParams["scale"]?.toIntOrNull()?.let { ScreenCastManager.updateConfig(null, null, it) }

                    socket.soTimeout = 0
                    val boundary = "phonedeckframe"
                    val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=$boundary\r\n" +
                        "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Connection: close\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n"
                    output.write(header.toByteArray(StandardCharsets.US_ASCII))
                    output.flush()

                    var lastSentCount = -1L
                    while (isActive && ScreenCastManager.isCasting.value && !socket.isClosed) {
                        val currentCount = ScreenCastManager.frameCount.get()
                        val frame = ScreenCastManager.getLatestFrame()
                        if (frame != null && currentCount != lastSentCount) {
                            lastSentCount = currentCount
                            val partHeader = "--$boundary\r\n" +
                                "Content-Type: image/jpeg\r\n" +
                                "Content-Length: ${frame.size}\r\n\r\n"
                            output.write(partHeader.toByteArray(StandardCharsets.US_ASCII))
                            output.write(frame)
                            output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
                            output.flush()
                        }
                        val fps = ScreenCastManager.targetFps.get().coerceIn(1, 20)
                        delay((1000L / fps).coerceAtLeast(35L))
                    }
                }

                /* HARDWARE & DEVICE ACTIONS */
                method == "POST" && path == "/api/action" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val action = json.optString("action")
                    val responseJson = JSONObject()

                    var success = true
                    var msg = "Action completed"

                    when (action) {
                        "torch_toggle" -> {
                            success = DeviceController.toggleTorch(context)
                            msg = if (success) "Flashlight toggled" else "Camera permission or flash unavailable"
                        }
                        "torch_on" -> {
                            success = DeviceController.toggleTorch(context, true)
                            msg = if (success) "Flashlight ON" else "Flashlight unavailable"
                        }
                        "torch_off" -> {
                            success = DeviceController.toggleTorch(context, false)
                            msg = if (success) "Flashlight OFF" else "Flashlight unavailable"
                        }
                        "speak" -> {
                            val text = json.optString("text", "Notification from PhoneDeck")
                            success = DeviceController.speak(text)
                            msg = if (success) "Spoke: \"$text\"" else "TTS unavailable"
                        }
                        "vibrate" -> {
                            val pattern = json.optString("pattern", "short")
                            val dur = json.optLong("duration", 300)
                            success = DeviceController.vibrate(context, pattern, dur)
                            msg = "Vibrated ($pattern)"
                        }
                        "play_sound" -> {
                            val type = json.optString("type", "beep")
                            success = DeviceController.playSound(context, type)
                            msg = "Played sound ($type)"
                        }
                        "set_volume" -> {
                            val stream = json.optString("stream", "media")
                            val percent = json.optInt("percent", 50)
                            success = DeviceController.setVolume(context, stream, percent)
                            msg = "Set $stream volume to $percent%"
                        }
                        "set_clipboard" -> {
                            val text = json.optString("text", "")
                            success = DeviceController.setClipboard(context, text)
                            msg = "Copied to phone clipboard"
                        }
                        "get_clipboard" -> {
                            val clip = DeviceController.getClipboard(context)
                            responseJson.put("clipboard", clip)
                            msg = "Clipboard fetched"
                        }
                        "open_url" -> {
                            val url = json.optString("url", "https://google.com")
                            success = DeviceController.openUrl(context, url)
                            msg = "Opened URL: $url"
                        }
                        "launch_app" -> {
                            val pkg = json.optString("packageName", "")
                            success = DeviceController.launchApp(context, pkg)
                            msg = if (success) "Launched: $pkg" else "Could not launch: $pkg"
                        }
                        "wake_screen" -> {
                            success = DeviceController.wakeScreen(context)
                            msg = "Screen wake signal dispatched"
                        }
                        "show_notification" -> {
                            val title = json.optString("title", "PhoneDeck Alert")
                            val message = json.optString("message", "Message from Desktop")
                            success = DeviceController.postTestNotification(context, title, message)
                            msg = "Notification posted"
                        }
                        "clear_notifications" -> {
                            NotificationRepository.clear()
                            msg = "Notifications cleared"
                        }
                        else -> {
                            success = false
                            msg = "Unknown action: $action"
                        }
                    }

                    responseJson.put("success", success)
                    responseJson.put("message", msg)
                    sendJson(output, responseJson.toString())
                    CommandLogRepository.log("POST", "/api/action", clientIp, 200, "$action: $msg")
                }

                /* FILES */
                method == "GET" && path == "/api/files" -> {
                    val targetDir = queryParams["path"]
                    val res = ContentDataProvider.browseFiles(targetDir, context)
                    val itemsArr = JSONArray()
                    res.items.forEach { f ->
                        itemsArr.put(JSONObject().apply {
                            put("name", f.name)
                            put("path", f.path)
                            put("isDirectory", f.isDirectory)
                            put("sizeBytes", f.sizeBytes)
                            put("lastModified", f.lastModified)
                        })
                    }
                    val resp = JSONObject().apply {
                        put("currentPath", res.currentPath)
                        put("parentPath", res.parentPath ?: "")
                        put("hasAllFilesAccess", res.hasAllFilesAccess)
                        put("items", itemsArr)
                    }
                    sendJson(output, resp.toString())
                }

                method == "POST" && path == "/api/files/delete" -> {
                    val bodyStr = readBodyString(input, headers)
                    val targetPath = runCatching { JSONObject(bodyStr).optString("path") }.getOrDefault("")
                    val ok = ContentDataProvider.deleteFile(targetPath)
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                method == "POST" && path == "/api/files/mkdir" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val ok = ContentDataProvider.createFolder(json.optString("parentPath"), json.optString("name"))
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                /* MEDIA (STREAMING - NO RAM BUFFER OVERFLOW) */
                method == "GET" && path == "/api/media" -> {
                    val type = queryParams["type"] ?: "images"
                    val items = ContentDataProvider.getMediaItems(context, type)
                    val arr = JSONArray()
                    items.forEach { m ->
                        arr.put(JSONObject().apply {
                            put("id", m.id)
                            put("name", m.name)
                            put("type", m.type)
                            put("sizeBytes", m.sizeBytes)
                            put("dateAddedSec", m.dateAddedSec)
                            put("mimeType", m.mimeType)
                        })
                    }
                    sendJson(output, JSONObject().put("items", arr).toString())
                }

                method == "GET" && path == "/api/media/raw" -> {
                    val id = queryParams["id"]?.toLongOrNull()
                    val type = queryParams["type"] ?: "images"
                    if (id == null) {
                        sendResponse(output, 400, "Bad Request", "text/plain", "Missing id".toByteArray())
                        return@withContext
                    }
                    val streamPair = ContentDataProvider.openMediaStream(context, id, type)
                    if (streamPair == null) {
                        sendResponse(output, 404, "Not Found", "text/plain", "Media not found".toByteArray())
                        return@withContext
                    }
                    val (mime, stream) = streamPair
                    stream.use { inStream ->
                        val header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: $mime\r\n" +
                            "Transfer-Encoding: chunked\r\n" +
                            "Connection: close\r\n" +
                            "Access-Control-Allow-Origin: *\r\n\r\n"
                        output.write(header.toByteArray(StandardCharsets.US_ASCII))
                        val buf = ByteArray(65536)
                        var read: Int
                        while (inStream.read(buf).also { read = it } != -1) {
                            output.write("${Integer.toHexString(read)}\r\n".toByteArray(StandardCharsets.US_ASCII))
                            output.write(buf, 0, read)
                            output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
                        }
                        output.write("0\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                        output.flush()
                    }
                }

                /* CONTACTS, SMS, CALLS, CALENDAR, LOCATION, BLUETOOTH, USAGE */
                method == "GET" && path == "/api/contacts" -> {
                    val list = ContentDataProvider.getContacts(context)
                    val arr = JSONArray()
                    list.forEach { c ->
                        arr.put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                        })
                    }
                    sendJson(output, JSONObject().put("items", arr).toString())
                }

                method == "POST" && path == "/api/contacts/add" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val ok = ContentDataProvider.addContact(
                        context,
                        json.optString("name"),
                        json.optString("phone"),
                        json.optString("email")
                    )
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                method == "GET" && path == "/api/sms" -> {
                    val list = ContentDataProvider.getSmsMessages(context)
                    val arr = JSONArray()
                    list.forEach { s ->
                        arr.put(JSONObject().apply {
                            put("id", s.id)
                            put("address", s.address)
                            put("body", s.body)
                            put("date", s.date)
                            put("type", s.type)
                        })
                    }
                    sendJson(output, JSONObject().put("items", arr).toString())
                }

                method == "POST" && path == "/api/sms/send" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val ok = ContentDataProvider.sendSms(context, json.optString("phone"), json.optString("message"))
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                method == "GET" && path == "/api/calls" -> {
                    val logs = ContentDataProvider.getCallLogs(context)
                    val phoneInfo = ContentDataProvider.getPhoneNetworkInfo(context)
                    val arr = JSONArray()
                    logs.forEach { c ->
                        arr.put(JSONObject().apply {
                            put("number", c.number)
                            put("name", c.name)
                            put("type", c.type)
                            put("date", c.date)
                            put("durationSec", c.durationSec)
                        })
                    }
                    val resp = JSONObject().apply {
                        put("items", arr)
                        put("phone", JSONObject().apply {
                            put("operatorName", phoneInfo.operatorName)
                            put("networkType", phoneInfo.networkType)
                            put("simState", phoneInfo.simState)
                            put("phoneType", phoneInfo.phoneType)
                        })
                    }
                    sendJson(output, resp.toString())
                }

                method == "POST" && path == "/api/calls/dial" -> {
                    val bodyStr = readBodyString(input, headers)
                    val num = runCatching { JSONObject(bodyStr).optString("number") }.getOrDefault("")
                    val ok = ContentDataProvider.makeCall(context, num)
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                method == "GET" && path == "/api/calendar" -> {
                    val events = ContentDataProvider.getCalendarEvents(context)
                    val arr = JSONArray()
                    events.forEach { e ->
                        arr.put(JSONObject().apply {
                            put("id", e.id)
                            put("title", e.title)
                            put("startMs", e.startMs)
                            put("endMs", e.endMs)
                            put("location", e.location)
                            put("description", e.description)
                        })
                    }
                    sendJson(output, JSONObject().put("items", arr).toString())
                }

                method == "POST" && path == "/api/calendar/add" -> {
                    val bodyStr = readBodyString(input, headers)
                    val json = runCatching { JSONObject(bodyStr) }.getOrDefault(JSONObject())
                    val now = System.currentTimeMillis()
                    val ok = ContentDataProvider.addCalendarEvent(
                        context,
                        json.optString("title", "PhoneDeck Event"),
                        json.optLong("startMs", now + 3600000L),
                        json.optLong("endMs", now + 7200000L),
                        json.optString("location"),
                        json.optString("description")
                    )
                    sendJson(output, JSONObject().put("success", ok).toString())
                }

                method == "GET" && path == "/api/location" -> {
                    val loc = ContentDataProvider.getLocationInfo(context)
                    val json = JSONObject().apply {
                        put("available", loc.available)
                        put("latitude", loc.latitude)
                        put("longitude", loc.longitude)
                        put("accuracyMeters", loc.accuracyMeters.toDouble())
                        put("altitudeMeters", loc.altitudeMeters)
                        put("speedMps", loc.speedMps.toDouble())
                        put("provider", loc.provider)
                        put("timestamp", loc.timestamp)
                    }
                    sendJson(output, json.toString())
                }

                method == "GET" && path == "/api/bluetooth" -> {
                    val bt = ContentDataProvider.getBluetoothSummary(context)
                    val devArr = JSONArray()
                    bt.devices.forEach { d ->
                        devArr.put(JSONObject().apply {
                            put("name", d.name)
                            put("address", d.address)
                            put("bondState", d.bondState)
                        })
                    }
                    val json = JSONObject().apply {
                        put("isSupported", bt.isSupported)
                        put("isEnabled", bt.isEnabled)
                        put("adapterName", bt.adapterName)
                        put("devices", devArr)
                    }
                    sendJson(output, json.toString())
                }

                method == "GET" && path == "/api/usage" -> {
                    val stats = ContentDataProvider.getAppUsageStats(context)
                    val arr = JSONArray()
                    stats.forEach { u ->
                        arr.put(JSONObject().apply {
                            put("packageName", u.packageName)
                            put("appName", u.appName)
                            put("totalMinutes", u.totalMinutes)
                            put("lastUsedMs", u.lastUsedMs)
                        })
                    }
                    sendJson(output, JSONObject().put("items", arr).toString())
                }

                /* APPS + REMOTE INSTALL & UNINSTALL */
                method == "GET" && path == "/api/apps" -> {
                    val apps = DeviceController.getInstalledApps(context)
                    val array = JSONArray()
                    apps.forEach { app ->
                        array.put(JSONObject().apply {
                            put("name", app.name)
                            put("packageName", app.packageName)
                            put("versionName", app.versionName)
                            put("isSystem", app.isSystem)
                        })
                    }
                    sendJson(output, array.toString())
                }

                method == "GET" && path == "/api/apps/status" -> {
                    val last = AppManagerController.lastOpStatus.get()
                    val canInstall = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
                    } else true
                    val json = JSONObject().apply {
                        put("state", last.state)
                        put("message", last.message)
                        put("packageName", last.packageName)
                        put("timestamp", last.timestamp)
                        put("canInstallUnknownApps", canInstall)
                        put("accessibilityEnabled", PhoneDeckAccessibilityService.isEnabled(context))
                    }
                    sendJson(output, json.toString())
                }

                method == "POST" && path == "/api/apps/install" -> {
                    val bodyStr = readBodyString(input, headers)
                    val apkPath = runCatching { JSONObject(bodyStr).optString("path") }.getOrDefault("")
                    val res = AppManagerController.installApk(context, apkPath)
                    sendJson(
                        output,
                        JSONObject()
                            .put("success", res.success)
                            .put("mode", res.mode)
                            .put("message", res.message)
                            .toString()
                    )
                }

                method == "POST" && path == "/api/apps/upload-install" -> {
                    val rawName = queryParams["name"] ?: "app_${System.currentTimeMillis()}.apk"
                    val safeName = sanitizeFilename(rawName, "apk")
                    val cacheFolder = File(context.cacheDir, "remote_installs").apply { mkdirs() }
                    val destFile = File(cacheFolder, safeName)

                    val length = headers["content-length"]?.toLongOrNull() ?: 0L
                    if (length <= 0L) {
                        sendJson(output, JSONObject().put("success", false).put("message", "Empty APK body").toString())
                        return@withContext
                    }

                    val written = streamBodyToFile(input, destFile, length)
                    if (written <= 0L) {
                        sendJson(output, JSONObject().put("success", false).put("message", "Failed writing APK").toString())
                        return@withContext
                    }

                    val res = AppManagerController.installApk(context, destFile.absolutePath)
                    sendJson(
                        output,
                        JSONObject()
                            .put("success", res.success)
                            .put("mode", res.mode)
                            .put("message", res.message)
                            .put("apkPath", destFile.absolutePath)
                            .toString()
                    )
                }

                method == "POST" && path == "/api/apps/uninstall" -> {
                    val bodyStr = readBodyString(input, headers)
                    val pkg = runCatching { JSONObject(bodyStr).optString("packageName") }.getOrDefault("")
                    val res = AppManagerController.uninstallApp(context, pkg)
                    sendJson(
                        output,
                        JSONObject()
                            .put("success", res.success)
                            .put("mode", res.mode)
                            .put("message", res.message)
                            .put("packageName", res.packageName)
                            .toString()
                    )
                }

                method == "GET" && path == "/api/notifications" -> {
                    val notifs = NotificationRepository.notifications.value
                    val array = JSONArray()
                    notifs.forEach { n ->
                        array.put(JSONObject().apply {
                            put("id", n.id)
                            put("packageName", n.packageName)
                            put("appName", n.appName)
                            put("title", n.title)
                            put("text", n.text)
                            put("timestamp", n.timestamp)
                            put("category", n.category)
                        })
                    }
                    sendJson(output, array.toString())
                }

                method == "POST" && path == "/api/shell" -> {
                    val bodyStr = readBodyString(input, headers)
                    val cmd = runCatching { JSONObject(bodyStr).optString("command") }.getOrDefault(bodyStr).trim()
                    val result = DeviceController.executeShell(cmd)
                    val json = JSONObject().apply {
                        put("exitCode", result.exitCode)
                        put("stdout", result.stdout)
                        put("stderr", result.stderr)
                        put("executionTimeMs", result.executionTimeMs)
                    }
                    sendJson(output, json.toString())
                }

                method == "GET" && path == "/api/download" -> {
                    val targetPath = queryParams["path"]
                    if (targetPath.isNullOrBlank()) {
                        sendResponse(output, 400, "Bad Request", "text/plain", "Missing path".toByteArray())
                        return@withContext
                    }
                    val file = File(targetPath)
                    if (!file.exists() || file.isDirectory) {
                        sendResponse(output, 404, "Not Found", "text/plain", "File not found".toByteArray())
                        return@withContext
                    }
                    val length = file.length()
                    val header = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Length: $length\r\n" +
                        "Content-Disposition: attachment; filename=\"${file.name.replace("\"", "_")}\"\r\n" +
                        "Connection: close\r\n" +
                        "Access-Control-Allow-Origin: *\r\n\r\n"
                    output.write(header.toByteArray(StandardCharsets.UTF_8))
                    FileInputStream(file).use { fis ->
                        val buf = ByteArray(65536)
                        var read: Int
                        while (fis.read(buf).also { read = it } != -1) {
                            output.write(buf, 0, read)
                        }
                    }
                    output.flush()
                }

                method == "POST" && path == "/api/upload" -> {
                    val rawName = queryParams["name"] ?: "upload_${System.currentTimeMillis()}.bin"
                    val fileName = sanitizeFilename(rawName, "bin")
                    val targetDirPath = queryParams["dir"]
                    val targetDir = if (!targetDirPath.isNullOrBlank() && File(targetDirPath).isDirectory) {
                        File(targetDirPath)
                    } else {
                        runCatching {
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        }.getOrNull()
                            ?: runCatching { context.getExternalFilesDir(null) }.getOrNull()
                            ?: context.filesDir
                    }
                    targetDir.mkdirs()
                    val destFile = File(targetDir, fileName)

                    val length = headers["content-length"]?.toLongOrNull() ?: 0L
                    if (length > 0L) {
                        val written = streamBodyToFile(input, destFile, length)
                        val resp = JSONObject().apply {
                            put("success", written > 0L)
                            put("bytesWritten", written)
                            put("message", "Saved to: ${destFile.absolutePath}")
                            put("path", destFile.absolutePath)
                        }
                        sendJson(output, resp.toString())
                    } else {
                        sendJson(output, JSONObject().put("success", false).put("message", "Empty upload body").toString())
                    }
                }

                else -> {
                    sendResponse(output, 404, "Not Found", "text/plain", "Endpoint not found: $path".toByteArray())
                }
            }
        } catch (e: Exception) {
            CommandLogRepository.log("ERROR", "handleClient", clientIp, 500, e.message ?: "Connection exception")
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun sanitizeFilename(raw: String, fallbackExt: String): String {
        val base = File(raw).name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (base.isBlank() || base == "." || base == "..") {
            "file_${System.currentTimeMillis()}.$fallbackExt"
        } else {
            base
        }
    }

    private fun streamBodyToFile(input: InputStream, destFile: File, contentLength: Long): Long {
        return runCatching {
            FileOutputStream(destFile).use { fos ->
                val buf = ByteArray(65536)
                var remaining = contentLength
                var total = 0L
                while (remaining > 0L) {
                    val toRead = minOf(buf.size.toLong(), remaining).toInt()
                    val read = input.read(buf, 0, toRead)
                    if (read == -1) break
                    fos.write(buf, 0, read)
                    remaining -= read
                    total += read
                }
                fos.flush()
                total
            }
        }.getOrDefault(-1L)
    }

    private fun sendJson(
        output: OutputStream,
        jsonString: String,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        sendResponse(
            output,
            200,
            "OK",
            "application/json; charset=utf-8",
            jsonString.toByteArray(StandardCharsets.UTF_8),
            extraHeaders
        )
    }

    private fun readLine(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        var c: Int
        while (input.read().also { c = it } != -1) {
            if (c == '\n'.code) break
            if (c != '\r'.code) {
                out.write(c)
            }
        }
        if (out.size() == 0 && c == -1) return null
        return out.toString("UTF-8")
    }

    private fun readBodyString(input: InputStream, headers: Map<String, String>): String {
        val length = headers["content-length"]?.toIntOrNull()?.coerceAtMost(4 * 1024 * 1024) ?: return ""
        if (length <= 0) return ""
        val bytes = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(bytes, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return String(bytes, 0, totalRead, StandardCharsets.UTF_8)
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusMsg: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val header = StringBuilder()
        header.append("HTTP/1.1 $statusCode $statusMsg\r\n")
        header.append("Content-Type: $contentType\r\n")
        header.append("Content-Length: ${body.size}\r\n")
        header.append("Connection: close\r\n")
        header.append("Access-Control-Allow-Origin: *\r\n")
        header.append("Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n")
        header.append("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n")
        for ((k, v) in extraHeaders) {
            header.append("$k: $v\r\n")
        }
        header.append("\r\n")

        output.write(header.toString().toByteArray(StandardCharsets.UTF_8))
        if (body.isNotEmpty()) {
            output.write(body)
        }
        output.flush()
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (param in query.split("&")) {
            val pair = param.split("=")
            if (pair.isNotEmpty()) {
                val key = runCatching { URLDecoder.decode(pair[0], "UTF-8") }.getOrDefault(pair[0])
                val value = if (pair.size > 1) runCatching { URLDecoder.decode(pair[1], "UTF-8") }.getOrDefault(pair[1]) else ""
                map[key] = value
            }
        }
        return map
    }

    private suspend fun handleMcpRequest(
        method: String,
        path: String,
        queryParams: Map<String, String>,
        headers: Map<String, String>,
        input: InputStream,
        output: BufferedOutputStream,
        socket: Socket,
        clientIp: String
    ) {
        if (!McpManager.isMcpEnabled(context)) {
            val json = JSONObject()
                .put("jsonrpc", "2.0")
                .put("error", JSONObject().put("code", -32000).put("message", "PhoneDeck MCP server is disabled in Web UI settings."))
            sendResponse(
                output,
                503,
                "Service Unavailable",
                "application/json; charset=utf-8",
                json.toString().toByteArray(StandardCharsets.UTF_8),
                mapOf("Access-Control-Allow-Origin" to "*")
            )
            return
        }

        val isAuthorized = McpManager.verifyToken(context, headers["authorization"], queryParams["token"])
        if (!isAuthorized) {
            val json = JSONObject()
                .put("jsonrpc", "2.0")
                .put("error", JSONObject().put("code", -32001).put("message", "Unauthorized: Valid MCP Bearer token required."))
            sendResponse(
                output,
                401,
                "Unauthorized",
                "application/json; charset=utf-8",
                json.toString().toByteArray(StandardCharsets.UTF_8),
                mapOf(
                    "WWW-Authenticate" to "Bearer",
                    "Access-Control-Allow-Origin" to "*",
                    "Access-Control-Allow-Headers" to "Authorization, Content-Type, Mcp-Session-Id"
                )
            )
            CommandLogRepository.log("MCP", path, clientIp, 401, "MCP Bearer token rejected")
            return
        }

        when {
            // Modern Streamable HTTP endpoint: POST /mcp
            method == "POST" && path == "/mcp" -> {
                val bodyStr = readBodyString(input, headers)
                val responseStr = McpEngine.processJsonRpc(context, bodyStr)
                sendResponse(
                    output,
                    200,
                    "OK",
                    "application/json; charset=utf-8",
                    responseStr.toByteArray(StandardCharsets.UTF_8),
                    mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Access-Control-Allow-Headers" to "Authorization, Content-Type, Mcp-Session-Id"
                    )
                )
            }

            // Streamable HTTP info / discovery: GET /mcp
            method == "GET" && path == "/mcp" -> {
                val json = JSONObject().apply {
                    put("name", "PhoneDeck MCP Server")
                    put("status", "running")
                    put("protocolVersion", McpEngine.MCP_PROTOCOL_VERSION)
                    put("transports", JSONArray(listOf("Streamable HTTP", "Legacy SSE")))
                    put("activeToolsCount", McpEngine.getActiveTools(context).size)
                }
                sendResponse(
                    output,
                    200,
                    "OK",
                    "application/json; charset=utf-8",
                    json.toString(2).toByteArray(StandardCharsets.UTF_8),
                    mapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Access-Control-Allow-Headers" to "Authorization, Content-Type, Mcp-Session-Id"
                    )
                )
            }

            // Legacy SSE transport: GET /mcp/sse
            method == "GET" && path == "/mcp/sse" -> {
                socket.soTimeout = 0
                val sessionId = UUID.randomUUID().toString()
                val sseHeader = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/event-stream\r\n" +
                    "Cache-Control: no-cache, no-store, must-revalidate\r\n" +
                    "Pragma: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Headers: Authorization, Content-Type, Mcp-Session-Id\r\n\r\n"
                output.write(sseHeader.toByteArray(StandardCharsets.US_ASCII))

                val initEvent = "event: endpoint\r\ndata: /mcp/messages?sessionId=$sessionId\r\n\r\n"
                output.write(initEvent.toByteArray(StandardCharsets.UTF_8))
                output.flush()

                McpManager.registerSseSession(sessionId, output)
                CommandLogRepository.log("MCP", "/mcp/sse", clientIp, 200, "SSE session started: $sessionId")

                try {
                    while (currentCoroutineContext().isActive && (serverJob?.isActive == true) && !socket.isClosed) {
                        delay(15000L)
                        output.write(": keepalive\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                        output.flush()
                    }
                } finally {
                    McpManager.unregisterSseSession(sessionId)
                    CommandLogRepository.log("MCP", "/mcp/sse", clientIp, 200, "SSE session closed: $sessionId")
                }
            }

            // Legacy SSE client message endpoint: POST /mcp/messages
            method == "POST" && path == "/mcp/messages" -> {
                val bodyStr = readBodyString(input, headers)
                val responseStr = McpEngine.processJsonRpc(context, bodyStr)
                val sessionId = queryParams["sessionId"] ?: headers["mcp-session-id"]

                val sseStream = if (!sessionId.isNullOrBlank()) McpManager.getSseSession(sessionId) else null
                if (sseStream != null) {
                    val msgEvent = "event: message\r\ndata: $responseStr\r\n\r\n"
                    synchronized(sseStream) {
                        runCatching {
                            sseStream.write(msgEvent.toByteArray(StandardCharsets.UTF_8))
                            sseStream.flush()
                        }
                    }
                    sendResponse(
                        output,
                        202,
                        "Accepted",
                        "application/json; charset=utf-8",
                        "{\"accepted\":true}".toByteArray(StandardCharsets.UTF_8),
                        mapOf("Access-Control-Allow-Origin" to "*")
                    )
                } else {
                    sendResponse(
                        output,
                        200,
                        "OK",
                        "application/json; charset=utf-8",
                        responseStr.toByteArray(StandardCharsets.UTF_8),
                        mapOf("Access-Control-Allow-Origin" to "*")
                    )
                }
            }

            else -> {
                sendResponse(output, 404, "Not Found", "text/plain", "MCP endpoint not found: $path".toByteArray())
            }
        }
    }
}
