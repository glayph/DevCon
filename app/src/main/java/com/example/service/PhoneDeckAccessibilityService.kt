package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicLong

class PhoneDeckAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var activeInstance: PhoneDeckAccessibilityService? = null

        private val autoConfirmUntilMs = AtomicLong(0L)
        private val lastClickTimeMs = AtomicLong(0L)

        private val CONFIRM_TEXTS = listOf(
            "install",
            "update",
            "uninstall",
            "ok",
            "allow",
            "done"
        )

        private val REJECT_TEXTS = listOf(
            "cancel",
            "deny",
            "don't allow",
            "decline"
        )

        fun armAutoConfirm(durationMs: Long = 25_000L) {
            autoConfirmUntilMs.set(System.currentTimeMillis() + durationMs)
        }

        fun disarmAutoConfirm() {
            autoConfirmUntilMs.set(0L)
        }

        fun isEnabled(context: Context): Boolean {
            if (activeInstance != null) return true
            return runCatching {
                val cn = ComponentName(context, PhoneDeckAccessibilityService::class.java)
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return@runCatching false
                enabledServices.contains(cn.flattenToString()) ||
                    enabledServices.contains(cn.flattenToShortString())
            }.getOrDefault(false)
        }

        private fun getDisplayBounds(svc: Context): Pair<Int, Int> {
            val wm = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val b: Rect = wm.maximumWindowMetrics.bounds
                b.width().coerceAtLeast(1) to b.height().coerceAtLeast(1)
            } else {
                val dm = svc.resources.displayMetrics
                dm.widthPixels.coerceAtLeast(1) to dm.heightPixels.coerceAtLeast(1)
            }
        }

        fun dispatchTap(xNorm: Float, yNorm: Float): Boolean {
            val svc = activeInstance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
            val (w, h) = getDisplayBounds(svc)
            val px = (xNorm.coerceIn(0.01f, 0.99f) * w)
            val py = (yNorm.coerceIn(0.01f, 0.99f) * h)
            val path = Path().apply { moveTo(px, py) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 65L)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return svc.dispatchGesture(gesture, null, null)
        }

        fun dispatchSwipe(
            x1Norm: Float,
            y1Norm: Float,
            x2Norm: Float,
            y2Norm: Float,
            durationMs: Long = 280L
        ): Boolean {
            val svc = activeInstance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
            val (w, h) = getDisplayBounds(svc)
            val px1 = (x1Norm.coerceIn(0.01f, 0.99f) * w)
            val py1 = (y1Norm.coerceIn(0.01f, 0.99f) * h)
            val px2 = (x2Norm.coerceIn(0.01f, 0.99f) * w)
            val py2 = (y2Norm.coerceIn(0.01f, 0.99f) * h)
            val dur = durationMs.coerceIn(80L, 1800L)
            val path = Path().apply {
                moveTo(px1, py1)
                lineTo(px2, py2)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0L, dur)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            return svc.dispatchGesture(gesture, null, null)
        }

        fun performNavAction(actionName: String): Boolean {
            val svc = activeInstance ?: return false
            val code = when (actionName.lowercase()) {
                "back" -> GLOBAL_ACTION_BACK
                "home" -> GLOBAL_ACTION_HOME
                "recents" -> GLOBAL_ACTION_RECENTS
                "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                "quick_settings" -> GLOBAL_ACTION_QUICK_SETTINGS
                "power" -> GLOBAL_ACTION_POWER_DIALOG
                "lock" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_LOCK_SCREEN else return false
                "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) GLOBAL_ACTION_TAKE_SCREENSHOT else return false
                else -> return false
            }
            return svc.performGlobalAction(code)
        }

        fun injectText(text: String): Boolean {
            val svc = activeInstance ?: return false
            if (text.isEmpty()) return false
            return runCatching {
                val root = svc.rootInActiveWindow ?: return@runCatching false
                val target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    ?: findFirstEditable(root)
                if (target != null) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    }
                    val setOk = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (!setOk) {
                        val cb = svc.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("PhoneDeck Input", text))
                        target.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                    } else true
                } else {
                    false
                }
            }.getOrDefault(false)
        }

        private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isEditable && node.isFocusable) return node
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findFirstEditable(child)
                if (found != null) return found
            }
            return null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        RemoteServerService.startService(
            applicationContext,
            com.example.data.ServerStateManager.getSavedPort(applicationContext)
        )
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (now > autoConfirmUntilMs.get()) return
        if (now - lastClickTimeMs.get() < 750L) return

        val pkg = event?.packageName?.toString()?.lowercase() ?: return
        val isInstallerPkg = pkg.contains("packageinstaller") || pkg == "com.android.settings"
        if (!isInstallerPkg) return

        val root = runCatching { rootInActiveWindow }.getOrNull() ?: return
        try {
            val targetNode = findConfirmButton(root)
            if (targetNode != null) {
                lastClickTimeMs.set(now)
                targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } finally {
            runCatching { root.recycle() }
        }
    }

    private fun findConfirmButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable && node.isEnabled) {
            val label = node.text?.toString()?.trim()?.lowercase()
                ?: node.contentDescription?.toString()?.trim()?.lowercase()
                ?: ""
            if (label.isNotEmpty() && REJECT_TEXTS.none { label == it }) {
                if (CONFIRM_TEXTS.any { label == it || label.startsWith("$it ") }) {
                    return node
                }
            }
            // Standard Android PackageInstaller positive button ID
            val viewId = node.viewIdResourceName?.lowercase() ?: ""
            if (viewId.endsWith("button1") && label !in REJECT_TEXTS) {
                if (label.isEmpty() || CONFIRM_TEXTS.any { label.contains(it) }) {
                    return node
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findConfirmButton(child)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        // No-op
    }
}
