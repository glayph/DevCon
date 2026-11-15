package com.example.controller

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.PermissionItemState
import com.example.service.PhoneNotificationListener

object PermissionHelper {

    const val ID_ALL_FILES = "all_files"
    const val ID_MEDIA = "media"
    const val ID_CONTACTS = "contacts"
    const val ID_SMS = "sms"
    const val ID_CALLS_PHONE = "calls_phone"
    const val ID_CALENDAR = "calendar"
    const val ID_LOCATION = "location"
    const val ID_CAMERA = "camera"
    const val ID_MICROPHONE = "microphone"
    const val ID_BLUETOOTH = "bluetooth"
    const val ID_NOTIFICATION_LISTENER = "notification_listener"
    const val ID_BATTERY_OPTIMIZATION = "battery_optimization"
    const val ID_OVERLAY = "overlay"
    const val ID_WRITE_SETTINGS = "write_settings"
    const val ID_USAGE_STATS = "usage_stats"
    const val ID_SCREEN_CAST = "screen_cast"
    const val ID_INSTALL_PACKAGES = "install_packages"
    const val ID_ACCESSIBILITY = "accessibility_assistant"

    fun getAllPermissionStates(context: Context): List<PermissionItemState> {
        val list = mutableListOf<PermissionItemState>()

        // 1. All Files Access
        val allFilesGranted = isAllFilesGranted(context)
        val allFilesRuntime = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else emptyList()
        list.add(
            PermissionItemState(
                id = ID_ALL_FILES,
                title = "All Files Access",
                webPageId = "files",
                isGranted = allFilesGranted,
                isSpecial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
                runtimePermissions = allFilesRuntime
            )
        )

        // 2. Media Gallery (Photos, Videos, Audio)
        val mediaPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        list.add(
            PermissionItemState(
                id = ID_MEDIA,
                title = "Photos, Video & Audio",
                webPageId = "media",
                isGranted = areAllRuntimeGranted(context, mediaPerms),
                isSpecial = false,
                runtimePermissions = mediaPerms
            )
        )

        // 3. Contacts
        val contactPerms = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
        list.add(
            PermissionItemState(
                id = ID_CONTACTS,
                title = "Contacts",
                webPageId = "contacts",
                isGranted = areAllRuntimeGranted(context, contactPerms),
                isSpecial = false,
                runtimePermissions = contactPerms
            )
        )

        // 4. SMS & Messages
        val smsPerms = listOf(Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
        list.add(
            PermissionItemState(
                id = ID_SMS,
                title = "SMS & Messages",
                webPageId = "sms",
                isGranted = areAllRuntimeGranted(context, smsPerms),
                isSpecial = false,
                runtimePermissions = smsPerms
            )
        )

        // 5. Call Logs & Phone State
        val callPerms = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )
        list.add(
            PermissionItemState(
                id = ID_CALLS_PHONE,
                title = "Call Logs & Phone",
                webPageId = "calls",
                isGranted = areAllRuntimeGranted(context, callPerms),
                isSpecial = false,
                runtimePermissions = callPerms
            )
        )

        // 6. Calendar
        val calPerms = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        list.add(
            PermissionItemState(
                id = ID_CALENDAR,
                title = "Calendar Events",
                webPageId = "calendar",
                isGranted = areAllRuntimeGranted(context, calPerms),
                isSpecial = false,
                runtimePermissions = calPerms
            )
        )

        // 7. GPS Location
        val locPerms = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        list.add(
            PermissionItemState(
                id = ID_LOCATION,
                title = "GPS Location",
                webPageId = "location",
                isGranted = areAllRuntimeGranted(context, locPerms),
                isSpecial = false,
                runtimePermissions = locPerms
            )
        )

        // 8. Camera & Torch
        val camPerms = listOf(Manifest.permission.CAMERA)
        list.add(
            PermissionItemState(
                id = ID_CAMERA,
                title = "Camera & Torch",
                webPageId = "controls",
                isGranted = areAllRuntimeGranted(context, camPerms),
                isSpecial = false,
                runtimePermissions = camPerms
            )
        )

        // 9. Microphone
        val micPerms = listOf(Manifest.permission.RECORD_AUDIO)
        list.add(
            PermissionItemState(
                id = ID_MICROPHONE,
                title = "Microphone",
                webPageId = "controls",
                isGranted = areAllRuntimeGranted(context, micPerms),
                isSpecial = false,
                runtimePermissions = micPerms
            )
        )

        // 10. Bluetooth & Nearby
        val btPerms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            emptyList()
        }
        list.add(
            PermissionItemState(
                id = ID_BLUETOOTH,
                title = "Bluetooth & Nearby",
                webPageId = "bluetooth",
                isGranted = if (btPerms.isEmpty()) true else areAllRuntimeGranted(context, btPerms),
                isSpecial = false,
                runtimePermissions = btPerms
            )
        )

