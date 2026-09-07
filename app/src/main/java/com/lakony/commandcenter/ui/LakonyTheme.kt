package com.lakony.commandcenter.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Keep these legacy enum identifiers stable: ThemeController historically persisted AppThemeStyle.name.
// All legacy values now resolve to the same Navy Blue design so existing installs migrate visually
// without breaking stored preferences.
enum class AppThemeStyle(val label: String) {
    BAUHAUS_BLUE("Navy Blue"),
    SWISS_RED("Navy Blue"),
    GRAPHITE("Navy Blue"),
    FOREST("Navy Blue"),
}

private val NavyBlue = lightColorScheme(
    primary = Color(0xFF0B2A4A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E5F3),
    onPrimaryContainer = Color(0xFF071E35),
    secondary = Color(0xFF315F89),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1EBF5),
    onSecondaryContainer = Color(0xFF183A59),
    tertiary = Color(0xFF4D7397),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFEAF0F6),
    onBackground = Color(0xFF101B27),
    surface = Color(0xFFF8FAFD),
    onSurface = Color(0xFF101B27),
    surfaceVariant = Color(0xFFDDE7F0),
    onSurfaceVariant = Color(0xFF506273),
    outline = Color(0xFFB6C7D7),
    outlineVariant = Color(0xFFD2DEE8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
)

private val ProfessionalTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.15.sp),
)

private val ProfessionalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

val DeepBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val PrimaryBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val BrightBlue: Color
    @Composable get() = MaterialTheme.colorScheme.secondary
val SoftBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
val SurfaceBlue: Color
    @Composable get() = MaterialTheme.colorScheme.background
val MutedText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun LakonyTheme(style: AppThemeStyle = AppThemeStyle.BAUHAUS_BLUE, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NavyBlue,
        typography = ProfessionalTypography,
        shapes = ProfessionalShapes,
        content = content,
    )
}
