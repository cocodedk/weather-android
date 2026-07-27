package dk.cocode.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Place
import dk.cocode.weather.ui.SearchUiState
import dk.cocode.weather.ui.theme.LocalPalette

/**
 * Location picker: type-ahead search over Open-Meteo geocoding, a one-tap GPS
 * option, and the saved list. This is the whole "not just Copenhagen" feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSheet(
    search: SearchUiState,
    saved: List<Place>,
    selectedKey: String?,
    locating: Boolean,
    onQueryChange: (String) -> Unit,
    onPick: (Place) -> Unit,
    onRemove: (Place) -> Unit,
    onUseDeviceLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.bg1,
        contentColor = palette.fg,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Locations", color = palette.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = search.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search any city or town", color = palette.fgDim) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = palette.fgDim) },
                trailingIcon = {
                    if (search.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, "Clear", tint = palette.fgDim)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = palette.tile,
                    unfocusedContainerColor = palette.tile2,
                    focusedTextColor = palette.fg,
                    unfocusedTextColor = palette.fg,
                    cursorColor = palette.accent,
                    focusedIndicatorColor = palette.accent,
                    unfocusedIndicatorColor = palette.tile,
                ),
            )

            Spacer(Modifier.height(12.dp))
            GpsRow(locating = locating, onClick = onUseDeviceLocation)
            Spacer(Modifier.height(8.dp))

            when {
                search.searching -> CenteredHint("Searching…")
                search.error != null -> CenteredHint(search.error)
                search.query.trim().length >= 2 && search.results.isEmpty() ->
                    CenteredHint("No places match “${search.query.trim()}”")
            }

            val showingResults = search.results.isNotEmpty()
            if (showingResults || saved.isNotEmpty()) {
                Text(
                    text = if (showingResults) "SEARCH RESULTS" else "SAVED",
                    color = palette.fgDim,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (showingResults) {
                    // Results are keyed by name too: two distinct places can round
                    // to the same coordinate key at 2 decimal places.
                    items(search.results, key = { it.key + it.name }) { place ->
                        PlaceRow(place, selected = false, onClick = { onPick(place) })
                    }
                } else {
                    items(saved, key = { it.key }) { place ->
                        PlaceRow(
                            place = place,
                            selected = place.key == selectedKey,
                            onClick = { onPick(place) },
                            onRemove = { onRemove(place) }.takeIf { saved.size > 1 },
                        )
                    }
                }
            }
        }
    }
}
