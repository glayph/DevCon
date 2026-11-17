package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Controls only the launcher alias. The app activity, services, data, and process
 * remain untouched when the alias is hidden.
 */
object LauncherVisibilityManager {
    private const val LAUNCHER_ALIAS = "com.example.MainActivityLauncher"

    private fun alias(context: Context): ComponentName =
        ComponentName(context.applicationContext.packageName, LAUNCHER_ALIAS)

    fun isVisible(context: Context): Boolean {
        val state = context.packageManager.getComponentEnabledSetting(alias(context))
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setVisible(context: Context, visible: Boolean): Boolean {
        return runCatching {
            context.packageManager.setComponentEnabledSetting(
                alias(context),
                if (visible) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP
            )
            isVisible(context) == visible
        }.getOrDefault(false)
    }

    fun toggle(context: Context): Boolean = setVisible(context, !isVisible(context))
}
