package com.lakony.commandcenter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepBlue = Color(0xFF071B33)
val PrimaryBlue = Color(0xFF0D5CBD)
val BrightBlue = Color(0xFF42A5F5)
val SoftBlue = Color(0xFFEAF3FF)
val SurfaceBlue = Color(0xFFF6FAFF)
val MutedText = Color(0xFF5B6B7E)

private val LakonyColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = BrightBlue,
    background = SurfaceBlue,
    surface = Color.White,
    primaryContainer = SoftBlue,
    onPrimary = Color.White,
    onBackground = DeepBlue,
    onSurface = DeepBlue
)

@Composable
fun LakonyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LakonyColors, content = content)
}
