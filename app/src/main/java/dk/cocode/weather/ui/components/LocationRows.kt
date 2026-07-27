package dk.cocode.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Place
import dk.cocode.weather.ui.theme.LocalPalette

/** The "use my current location" action, with an inline spinner while locating. */
@Composable
fun GpsRow(locating: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.tile, RoundedCornerShape(12.dp))
            .clickable(enabled = !locating, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = palette.accent,
            )
        } else {
            Icon(
                Icons.Default.MyLocation, null,
                tint = palette.accent, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (locating) "Finding your location…" else "Use my current location",
            color = palette.fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * One place in the search results or the saved list. [onRemove] is null for search
 * results and for the last remaining saved place, which must not be deletable.
 */
@Composable
fun PlaceRow(
    place: Place,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) palette.tile else palette.tile2, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Icon(
            imageVector = if (place.isDeviceLocation) Icons.Default.MyLocation
            else Icons.Default.LocationOn,
            contentDescription = null,
            tint = if (selected) palette.accent else palette.fgDim,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place.name,
                color = palette.fg,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            val sub = place.region.ifBlank {
                if (place.isDeviceLocation) "Device location" else ""
            }
            if (sub.isNotBlank()) {
                Text(sub, color = palette.fgDim, fontSize = 12.sp)
            }
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete, "Remove ${place.name}",
                    tint = palette.fgDim, modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun CenteredHint(text: String) {
    val palette = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = palette.fgDim, fontSize = 14.sp)
    }
}
