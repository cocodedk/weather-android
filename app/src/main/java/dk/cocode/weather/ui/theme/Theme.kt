package dk.cocode.weather.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The Tizen app's two palettes, carried over verbatim so the phone and the TV read
 * as the same product. Night is selected per the *forecast location's* day/night,
 * not the phone's system theme — a clear night in Sydney should look like night
 * even at noon in Copenhagen.
 */
data class WeatherPalette(
    val bg1: Color,
    val bg2: Color,
    val fg: Color,
    val fgDim: Color,
    val tile: Color,
    val tile2: Color,
    val accent: Color,
    val wet: Color,
    val isNight: Boolean,
) {
    val gradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(bg1, bg2),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
}

val DayPalette = WeatherPalette(
    bg1 = Color(0xFF17457F),
    bg2 = Color(0xFF3D87C7),
    fg = Color(0xFFFFFFFF),
    fgDim = Color(0xFFFFFFFF).copy(alpha = 0.72f),
    tile = Color(0xFFFFFFFF).copy(alpha = 0.15f),
    tile2 = Color(0xFFFFFFFF).copy(alpha = 0.10f),
    accent = Color(0xFFFFD257),
    wet = Color(0xFF7FD6FF),
    isNight = false,
)

val NightPalette = WeatherPalette(
    bg1 = Color(0xFF060C22),
    bg2 = Color(0xFF1B2A5E),
    fg = Color(0xFFEAF0FF),
    fgDim = Color(0xFFEAF0FF).copy(alpha = 0.66f),
    tile = Color(0xFFFFFFFF).copy(alpha = 0.075f),
    tile2 = Color(0xFFFFFFFF).copy(alpha = 0.05f),
    accent = Color(0xFFFFD257),
    wet = Color(0xFF63C8FF),
    isNight = true,
)

val LocalPalette: ProvidableCompositionLocal<WeatherPalette> = compositionLocalOf { DayPalette }

@Composable
fun WeatherTheme(isNight: Boolean, content: @Composable () -> Unit) {
    val palette = if (isNight) NightPalette else DayPalette

    // The app always paints light-on-dark, so the Material scheme is pinned to dark
    // regardless of the system setting; only the gradient swaps.
    val colors = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color(0xFF1A1A1A),
        background = palette.bg1,
        onBackground = palette.fg,
        surface = palette.bg2,
        onSurface = palette.fg,
    )

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colors, typography = Typography()) {
            Box(Modifier.fillMaxSize().background(palette.gradient)) { content() }
        }
    }
}
