package com.example.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.data.TunnelStatus
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pure-JVM outbound SSH reverse tunnel client (via JSch) for zero-account anonymous HTTPS
 * exposure across any Wi-Fi or mobile network without Cloudflare setup.
 *
 * Connects outbound over port 443 to Pinggy (`free.pinggy.io`), with automatic fallback to
 * localhost.run (`nokey@localhost.run`), parses the live assigned public HTTPS URL, and
 * monitors network changes + session lifetime for automatic reconnection.
 */
class RemoteTunnelClient(
    private val context: Context,
    private val localPortProvider: () -> Int
) {
    private val appCtx = context.applicationContext
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isStarted = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val lastReconnectAtMs = AtomicLong(0L)

    @Volatile
    private var activeSession: Session? = null

    @Volatile
    private var activeChannel: ChannelShell? = null

    @Volatile
    private var tunnelJob: Job? = null

    @Volatile
    private var networkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isStarted.get() && ServerStateManager.tunnelStatus.value != TunnelStatus.CONNECTED) {
                reconnectImmediately("Network connected")
            }
        }

        override fun onLost(network: Network) {
            if (isStarted.get()) {
                val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val hasActive = cm?.activeNetwork != null
                closeActiveSsh()
                if (!hasActive) {
                    ServerStateManager.setTunnelState(
                        TunnelStatus.RECONNECTING,
                        error = "Waiting for network"
                    )
                } else {
                    reconnectImmediately("Network changed")
                }
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (isStarted.get() && ServerStateManager.tunnelStatus.value != TunnelStatus.CONNECTED) {
                reconnectImmediately("Network ready")
            }
        }
    }

    @Synchronized
    fun start() {
        ServerStateManager.refreshAuthInfo(appCtx)
        if (isStarted.getAndSet(true)) {
            if (tunnelJob?.isActive != true) {
                reconnectAttempts.set(0)
                launchTunnelLoop(0L)
            }
            return
        }

        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }
        registerNetworkMonitor()
        reconnectAttempts.set(0)
        launchTunnelLoop(0L)
    }

    @Synchronized
    fun stop() {
        isStarted.set(false)
        tunnelJob?.cancel()
        tunnelJob = null
        unregisterNetworkMonitor()
        closeActiveSsh()
        runCatching { scope.cancel() }
        ServerStateManager.setTunnelState(TunnelStatus.DISCONNECTED)
    }

    private fun reconnectImmediately(reason: String) {
        if (!isStarted.get()) return
        val now = System.currentTimeMillis()
        while (true) {
            val previous = lastReconnectAtMs.get()
            if (now - previous < 3_000L) return
            if (lastReconnectAtMs.compareAndSet(previous, now)) break
        }
        reconnectAttempts.set(0)
        closeActiveSsh()
        launchTunnelLoop(350L)
        CommandLogRepository.log("TUNNEL", "/ssh", "tunnel", 100, reason)
    }

    private fun closeActiveSsh() {
        val channel = activeChannel
        val session = activeSession
        activeChannel = null
        activeSession = null
        runCatching { channel?.disconnect() }
        runCatching { session?.disconnect() }
    }

    private fun launchTunnelLoop(initialDelayMs: Long) {
        tunnelJob?.cancel()
        tunnelJob = scope.launch {
            if (initialDelayMs > 0) delay(initialDelayMs)
            while (isActive && isStarted.get()) {
                val nextStatus = if (reconnectAttempts.get() == 0) {
                    TunnelStatus.CONNECTING
                } else {
                    TunnelStatus.RECONNECTING
                }
                ServerStateManager.setTunnelState(nextStatus)

                val err = runCatching {
                    var ok = connectProvider(Provider.PINGGY_443)
                    if (!ok && isActive && isStarted.get()) {
                        ok = connectProvider(Provider.PINGGY_22)
                    }
                    if (!ok && isActive && isStarted.get()) {
                        ok = connectProvider(Provider.LOCALHOST_RUN)
                    }
                    if (!ok && isActive && isStarted.get()) {
                        ok = connectProvider(Provider.SERVEO)
                    }
                    if (ok) "" else "Tunnel standby (Localhost & LAN active)"
                }.getOrElse { ex ->
                    ex.message ?: "Tunnel standby (Localhost & LAN active)"
                }

                if (!isActive || !isStarted.get()) break
                closeActiveSsh()

                val attempt = reconnectAttempts.incrementAndGet()
                val backoffSec = minOf(60L, 5L * attempt)
                ServerStateManager.setTunnelState(
                    TunnelStatus.RECONNECTING,
                    publicUrl = "",
                    error = if (err.isNotBlank()) err else "Localhost & LAN 100% active"
                )
                delay(backoffSec * 1000L)
            }
        }
    }

    private enum class Provider(
        val label: String,
        val host: String,
        val port: Int,
        val user: String,
        val remoteBindPort: Int,
        val needsEphemeralKey: Boolean
    ) {
        PINGGY_443("pinggy-443", "free.pinggy.io", 443, "qr", 0, false),
        PINGGY_22("pinggy-22", "a.pinggy.io", 22, "qr", 0, false),
        LOCALHOST_RUN("localhost.run", "nokey@localhost.run", 22, "nokey", 80, true),
        SERVEO("serveo", "serveo.net", 22, "phonedeck", 80, true)
    }

    private fun connectProvider(provider: Provider): Boolean {
        val host = provider.host.substringAfter("@")
        val localPort = localPortProvider()

        val jsch = JSch()
        if (provider.needsEphemeralKey) {
            runCatching {
                val kp = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048)
                val prvOut = ByteArrayOutputStream()
                val pubOut = ByteArrayOutputStream()
                kp.writePrivateKey(prvOut)
                kp.writePublicKey(pubOut, "phonedeck")
                kp.dispose()
                jsch.addIdentity("phonedeck-ephemeral", prvOut.toByteArray(), pubOut.toByteArray(), null)
            }
        }

        val config = Properties().apply {
            put("StrictHostKeyChecking", "no")
            put(
                "PreferredAuthentications",
                if (provider.needsEphemeralKey) "publickey,keyboard-interactive,password,none"
                else "keyboard-interactive,password,none"
            )
        }

        val session = jsch.getSession(provider.user, host, provider.port).apply {
            setConfig(config)
            setPassword("")
            serverAliveInterval = 30_000
            serverAliveCountMax = 3
            timeout = 45_000
        }

        activeSession = session
        session.connect(15_000)

        if (!isStarted.get()) {
            session.disconnect()
            return false
        }

        session.setPortForwardingR(provider.remoteBindPort, "127.0.0.1", localPort)

        val channel = (session.openChannel("shell") as ChannelShell).apply {
            setPty(false)
        }
        activeChannel = channel
        val inputStream = channel.inputStream
        channel.connect(10_000)

        var publishedUrl = false
        val startMs = System.currentTimeMillis()
        val buffer = ByteArray(2048)
        val accumulatedText = StringBuilder()

        // Blocking read suspends thread cleanly in kernel I/O with 0% CPU until packets arrive
        while (isStarted.get() && session.isConnected && !channel.isClosed) {
            if (!publishedUrl && System.currentTimeMillis() - startMs > 15_000L) {
                return false
            }

            val bytesRead = try {
                inputStream.read(buffer)
            } catch (_: java.net.SocketTimeoutException) {
                if (!publishedUrl && System.currentTimeMillis() - startMs > 15_000L) {
                    return false
                }
                continue
            } catch (_: Exception) {
                -1
            }

            if (bytesRead == -1) {
                break
            }
            if (bytesRead > 0) {
                val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                accumulatedText.append(chunk)

                if (!publishedUrl) {
                    val foundUrl = extractPublicHttpsUrl(accumulatedText.toString())
                    if (foundUrl != null) {
                        publishedUrl = true
                        reconnectAttempts.set(0)
                        ServerStateManager.setTunnelState(
                            TunnelStatus.CONNECTED,
                            publicUrl = foundUrl,
                            error = ""
                        )
                        CommandLogRepository.log(
                            "TUNNEL",
                            "/ssh/${provider.label}",
                            provider.label,
                            200,
                            "Active: $foundUrl"
                        )
                    }
                }

                if (accumulatedText.length > 8192) {
                    accumulatedText.delete(0, 4096)
                }
            }
        }
        return publishedUrl
    }

    private fun registerNetworkMonitor() {
        if (networkCallbackRegistered) return
        runCatching {
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            cm.registerDefaultNetworkCallback(networkCallback)
            networkCallbackRegistered = true
        }
    }

    private fun unregisterNetworkMonitor() {
        if (!networkCallbackRegistered) return
        runCatching {
            val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            cm.unregisterNetworkCallback(networkCallback)
        }
        networkCallbackRegistered = false
    }

    companion object {
        private val ansiRegex = Regex("\u001B(?:\\[[0-9;?]*[ -/]*[@-~]|\\][^\u0007\u001B]*(?:\u0007|\u001B\\\\))")
        private val urlRegex = Regex("https://[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)+(?::\\d+)?(?:/[^\\s\"'<>]*)?")
        private val allowedTunnelHosts = listOf(
            ".a.pinggy.link",
            ".pinggy.link",
            ".pinggy-free.link",
            ".pinggy.net",
            ".lhr.life",
            ".lhr.rocks",
            ".localhost.run",
            ".serveo.net"
        )

        fun extractPublicHttpsUrl(rawOutput: String): String? {
            val clean = ansiRegex.replace(rawOutput, "")
            val matches = urlRegex.findAll(clean)
            for (m in matches) {
                val rawUrl = m.value.trimEnd('.', ',', ';', ')', ']')
                val host = rawUrl.removePrefix("https://").substringBefore("/").substringBefore(":").lowercase()
                if (host.startsWith("dashboard.") || host.startsWith("admin.") || host.startsWith("docs.")) {
                    continue
                }
                if (allowedTunnelHosts.any { suffix -> host.endsWith(suffix) && host.length > suffix.length }) {
                    return if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
                }
            }
            return null
        }
    }
}
