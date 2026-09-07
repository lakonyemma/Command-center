package com.lakony.commandcenter.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Keep these legacy enum identifiers stable because older installs persisted AppThemeStyle.name.
// Every legacy value now resolves to the same Midnight Blue design system.
enum class AppThemeStyle(val label: String) {
    BAUHAUS_BLUE("Midnight Blue"),
    SWISS_RED("Midnight Blue"),
    GRAPHITE("Midnight Blue"),
    FOREST("Midnight Blue"),
}

private val MidnightBlue = darkColorScheme(
    primary = Color(0xFF78B7FF),
    onPrimary = Color(0xFF06182A),
    primaryContainer = Color(0xFF123A60),
    onPrimaryContainer = Color(0xFFDCEEFF),
    secondary = Color(0xFF9CBAD6),
    onSecondary = Color(0xFF10283E),
    secondaryContainer = Color(0xFF223B53),
    onSecondaryContainer = Color(0xFFDCE8F4),
    tertiary = Color(0xFF7FA8C8),
    onTertiary = Color(0xFF0D263A),
    background = Color(0xFF07111D),
    onBackground = Color(0xFFEAF1F8),
    surface = Color(0xFF0C1927),
    onSurface = Color(0xFFEAF1F8),
    surfaceVariant = Color(0xFF142638),
    onSurfaceVariant = Color(0xFFB8C7D6),
    outline = Color(0xFF35516B),
    outlineVariant = Color(0xFF23384B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
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
    @Composable get() = MaterialTheme.colorScheme.background
val PrimaryBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val BrightBlue: Color
    @Composable get() = MaterialTheme.colorScheme.secondary
val SoftBlue: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
val SurfaceBlue: Color
    @Composable get() = MaterialTheme.colorScheme.surface
val MutedText: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun LakonyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MidnightBlue,
        typography = ProfessionalTypography,
        shapes = ProfessionalShapes,
        content = content,
    )
}
