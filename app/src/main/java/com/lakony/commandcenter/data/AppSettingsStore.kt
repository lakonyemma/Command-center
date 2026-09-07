package com.lakony.commandcenter.data

import android.content.Context

class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("command_center_settings", Context.MODE_PRIVATE)

    var themeStyle: String
        get() = prefs.getString("theme_style", "BAUHAUS_BLUE") ?: "BAUHAUS_BLUE"
        set(value) = prefs.edit().putString("theme_style", value).apply()

    var tasklyDueNotifications: Boolean
        get() = prefs.getBoolean("taskly_due_notifications", true)
        set(value) = prefs.edit().putBoolean("taskly_due_notifications", value).apply()
}
