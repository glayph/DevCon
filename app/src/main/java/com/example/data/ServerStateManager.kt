package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

enum class TunnelStatus {
    UNCONFIGURED,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

object ServerStateManager {
    private const val PREFS_NAME = "phonedeck_prefs"
    private const val KEY_MANUALLY_STOPPED = "user_stopped_manually"
    private const val KEY_SERVER_PORT = "server_port"

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _isWakeLockHeld = MutableStateFlow(true)
    val isWakeLockHeld: StateFlow<Boolean> = _isWakeLockHeld.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    // Remote Access Tunnel & Device Identity State
    private val _tunnelStatus = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val tunnelStatus: StateFlow<TunnelStatus> = _tunnelStatus.asStateFlow()

    private val _publicUrl = MutableStateFlow("")
    val publicUrl: StateFlow<String> = _publicUrl.asStateFlow()

    private val _tunnelError = MutableStateFlow("")
    val tunnelError: StateFlow<String> = _tunnelError.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _registeredUserId = MutableStateFlow("")
    val registeredUserId: StateFlow<String> = _registeredUserId.asStateFlow()

    val totalRequests = AtomicLong(0)
    val serverStartTime = AtomicLong(0)

    fun refreshAuthInfo(context: Context) {
        val appCtx = context.applicationContext
        _deviceId.value = AuthManager.getOrCreateDeviceId(appCtx)
        _isRegistered.value = AuthManager.isRegistered(appCtx)
        _registeredUserId.value = AuthManager.getRegisteredUserId(appCtx)
    }

    fun setTunnelState(
        status: TunnelStatus,
        publicUrl: String = "",
        error: String = ""
    ) {
        _tunnelStatus.value = status
        // Never expose a public URL unless the tunnel is actually connected
        _publicUrl.value = if (status == TunnelStatus.CONNECTED) publicUrl else ""
        _tunnelError.value = error
    }

    fun setServerRunning(running: Boolean, port: Int = 8080, ip: String = "127.0.0.1") {
        _isServerRunning.value = running
        _serverPort.value = port
        _serverIp.value = ip
        if (running && serverStartTime.get() == 0L) {
            serverStartTime.set(System.currentTimeMillis())
        } else if (!running) {
            serverStartTime.set(0L)
        }
    }

    fun setManuallyStopped(context: Context, @Suppress("UNUSED_PARAMETER") stopped: Boolean) {
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MANUALLY_STOPPED, false)
                .apply()
        }
    }

    fun isManuallyStopped(context: Context): Boolean {
        return false
    }

    fun savePort(context: Context, port: Int) {
        _serverPort.value = port
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SERVER_PORT, port)
                .apply()
        }
    }

    fun getSavedPort(context: Context): Int {
        return runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SERVER_PORT, 8080)
        }.getOrDefault(8080)
    }

    fun setPort(port: Int) {
        _serverPort.value = port
    }

    fun setIp(ip: String) {
        _serverIp.value = ip
    }

    fun setWakeLockHeld(held: Boolean) {
        _isWakeLockHeld.value = held
    }

    fun setTorchOn(on: Boolean) {
        _isTorchOn.value = on
    }

    fun incrementRequests() {
        totalRequests.incrementAndGet()
    }
}
