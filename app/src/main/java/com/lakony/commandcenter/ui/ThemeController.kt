package com.lakony.commandcenter.ui

import android.content.Context
import com.lakony.commandcenter.data.AppSettingsStore

object ThemeController {
    fun initialize(context: Context) {
        AppSettingsStore(context).themeStyle = AppThemeStyle.BAUHAUS_BLUE.name
    }
}
