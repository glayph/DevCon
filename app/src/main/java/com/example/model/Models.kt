package com.example.model

data class DeviceStatus(
    val model: String,
    val manufacturer: String,
    val brand: String,
    val androidVersion: String,
    val sdkInt: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val pluggedType: String,
    val batteryTempC: Float,
    val batteryVoltageMv: Int,
    val batteryHealth: String,
    val totalRamMb: Long,
    val availRamMb: Long,
    val totalStorageGb: Float,
    val freeStorageGb: Float,
    val isScreenOn: Boolean,
    val mediaVolumePercent: Int,
    val ringVolumePercent: Int,
    val alarmVolumePercent: Int,
    val isMuted: Boolean,
    val wifiSsid: String,
    val localIp: String,
    val serverPort: Int,
    val isWakeLockActive: Boolean,
    val isTorchActive: Boolean,
    val uptimeSeconds: Long,
    val totalRequests: Long,
    val clipboardPreview: String
)

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val category: String = "general"
)

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val isSystem: Boolean
)

data class ServerLogEntry(
    val id: Long,
    val timestamp: Long,
    val method: String,
    val path: String,
    val clientIp: String,
    val statusCode: Int,
    val details: String
)

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTimeMs: Long,
    val cwd: String = ""
)

data class FileItemInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

data class FileBrowseResult(
    val currentPath: String,
    val parentPath: String?,
    val hasAllFilesAccess: Boolean,
    val items: List<FileItemInfo>
)

data class PermissionItemState(
    val id: String,
    val title: String,
    val webPageId: String,
    val isGranted: Boolean,
    val isSpecial: Boolean,
    val runtimePermissions: List<String> = emptyList()
)

data class ContactInfo(
    val id: String,
    val name: String,
    val phone: String,
    val email: String
)

data class SmsMessageInfo(
    val id: String,
    val address: String,
    val body: String,
    val date: Long,
    val type: String,
    val threadId: Long = 0L,
    val contactName: String = ""
)

data class CallLogInfo(
    val number: String,
    val name: String,
    val type: String,
    val date: Long,
    val durationSec: Long
)

data class PhoneNetworkInfo(
    val operatorName: String,
    val networkType: String,
    val simState: String,
    val phoneType: String
)

data class CalendarEventInfo(
    val id: Long,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val location: String,
    val description: String
)

data class MediaItemInfo(
    val id: Long,
    val name: String,
    val type: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mimeType: String
)

data class LocationInfo(
    val available: Boolean,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double,
    val speedMps: Float,
    val provider: String,
    val timestamp: Long
)

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val bondState: String
)

data class BluetoothSummary(
    val isSupported: Boolean,
    val isEnabled: Boolean,
    val adapterName: String,
    val devices: List<BluetoothDeviceInfo>
)

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalMinutes: Long,
    val lastUsedMs: Long
)
