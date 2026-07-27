package dk.cocode.weather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.data.Place
import dk.cocode.weather.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Place name, region, and the clock *at that place*.
 *
 * The time comes from the API's `utc_offset_seconds` rather than the phone's
 * timezone, so checking the weather in Tokyo also tells you what time it is there.
 * The Tizen app did the same for Copenhagen; here it finally earns its keep.
 */
@Composable
fun PlaceHeader(
    place: Place,
    utcOffsetSeconds: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            nowMs = System.currentTimeMillis()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = place.name,
                    color = palette.fg,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (place.isDeviceLocation) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "Device location",
                        tint = palette.accent,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            val sub = place.region.ifBlank { if (place.isDeviceLocation) "Device location" else "" }
            if (sub.isNotBlank()) {
                Text(sub, color = palette.fgDim, fontSize = 13.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }

        if (utcOffsetSeconds != null) {
            Text(
                text = localTime(nowMs, utcOffsetSeconds),
                color = palette.fg,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Shifts the current instant by the location's offset and reads it as UTC — the
 * arithmetic-only way to get a place's wall clock without needing its zone rules.
 */
private fun localTime(nowMs: Long, offsetSeconds: Int): String =
    Instant.ofEpochMilli(nowMs + offsetSeconds * 1000L)
        .atZone(ZoneOffset.UTC)
        .format(HHMM)
