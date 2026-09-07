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
        style = runCatching { AppThemeStyle.valueOf(AppSettingsStore(context).themeStyle) }
            .getOrDefault(AppThemeStyle.BAUHAUS_BLUE)
    }

    fun set(context: Context, newStyle: AppThemeStyle) {
        style = newStyle
        AppSettingsStore(context).themeStyle = newStyle.name
    }
}
