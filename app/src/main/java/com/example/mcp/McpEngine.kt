package com.example.mcp

import android.content.Context
import android.util.Base64
import com.example.controller.AppManagerController
import com.example.controller.ContentDataProvider
import com.example.controller.DeviceController
import com.example.controller.PermissionHelper
import com.example.controller.ScreenCastManager
import com.example.data.CommandLogRepository
import com.example.data.McpManager
import com.example.data.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object McpEngine {

    const val MCP_PROTOCOL_VERSION = "2024-11-05"

    data class ToolDefinition(
        val name: String,
        val description: String,
        val category: String,
        val inputSchema: JSONObject
    )

    private val allTools = listOf(
        // Category: Device & Telemetry
        ToolDefinition(
            name = "get_device_status",
            description = "Get real-time device status: battery level, temperature, charging status, RAM, storage, Wi-Fi SSID, screen state, audio volume, and uptime.",
            category = McpManager.CAT_DEVICE,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "get_permissions_status",
            description = "Check which Android permissions are granted or missing (storage, media, contacts, sms, calls, location, accessibility, screen cast, etc.).",
            category = McpManager.CAT_DEVICE,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "get_location",
            description = "Get live GPS coordinates (latitude, longitude, accuracy in meters, altitude, speed, and provider) of the device.",
            category = McpManager.CAT_DEVICE,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "get_bluetooth_info",
            description = "Get Bluetooth state, adapter name, and list of paired Bluetooth devices.",
            category = McpManager.CAT_DEVICE,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "get_app_usage_stats",
            description = "Get top 24-hour application usage statistics (app name, package ID, and minutes used).",
            category = McpManager.CAT_DEVICE,
            inputSchema = emptySchema()
        ),

        // Category: Hardware Controls
        ToolDefinition(
            name = "toggle_flashlight",
            description = "Control the phone's camera flashlight / torch. State can be 'on', 'off', or 'toggle'.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf("state" to stringProp("Desired state: 'on', 'off', or 'toggle' (default is toggle)"))
            )
        ),
        ToolDefinition(
            name = "set_volume",
            description = "Adjust audio volume percentage (0 to 100) for media, ring, alarm, or notification stream.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "stream" to stringProp("Stream type: 'media', 'ring', 'alarm', or 'notification' (default 'media')"),
                    "percent" to intProp("Volume percentage from 0 to 100")
                ),
                required = listOf("percent")
            )
        ),
        ToolDefinition(
            name = "vibrate_device",
            description = "Trigger a haptic vibration pattern on the device.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "pattern" to stringProp("Vibration pattern: 'short', 'pulse', or 'sos' (default 'short')"),
                    "duration_ms" to intProp("Duration in milliseconds (50 to 5000, default 300)")
                )
            )
        ),
        ToolDefinition(
            name = "play_sound",
            description = "Play an audible beep, notification ringtone, or alarm sound through phone speaker.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf("type" to stringProp("Alert type: 'beep', 'ring', or 'alarm' (default 'beep')"))
            )
        ),
        ToolDefinition(
            name = "speak_tts",
            description = "Speak text out loud using the Android Text-to-Speech (TTS) synthesizer on the phone.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf("text" to stringProp("The message to speak aloud")),
                required = listOf("text")
            )
        ),
        ToolDefinition(
            name = "wake_screen",
            description = "Wake up the device screen and turn display on.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "open_url",
            description = "Open a URL in the phone's default web browser or matching handler app.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf("url" to stringProp("Web URL to open (e.g. https://google.com)")),
                required = listOf("url")
            )
        ),
        ToolDefinition(
            name = "get_clipboard",
            description = "Fetch the current text copied to the phone's system clipboard.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "set_clipboard",
            description = "Write text directly into the phone's system clipboard.",
            category = McpManager.CAT_CONTROLS,
            inputSchema = schemaWithProps(
                props = mapOf("text" to stringProp("Text to copy into clipboard")),
                required = listOf("text")
            )
        ),

        // Category: Files & Media
        ToolDefinition(
            name = "list_files",
            description = "Browse files and directories on device storage. Leave path empty to browse Downloads.",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf("path" to stringProp("Directory path on phone (optional)"))
            )
        ),
        ToolDefinition(
            name = "read_text_file",
            description = "Read the content of a text or code file from phone storage (up to 2MB).",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf("path" to stringProp("Absolute file path on phone")),
                required = listOf("path")
            )
        ),
        ToolDefinition(
            name = "write_text_file",
            description = "Write or overwrite text content to a file on phone storage.",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "path" to stringProp("Absolute file path to write to"),
                    "content" to stringProp("Text content to write")
                ),
                required = listOf("path", "content")
            )
        ),
        ToolDefinition(
            name = "create_folder",
            description = "Create a new folder on phone storage.",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "parent_path" to stringProp("Parent directory path"),
                    "name" to stringProp("New directory name")
                ),
                required = listOf("parent_path", "name")
            )
        ),
        ToolDefinition(
            name = "delete_file",
            description = "Delete a file or recursively remove a folder on the phone.",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf("path" to stringProp("Path to file or folder to delete")),
                required = listOf("path")
            )
        ),
        ToolDefinition(
            name = "list_media",
            description = "List photos, videos, or audio files indexed in Android MediaStore.",
            category = McpManager.CAT_FILES,
            inputSchema = schemaWithProps(
                props = mapOf("type" to stringProp("Media type: 'images', 'videos', or 'audio' (default 'images')"))
            )
        ),

        // Category: Comms & PIM
        ToolDefinition(
            name = "list_contacts",
            description = "Read contacts from the phone's address book.",
            category = McpManager.CAT_COMMS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "add_contact",
            description = "Add a new contact to the phone's address book.",
            category = McpManager.CAT_COMMS,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "name" to stringProp("Full contact name"),
                    "phone" to stringProp("Phone number"),
                    "email" to stringProp("Email address (optional)")
                ),
                required = listOf("name", "phone")
            )
        ),
        ToolDefinition(
            name = "list_sms",
            description = "Read recent incoming and sent SMS text messages from the phone.",
            category = McpManager.CAT_COMMS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "send_sms",
            description = "Send an SMS text message directly from the phone's SIM card.",
            category = McpManager.CAT_COMMS,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "phone" to stringProp("Destination phone number"),
                    "message" to stringProp("SMS message body")
                ),
                required = listOf("phone", "message")
            )
        ),
        ToolDefinition(
            name = "list_calls",
            description = "Fetch recent call history (incoming, outgoing, missed) and cellular network status.",
            category = McpManager.CAT_COMMS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "dial_number",
            description = "Initiate a phone call to a specified number.",
            category = McpManager.CAT_COMMS,
            inputSchema = schemaWithProps(
                props = mapOf("number" to stringProp("Phone number to call")),
                required = listOf("number")
            )
        ),
        ToolDefinition(
            name = "list_calendar",
            description = "Fetch upcoming calendar events from the phone.",
            category = McpManager.CAT_COMMS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "add_calendar_event",
            description = "Schedule a calendar event on the phone.",
            category = McpManager.CAT_COMMS,
            inputSchema = schemaWithProps(
                props = mapOf(
                    "title" to stringProp("Event title"),
                    "start_ms" to intProp("Start timestamp in epoch milliseconds (optional, defaults to now+1h)"),
                    "end_ms" to intProp("End timestamp in epoch milliseconds (optional, defaults to start+1h)"),
                    "location" to stringProp("Event location (optional)"),
                    "description" to stringProp("Event description (optional)")
                ),
                required = listOf("title")
            )
        ),
        ToolDefinition(
            name = "get_notifications",
            description = "Fetch real-time notifications posted by other apps on the phone.",
            category = McpManager.CAT_COMMS,
            inputSchema = emptySchema()
        ),

        // Category: Apps & Live Screen
        ToolDefinition(
            name = "list_installed_apps",
            description = "List all applications installed on the device (app name, package ID, version, system flag).",
            category = McpManager.CAT_APPS,
            inputSchema = emptySchema()
        ),
        ToolDefinition(
            name = "launch_app",
            description = "Launch an application on the phone using its package name.",
            category = McpManager.CAT_APPS,
            inputSchema = schemaWithProps(
                props = mapOf("package_name" to stringProp("Package ID of the app (e.g. com.google.android.youtube)")),
                required = listOf("package_name")
            )
        ),
        ToolDefinition(
            name = "install_apk",
            description = "Remotely install an APK package from phone storage (using silent shell / root or PackageInstaller + Hands-Free Assistant).",
            category = McpManager.CAT_APPS,
            inputSchema = schemaWithProps(
                props = mapOf("apk_path" to stringProp("Absolute path to .apk file on device")),
                required = listOf("apk_path")
            )
        ),
        ToolDefinition(
            name = "uninstall_app",
            description = "Remotely uninstall an app by package ID (using silent shell / root or Android confirmation).",
            category = McpManager.CAT_APPS,
            inputSchema = schemaWithProps(
                props = mapOf("package_name" to stringProp("Package ID of the app to remove")),
                required = listOf("package_name")
            )
        ),
        ToolDefinition(
            name = "get_screen_snapshot",
            description = "Capture an image snapshot of the live phone screen (returns JPEG base64 and dimension metadata).",
            category = McpManager.CAT_APPS,
            inputSchema = schemaWithProps(
                props = mapOf("quality" to intProp("JPEG quality 30-95 (default 75)"))
            )
        ),

        // Category: Shell Terminal
        ToolDefinition(
            name = "execute_shell",
            description = "Execute a command in Android's sh shell environment and return exit code, stdout, and stderr.",
            category = McpManager.CAT_SHELL,
            inputSchema = schemaWithProps(
                props = mapOf("command" to stringProp("Shell command string to run")),
                required = listOf("command")
            )
        )
    )

    fun getActiveTools(context: Context): List<ToolDefinition> {
        return allTools.filter { tool ->
            McpManager.isCategoryEnabled(context, tool.category)
        }
    }

    suspend fun processJsonRpc(context: Context, payload: String): String = withContext(Dispatchers.IO) {
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            val array = runCatching { JSONArray(trimmed) }.getOrDefault(JSONArray())
            val responses = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                responses.put(handleSingleRequest(context, obj))
            }
            return@withContext responses.toString()
        } else {
            val obj = runCatching { JSONObject(trimmed) }.getOrDefault(JSONObject())
            val response = handleSingleRequest(context, obj)
            return@withContext response.toString()
        }
    }

    private suspend fun handleSingleRequest(context: Context, req: JSONObject): JSONObject {
        val id = if (req.has("id")) req.get("id") else null
        val method = req.optString("method", "")
        val params = req.optJSONObject("params") ?: JSONObject()

        // Handle notifications (no id)
        if (id == null && method.startsWith("notifications/")) {
            return JSONObject() // Acknowledgement
        }

        val res = JSONObject().put("jsonrpc", "2.0").put("id", id)

        try {
            when (method) {
                "initialize" -> {
                    val clientProtocol = params.optString("protocolVersion", MCP_PROTOCOL_VERSION)
                    val result = JSONObject().apply {
                        put("protocolVersion", clientProtocol.ifBlank { MCP_PROTOCOL_VERSION })
                        put("capabilities", JSONObject().apply {
                            put("tools", JSONObject().put("listChanged", true))
                            put("resources", JSONObject().put("listChanged", false))
                            put("logging", JSONObject())
                        })
                        put("serverInfo", JSONObject().apply {
                            put("name", "PhoneDeck MCP Server")
                            put("version", "1.0.0")
                        })
                    }
                    res.put("result", result)
                }

                "notifications/initialized", "initialized" -> {
                    res.put("result", JSONObject().put("status", "ready"))
                }

                "ping" -> {
                    res.put("result", JSONObject())
                }

                "tools/list" -> {
                    val toolsList = getActiveTools(context)
                    val toolsArray = JSONArray()
                    for (t in toolsList) {
                        toolsArray.put(JSONObject().apply {
                            put("name", t.name)
                            put("description", t.description)
                            put("inputSchema", t.inputSchema)
                        })
                    }
                    res.put("result", JSONObject().put("tools", toolsArray))
                }

                "tools/call" -> {
                    val toolName = params.optString("name", "")
                    val args = params.optJSONObject("arguments") ?: JSONObject()
                    val toolResult = executeTool(context, toolName, args)
                    res.put("result", toolResult)
                }

                "resources/list" -> {
                    val resourcesArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("uri", "device://telemetry")
                            put("name", "Device Telemetry & Status")
                            put("description", "Real-time battery, RAM, storage, network, and system uptime")
                            put("mimeType", "application/json")
                        })
                        put(JSONObject().apply {
                            put("uri", "device://logs")
                            put("name", "PhoneDeck Action Logs")
                            put("description", "Recent HTTP and MCP execution logs")
                            put("mimeType", "application/json")
                        })
                    }
                    res.put("result", JSONObject().put("resources", resourcesArray))
                }

                "resources/read" -> {
                    val uri = params.optString("uri", "")
                    val contentJson = when (uri) {
                        "device://telemetry" -> {
                            val st = DeviceController.getDeviceStatus(context)
                            JSONObject().apply {
                                put("model", st.model)
                                put("batteryLevel", st.batteryLevel)
                                put("isCharging", st.isCharging)
                                put("totalRamMb", st.totalRamMb)
                                put("freeStorageGb", st.freeStorageGb)
                                put("wifiSsid", st.wifiSsid)
                            }.toString()
                        }
                        "device://logs" -> {
                            val logs = CommandLogRepository.logs.value
                            val arr = JSONArray()
                            logs.take(20).forEach { l ->
                                arr.put(JSONObject().apply {
                                    put("time", l.timestamp)
                                    put("method", l.method)
                                    put("endpoint", l.path)
                                    put("detail", l.details)
                                })
                            }
                            arr.toString()
                        }
                        else -> "Resource not found: $uri"
                    }

                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("uri", uri)
                            put("mimeType", "application/json")
                            put("text", contentJson)
                        })
                    }
                    res.put("result", JSONObject().put("contents", contents))
                }

                else -> {
                    res.put("error", JSONObject().apply {
                        put("code", -32601)
                        put("message", "Method not found: $method")
                    })
                }
            }
        } catch (e: Exception) {
            res.put("error", JSONObject().apply {
                put("code", -32603)
                put("message", e.message ?: "Internal JSON-RPC error")
            })
        }

        return res
    }

    private suspend fun executeTool(context: Context, name: String, args: JSONObject): JSONObject {
        val tool = allTools.firstOrNull { it.name == name }
            ?: return makeTextError("Tool '$name' does not exist")

        if (!McpManager.isCategoryEnabled(context, tool.category)) {
            return makeTextError(
                "Tool '$name' is currently disabled in PhoneDeck Web UI settings under category '${tool.category}'."
            )
        }

        CommandLogRepository.log("MCP", "tools/call:$name", "agent", 200, "Arguments: $args")

        return try {
            when (name) {
                // Device
                "get_device_status" -> {
                    val st = DeviceController.getDeviceStatus(context)
                    val json = JSONObject().apply {
                        put("model", st.model)
                        put("manufacturer", st.manufacturer)
                        put("brand", st.brand)
                        put("androidVersion", st.androidVersion)
                        put("batteryLevel", st.batteryLevel)
                        put("isCharging", st.isCharging)
                        put("pluggedType", st.pluggedType)
                        put("batteryTempC", st.batteryTempC)
                        put("batteryVoltageMv", st.batteryVoltageMv)
                        put("totalRamMb", st.totalRamMb)
                        put("availRamMb", st.availRamMb)
                        put("totalStorageGb", st.totalStorageGb)
                        put("freeStorageGb", st.freeStorageGb)
                        put("isScreenOn", st.isScreenOn)
                        put("wifiSsid", st.wifiSsid)
                        put("localIp", st.localIp)
                        put("uptimeSeconds", st.uptimeSeconds)
                        put("clipboardPreview", st.clipboardPreview)
                    }
                    makeTextResult(json.toString(2))
                }

                "get_permissions_status" -> {
                    val perms = PermissionHelper.getAllPermissionStates(context)
                    val arr = JSONArray()
                    perms.forEach { p ->
                        arr.put(JSONObject().apply {
                            put("id", p.id)
                            put("title", p.title)
                            put("isGranted", p.isGranted)
                            put("isSpecial", p.isSpecial)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "get_location" -> {
                    val loc = ContentDataProvider.getLocationInfo(context)
                    val json = JSONObject().apply {
                        put("available", loc.available)
                        put("latitude", loc.latitude)
                        put("longitude", loc.longitude)
                        put("accuracyMeters", loc.accuracyMeters)
                        put("altitudeMeters", loc.altitudeMeters)
                        put("speedMps", loc.speedMps)
                        put("provider", loc.provider)
                    }
                    makeTextResult(json.toString(2))
                }

                "get_bluetooth_info" -> {
                    val bt = ContentDataProvider.getBluetoothSummary(context)
                    val arr = JSONArray()
                    bt.devices.forEach { d ->
                        arr.put(JSONObject().apply {
                            put("name", d.name)
                            put("address", d.address)
                            put("bondState", d.bondState)
                        })
                    }
                    val json = JSONObject().apply {
                        put("isSupported", bt.isSupported)
                        put("isEnabled", bt.isEnabled)
                        put("adapterName", bt.adapterName)
                        put("pairedDevices", arr)
                    }
                    makeTextResult(json.toString(2))
                }

                "get_app_usage_stats" -> {
                    val usage = ContentDataProvider.getAppUsageStats(context)
                    val arr = JSONArray()
                    usage.forEach { u ->
                        arr.put(JSONObject().apply {
                            put("appName", u.appName)
                            put("packageName", u.packageName)
                            put("totalMinutes", u.totalMinutes)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                // Controls
                "toggle_flashlight" -> {
                    val state = args.optString("state", "toggle").lowercase()
                    val force = when (state) {
                        "on" -> true
                        "off" -> false
                        else -> null
                    }
                    val ok = DeviceController.toggleTorch(context, force)
                    makeTextResult("Flashlight ${if (ok) "updated ($state)" else "failed (camera/torch permission required)"}")
                }

                "set_volume" -> {
                    val stream = args.optString("stream", "media")
                    val percent = args.optInt("percent", 50)
                    val ok = DeviceController.setVolume(context, stream, percent)
                    makeTextResult("Volume $stream set to $percent%: ${if (ok) "success" else "failed"}")
                }

                "vibrate_device" -> {
                    val pattern = args.optString("pattern", "short")
                    val durationMs = args.optLong("duration_ms", 300)
                    val ok = DeviceController.vibrate(context, pattern, durationMs)
                    makeTextResult("Vibrated pattern '$pattern' (${durationMs}ms): ${if (ok) "success" else "hardware vibrator unavailable"}")
                }

                "play_sound" -> {
                    val type = args.optString("type", "beep")
                    val ok = DeviceController.playSound(context, type)
                    makeTextResult("Played alert sound '$type': ${if (ok) "success" else "failed"}")
                }

                "speak_tts" -> {
                    val text = args.optString("text", "")
                    val ok = DeviceController.speak(text)
                    makeTextResult("Text-to-speech spoken: ${if (ok) "success" else "TTS engine not ready"}")
                }

                "wake_screen" -> {
                    val ok = DeviceController.wakeScreen(context)
                    makeTextResult("Screen wake signal dispatched: ${if (ok) "success" else "failed"}")
                }

                "open_url" -> {
                    val url = args.optString("url", "")
                    val ok = DeviceController.openUrl(context, url)
                    makeTextResult("Opened URL '$url': ${if (ok) "success" else "failed"}")
                }

                "get_clipboard" -> {
                    val clip = DeviceController.getClipboard(context)
                    makeTextResult(if (clip.isEmpty()) "(Clipboard is empty)" else clip)
                }

                "set_clipboard" -> {
                    val text = args.optString("text", "")
                    val ok = DeviceController.setClipboard(context, text)
                    makeTextResult("Clipboard set: ${if (ok) "success" else "failed"}")
                }

                // Files
                "list_files" -> {
                    val path = args.optString("path", "")
                    val res = ContentDataProvider.browseFiles(path.ifBlank { null }, context)
                    val arr = JSONArray()
                    res.items.forEach { f ->
                        arr.put(JSONObject().apply {
                            put("name", f.name)
                            put("path", f.path)
                            put("isDirectory", f.isDirectory)
                            put("sizeBytes", f.sizeBytes)
                        })
                    }
                    val json = JSONObject().apply {
                        put("currentPath", res.currentPath)
                        put("parentPath", res.parentPath ?: "")
                        put("items", arr)
                    }
                    makeTextResult(json.toString(2))
                }

                "read_text_file" -> {
                    val path = args.optString("path", "")
                    val f = File(path)
                    if (!f.exists() || !f.isFile) {
                        return makeTextError("File not found: $path")
                    }
                    if (f.length() > 2 * 1024 * 1024) {
                        return makeTextError("File too large (${f.length()} bytes). Max readable text size is 2MB.")
                    }
                    val content = f.readText()
                    makeTextResult(content)
                }

                "write_text_file" -> {
                    val path = args.optString("path", "")
                    val content = args.optString("content", "")
                    val f = File(path)
                    f.parentFile?.mkdirs()
                    f.writeText(content)
                    makeTextResult("Wrote ${content.length} characters to $path")
                }

                "create_folder" -> {
                    val parent = args.optString("parent_path", "")
                    val name = args.optString("name", "")
                    val ok = ContentDataProvider.createFolder(parent, name)
                    makeTextResult("Folder '$name' created in '$parent': ${if (ok) "success" else "failed"}")
                }

                "delete_file" -> {
                    val path = args.optString("path", "")
                    val ok = ContentDataProvider.deleteFile(path)
                    makeTextResult("Deleted '$path': ${if (ok) "success" else "failed (file missing or write-protected)"}")
                }

                "list_media" -> {
                    val type = args.optString("type", "images")
                    val items = ContentDataProvider.getMediaItems(context, type)
                    val arr = JSONArray()
                    items.take(50).forEach { m ->
                        arr.put(JSONObject().apply {
                            put("id", m.id)
                            put("name", m.name)
                            put("type", m.type)
                            put("sizeBytes", m.sizeBytes)
                            put("mimeType", m.mimeType)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                // Comms
                "list_contacts" -> {
                    val contacts = ContentDataProvider.getContacts(context)
                    val arr = JSONArray()
                    contacts.forEach { c ->
                        arr.put(JSONObject().apply {
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "add_contact" -> {
                    val name = args.optString("name", "")
                    val phone = args.optString("phone", "")
                    val email = args.optString("email", "")
                    val ok = ContentDataProvider.addContact(context, name, phone, email)
                    makeTextResult("Contact '$name' added: ${if (ok) "success" else "failed (contacts permission required)"}")
                }

                "list_sms" -> {
                    val smsList = ContentDataProvider.getSmsMessages(context)
                    val arr = JSONArray()
                    smsList.take(30).forEach { s ->
                        arr.put(JSONObject().apply {
                            put("type", s.type)
                            put("address", s.address)
                            put("body", s.body)
                            put("date", s.date)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "send_sms" -> {
                    val phone = args.optString("phone", "")
                    val msg = args.optString("message", "")
                    val ok = ContentDataProvider.sendSms(context, phone, msg)
                    makeTextResult("SMS to $phone: ${if (ok) "sent successfully" else "failed (SMS permission required)"}")
                }

                "list_calls" -> {
                    val calls = ContentDataProvider.getCallLogs(context)
                    val arr = JSONArray()
                    calls.take(30).forEach { c ->
                        arr.put(JSONObject().apply {
                            put("number", c.number)
                            put("name", c.name)
                            put("type", c.type)
                            put("durationSec", c.durationSec)
                            put("date", c.date)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "dial_number" -> {
                    val number = args.optString("number", "")
                    val ok = ContentDataProvider.makeCall(context, number)
                    makeTextResult("Dialed '$number': ${if (ok) "call initiated" else "failed (call permission required)"}")
                }

                "list_calendar" -> {
                    val events = ContentDataProvider.getCalendarEvents(context)
                    val arr = JSONArray()
                    events.forEach { e ->
                        arr.put(JSONObject().apply {
                            put("title", e.title)
                            put("startMs", e.startMs)
                            put("endMs", e.endMs)
                            put("location", e.location)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "add_calendar_event" -> {
                    val title = args.optString("title", "")
                    val now = System.currentTimeMillis()
                    val start = args.optLong("start_ms", now + 3600000L)
                    val end = args.optLong("end_ms", start + 3600000L)
                    val loc = args.optString("location", "")
                    val desc = args.optString("description", "")
                    val ok = ContentDataProvider.addCalendarEvent(context, title, start, end, loc, desc)
                    makeTextResult("Calendar event '$title': ${if (ok) "created" else "failed (calendar permission required)"}")
                }

                "get_notifications" -> {
                    val notifs = NotificationRepository.notifications.value
                    val arr = JSONArray()
                    notifs.take(25).forEach { n ->
                        arr.put(JSONObject().apply {
                            put("appName", n.appName)
                            put("packageName", n.packageName)
                            put("title", n.title)
                            put("text", n.text)
                            put("time", n.timestamp)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                // Apps
                "list_installed_apps" -> {
                    val apps = DeviceController.getInstalledApps(context)
                    val arr = JSONArray()
                    apps.forEach { a ->
                        arr.put(JSONObject().apply {
                            put("name", a.name)
                            put("packageName", a.packageName)
                            put("versionName", a.versionName)
                            put("isSystem", a.isSystem)
                        })
                    }
                    makeTextResult(arr.toString(2))
                }

                "launch_app" -> {
                    val pkg = args.optString("package_name", "")
                    val ok = DeviceController.launchApp(context, pkg)
                    makeTextResult("App '$pkg' launch: ${if (ok) "success" else "app not found or not launchable"}")
                }

                "install_apk" -> {
                    val apkPath = args.optString("apk_path", "")
                    val res = AppManagerController.installApk(context, apkPath)
                    makeTextResult("APK install result: success=${res.success}, mode=${res.mode}, message=${res.message}")
                }

                "uninstall_app" -> {
                    val pkg = args.optString("package_name", "")
                    val res = AppManagerController.uninstallApp(context, pkg)
                    makeTextResult("Uninstall result: success=${res.success}, mode=${res.mode}, message=${res.message}")
                }

                "get_screen_snapshot" -> {
                    val frame = ScreenCastManager.getLatestFrame()
                    if (frame != null && frame.isNotEmpty()) {
                        val base64 = Base64.encodeToString(frame, Base64.NO_WRAP)
                        JSONObject().apply {
                            val contentArr = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image")
                                    put("data", base64)
                                    put("mimeType", "image/jpeg")
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", "Screen snapshot captured from live stream (${frame.size} bytes).")
                                })
                            }
                            put("content", contentArr)
                            put("isError", false)
                        }
                    } else {
                        // Attempt shell screencap fallback
                        val shellResult = DeviceController.executeShell("screencap -p")
                        if (shellResult.exitCode == 0 && shellResult.stdout.isNotBlank()) {
                            makeTextResult("Live screen cast is inactive on the phone. Start live casting in the Web UI to receive binary visual frames. Shell screencap status: ok.")
                        } else {
                            makeTextResult("Live screen cast is currently inactive. You can start live casting in the PhoneDeck Web UI (Live Screen Cast tab) or use hardware control tools.")
                        }
                    }
                }

                // Shell
                "execute_shell" -> {
                    val cmd = args.optString("command", "")
                    if (cmd.isBlank()) {
                        return makeTextError("Missing 'command' argument")
                    }
                    val shell = DeviceController.executeShell(cmd)
                    val json = JSONObject().apply {
                        put("exitCode", shell.exitCode)
                        put("stdout", shell.stdout)
                        put("stderr", shell.stderr)
                        put("executionTimeMs", shell.executionTimeMs)
                    }
                    makeTextResult(json.toString(2))
                }

                else -> makeTextError("Tool implementation not found: $name")
            }
        } catch (e: Exception) {
            makeTextError("Tool execution exception: ${e.message}")
        }
    }

    private fun makeTextResult(text: String): JSONObject {
        return JSONObject().apply {
            val contentArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
            }
            put("content", contentArr)
            put("isError", false)
        }
    }

    private fun makeTextError(message: String): JSONObject {
        return JSONObject().apply {
            val contentArr = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", message)
                })
            }
            put("content", contentArr)
            put("isError", true)
        }
    }

    private fun emptySchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject())
            put("required", JSONArray())
        }
    }

    private fun schemaWithProps(props: Map<String, JSONObject>, required: List<String> = emptyList()): JSONObject {
        val properties = JSONObject()
        for ((k, v) in props) {
            properties.put(k, v)
        }
        val reqArr = JSONArray()
        required.forEach { reqArr.put(it) }
        return JSONObject().apply {
            put("type", "object")
            put("properties", properties)
            put("required", reqArr)
        }
    }

    private fun stringProp(desc: String): JSONObject {
        return JSONObject().apply {
            put("type", "string")
            put("description", desc)
        }
    }

    private fun intProp(desc: String): JSONObject {
        return JSONObject().apply {
            put("type", "integer")
            put("description", desc)
        }
    }
}
