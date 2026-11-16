package com.example.server

import android.app.NotificationManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import com.example.data.CommandLogRepository
import com.example.data.OtpNotificationHelper
import com.example.data.ServerStateManager
import com.example.service.BootAndWatchdogReceiver

/**
 * Process-scoped server & tunnel runtime optimized for low CPU/RAM/battery footprint:
 * - No permanent 24/7 wakelocks; uses short 25s timed activity leases only during requests.
 * - Strictly enforces OTP-only notifications by canceling any stray non-OTP notifications
 *   and removing legacy foreground-service channels.
 */
object ServerRuntime {

    private val legacyBackgroundChannelIds = listOf(
        "phonedeck_service",
        "phonedeck_silent_service_v2",
        "phonedeck_alerts"
    )
    private const val LEGACY_SERVICE_NOTIFICATION_ID = 1001
    private const val DEFAULT_ACTIVITY_LEASE_MS = 25_000L

    private var server: HttpBridgeServer? = null
    private var tunnelClient: CloudflareTunnelClient? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var lastLeaseAtMs: Long = 0L

    fun cleanupNonOtpNotifications(context: Context) {
        runCatching {
            val nm = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching {
                    nm.activeNotifications.forEach { sbn ->
                        if (sbn.id != OtpNotificationHelper.NOTIFICATION_ID &&
                            sbn.id != LEGACY_SERVICE_NOTIFICATION_ID
                        ) {
                            nm.cancel(sbn.tag, sbn.id)
                            nm.cancel(sbn.id)
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                legacyBackgroundChannelIds.forEach { channelId ->
                    runCatching { nm.deleteNotificationChannel(channelId) }
                }
            }
        }
    }

    @Synchronized
    fun start(context: Context, port: Int = ServerStateManager.getSavedPort(context)) {
        val appCtx = context.applicationContext
        cleanupNonOtpNotifications(appCtx)

        ServerStateManager.setManuallyStopped(appCtx, false)
        ServerStateManager.savePort(appCtx, port)
        ServerStateManager.refreshAuthInfo(appCtx)
        acquireWakeLocks(appCtx)
        ServerStateManager.setWakeLockHeld(true)

        if (server == null || !ServerStateManager.isServerRunning.value) {
            runCatching { server?.stop() }
            server = HttpBridgeServer(appCtx, port).also {
                it.start()
            }
        }

        if (tunnelClient == null) {
            tunnelClient = CloudflareTunnelClient(appCtx) {
                ServerStateManager.serverPort.value
            }
        }
        tunnelClient?.start()
        BootAndWatchdogReceiver.scheduleWatchdog(appCtx)
        com.example.service.KeepAliveJobService.scheduleAll(appCtx)
    }

    @Synchronized
    fun ensureRunning(context: Context) {
        val appCtx = context.applicationContext
        cleanupNonOtpNotifications(appCtx)
        ServerStateManager.setManuallyStopped(appCtx, false)
        if (!ServerStateManager.isServerRunning.value) {
            start(appCtx, ServerStateManager.getSavedPort(appCtx))
        } else {
            if (tunnelClient == null) {
                tunnelClient = CloudflareTunnelClient(appCtx) {
                    ServerStateManager.serverPort.value
                }
            }
            tunnelClient?.start()
            BootAndWatchdogReceiver.scheduleWatchdog(appCtx)
            com.example.service.KeepAliveJobService.scheduleAll(appCtx)
        }
    }

    @Synchronized
    fun stop(context: Context) {
        val appCtx = context.applicationContext
        BootAndWatchdogReceiver.cancelWatchdog(appCtx)
        runCatching { tunnelClient?.stop() }
        tunnelClient = null
        runCatching { server?.stop() }
        server = null
        releaseWakeLocks()
        ServerStateManager.setWakeLockHeld(false)
        cleanupNonOtpNotifications(appCtx)
    }

    /**
     * Grants a short timed wake lease only while remote commands or transfers are active,
     * allowing Android Doze/idle sleep when nothing is happening.
     */
    fun pulseActivity(context: Context, durationMs: Long = DEFAULT_ACTIVITY_LEASE_MS) {
        val now = System.currentTimeMillis()
        if (now - lastLeaseAtMs < 8_000L) return
        lastLeaseAtMs = now
        val appCtx = context.applicationContext
        runCatching {
            val wl = wakeLock ?: (appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "phonedeck:activity_lease")
                .apply { setReferenceCounted(false) }
                .also { wakeLock = it }
            wl.acquire(durationMs.coerceIn(5_000L, 60_000L))
        }
    }

    @Synchronized
    fun toggleWakeLocks(context: Context) {
        val appCtx = context.applicationContext
        if (ServerStateManager.isWakeLockHeld.value) {
            releaseWakeLocks()
            ServerStateManager.setWakeLockHeld(false)
        } else {
            acquireWakeLocks(appCtx)
            ServerStateManager.setWakeLockHeld(true)
        }
    }

    @Synchronized
    private fun acquireWakeLocks(appCtx: Context) {
        try {
            if (wakeLock == null) {
                val powerManager = appCtx.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "phonedeck:server_wakelock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(10 * 60 * 1000L)
            }

            if (wifiLock == null) {
                val wifiManager = appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "phonedeck:wifi_lock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld != true) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            CommandLogRepository.log("WARN", "wakeLock", "local", 0, "Could not acquire lock: ${e.message}")
        }
    }

    @Synchronized
    private fun releaseWakeLocks() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        }
    }
}
