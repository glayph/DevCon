package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Posts and cleans up one-time 4-digit OTP notifications on the host Android device's
 * system notification shade. Codes are never displayed inside the PhoneDeck Compose UI.
 */
object OtpNotificationHelper {

    const val CHANNEL_ID = "phonedeck_one_time_otp"
    const val NOTIFICATION_ID = 2049

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun postOneTimeOtpNotification(
        context: Context,
        userId: String,
        code: String,
        isRecovery: Boolean
    ): Boolean {
        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Verification OTP",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time verification code for desktop setup"
                setShowBadge(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            nm.createNotificationChannel(channel)
        }

        val title = if (isRecovery) "PhoneDeck Password Reset" else "PhoneDeck Verification"
        val body = "Code: $code  (@$userId)"

        val notification = NotificationCompat.Builder(appCtx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_phonedeck)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setTimeoutAfter(5L * 60L * 1000L)
            .build()

        return runCatching {
            nm.notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun cancelOtpNotification(context: Context) {
        runCatching {
            val nm = context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        }
    }
}
