package com.lakony.commandcenter.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Keep these legacy enum identifiers stable: ThemeController persists AppThemeStyle.name.
// The user-facing labels were modernized, but renaming the enum constants would break
// existing saved theme preferences unless a migration is added first.
enum class AppThemeStyle(val label: String) {
    BAUHAUS_BLUE("Executive Navy"),
    SWISS_RED("Midnight Blue"),
    GRAPHITE("Graphite"),
    FOREST("Slate Teal"),
}

private val ExecutiveNavy = lightColorScheme(
    primary = Color(0xFF163A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE8F4),
    onPrimaryContainer = Color(0xFF102A44),
    secondary = Color(0xFF52718F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5EDF5),
    onSecondaryContainer = Color(0xFF243B53),
    tertiary = Color(0xFF3E6C73),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF17212B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF52606D),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFB42318),
)

private val MidnightBlue = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF0B2B47),
    primaryContainer = Color(0xFF153C5E),
    onPrimaryContainer = Color(0xFFD8ECFF),
    secondary = Color(0xFFB8C8DA),
    onSecondary = Color(0xFF243647),
    secondaryContainer = Color(0xFF2B3F52),
    onSecondaryContainer = Color(0xFFD7E4F2),
    tertiary = Color(0xFF8FC9C6),
    onTertiary = Color(0xFF173B3A),
    background = Color(0xFF0E1720),
    onBackground = Color(0xFFE8EEF5),
    surface = Color(0xFF151F29),
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF202D3A),
    onSurfaceVariant = Color(0xFFB9C6D3),
    outline = Color(0xFF435465),
    error = Color(0xFFFFB4AB),
)

private val Graphite = lightColorScheme(
    primary = Color(0xFF303A46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3E8ED),
    onPrimaryContainer = Color(0xFF202A34),
    secondary = Color(0xFF64717D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBEEF1),
    onSecondaryContainer = Color(0xFF39434D),
    tertiary = Color(0xFF46657B),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F8),
    onBackground = Color(0xFF1B1F23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F23),
    surfaceVariant = Color(0xFFF0F2F4),
    onSurfaceVariant = Color(0xFF5C6670),
    outline = Color(0xFFD2D8DE),
    error = Color(0xFFB42318),
)

private val SlateTeal = lightColorScheme(
    primary = Color(0xFF245C64),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EBEC),
    onPrimaryContainer = Color(0xFF163C41),
    secondary = Color(0xFF5F7780),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4ECEF),
    onSecondaryContainer = Color(0xFF344950),
    tertiary = Color(0xFF496B86),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F7F7),
    onBackground = Color(0xFF172426),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172426),
    surfaceVariant = Color(0xFFEDF2F2),
    onSurfaceVariant = Color(0xFF556568),
    outline = Color(0xFFCDD8DA),
    error = Color(0xFFB42318),
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
    @Composable get() = MaterialTheme.colorScheme.onBackground
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
    val colors = when (style) {
        AppThemeStyle.BAUHAUS_BLUE -> ExecutiveNavy
        AppThemeStyle.SWISS_RED -> MidnightBlue
        AppThemeStyle.GRAPHITE -> Graphite
        AppThemeStyle.FOREST -> SlateTeal
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ProfessionalTypography,
        shapes = ProfessionalShapes,
        content = content,
    )
}
