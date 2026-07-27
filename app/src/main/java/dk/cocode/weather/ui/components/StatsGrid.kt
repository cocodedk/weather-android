package dk.cocode.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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

private data class Stat(
    val icon: WeatherIcon,
    val key: String,
    val value: String,
    val sub: String,
)

/**
 * The six stat tiles. Today shows live readings; a future day shows that day's
 * aggregates — the same two sets the Tizen `statsToday`/`statsDay` produced.
 *
 * Laid out as fixed rows rather than a LazyVerticalGrid because this sits inside a
 * vertically scrolling column, where a lazy grid has no bounded height.
 */
@Composable
fun StatsGrid(forecast: Forecast, dayIndex: Int, units: Units, modifier: Modifier = Modifier) {
    val stats = if (dayIndex == 0) todayStats(forecast, units) else dayStats(forecast, dayIndex, units)

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        stats.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEachIndexed { i, stat ->
                    if (i > 0) Spacer(Modifier.width(10.dp))
                    StatTile(stat, Modifier.weight(1f))
                }
                // Keeps a lone trailing tile at half width instead of stretching it.
                if (pair.size == 1) {
                    Spacer(Modifier.width(10.dp))
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StatTile(stat: Stat, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier
            .height(76.dp)
            .background(palette.tile, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WeatherIconView(stat.icon, Modifier.size(26.dp), palette.fgDim)
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = stat.key.uppercase(),
                color = palette.fgDim,
                fontSize = 10.sp,
                letterSpacing = 1.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stat.value,
                color = palette.fg,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stat.sub,
                color = palette.fgDim,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun todayStats(f: Forecast, u: Units): List<Stat> {
    val c = f.current
    val d0 = f.daily.firstOrNull()
    val hNow = f.hourly.getOrNull(nowIndex(f))
    return listOf(
        Stat(WeatherIcon.THERMO, "Feels like", u.tempFull(c.apparentTemperature),
            "Actual ${u.tempFull(c.temperature)}"),
        Stat(WeatherIcon.WIND, "Wind", u.wind(c.windSpeed),
            "From ${u.bearing(c.windDirection)} · max ${u.wind(d0?.windSpeedMax)}"),
        Stat(WeatherIcon.DROP, "Humidity", u.percent(c.humidity), "Relative"),
        Stat(WeatherIcon.RAIN, "Precipitation", u.precip(c.precipitation),
            "Last hour · ${u.percent(hNow?.precipitationProbability)} chance"),
        Stat(WeatherIcon.GAUGE, "Pressure", u.pressure(c.pressure), "At surface"),
        Stat(WeatherIcon.SUNRISE, "Sunrise", u.clock(d0?.sunrise), "Sunset ${u.clock(d0?.sunset)}"),
    )
}

private fun dayStats(f: Forecast, dayIndex: Int, u: Units): List<Stat> {
    val d = f.daily.getOrNull(dayIndex)
    val uv = d?.uvIndexMax
    return listOf(
        Stat(WeatherIcon.THERMO, "High / low",
            "${u.temp(d?.temperatureMax)} / ${u.temp(d?.temperatureMin)}${u.tempUnit()}",
            "Daily range"),
        Stat(WeatherIcon.WIND, "Wind", u.wind(d?.windSpeedMax), "Strongest of the day"),
        Stat(WeatherIcon.DROP, "Chance of rain", u.percent(d?.precipitationProbabilityMax),
            "Peak for the day"),
        Stat(WeatherIcon.RAIN, "Precipitation", u.precip(d?.precipitationSum), "Total for the day"),
        Stat(WeatherIcon.UV, "UV index",
            if (uv == null) "--" else String.format(java.util.Locale.US, "%.1f", uv),
            Wmo.uvBand(uv)),
        Stat(WeatherIcon.SUNRISE, "Sunrise", u.clock(d?.sunrise), "Sunset ${u.clock(d?.sunset)}"),
    )
}

/**
 * Index of the hourly row covering the current wall-clock hour.
 *
 * The API's local ISO strings sort lexicographically, so when the exact hour is
 * missing we fall back to the latest hour that has already started — never to 0,
 * which would label midnight as "Now".
 */
fun nowIndex(f: Forecast): Int {
    val cur = f.current.time ?: return 0
    val hourKey = cur.take(13)
    var last = 0
    f.hourly.forEachIndexed { i, h ->
        if (h.time.take(13) == hourKey) return i
        if (h.time < cur) last = i
    }
    return last
}
