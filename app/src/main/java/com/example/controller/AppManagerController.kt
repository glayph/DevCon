package com.example.controller

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.service.PhoneDeckAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicReference

data class AppOpResult(
    val success: Boolean,
    val mode: String,
    val message: String,
    val packageName: String = ""
)

data class AppOpState(
    val state: String = "idle",
    val message: String = "",
    val packageName: String = "",
    val timestamp: Long = 0L
)

object AppManagerController {

    private const val ACTION_INSTALL_COMMIT = "com.example.controller.ACTION_INSTALL_COMMIT"

    val lastOpStatus = AtomicReference(AppOpState())

    private fun updateOp(state: String, message: String, packageName: String = "") {
        lastOpStatus.set(
            AppOpState(
                state = state,
                message = message,
                packageName = packageName,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun installApk(context: Context, apkPath: String): AppOpResult = withContext(Dispatchers.IO) {
        val apkFile = File(apkPath)
        if (!apkFile.exists() || !apkFile.isFile) {
            val msg = "APK file not found: $apkPath"
            updateOp("error", msg)
            return@withContext AppOpResult(false, "error", msg)
        }

        updateOp("installing", "Installing ${apkFile.name}...")

        // Tier 1: Try silent shell (ADB / Root / elevated UID)
        val escaped = apkFile.absolutePath.replace("\"", "\\\"")
        val shellRes = DeviceController.executeShell("pm install -r -d \"$escaped\"")
        if (shellRes.exitCode == 0 && shellRes.stdout.contains("Success", ignoreCase = true)) {
            val msg = "Installed silently via shell: ${apkFile.name}"
            updateOp("success", msg)
            return@withContext AppOpResult(true, "silent_shell", msg)
        }

        // Try su fallback if present
        val suRes = DeviceController.executeShell("su -c 'pm install -r -d \"$escaped\"'")
        if (suRes.exitCode == 0 && suRes.stdout.contains("Success", ignoreCase = true)) {
            val msg = "Installed silently via root: ${apkFile.name}"
            updateOp("success", msg)
            return@withContext AppOpResult(true, "silent_root", msg)
        }

        // Tier 2: Android PackageInstaller session + optional Accessibility hands-free confirmation
        return@withContext runCatching {
            val appCtx = context.applicationContext
            val installer = appCtx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            session.openWrite("base.apk", 0, apkFile.length()).use { out ->
                FileInputStream(apkFile).use { input ->
                    val buf = ByteArray(65536)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                    }
                    session.fsync(out)
                }
            }

            val accessibilityActive = PhoneDeckAccessibilityService.isEnabled(appCtx)
            if (accessibilityActive) {
                PhoneDeckAccessibilityService.armAutoConfirm(30_000L)
            }

            registerInstallCallbackReceiver(appCtx)

            val callbackIntent = Intent(ACTION_INSTALL_COMMIT).apply {
                setPackage(appCtx.packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(appCtx, sessionId, callbackIntent, flags)
            session.commit(pendingIntent.intentSender)
            session.close()

            val mode = if (accessibilityActive) "hands_free_assisted" else "package_installer_prompt"
            val msg = if (accessibilityActive) {
                "Installing ${apkFile.name} (Hands-Free Assistant armed)"
            } else {
                "Install prompt opened for ${apkFile.name} (enable Hands-Free Assistant for zero-touch)"
            }
            updateOp("installing", msg)
            AppOpResult(true, mode, msg)
        }.getOrElse { err ->
            val msg = "Install failed: ${err.message ?: "unknown error"}"
            updateOp("error", msg)
            AppOpResult(false, "error", msg)
        }
    }

    suspend fun uninstallApp(context: Context, packageName: String): AppOpResult = withContext(Dispatchers.IO) {
        val pkg = packageName.trim()
        if (pkg.isEmpty()) {
            return@withContext AppOpResult(false, "error", "Missing package name")
        }
        if (pkg == context.packageName) {
            return@withContext AppOpResult(false, "error", "Cannot uninstall PhoneDeck itself", pkg)
        }

        updateOp("uninstalling", "Uninstalling $pkg...", pkg)

        // Tier 1: Try silent shell
        val shellRes = DeviceController.executeShell("pm uninstall $pkg")
        if (shellRes.exitCode == 0 && shellRes.stdout.contains("Success", ignoreCase = true)) {
            val msg = "Uninstalled silently: $pkg"
            updateOp("success", msg, pkg)
            return@withContext AppOpResult(true, "silent_shell", msg, pkg)
        }

        val suRes = DeviceController.executeShell("su -c 'pm uninstall $pkg'")
        if (suRes.exitCode == 0 && suRes.stdout.contains("Success", ignoreCase = true)) {
            val msg = "Uninstalled silently via root: $pkg"
            updateOp("success", msg, pkg)
            return@withContext AppOpResult(true, "silent_root", msg, pkg)
        }

        // Tier 2: ACTION_DELETE + optional Accessibility hands-free auto-confirm
        return@withContext runCatching {
            val appCtx = context.applicationContext
            val accessibilityActive = PhoneDeckAccessibilityService.isEnabled(appCtx)
            if (accessibilityActive) {
                PhoneDeckAccessibilityService.armAutoConfirm(25_000L)
            }

            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appCtx.startActivity(intent)

            val mode = if (accessibilityActive) "hands_free_assisted" else "uninstall_prompt"
            val msg = if (accessibilityActive) {
                "Uninstalling $pkg (Hands-Free Assistant armed)"
            } else {
                "Uninstall dialog opened for $pkg"
            }
            updateOp("uninstalling", msg, pkg)
            AppOpResult(true, mode, msg, pkg)
        }.getOrElse { err ->
            val msg = "Uninstall failed: ${err.message ?: "unknown error"}"
            updateOp("error", msg, pkg)
            AppOpResult(false, "error", msg, pkg)
        }
    }

    @Volatile
    private var receiverRegistered = false

    private fun registerInstallCallbackReceiver(appCtx: Context) {
        if (receiverRegistered) return
        synchronized(this) {
            if (receiverRegistered) return
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    val pkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME) ?: ""
                    val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                    when (status) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(Intent.EXTRA_INTENT)
                            }
                            if (confirmIntent != null) {
                                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { ctx.startActivity(confirmIntent) }
                            }
                        }
                        PackageInstaller.STATUS_SUCCESS -> {
                            PhoneDeckAccessibilityService.disarmAutoConfirm()
                            updateOp("success", "Installed successfully${if (pkg.isNotEmpty()) ": $pkg" else ""}", pkg)
                        }
                        else -> {
                            updateOp("error", if (msg.isNotBlank()) msg else "Install failed (status $status)", pkg)
                        }
                    }
                }
            }
            val filter = IntentFilter(ACTION_INSTALL_COMMIT)
            ContextCompat.registerReceiver(appCtx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
    }
}
