package dk.cocode.weather.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dk.cocode.weather.R
import dk.cocode.weather.data.WeatherStore
import dk.cocode.weather.ui.WeatherUiState
import dk.cocode.weather.ui.theme.LocalPalette
import dk.cocode.weather.widget.WeatherWidgetProvider

/**
 * Place header plus the two controls. The app draws edge to edge, so this insets
 * itself past the status bar — without that it sits on top of the system clock.
 */
@Composable
fun WeatherTopBar(
    state: WeatherUiState,
    onOpenLocations: () -> Unit,
    onToggleUnits: () -> Unit,
    onCycleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onAddWidget: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val palette = LocalPalette.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.selected?.let { place ->
            Box(Modifier.weight(1f)) {
                PlaceHeader(
                    place = place,
                    utcOffsetSeconds = state.forecast?.utcOffsetSeconds,
                    onClick = onOpenLocations,
                )
            }
        }

        IconButton(onClick = onOpenLocations) {
            Icon(Icons.Default.Place, stringResource(R.string.cd_change_location), tint = palette.fg)
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.cd_more), tint = palette.fg)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (state.imperial) R.string.menu_units_metric
                                else R.string.menu_units_imperial,
                            ),
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Thermostat, null) },
                    onClick = { onToggleUnits(); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_theme, stringResource(themeLabel(state.theme)))) },
                    leadingIcon = { Icon(Icons.Default.Brightness4, null) },
                    onClick = { onCycleTheme(); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_refresh)) },
                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                    onClick = { onRefresh(); menuOpen = false },
                )
                // Only offered where the launcher can actually honour it; on the rest,
                // the widget is still available from the long-press widget drawer.
                if (WeatherWidgetProvider.canPin(LocalContext.current)) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_add_widget)) },
                        leadingIcon = { Icon(Icons.Default.Widgets, null) },
                        onClick = { onAddWidget(); menuOpen = false },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about_title)) },
                    leadingIcon = { Icon(Icons.Default.Info, null) },
                    onClick = { onOpenAbout(); menuOpen = false },
                )
            }
        }
    }
}

@StringRes
private fun themeLabel(theme: String) = when (theme) {
    WeatherStore.THEME_DAY -> R.string.theme_day
    WeatherStore.THEME_NIGHT -> R.string.theme_night
    else -> R.string.theme_auto
}
