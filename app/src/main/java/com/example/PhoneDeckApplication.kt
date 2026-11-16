package com.example

import android.app.Application
import android.os.Build
import android.os.Looper
import com.example.data.CommandLogRepository
import com.example.service.BootAndWatchdogReceiver
import com.example.service.KeepAliveJobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneDeckApplication : Application() {
    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        installCrashGuard()
        startupScope.launch {
            runCatching {
                com.example.server.ServerRuntime.cleanupNonOtpNotifications(applicationContext)
                if (!Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) {
                    com.example.server.ServerRuntime.ensureRunning(applicationContext)
                    KeepAliveJobService.scheduleAll(applicationContext)
                }
            }
        }
    }

    private fun installCrashGuard() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                CommandLogRepository.log(
                    "CRASH_GUARD",
                    thread.name,
                    "local",
                    500,
                    throwable.message ?: throwable.javaClass.simpleName
                )
            }
            BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1500L)
            KeepAliveJobService.scheduleRecovery(applicationContext, 1500L)
            // Background thread exception: isolate and keep 24/7 bridge alive
            if (thread !== Looper.getMainLooper().thread) {
                return@setDefaultUncaughtExceptionHandler
            }

            // Main thread exception: delegate after scheduling restart
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
