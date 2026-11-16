package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.controller.DeviceController
import com.example.controller.ScreenCastManager
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.server.ServerRuntime

/**
 * 24/7 Non-stop Foreground Service that keeps PhoneDeck HTTP Bridge, Screen Cast,
 * WebSocket/SSE, and Device Controls running continuously in the background even when screen is off.
 */
class RemoteServerService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_RECONNECT = "com.example.service.ACTION_RECONNECT"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE_WAKELOCK = "com.example.service.ACTION_TOGGLE_WAKELOCK"
        const val ACTION_START_CAST = "com.example.service.ACTION_START_CAST"
        const val ACTION_STOP_CAST = "com.example.service.ACTION_STOP_CAST"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "phonedeck_service_channel_v3"

        fun startService(context: Context, port: Int = 8080) {
            val appCtx = context.applicationContext
            // A foreground start is a user-visible opt-in to keep the server enabled.
            ServerStateManager.setManuallyStopped(appCtx, false)
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            try {
                // Let the service own startup. Starting the backend before promoting the
                // service leaves a window where Android can kill an ordinary background process.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(intent)
                } else {
                    appCtx.startService(intent)
                }
                KeepAliveJobService.scheduleAll(appCtx)
                BootAndWatchdogReceiver.scheduleWatchdog(appCtx)
            } catch (e: Exception) {
                CommandLogRepository.log(
                    "WARN", "foreground_service", "local", 0,
                    "Could not start remote server foreground service: ${e.message}"
                )
            }
        }

        fun reconnectTunnel(context: Context, port: Int = 8080) {
            val appCtx = context.applicationContext
            ServerStateManager.setManuallyStopped(appCtx, false)
            val intent = Intent(appCtx, RemoteServerService::class.java).apply {
                action = ACTION_RECONNECT
                putExtra(EXTRA_PORT, port)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(intent)
                } else {
                    appCtx.startService(intent)
                }
            } catch (e: Exception) {
                CommandLogRepository.log(
                    "WARN", "foreground_service", "local", 0,
                    "Could not request tunnel reconnect: ${e.message}"
                )
            }
        }

        fun stopService(context: Context) {
            val appCtx = context.applicationContext
            ServerRuntime.stop(appCtx)
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
        ServerStateManager.refreshAuthInfo(applicationContext)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra(EXTRA_PORT, ServerStateManager.getSavedPort(applicationContext))
            ?: ServerStateManager.getSavedPort(applicationContext)
        val action = intent?.action

        if (action == ACTION_STOP) {
            ServerRuntime.stop(applicationContext)
            KeepAliveJobService.cancelAll(applicationContext)
            BootAndWatchdogReceiver.cancelWatchdog(applicationContext)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        // Promote immediately. Android requires a service started with
        // startForegroundService() to call startForeground() within five seconds.
        // Starting sockets or waiting on the relay must happen only after this call.
        val castResultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val castResultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val projectionRequested = ScreenCastManager.isCasting.value ||
            (action == ACTION_START_CAST && castResultCode == Activity.RESULT_OK && castResultData != null)
        try {
            promoteForeground(port, isCasting = projectionRequested)
        } catch (e: Exception) {
            CommandLogRepository.log(
                "ERROR", "foreground_service", "local", 0,
                "Android refused to promote PhoneDeck to foreground service: ${e.message}"
            )
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_TOGGLE_WAKELOCK -> {
                ServerRuntime.toggleWakeLocks(applicationContext)
                updateNotification(port, ScreenCastManager.isCasting.value)
            }
            ACTION_RECONNECT -> {
                ServerRuntime.start(applicationContext, port)
                ServerRuntime.reconnectTunnel(applicationContext)
                updateNotification(port, ScreenCastManager.isCasting.value)
            }
            ACTION_START_CAST -> {
                if (castResultCode == Activity.RESULT_OK && castResultData != null) {
                    ScreenCastManager.startCast(applicationContext, castResultCode, castResultData)
                }
                updateNotification(port, ScreenCastManager.isCasting.value)
            }
            ACTION_STOP_CAST -> {
                ScreenCastManager.stopCast()
                promoteForeground(port, isCasting = false)
                updateNotification(port, isCasting = false)
            }
            else -> {
                ServerRuntime.start(applicationContext, port)
                updateNotification(port, ScreenCastManager.isCasting.value)
            }
        }
        return START_STICKY
    }

    private fun promoteForeground(port: Int, isCasting: Boolean) {
        ensureNotificationChannel()
        val notif = buildServiceNotification(port, isCasting)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (isCasting) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
                startForeground(NOTIFICATION_ID, notif, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
        } catch (e: Exception) {
            // Do not silently fall back to a foreground type that was not declared or
            // whose permission/policy checks failed. The caller records the failure.
            throw e
        }
    }

    private fun updateNotification(port: Int, isCasting: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIFICATION_ID, buildServiceNotification(port, isCasting))
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PhoneDeck Active Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps PhoneDeck remote control server running non-stop in the background"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(port: Int, isCasting: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val localIp = ServerStateManager.serverIp.value
        val subtitle = if (isCasting) {
            "Screen cast streaming active"
        } else {
            "24/7 Server active on http://$localIp:$port"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_phonedeck)
            .setContentTitle("PhoneDeck Running Non-Stop")
            .setContentText(subtitle)
            .setSubText("Remote Active")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!ServerStateManager.isManuallyStopped(applicationContext)) {
            BootAndWatchdogReceiver.scheduleRestart(applicationContext, 500L)
            KeepAliveJobService.scheduleRecovery(applicationContext, 1000L)
        }
    }

    override fun onDestroy() {
        if (!ServerStateManager.isManuallyStopped(applicationContext)) {
            BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1000L)
            KeepAliveJobService.scheduleRecovery(applicationContext, 1500L)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
