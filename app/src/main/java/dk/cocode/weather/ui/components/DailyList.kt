package dk.cocode.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Forecast
import dk.cocode.weather.domain.Units
import dk.cocode.weather.domain.Wmo
import dk.cocode.weather.ui.icons.WeatherIconView
import dk.cocode.weather.ui.theme.LocalPalette

/**
 * The 7-day forecast. The TV laid these out as a horizontal row of cards because
 * the remote moves left/right; on a phone a vertical list reads better and gives
 * each day room for its full name.
 *
 * Tapping a day drives the hero, stats and hourly strip above — the same selection
 * model as the Tizen app's `selectDay`.
 */
@Composable
fun DailyList(
    forecast: Forecast,
    dayIndex: Int,
    units: Units,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    Column(modifier) {
        SectionTitle("Next ${forecast.daily.size} days")
        Column(Modifier.padding(horizontal = 20.dp)) {
            forecast.daily.forEachIndexed { i, d ->
                val selected = i == dayIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            if (selected) palette.tile else palette.tile2,
                            RoundedCornerShape(12.dp),
                        )
                        .then(
                            if (selected) Modifier.border(
                                1.5.dp, palette.accent, RoundedCornerShape(12.dp),
                            ) else Modifier
                        )
                        .clickable { onSelectDay(i) }
                        .padding(horizontal = 14.dp),
                ) {
                    Column(Modifier.width(96.dp)) {
                        Text(
                            text = if (i == 0) "Today" else units.weekday(d.time),
                            color = palette.fg,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = units.dateLabel(d.time),
                            color = palette.fgDim,
                            fontSize = 11.sp,
                        )
                    }

                    WeatherIconView(
                        icon = Wmo.icon(d.weatherCode, isDay = true),
                        modifier = Modifier.size(28.dp),
                        tint = palette.fg,
                    )

                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = units.percent(d.precipitationProbabilityMax),
                        color = if ((d.precipitationProbabilityMax ?: 0.0) < 5) palette.fgDim
                        else palette.wet,
                        fontSize = 13.sp,
                        modifier = Modifier.width(44.dp),
                    )

                    Spacer(Modifier.weight(1f))
                    Text(
                        text = units.temp(d.temperatureMax) + "°",
                        color = palette.fg,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(42.dp),
                    )
                    Text(
                        text = units.temp(d.temperatureMin) + "°",
                        color = palette.fgDim,
                        fontSize = 17.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(42.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
