package dk.cocode.weather.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import dk.cocode.weather.R
import dk.cocode.weather.domain.Condition
import dk.cocode.weather.domain.UnitLabels
import dk.cocode.weather.domain.Units
import dk.cocode.weather.domain.UvBand

/**
 * Where the pure domain values meet string resources. The widget and the screens
 * both come through here, so a condition is worded the same way in both.
 */
@StringRes
fun Condition.labelRes(): Int = when (this) {
    Condition.CLEAR_SKY -> R.string.wmo_clear_sky
    Condition.MAINLY_CLEAR -> R.string.wmo_mainly_clear
    Condition.PARTLY_CLOUDY -> R.string.wmo_partly_cloudy
    Condition.OVERCAST -> R.string.wmo_overcast
    Condition.FOG -> R.string.wmo_fog
    Condition.FREEZING_FOG -> R.string.wmo_freezing_fog
    Condition.LIGHT_DRIZZLE -> R.string.wmo_light_drizzle
    Condition.DRIZZLE -> R.string.wmo_drizzle
    Condition.DENSE_DRIZZLE -> R.string.wmo_dense_drizzle
    Condition.FREEZING_DRIZZLE -> R.string.wmo_freezing_drizzle
    Condition.LIGHT_RAIN -> R.string.wmo_light_rain
    Condition.RAIN -> R.string.wmo_rain
    Condition.HEAVY_RAIN -> R.string.wmo_heavy_rain
    Condition.FREEZING_RAIN -> R.string.wmo_freezing_rain
    Condition.LIGHT_SNOW -> R.string.wmo_light_snow
    Condition.SNOW -> R.string.wmo_snow
    Condition.HEAVY_SNOW -> R.string.wmo_heavy_snow
    Condition.SNOW_GRAINS -> R.string.wmo_snow_grains
    Condition.LIGHT_SHOWERS -> R.string.wmo_light_showers
    Condition.SHOWERS -> R.string.wmo_showers
    Condition.VIOLENT_SHOWERS -> R.string.wmo_violent_showers
    Condition.SNOW_SHOWERS -> R.string.wmo_snow_showers
    Condition.HEAVY_SNOW_SHOWERS -> R.string.wmo_heavy_snow_showers
    Condition.THUNDERSTORM -> R.string.wmo_thunderstorm
    Condition.THUNDERSTORM_HAIL -> R.string.wmo_thunderstorm_hail
    Condition.UNKNOWN -> R.string.wmo_unknown
}

@StringRes
fun UvBand.labelRes(): Int = when (this) {
    UvBand.LOW -> R.string.uv_low
    UvBand.MODERATE -> R.string.uv_moderate
    UvBand.HIGH -> R.string.uv_high
    UvBand.VERY_HIGH -> R.string.uv_very_high
    UvBand.EXTREME -> R.string.uv_extreme
}

private fun unitLabels(resources: Resources) = UnitLabels(
    compass = resources.getStringArray(R.array.compass_points).toList(),
    am = resources.getString(R.string.clock_am),
    pm = resources.getString(R.string.clock_pm),
)

/** The widget's [Units]; it has no composition, so it reads the resources directly. */
fun unitsFor(resources: Resources, imperial: Boolean, use24Hour: Boolean) =
    Units(unitLabels(resources), imperial, use24Hour)

/** The screens' [Units]. The resource lookups follow the phone's language if it changes. */
@Composable
fun rememberUnits(state: WeatherUiState): Units {
    val labels = UnitLabels(
        compass = stringArrayResource(R.array.compass_points).toList(),
        am = stringResource(R.string.clock_am),
        pm = stringResource(R.string.clock_pm),
    )
    return remember(labels, state.imperial, state.use24Hour) {
        Units(labels, state.imperial, state.use24Hour)
    }
}
