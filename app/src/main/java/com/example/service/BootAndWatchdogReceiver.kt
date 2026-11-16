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
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L

        fun scheduleRestart(context: Context, delayMs: Long = 1500L) {
            runCatching {
                val appCtx = context.applicationContext
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
                    AlarmManager.ELAPSED_REALTIME,
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

        val shouldStart = when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_WATCHDOG -> true
            else -> false
        }

        if (shouldStart) {
            if (ServerStateManager.isManuallyStopped(appCtx)) {
                cancelWatchdog(appCtx)
                KeepAliveJobService.cancelAll(appCtx)
                return
            }
            scheduleWatchdog(appCtx)
            KeepAliveJobService.scheduleAll(appCtx)

            // Do not start the backend from an ordinary receiver process. Promote it to
            // the connectedDevice foreground service so Android can manage its lifetime.
            // LOCKED_BOOT_COMPLETED runs before encrypted app storage is available; the
            // normal BOOT_COMPLETED broadcast starts the service after the user unlocks.
            if (action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
                RemoteServerService.startService(appCtx, ServerStateManager.getSavedPort(appCtx))
                CommandLogRepository.log(
                    "SYSTEM", "boot_service", "local", 200,
                    "Foreground server service requested via $action"
                )
            }
        }
    }
}
