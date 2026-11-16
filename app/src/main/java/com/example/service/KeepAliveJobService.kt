package com.example.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.example.data.CommandLogRepository

/**
 * Persisted Android JobScheduler daemon watchdog that automatically restarts the
 * PhoneDeck foreground server service after reboot, network availability, and process
 * recovery events. The foreground service owns the persistent user-visible notification.
 */
class KeepAliveJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        val appCtx = applicationContext
        if (com.example.data.ServerStateManager.isManuallyStopped(appCtx)) {
            cancelAll(appCtx)
            jobFinished(params, false)
            return false
        }
        runCatching {
            // A JobService is temporary. Do not start a long-lived socket/server in its
            // background process and then immediately finish the job. Ask the dedicated
            // foreground service to own the backend lifecycle instead.
            RemoteServerService.startService(
                appCtx,
                com.example.data.ServerStateManager.getSavedPort(appCtx)
            )
            BootAndWatchdogReceiver.scheduleWatchdog(appCtx)
            CommandLogRepository.log(
                "SYSTEM",
                "job_watchdog",
                "local",
                200,
                "Foreground server service requested (jobId=${params?.jobId ?: 0})"
            )
        }.onFailure {
            CommandLogRepository.log(
                "WARN", "job_watchdog", "local", 0,
                "Could not request foreground server service: ${it.message}"
            )
        }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    companion object {
        const val JOB_ID_PERIODIC = 7001
        const val JOB_ID_NETWORK = 7002
        const val JOB_ID_RECOVERY = 7003
        private const val PERIODIC_INTERVAL_MS = 15 * 60 * 1000L

        fun scheduleAll(context: Context) {
            val appCtx = context.applicationContext
            if (com.example.data.ServerStateManager.isManuallyStopped(appCtx)) return
            val js = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val component = ComponentName(appCtx, KeepAliveJobService::class.java)

            // 1. Reboot-persisted periodic health & keep-alive job (15 min)
            if (!isJobScheduled(js, JOB_ID_PERIODIC)) {
                val periodicBuilder = JobInfo.Builder(JOB_ID_PERIODIC, component)
                    .setPersisted(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    periodicBuilder.setPeriodic(PERIODIC_INTERVAL_MS, 5 * 60 * 1000L)
                } else {
                    periodicBuilder.setPeriodic(PERIODIC_INTERVAL_MS)
                }
                runCatching { js.schedule(periodicBuilder.build()) }
            }

            // 2. Reboot-persisted network-available wake job (reconnects tunnel after boot/Wi-Fi)
            if (!isJobScheduled(js, JOB_ID_NETWORK)) {
                val netJob = JobInfo.Builder(JOB_ID_NETWORK, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build()
                runCatching { js.schedule(netJob) }
            }
        }

        fun scheduleRecovery(context: Context, delayMs: Long = 3_000L) {
            val appCtx = context.applicationContext
            if (com.example.data.ServerStateManager.isManuallyStopped(appCtx)) return
            val js = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val component = ComponentName(appCtx, KeepAliveJobService::class.java)
            val recoveryJob = JobInfo.Builder(JOB_ID_RECOVERY, component)
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + 10_000L)
                .setPersisted(true)
                .build()
            runCatching { js.schedule(recoveryJob) }
        }


        fun cancelAll(context: Context) {
            val appCtx = context.applicationContext
            val js = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            listOf(JOB_ID_PERIODIC, JOB_ID_NETWORK, JOB_ID_RECOVERY).forEach { id ->
                runCatching { js.cancel(id) }
            }
        }

        private fun isJobScheduled(js: JobScheduler, jobId: Int): Boolean {
            return runCatching {
                js.allPendingJobs.any { it.id == jobId }
            }.getOrDefault(false)
        }
    }
}
