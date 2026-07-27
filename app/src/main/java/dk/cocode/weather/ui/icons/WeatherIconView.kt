package dk.cocode.weather.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import dk.cocode.weather.domain.WeatherIcon

/**
 * Draws a weather sprite. The Tizen build inlined SVG symbols because a TV app
 * cannot rely on the network for images; here the same geometry is drawn straight
 * to a Canvas, which keeps the icons resolution-independent with no drawable
 * assets to keep in sync.
 */
@Composable
fun WeatherIconView(
    icon: WeatherIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Canvas(modifier) {
        val s = size.minDimension / VIEW_BOX
        val dx = (size.width - VIEW_BOX * s) / 2f
        val dy = (size.height - VIEW_BOX * s) / 2f
        withTransform({
            translate(dx, dy)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawIcon(icon, tint)
        }
    }
}

private const val VIEW_BOX = 64f

private fun DrawScope.drawIcon(icon: WeatherIcon, c: Color) = when (icon) {
    WeatherIcon.CLEAR -> {
        dot(c, 32f, 32f, 12f)
        sunRays(c, 32f, 32f, inner = 20f, outer = 28f, width = 4f)
    }

    WeatherIcon.CLEAR_NIGHT ->
        fillPath(c, "M56 34.1A24 24 0 1 1 29.9 8 18.7 18.7 0 0 0 56 34.1z")

    WeatherIcon.CLOUDY ->
        cloud(c, 26f, 26f, 11f, 41f, 30f, 9f, 14f, 30f, 36f, 13f, 6.5f)

    WeatherIcon.PARTLY -> {
        dot(c, 43f, 20f, 9f)
        ln(c, 43f, 3f, 43f, 7f, 3f)
        ln(c, 58f, 20f, 62f, 20f, 3f)
        ln(c, 54.3f, 9.7f, 57f, 7f, 3f)
        ln(c, 31.7f, 9.7f, 29f, 7f, 3f)
        cloud(c, 23f, 35f, 10f, 36f, 38f, 8f, 13f, 38f, 31f, 12f, 6f)
    }

    WeatherIcon.PARTLY_NIGHT -> {
        // The sprite renders the moon at half scale, offset into the top-right.
        withTransform({
            translate(30f, 2f)
            scale(0.5f, 0.5f, pivot = Offset.Zero)
        }) {
            fillPath(c, "M56 34.1A24 24 0 1 1 29.9 8 18.7 18.7 0 0 0 56 34.1z")
        }
        cloud(c, 23f, 35f, 10f, 36f, 38f, 8f, 13f, 38f, 31f, 12f, 6f)
    }

    WeatherIcon.FOG -> {
        standardCloud(c)
        ln(c, 14f, 46f, 50f, 46f, 4f, alpha = 0.75f)
        ln(c, 20f, 54f, 44f, 54f, 4f, alpha = 0.75f)
    }

    WeatherIcon.DRIZZLE -> {
        standardCloud(c)
        ln(c, 22f, 45f, 20f, 50f, 4f)
        ln(c, 32f, 45f, 30f, 50f, 4f)
        ln(c, 42f, 45f, 40f, 50f, 4f)
    }

    WeatherIcon.RAIN -> {
        standardCloud(c)
        ln(c, 23f, 44f, 19f, 56f, 4f)
        ln(c, 33f, 44f, 29f, 56f, 4f)
        ln(c, 43f, 44f, 39f, 56f, 4f)
    }

    WeatherIcon.SHOWERS -> {
        dot(c, 47f, 14f, 7f)
        ln(c, 47f, 2f, 47f, 5f, 3f)
        ln(c, 58f, 14f, 61f, 14f, 3f)
        ln(c, 55.5f, 5.5f, 58f, 3f, 3f)
        cloud(c, 24f, 26f, 10f, 37f, 30f, 8f, 13f, 30f, 33f, 12f, 6f)
        ln(c, 22f, 48f, 18f, 58f, 4f)
        ln(c, 34f, 48f, 30f, 58f, 4f)
    }

    WeatherIcon.SLEET -> {
        standardCloud(c)
        ln(c, 24f, 44f, 20f, 56f, 4f)
        ln(c, 42f, 44f, 38f, 56f, 4f)
        ln(c, 28f, 50f, 36f, 50f, 4f)
        ln(c, 32f, 46f, 32f, 54f, 4f)
    }

    WeatherIcon.SNOW -> {
        standardCloud(c)
        snowflake(c, 22f, 50f, r = 4f, width = 3.5f)
        snowflake(c, 42f, 50f, r = 4f, width = 3.5f)
    }

    WeatherIcon.THUNDER -> {
        standardCloud(c)
        fillPath(c, "M34 41h9l-7 9h8L26 62l5-10h-6z")
    }

    WeatherIcon.WIND -> {
        strokePath(c, "M6 22h30a7 7 0 1 0-7-7", 4.5f)
        strokePath(c, "M6 34h40a7 7 0 1 1-7 7", 4.5f)
        ln(c, 6f, 46f, 28f, 46f, 4.5f)
    }

    WeatherIcon.DROP ->
        fillPath(c, "M32 6s16 18 16 28a16 16 0 0 1-32 0C16 24 32 6 32 6z")

    WeatherIcon.THERMO -> {
        fillPath(
            c,
            "M32 4a9 9 0 0 0-9 9v22a14 14 0 1 0 18 0V13a9 9 0 0 0-9-9zm0 6a3 3 0 0 1 3 3v25.4l1.9 " +
                "1.3a8 8 0 1 1-9.8 0l1.9-1.3V13a3 3 0 0 1 3-3z",
        )
        dot(c, 32f, 46f, 6f)
    }

    WeatherIcon.GAUGE -> {
        strokePath(c, "M8 44a24 24 0 1 1 48 0", 4.5f)
        ln(c, 32f, 44f, 45f, 28f, 4.5f)
        dot(c, 32f, 44f, 4f)
    }

    WeatherIcon.SUNRISE -> {
        dot(c, 32f, 40f, 10f)
        ln(c, 6f, 54f, 58f, 54f, 4f)
        ln(c, 32f, 8f, 32f, 18f, 4f)
        ln(c, 22f, 22f, 16f, 16f, 4f)
        ln(c, 42f, 22f, 48f, 16f, 4f)
    }

    WeatherIcon.UV -> {
        dot(c, 32f, 32f, 10f)
        sunRays(c, 32f, 32f, inner = 18f, outer = 26f, width = 4f)
    }
}
