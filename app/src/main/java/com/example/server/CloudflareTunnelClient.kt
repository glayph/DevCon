package com.example.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.util.Base64
import com.example.data.AuthManager
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.data.TunnelStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.currentCoroutineContext
import kotlin.random.Random

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
        // Keeps the WebSocket path alive and detects dead network paths. Cloudflare's
        // Durable Object runtime automatically answers protocol-level ping frames.
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val workerSocketUrl = CloudflareRelayConfig.DEVICE_SOCKET_URL

    @Volatile private var started = false
    @Volatile private var socket: WebSocket? = null
    @Volatile private var defaultNetwork: Network? = null
    @Volatile private var networkValidated = false
    @Volatile private var networkCallbackRegistered = false
    private var loopJob: Job? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val previous = defaultNetwork
            defaultNetwork = network
            if (previous != network && started) {
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    error = "Network changed; reconnecting relay"
                )
                restartConnectionLoopImmediately()
            }
        }

        override fun onLost(network: Network) {
            // Ignore a late onLost() for the previous network after a replacement is active.
            if (defaultNetwork == network) {
                defaultNetwork = null
                networkValidated = false
                if (started) {
                    ServerStateManager.setTunnelState(
                        TunnelStatus.RECONNECTING,
                        error = "Waiting for network"
                    )
                    socket?.cancel()
                }
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (defaultNetwork != network) return
            val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val wasValidated = networkValidated
            networkValidated = validated

            if (started && !validated && wasValidated) {
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    error = "Internet connection lost; waiting to reconnect"
                )
                socket?.cancel()
            } else if (started && validated && !wasValidated) {
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    error = "Internet restored; reconnecting relay"
                )
                // Interrupt backoff even if the last failed attempt left no socket.
                restartConnectionLoopImmediately()
            }
        }
    }

    @Synchronized
    fun start() {
        if (started) return
        started = true
        registerNetworkMonitor()
        loopJob = scope.launch { connectionLoop() }
    }

    @Synchronized
    fun reconnect() {
        if (!started) {
            start()
            return
        }
        ServerStateManager.setTunnelState(
            TunnelStatus.RECONNECTING,
            error = "Manual reconnect requested"
        )
        socket?.cancel()
        loopJob?.cancel()
        loopJob = scope.launch { connectionLoop() }
    }

    @Synchronized
    fun stop() {
        started = false
        unregisterNetworkMonitor()
        loopJob?.cancel()
        loopJob = null
        socket?.close(1000, "Stopped")
        socket?.cancel()
        socket = null
        ServerStateManager.setTunnelState(TunnelStatus.DISCONNECTED)
    }

    private suspend fun connectionLoop() {
        val deviceId = AuthManager.getOrCreateDeviceId(appCtx)
        val deviceSecret = AuthManager.getOrCreateTunnelSecret(appCtx)

        var retryDelayMs = RECONNECT_INITIAL_DELAY_MS
        while (currentCoroutineContext().isActive && started) {
            val attemptStartedAt = SystemClock.elapsedRealtime()
            ServerStateManager.setTunnelState(TunnelStatus.CONNECTING)
            try {
                runConnection(deviceId, deviceSecret)
            } catch (_: CancellationException) {
                break
            } catch (e: Exception) {
                CommandLogRepository.log(
                    "TUNNEL",
                    "/cloudflare",
                    "cloudflare",
                    0,
                    e.message ?: "Connection failed"
                )
            }

            // A connection that stayed up for a minute was healthy. Retry it quickly
            // after a later drop; repeated failed handshakes back off to reduce battery
            // and radio use while the device has no usable Internet connection.
            if (SystemClock.elapsedRealtime() - attemptStartedAt >= RECONNECT_STABLE_SESSION_MS) {
                retryDelayMs = RECONNECT_INITIAL_DELAY_MS
            }

            if (currentCoroutineContext().isActive && started) {
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    error = "Cloudflare relay reconnecting"
                )
                delay(retryDelayMs + Random.nextLong(RECONNECT_JITTER_MS + 1))
                retryDelayMs = (retryDelayMs * 2L).coerceAtMost(RECONNECT_MAX_DELAY_MS)
            }
        }
    }

    private suspend fun runConnection(deviceId: String, deviceSecret: String) =
        suspendCancellableCoroutine<Unit> { continuation ->
            val relayReady = CompletableDeferred<Unit>()
            val ended = CompletableDeferred<Unit>()

            val request = Request.Builder()
                .url("$workerSocketUrl?deviceId=$deviceId")
                .header("X-Device-Id", deviceId)
                .header("X-Device-Secret", deviceSecret)
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    socket = webSocket
                    // A successful HTTP 101 only confirms the WebSocket handshake. The
                    // public tunnel is marked ONLINE only after the Worker acknowledges it.
                    val sent = webSocket.send(JSONObject().apply {
                        put("type", "device_hello")
                    }.toString())
                    if (!sent) {
                        relayReady.completeExceptionally(IOException("Could not announce device to relay"))
                        webSocket.cancel()
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleMessage(webSocket, text, relayReady)
                }

                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    handleMessage(webSocket, bytes.utf8(), relayReady)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (socket === webSocket) socket = null
                    if (!relayReady.isCompleted) {
                        relayReady.completeExceptionally(
                            IOException("Relay closed before becoming ready ($code): $reason")
                        )
                    }
                    ended.complete(Unit)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (socket === webSocket) socket = null
                    if (!relayReady.isCompleted) relayReady.completeExceptionally(t)
                    ended.complete(Unit)
                }
            }

            val created = httpClient.newWebSocket(request, listener)
            socket = created
            val waiter = scope.launch(Dispatchers.IO) {
                try {
                    val acknowledged = withTimeoutOrNull(TUNNEL_HANDSHAKE_TIMEOUT_MS) {
                        relayReady.await()
                        true
                    } ?: false
                    if (!acknowledged) {
                        throw IOException("Relay did not acknowledge the device tunnel in time")
                    }
                    ended.await()
                    if (continuation.isActive) continuation.resume(Unit)
                } catch (e: Exception) {
                    created.cancel()
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }

            continuation.invokeOnCancellation {
                waiter.cancel()
                created.cancel()
                if (socket === created) socket = null
            }
        }

    private fun handleMessage(
        webSocket: WebSocket,
        raw: String,
        relayReady: CompletableDeferred<Unit>
    ) {
        val message = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (message.optString("type")) {
            "tunnel_ready" -> {
                val url = message.optString("publicUrl", "").ifBlank {
                    val deviceId = AuthManager.getOrCreateDeviceId(appCtx)
                    CloudflareRelayConfig.publicUrl(deviceId)
                }
                ServerStateManager.setTunnelState(TunnelStatus.CONNECTED, publicUrl = url)
                relayReady.complete(Unit)
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
        val url = "http://127.0.0.1:${localPortProvider()}$path" +
            if (query.isNotBlank()) "?$query" else ""
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
                    responseHeaders.put(
                        name,
                        if (name.equals("set-cookie", true)) values.joinToString("\n") else values.last()
                    )
                }
                val responseBody = response.body?.bytes() ?: ByteArray(0)
                responseJson.put("status", response.code)
                    .put("headers", responseHeaders)
                    .put("bodyBase64", Base64.encodeToString(responseBody, Base64.NO_WRAP))
            }
        } catch (e: Exception) {
            responseJson.put("status", 502)
                .put("headers", JSONObject().put("content-type", "text/plain; charset=utf-8"))
                .put(
                    "bodyBase64",
                    Base64.encodeToString(
                        (e.message ?: "Android bridge error").toByteArray(),
                        Base64.NO_WRAP
                    )
                )
        }

        if (!webSocket.send(responseJson.toString())) {
            CommandLogRepository.log(
                "TUNNEL", "/cloudflare/$requestId", "cloudflare", 502,
                "Could not send local response because the relay socket is closed"
            )
        }
    }

    @Synchronized
    private fun restartConnectionLoopImmediately() {
        if (!started) return
        socket?.cancel()
        loopJob?.cancel()
        loopJob = scope.launch { connectionLoop() }
    }

    private fun registerNetworkMonitor() {
        if (networkCallbackRegistered) return
        runCatching {
            val manager = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            defaultNetwork = manager.activeNetwork
            val active = defaultNetwork
            networkValidated = active?.let { network ->
                manager.getNetworkCapabilities(network)?.let { caps ->
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            } ?: false
            manager.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        }.onFailure {
            CommandLogRepository.log(
                "TUNNEL", "/cloudflare/network", "android", 0,
                "Network monitor unavailable: ${it.message}"
            )
        }
    }

    private fun unregisterNetworkMonitor() {
        if (!networkCallbackRegistered) return
        runCatching {
            val manager = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            manager.unregisterNetworkCallback(networkCallback)
        }
        networkCallbackRegistered = false
        defaultNetwork = null
        networkValidated = false
    }

    companion object {
        private const val RECONNECT_INITIAL_DELAY_MS = 2_000L
        private const val RECONNECT_MAX_DELAY_MS = 30_000L
        private const val RECONNECT_STABLE_SESSION_MS = 60_000L
        private const val RECONNECT_JITTER_MS = 1_000L
        private const val TUNNEL_HANDSHAKE_TIMEOUT_MS = 20_000L

        private val HOP_BY_HOP_HEADERS = setOf(
            "host", "content-length", "connection", "upgrade", "x-forwarded-host",
            "x-forwarded-proto", "x-real-ip", "cf-connecting-ip", "x-phonedeck-tunnel"
        )
    }
}
