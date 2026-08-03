package dk.cocode.weather.widget

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dk.cocode.weather.domain.WeatherIcon
import dk.cocode.weather.ui.icons.VIEW_BOX
import dk.cocode.weather.ui.icons.drawWeatherIcon

/**
 * Renders a weather icon to a Bitmap for RemoteViews.
 *
 * A widget cannot host a Composable, and RemoteViews only accepts drawables or
 * bitmaps. Rather than maintain a second, drifting set of vector drawables, this
 * drives the app's own `drawWeatherIcon` through a [CanvasDrawScope] off-screen —
 * so the widget's artwork is literally the app's, not a lookalike.
 */
object WidgetIcons {

    fun render(icon: WeatherIcon, sizePx: Int, tint: Color): Bitmap {
        val px = sizePx.coerceAtLeast(1)
        val image = ImageBitmap(px, px)
        val canvas = Canvas(image)
        val scale = px / VIEW_BOX

        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(px.toFloat(), px.toFloat()),
        ) {
            withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
                drawWeatherIcon(icon, tint)
            }
        }
        return image.asAndroidBitmap()
    }
}