        // 11. Notification Listener
        list.add(
            PermissionItemState(
                id = ID_NOTIFICATION_LISTENER,
                title = "Notification Listener",
                webPageId = "notifications",
                isGranted = PhoneNotificationListener.isNotificationServiceEnabled(context),
                isSpecial = true
            )
        )

        // 13. Battery Optimization
        val batteryGranted = runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }.getOrDefault(false)
        list.add(
            PermissionItemState(
                id = ID_BATTERY_OPTIMIZATION,
                title = "Battery Optimization",
                webPageId = "telemetry",
                isGranted = batteryGranted,
                isSpecial = true
            )
        )

        // 14. Display Over Other Apps
        val overlayGranted = runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
        list.add(
            PermissionItemState(
                id = ID_OVERLAY,
                title = "Display Over Apps",
                webPageId = "controls",
                isGranted = overlayGranted,
                isSpecial = true
            )
        )

        // 15. Modify System Settings
        val writeSettingsGranted = runCatching { Settings.System.canWrite(context) }.getOrDefault(false)
        list.add(
            PermissionItemState(
                id = ID_WRITE_SETTINGS,
                title = "Modify System Settings",
                webPageId = "controls",
                isGranted = writeSettingsGranted,
                isSpecial = true
            )
        )

        // 16. Usage Access
        list.add(
            PermissionItemState(
                id = ID_USAGE_STATS,
                title = "App Usage Access",
                webPageId = "apps",
                isGranted = isUsageStatsGranted(context),
                isSpecial = true
            )
        )

        // 17. Install Unknown Apps
        val installGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else true
        list.add(
            PermissionItemState(
                id = ID_INSTALL_PACKAGES,
                title = "Install Unknown Apps",
                webPageId = "apps",
                isGranted = installGranted,
                isSpecial = true
            )
        )

        // 18. Hands-Free Assistant (Accessibility)
        list.add(
            PermissionItemState(
                id = ID_ACCESSIBILITY,
                title = "Hands-Free Assistant",
                webPageId = "apps",
                isGranted = com.example.service.PhoneDeckAccessibilityService.isEnabled(context),
                isSpecial = true
            )
        )

        // 19. Live Screen Cast (MediaProjection)
        list.add(
            PermissionItemState(
                id = ID_SCREEN_CAST,
                title = "Live Screen Cast",
                webPageId = "screen",
                isGranted = ScreenCastManager.isCasting.value,
                isSpecial = true
            )
        )

        return list
    }

    fun isAllFilesGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        } else {
            areAllRuntimeGranted(
                context,
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }

    fun isUsageStatsGranted(context: Context): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    fun areAllRuntimeGranted(context: Context, permissions: List<String>): Boolean {
        if (permissions.isEmpty()) return true
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun buildSpecialPermissionIntent(context: Context, permissionId: String): Intent {
        return when (permissionId) {
            ID_ALL_FILES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else {
                    appDetailsIntent(context)
                }
            }
            ID_NOTIFICATION_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            ID_BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            ID_OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            ID_WRITE_SETTINGS -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            ID_USAGE_STATS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            ID_INSTALL_PACKAGES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else {
                    appDetailsIntent(context)
                }
            }
            ID_ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            ID_SCREEN_CAST -> Intent(context, com.example.service.ScreenCastPermissionActivity::class.java)
            else -> appDetailsIntent(context)
        }
    }

    fun appDetailsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
