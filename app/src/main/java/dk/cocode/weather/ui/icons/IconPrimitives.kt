package dk.cocode.weather.ui.icons

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Drawing primitives for the weather sprite, all expressed in the SVG's original
 * 64x64 coordinate space. [WeatherIconView] applies the scale, so these read
 * one-for-one against the `<symbol>` markup in the Tizen app's index.html and can
 * be diffed against it.
 */

internal fun DrawScope.dot(color: Color, cx: Float, cy: Float, r: Float) =
    drawCircle(color, radius = r, center = Offset(cx, cy))

internal fun DrawScope.ln(
    color: Color,
    x1: Float, y1: Float, x2: Float, y2: Float,
    width: Float,
    alpha: Float = 1f,
) = drawLine(
    color = color,
    start = Offset(x1, y1),
    end = Offset(x2, y2),
    strokeWidth = width,
    cap = StrokeCap.Round,
    alpha = alpha,
)

internal fun DrawScope.bar(
    color: Color,
    x: Float, y: Float, w: Float, h: Float, r: Float,
) = drawRoundRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(r, r),
)

/** Parses an SVG `d` attribute so path-based symbols can be copied over unchanged. */
internal fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

internal fun DrawScope.fillPath(color: Color, d: String) = drawPath(svgPath(d), color)

internal fun DrawScope.strokePath(color: Color, d: String, width: Float) =
    drawPath(svgPath(d), color, style = Stroke(width = width, cap = StrokeCap.Round))

/**
 * The three-lump cloud shared by most conditions. Each symbol positions it
 * slightly differently, so the geometry stays a parameter rather than a constant.
 */
internal fun DrawScope.cloud(
    color: Color,
    c1x: Float, c1y: Float, r1: Float,
    c2x: Float, c2y: Float, r2: Float,
    bx: Float, by: Float, bw: Float, bh: Float, br: Float,
) {
    dot(color, c1x, c1y, r1)
    dot(color, c2x, c2y, r2)
    bar(color, bx, by, bw, bh, br)
}

/** The cloud used by fog / drizzle / rain / sleet / snow / thunder. */
internal fun DrawScope.standardCloud(color: Color) =
    cloud(color, 26f, 22f, 10f, 39f, 26f, 8f, 15f, 26f, 33f, 12f, 6f)

/** An eight-spoke sun burst, centred on [cx],[cy]. */
internal fun DrawScope.sunRays(
    color: Color,
    cx: Float, cy: Float,
    inner: Float, outer: Float,
    width: Float,
) {
    // 0.7071 = cos(45deg): the diagonal spokes sit on the circle's 45-degree points.
    val d = 0.7071f
    val spokes = listOf(
        0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f,
        -d to -d, d to d, -d to d, d to -d,
    )
    spokes.forEach { (ux, uy) ->
        ln(color, cx + ux * inner, cy + uy * inner, cx + ux * outer, cy + uy * outer, width)
    }
}

/** A six-spoke snowflake — three crossed lines, as in the `i-snow` symbol. */
internal fun DrawScope.snowflake(color: Color, cx: Float, cy: Float, r: Float, width: Float) {
    ln(color, cx - r, cy, cx + r, cy, width)
    ln(color, cx, cy - r, cx, cy + r, width)
    val d = r * 0.7071f
    ln(color, cx - d, cy - d, cx + d, cy + d, width)
    ln(color, cx + d, cy - d, cx - d, cy + d, width)
}
