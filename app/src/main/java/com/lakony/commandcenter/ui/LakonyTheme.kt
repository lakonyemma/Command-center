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

enum class AppThemeStyle(val label: String) {
    BAUHAUS_BLUE("Bauhaus Blue"),
    SWISS_RED("Swiss Red"),
    GRAPHITE("Graphite"),
    FOREST("Forest"),
}

private val BauhausBlue = lightColorScheme(
    primary = Color(0xFF0757D9),
    secondary = Color(0xFFFFC400),
    tertiary = Color(0xFFE9342F),
    background = Color(0xFFF7F4EC),
    surface = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
)

private val SwissRed = lightColorScheme(
    primary = Color(0xFFE62117),
    secondary = Color(0xFF111111),
    tertiary = Color(0xFF276EF1),
    background = Color(0xFFF7F7F4),
    surface = Color.White,
    primaryContainer = Color(0xFFFFE2DF),
    onPrimary = Color.White,
    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),
)

private val Graphite = lightColorScheme(
    primary = Color(0xFF202124),
    secondary = Color(0xFF666A70),
    tertiary = Color(0xFF0D5CBD),
    background = Color(0xFFF2F2F0),
    surface = Color.White,
    primaryContainer = Color(0xFFE3E3E1),
    onPrimary = Color.White,
    onBackground = Color(0xFF101113),
    onSurface = Color(0xFF101113),
)

private val Forest = lightColorScheme(
    primary = Color(0xFF176B3A),
    secondary = Color(0xFFB57918),
    tertiary = Color(0xFF1A4FA3),
    background = Color(0xFFF3F5EF),
    surface = Color.White,
    primaryContainer = Color(0xFFDCEBDD),
    onPrimary = Color.White,
    onBackground = Color(0xFF142018),
    onSurface = Color(0xFF142018),
)

private val SwissTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp),
)

private val BauhausShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
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
    @Composable get() = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f)

@Composable
fun LakonyTheme(style: AppThemeStyle = AppThemeStyle.BAUHAUS_BLUE, content: @Composable () -> Unit) {
    val colors = when (style) {
        AppThemeStyle.BAUHAUS_BLUE -> BauhausBlue
        AppThemeStyle.SWISS_RED -> SwissRed
        AppThemeStyle.GRAPHITE -> Graphite
        AppThemeStyle.FOREST -> Forest
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SwissTypography,
        shapes = BauhausShapes,
        content = content,
    )
}
