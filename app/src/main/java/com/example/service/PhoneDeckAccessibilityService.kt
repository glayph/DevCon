package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicLong

class PhoneDeckAccessibilityService : AccessibilityService() {

    companion object {
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
