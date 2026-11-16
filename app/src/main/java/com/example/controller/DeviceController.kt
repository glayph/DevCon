package com.example.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ServerStateManager
import com.example.model.DeviceStatus
import com.example.model.FileItemInfo
import com.example.model.InstalledAppInfo
import com.example.model.ShellResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.TimeUnit

object DeviceController {

    private var ttsAppContext: Context? = null
    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false

    @Volatile
    private var cachedApps: List<InstalledAppInfo>? = null
    @Volatile
    private var cachedAppsAtMs: Long = 0L
    private const val APPS_CACHE_TTL_MS = 30_000L

    fun initTts(context: Context) {
        ttsAppContext = context.applicationContext
    }

    @Synchronized
    fun invalidateAppsCache() {
        cachedApps = null
        cachedAppsAtMs = 0L
    }

    fun speak(text: String): Boolean {
        if (!isTtsReady || tts == null) {
            val ctx = ttsAppContext ?: return false
            val latch = java.util.concurrent.CountDownLatch(1)
            synchronized(this) {
                if (tts == null) {
                    tts = TextToSpeech(ctx) { status ->
                        isTtsReady = (status == TextToSpeech.SUCCESS)
                        if (isTtsReady) {
                            runCatching { tts?.language = Locale.getDefault() }
                        }
                        latch.countDown()
                    }
                } else if (isTtsReady) {
                    latch.countDown()
                }
            }
            if (!isTtsReady) {
                runCatching { latch.await(2000, TimeUnit.MILLISECONDS) }
            }
        }
        if (!isTtsReady || tts == null) return false
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "phonedeck_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    fun getDeviceStatus(context: Context): DeviceStatus {
        val appContext = context.applicationContext

        // Battery
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (scale > 0) ((rawLevel / scale.toFloat()) * 100).toInt() else rawLevel
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val pluggedType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "Battery"
        }
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempRaw / 10.0f
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val healthCode = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        // Memory
        val actManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalStorageGb = (stat.blockCountLong * blockSize) / (1024f * 1024f * 1024f)
        val freeStorageGb = (stat.availableBlocksLong * blockSize) / (1024f * 1024f * 1024f)

        // Display
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        // Audio
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaCur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaPercent = (mediaCur * 100) / mediaMax

        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val ringCur = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringPercent = (ringCur * 100) / ringMax

        val alarmMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val alarmCur = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val alarmPercent = (alarmCur * 100) / alarmMax

        val isMuted = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        // Network
        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiSsid = runCatching {
            val ssid = wifiManager?.connectionInfo?.ssid?.trim('"')
            if (ssid.isNullOrEmpty() || ssid == "<unknown ssid>") "Connected" else ssid
        }.getOrDefault("Wi-Fi")
        val ip = getLocalIpAddress()

        // Clipboard preview
        val clipPreview = runCatching {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }.getOrDefault("")

        val uptimeSec = if (ServerStateManager.serverStartTime.get() > 0) {
            (System.currentTimeMillis() - ServerStateManager.serverStartTime.get()) / 1000
        } else 0L

