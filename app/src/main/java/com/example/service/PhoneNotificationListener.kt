package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.NotificationRepository
import com.example.model.CapturedNotification

class PhoneNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        RemoteServerService.startService(
            applicationContext,
            com.example.data.ServerStateManager.getSavedPort(applicationContext)
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        // Skip our own persistent notification to avoid loop
        if (pkg == packageName) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // Never capture or mirror PhoneDeck OTP verification notifications
        val combinedLower = "$title $text".lowercase()
        if (combinedLower.contains("phonedeck verification") ||
            combinedLower.contains("phonedeck password reset") ||
            (com.example.data.AuthManager.isOtpPrivacyWindowActive() &&
                (sbn.notification?.category == android.app.Notification.CATEGORY_MESSAGE ||
                    pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms")))
        ) {
            return
        }

        val pm = packageManager
        val appName = runCatching {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrDefault(pkg)

        val captured = CapturedNotification(
            id = sbn.key ?: "${pkg}_${sbn.id}_${sbn.postTime}",
            packageName = pkg,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            category = sbn.notification?.category ?: "general"
        )

        NotificationRepository.addNotification(captured)
    }

    companion object {
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrBlank()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == context.packageName) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
