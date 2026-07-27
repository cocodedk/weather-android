package dk.cocode.weather.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.domain.Units
import dk.cocode.weather.domain.WeatherIcon
import dk.cocode.weather.domain.Wmo
import dk.cocode.weather.ui.icons.WeatherIconView
import dk.cocode.weather.ui.theme.LocalPalette

/**
 * Current conditions, or the headline for a selected future day. Mirrors the
 * `#hero` block: big icon, oversized temperature, condition, and a range line.
 */
@Composable
fun Hero(forecast: Forecast, dayIndex: Int, units: Units, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val today = dayIndex == 0
    val day = forecast.daily.getOrNull(dayIndex)

    val icon = if (today) {
        Wmo.icon(forecast.current.weatherCode, forecast.current.isDay)
    } else {
        Wmo.icon(day?.weatherCode, isDay = true)
    }
    val bigTemp = if (today) forecast.current.temperature else day?.temperatureMax
    val condition = Wmo.label(if (today) forecast.current.weatherCode else day?.weatherCode)

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeatherIconView(
            icon = icon,
            modifier = Modifier.size(88.dp),
            // Accent is the sun/moon colour. Tinting a rain cloud gold reads as a
            // warning rather than as weather, so only sky icons take it.
            tint = if (icon in SKY_ICONS) palette.accent else palette.fg,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = units.temp(bigTemp),
                    color = palette.fg,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = (-2).sp,
                )
                Text(
                    text = units.tempUnit(),
                    color = palette.fg,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(top = 14.dp, start = 3.dp),
                )
            }
            Text(
                text = condition,
                color = palette.fg,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = rangeLine(forecast, dayIndex, units),
                color = palette.fgDim,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Icons whose artwork contains a sun or moon, and so carry the accent colour. */
private val SKY_ICONS = setOf(
    WeatherIcon.CLEAR, WeatherIcon.CLEAR_NIGHT,
    WeatherIcon.PARTLY, WeatherIcon.PARTLY_NIGHT,
    WeatherIcon.SHOWERS,
)

/** "High 21° · Low 12° · 2.4 mm today" — the `#hero-range` line. */
private fun rangeLine(forecast: Forecast, dayIndex: Int, units: Units): String {
    val day = forecast.daily.getOrNull(dayIndex) ?: return ""
    val hi = units.temp(day.temperatureMax)
    val lo = units.temp(day.temperatureMin)

    if (dayIndex != 0) {
        return "${units.weekday(day.time)} ${units.dateLabel(day.time)} · " +
            "High $hi° · Low $lo°"
    }

    val sum = day.precipitationSum
    val wet = if (sum != null && sum > 0) "${units.precip(sum)} today" else "Dry day"
    return "High $hi° · Low $lo° · $wet"
}