        return DeviceStatus(
            model = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Generic",
            brand = Build.BRAND?.replaceFirstChar { it.uppercase() } ?: "",
            androidVersion = Build.VERSION.RELEASE ?: "14",
            sdkInt = Build.VERSION.SDK_INT,
            batteryLevel = batteryPct,
            isCharging = isCharging,
            pluggedType = pluggedType,
            batteryTempC = tempC,
            batteryVoltageMv = voltageMv,
            batteryHealth = health,
            totalRamMb = totalRamMb,
            availRamMb = availRamMb,
            totalStorageGb = String.format(Locale.US, "%.1f", totalStorageGb).toFloat(),
            freeStorageGb = String.format(Locale.US, "%.1f", freeStorageGb).toFloat(),
            isScreenOn = isScreenOn,
            mediaVolumePercent = mediaPercent,
            ringVolumePercent = ringPercent,
            alarmVolumePercent = alarmPercent,
            isMuted = isMuted,
            wifiSsid = wifiSsid,
            localIp = ip,
            serverPort = ServerStateManager.serverPort.value,
            isWakeLockActive = ServerStateManager.isWakeLockHeld.value,
            isTorchActive = ServerStateManager.isTorchOn.value,
            uptimeSeconds = uptimeSec,
            totalRequests = ServerStateManager.totalRequests.get(),
            clipboardPreview = if (clipPreview.length > 50) clipPreview.take(50) + "..." else clipPreview
        )
    }

    fun getLocalIpAddress(): String {
        return runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        }.getOrDefault("127.0.0.1")
    }

    fun toggleTorch(context: Context, forceState: Boolean? = null): Boolean {
        return runCatching {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            val newState = forceState ?: !ServerStateManager.isTorchOn.value
            cameraManager.setTorchMode(cameraId, newState)
            ServerStateManager.setTorchOn(newState)
            true
        }.getOrDefault(false)
    }

    fun setVolume(context: Context, stream: String, percent: Int): Boolean {
        return runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = when (stream.lowercase()) {
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                else -> AudioManager.STREAM_MUSIC
            }
            val max = audioManager.getStreamMaxVolume(streamType)
            val clamped = percent.coerceIn(0, 100)
            val index = (clamped * max) / 100
            audioManager.setStreamVolume(streamType, index, AudioManager.FLAG_SHOW_UI)
            true
        }.getOrDefault(false)
    }

    fun sendMediaKey(context: Context, key: String): Boolean {
        return runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (key.lowercase()) {
                "vol_up" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                    return true
                }
                "vol_down" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                    return true
                }
                "mute" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    }
                    return true
                }
            }
            val keyCode = when (key.lowercase()) {
                "play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
                "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
                "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                "prev", "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
                else -> return false
            }
            val now = android.os.SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
            true
        }.getOrDefault(false)
    }

    fun vibrate(context: Context, pattern: String, durationMs: Long): Boolean {
        return runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) return false

            val dur = durationMs.coerceIn(50L, 5000L)
            when (pattern.lowercase()) {
                "sos" -> {
                    val timings = longArrayOf(0, 100, 100, 100, 100, 100, 250, 300, 150, 300, 150, 300, 250, 100, 100, 100, 100, 100)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
                "pulse" -> {
                    val timings = longArrayOf(0, 200, 100, 200, 100, 200)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
                else -> {
                    vibrator.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
            true
        }.getOrDefault(false)
    }

    fun playSound(context: Context, type: String): Boolean {
        return runCatching {
            when (type.lowercase()) {
                "beep" -> {
                    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                }
                "alarm" -> {
                    val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1200)
                }
                else -> { // ringtone
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
                    ringtone.play()
                }
            }
            true
        }.getOrDefault(false)
    }

    fun setClipboard(context: Context, text: String): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PhoneDeck Desktop", text)
            clipboard.setPrimaryClip(clip)
            true
        }.getOrDefault(false)
    }

    fun getClipboard(context: Context): String {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        }.getOrDefault("")
    }

    fun openUrl(context: Context, url: String): Boolean {
        return runCatching {
            val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return runCatching {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName) ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun wakeScreen(context: Context): Boolean {
        return runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "phonedeck:wake_screen"
            )
            wakeLock.acquire(3000)
            true
        }.getOrDefault(false)
    }

    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val now = System.currentTimeMillis()
        val cached = cachedApps
        if (cached != null && now - cachedAppsAtMs < APPS_CACHE_TTL_MS) {
            return cached
        }
        return runCatching {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = mutableListOf<InstalledAppInfo>()
            val seen = mutableSetOf<String>()

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (seen.add(pkg)) {
                    val label = info.loadLabel(pm).toString()
                    val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val vName = runCatching {
                        pm.getPackageInfo(pkg, 0).versionName ?: "1.0"
                    }.getOrDefault("1.0")
                    apps.add(
                        InstalledAppInfo(
                            name = label,
                            packageName = pkg,
                            versionName = vName,
                            isSystem = isSystem
                        )
                    )
                }
            }
            apps.sortedBy { it.name.lowercase() }.also {
                cachedApps = it
                cachedAppsAtMs = System.currentTimeMillis()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun executeShell(command: String, workingDir: String? = null): ShellResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val initialDir = workingDir?.takeIf { it.isNotBlank() && File(it).isDirectory }
            ?: runCatching { Environment.getExternalStorageDirectory().absolutePath }.getOrNull()
            ?: "/sdcard"
        try {
            val marker = "__PD_PWD__:"
            val wrappedCmd = "cd '${initialDir.replace("'", "'\\''")}' 2>/dev/null || true; " +
                "$command; __pd_rc=$?; echo ''; echo \"$marker\$(pwd)\"; exit \$__pd_rc"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", wrappedCmd))
            val stdout = StringBuilder()
            val stderr = StringBuilder()

            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stdout.append(line).append("\n")
                    }
                }
            }
            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stderr.append(line).append("\n")
                    }
                }
            }

            outThread.start()
            errThread.start()

            val finished = process.waitFor(8, TimeUnit.SECONDS)
            if (!finished) {
                process.destroy()
                return@withContext ShellResult(
                    exitCode = -1,
                    stdout = stdout.toString(),
                    stderr = "Command timed out after 8 seconds",
                    executionTimeMs = System.currentTimeMillis() - start,
                    cwd = initialDir
                )
            }

            outThread.join(500)
            errThread.join(500)

            var rawOut = stdout.toString()
            var resolvedCwd = initialDir
            val markerIdx = rawOut.lastIndexOf(marker)
            if (markerIdx >= 0) {
                val afterMarker = rawOut.substring(markerIdx + marker.length).trim().lineSequence().firstOrNull().orEmpty()
                if (afterMarker.isNotBlank()) {
                    resolvedCwd = afterMarker.trim()
                }
                rawOut = rawOut.substring(0, markerIdx).trimEnd()
            }

            ShellResult(
                exitCode = process.exitValue(),
                stdout = rawOut.trimEnd(),
                stderr = stderr.toString().trimEnd(),
                executionTimeMs = System.currentTimeMillis() - start,
                cwd = resolvedCwd
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = 1,
                stdout = "",
                stderr = e.message ?: "Execution error",
                executionTimeMs = System.currentTimeMillis() - start,
                cwd = initialDir
            )
        }
    }

    fun listFiles(dirPath: String?, context: Context): List<FileItemInfo> {
        val target = if (dirPath.isNullOrBlank()) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            File(dirPath)
        }

        if (!target.exists() || !target.isDirectory) {
            // fallback to app files dir
            val fallback = context.getExternalFilesDir(null) ?: context.filesDir
            return fallback.listFiles()?.map {
                FileItemInfo(it.name, it.absolutePath, it.isDirectory, it.length(), it.lastModified())
            } ?: emptyList()
        }

        return target.listFiles()?.map {
            FileItemInfo(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                sizeBytes = if (it.isDirectory) 0L else it.length(),
                lastModified = it.lastModified()
            )
        }?.sortedWith(compareBy<FileItemInfo> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
    }

    fun postTestNotification(context: Context, title: String, message: String): Boolean {
        // Strict policy: only one-time OTP notifications may appear on the device.
        return false
    }
}
