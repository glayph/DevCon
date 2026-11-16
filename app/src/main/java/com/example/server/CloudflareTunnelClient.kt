package com.example.server

import android.content.Context
import android.util.Base64
import com.example.data.AuthManager
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.data.TunnelStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.currentCoroutineContext

/** Stable Cloudflare Workers relay for one Android device. */
class CloudflareTunnelClient(
    context: Context,
    private val localPortProvider: () -> Int
) {
    private val appCtx = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
    private val workerOrigin = "https://glayph.phonedeck.workers.dev"
    private val workerSocketUrl = "wss://glayph.phonedeck.workers.dev/ws/device"

    @Volatile private var started = false
    @Volatile private var socket: WebSocket? = null
    private var loopJob: Job? = null

    @Synchronized
    fun start() {
        if (started) return
        started = true
        loopJob = scope.launch { connectionLoop() }
    }

    @Synchronized
    fun stop() {
        started = false
        loopJob?.cancel()
        loopJob = null
        socket?.close(1000, "Stopped")
        socket = null
        ServerStateManager.setTunnelState(TunnelStatus.DISCONNECTED)
    }

    private suspend fun connectionLoop() {
        val deviceId = AuthManager.getOrCreateDeviceId(appCtx)
        val deviceSecret = AuthManager.getOrCreateTunnelSecret(appCtx)
        while (currentCoroutineContext().isActive && started) {
            ServerStateManager.setTunnelState(TunnelStatus.CONNECTING)
            try {
                runConnection(deviceId, deviceSecret)
            } catch (_: CancellationException) {
                break
            } catch (e: Exception) {
                CommandLogRepository.log("TUNNEL", "/cloudflare", "cloudflare", 0, e.message ?: "Connection failed")
            }
            if (currentCoroutineContext().isActive && started) {
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    error = "Cloudflare relay reconnecting"
                )
                delay(5_000L)
            }
        }
    }

    private suspend fun runConnection(deviceId: String, deviceSecret: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            val finished = CountDownLatch(1)
            val request = Request.Builder()
                .url("$workerSocketUrl?deviceId=$deviceId")
                .header("X-Device-Id", deviceId)
                .header("X-Device-Secret", deviceSecret)
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    socket = webSocket
                    ServerStateManager.setTunnelState(
                        TunnelStatus.CONNECTED,
                        publicUrl = "$workerOrigin/d/$deviceId/"
                    )
                    webSocket.send(JSONObject().apply {
                        put("type", "device_hello")
                    }.toString())
                    CommandLogRepository.log(
                        "TUNNEL", "/cloudflare", "cloudflare", 200,
                        "Connected: $workerOrigin/d/$deviceId/"
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(webSocket, text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    handleMessage(webSocket, bytes.utf8())
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket === webSocket) socket = null
                    finished.countDown()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket === webSocket) socket = null
                    finished.countDown()
                }
            }

            val created = httpClient.newWebSocket(request, listener)
            continuation.invokeOnCancellation { created.cancel() }
            scope.launch(Dispatchers.IO) {
                finished.await()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }

    private fun handleMessage(webSocket: WebSocket, raw: String) {
        val message = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (message.optString("type")) {
            "tunnel_ready" -> {
                val url = message.optString("publicUrl", "")
                if (url.isNotBlank()) {
                    ServerStateManager.setTunnelState(TunnelStatus.CONNECTED, publicUrl = url)
                }
            }
            "http_req" -> {
                scope.launch(Dispatchers.IO) {
                    processHttpRequest(webSocket, message)
                }
            }
        }
    }

    private fun processHttpRequest(webSocket: WebSocket, message: JSONObject) {
        val requestId = message.optString("id")
        if (requestId.isBlank()) return
        val method = message.optString("method", "GET").uppercase()
        val path = message.optString("path", "/").ifBlank { "/" }
        val query = message.optString("query", "")
        val url = "http://127.0.0.1:${localPortProvider()}$path" + if (query.isNotBlank()) "?$query" else ""
        val builder = Request.Builder().url(url)
        val headers = message.optJSONObject("headers")
        if (headers != null) {
            val keys = headers.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.lowercase() !in HOP_BY_HOP_HEADERS) {
                    builder.header(key, headers.optString(key))
                }
            }
        }
        val bodyEncoded = message.optString("bodyBase64", "")
        val body = if (bodyEncoded.isNotBlank()) {
            Base64.decode(bodyEncoded, Base64.DEFAULT)
        } else null
        if (method != "GET" && method != "HEAD") {
            val contentType = headers?.optString("content-type", "application/octet-stream")
                ?: "application/octet-stream"
            builder.method(method, (body ?: ByteArray(0)).toRequestBody(contentType.toMediaTypeOrNull()))
        } else {
            builder.method(method, null)
        }

        val responseJson = JSONObject().put("type", "http_res").put("id", requestId)
        try {
            httpClient.newCall(builder.build()).execute().use { response ->
                val responseHeaders = JSONObject()
                for (name in response.headers.names()) {
                    val values = response.headers.values(name)
                    responseHeaders.put(name, if (name.equals("set-cookie", true)) values.joinToString("\n") else values.last())
                }
                val responseBody = response.body?.bytes() ?: ByteArray(0)
                responseJson.put("status", response.code)
                    .put("headers", responseHeaders)
                    .put("bodyBase64", Base64.encodeToString(responseBody, Base64.NO_WRAP))
            }
        } catch (e: Exception) {
            responseJson.put("status", 502)
                .put("headers", JSONObject().put("content-type", "text/plain; charset=utf-8"))
                .put("bodyBase64", Base64.encodeToString((e.message ?: "Android bridge error").toByteArray(), Base64.NO_WRAP))
        }
        webSocket.send(responseJson.toString())
    }

    companion object {
        private val HOP_BY_HOP_HEADERS = setOf(
            "host", "content-length", "connection", "upgrade", "x-forwarded-host",
            "x-forwarded-proto", "x-real-ip", "cf-connecting-ip", "x-phonedeck-tunnel"
        )
    }
}
