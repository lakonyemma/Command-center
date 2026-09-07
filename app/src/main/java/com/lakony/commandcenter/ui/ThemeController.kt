package com.lakony.commandcenter.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lakony.commandcenter.data.AppSettingsStore

object ThemeController {
    var style by mutableStateOf(AppThemeStyle.BAUHAUS_BLUE)
        private set

    fun initialize(context: Context) {
        style = AppThemeStyle.BAUHAUS_BLUE
        AppSettingsStore(context).themeStyle = AppThemeStyle.BAUHAUS_BLUE.name
    }

    fun set(context: Context, newStyle: AppThemeStyle) {
        // The app now uses one cohesive Navy Blue design system throughout.
        style = AppThemeStyle.BAUHAUS_BLUE
        AppSettingsStore(context).themeStyle = AppThemeStyle.BAUHAUS_BLUE.name
    }
}
