package com.example

import android.app.Application
import android.os.Looper
import com.example.data.CommandLogRepository
import com.example.data.ServerStateManager
import com.example.service.BootAndWatchdogReceiver

class PhoneDeckApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashGuard()
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
            // Background thread exception: isolate and keep 24/7 bridge alive
            if (thread !== Looper.getMainLooper().thread) {
                if (!ServerStateManager.isManuallyStopped(applicationContext)) {
                    BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1500L)
                }
                return@setDefaultUncaughtExceptionHandler
            }

            // Main thread exception: schedule immediate restart before delegating
            if (!ServerStateManager.isManuallyStopped(applicationContext)) {
                BootAndWatchdogReceiver.scheduleRestart(applicationContext, 1500L)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
