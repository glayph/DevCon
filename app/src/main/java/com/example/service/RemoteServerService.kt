package com.example.service

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.controller.DeviceController
import com.example.controller.ScreenCastManager
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.server.HttpBridgeServer

class RemoteServerService : Service() {

    private var server: HttpBridgeServer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE_WAKELOCK = "com.example.service.ACTION_TOGGLE_WAKELOCK"
        const val ACTION_START_CAST = "com.example.service.ACTION_START_CAST"
        const val ACTION_STOP_CAST = "com.example.service.ACTION_STOP_CAST"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "phonedeck_silent_service_v2"
        private const val LEGACY_CHANNEL_ID = "phonedeck_service"

        fun startService(context: Context, port: Int = 8080) {
            val appCtx = context.applicationContext
            ServerStateManager.setManuallyStopped(appCtx, false)
            ServerStateManager.savePort(appCtx, port)
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(intent)
            } else {
                appCtx.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val appCtx = context.applicationContext
            ServerStateManager.setManuallyStopped(appCtx, true)
            BootAndWatchdogReceiver.cancelWatchdog(appCtx)
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { appCtx.startService(intent) }
        }

        fun startScreenCast(context: Context, resultCode: Int, resultData: Intent) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_START_CAST
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appCtx.startForegroundService(intent)
            } else {
                appCtx.startService(intent)
            }
        }

        fun stopScreenCast(context: Context) {
            val appCtx = context.applicationContext
            ScreenCastManager.stopCast()
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_STOP_CAST
            }
            runCatching { appCtx.startService(intent) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DeviceController.initTts(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                ScreenCastManager.stopCast()
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_WAKELOCK -> {
                val current = ServerStateManager.isWakeLockHeld.value
                if (current) {
                    releaseWakeLocks()
                    ServerStateManager.setWakeLockHeld(false)
                } else {
                    acquireWakeLocks()
                    ServerStateManager.setWakeLockHeld(true)
                }
            }
            ACTION_START_CAST -> {
                val port = ServerStateManager.getSavedPort(applicationContext)
                if (!ServerStateManager.isServerRunning.value) {
                    startServer(port, includeMediaProjection = true)
                } else {
                    promoteForeground(port, includeMediaProjection = true)
                }
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    ScreenCastManager.startCast(applicationContext, resultCode, resultData)
                }
            }
            ACTION_STOP_CAST -> {
                ScreenCastManager.stopCast()
                val port = ServerStateManager.getSavedPort(applicationContext)
                promoteForeground(port, includeMediaProjection = false)
            }
            ACTION_START, null -> {
                val port = intent?.getIntExtra(EXTRA_PORT, ServerStateManager.getSavedPort(applicationContext))
                    ?: ServerStateManager.getSavedPort(applicationContext)
                startServer(port, includeMediaProjection = ScreenCastManager.isCasting.value)
                BootAndWatchdogReceiver.scheduleWatchdog(applicationContext)
            }
        }
        return START_STICKY
    }

    private fun startServer(port: Int, includeMediaProjection: Boolean) {
        acquireWakeLocks()
        ServerStateManager.setWakeLockHeld(true)

        if (server == null || !ServerStateManager.isServerRunning.value) {
            server?.stop()
            server = HttpBridgeServer(applicationContext, port).also {
                it.start()
            }
        }

        promoteForeground(port, includeMediaProjection)
    }

    private fun promoteForeground(port: Int, includeMediaProjection: Boolean) {
        val notif = buildNotification(port)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    fgsTypes = fgsTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                if (includeMediaProjection) {
                    fgsTypes = fgsTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
                startForeground(NOTIFICATION_ID, notif, fgsTypes)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val fallbackType = if (includeMediaProjection) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    }
                    startForeground(NOTIFICATION_ID, notif, fallbackType)
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
            } catch (_: Exception) {
                runCatching { startForeground(NOTIFICATION_ID, notif) }
            }
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        releaseWakeLocks()
        ServerStateManager.setWakeLockHeld(false)
    }

    private fun acquireWakeLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "phonedeck:server_wakelock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire()
            }

            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            runCatching { nm?.deleteNotificationChannel(LEGACY_CHANNEL_ID) }
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
            }
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(port: Int): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_phonedeck)
            .setContentTitle("PhoneDeck")
            .setContentText("localhost:$port")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!ServerStateManager.isManuallyStopped(applicationContext)) {
            BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1000L)
        }
    }

    override fun onDestroy() {
        ScreenCastManager.stopCast()
        stopServer()
        if (!ServerStateManager.isManuallyStopped(applicationContext)) {
            BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1500L)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
