package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager

class BootAndWatchdogReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WATCHDOG = "com.example.service.ACTION_WATCHDOG"
        private const val REQ_RESTART = 4001
        private const val REQ_WATCHDOG = 4002
        private const val WATCHDOG_INTERVAL_MS = 3 * 60 * 1000L

        fun scheduleRestart(context: Context, delayMs: Long = 1500L) {
            runCatching {
                val appCtx = context.applicationContext
                if (ServerStateManager.isManuallyStopped(appCtx)) return
                val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(appCtx, BootAndWatchdogReceiver::class.java).apply {
                    action = ACTION_WATCHDOG
                }
                val pi = PendingIntent.getBroadcast(
                    appCtx,
                    REQ_RESTART,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayMs,
                    pi
                )
            }
        }

        fun scheduleWatchdog(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                if (ServerStateManager.isManuallyStopped(appCtx)) return
                val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(appCtx, BootAndWatchdogReceiver::class.java).apply {
                    action = ACTION_WATCHDOG
                }
                val pi = PendingIntent.getBroadcast(
                    appCtx,
                    REQ_WATCHDOG,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                am.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS,
                    WATCHDOG_INTERVAL_MS,
                    pi
                )
            }
        }

        fun cancelWatchdog(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(appCtx, BootAndWatchdogReceiver::class.java).apply {
                    action = ACTION_WATCHDOG
                }
                listOf(REQ_RESTART, REQ_WATCHDOG).forEach { code ->
                    val pi = PendingIntent.getBroadcast(
                        appCtx,
                        code,
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pi != null) {
                        am.cancel(pi)
                        pi.cancel()
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appCtx = context.applicationContext

        if (ServerStateManager.isManuallyStopped(appCtx)) return

        val shouldStart = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_WATCHDOG -> true
            else -> false
        }

        if (shouldStart) {
            scheduleWatchdog(appCtx)
            if (!ServerStateManager.isServerRunning.value) {
                runCatching {
                    val port = ServerStateManager.getSavedPort(appCtx)
                    RemoteServerService.startService(appCtx, port)
                    CommandLogRepository.log("SYSTEM", "watchdog", "local", 200, "Service ensured via $action")
                }
            }
        }
    }
}
