package dk.cocode.weather.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dk.cocode.weather.ui.theme.LocalPalette

/**
 * A section title, drawn like the rest of the app's titles and announced as a heading.
 * [capitals] is off for a title that holds a web address, which reads wrongly in capitals.
 */
@Composable
fun AboutHeading(@StringRes title: Int, modifier: Modifier = Modifier, capitals: Boolean = true) {
    val palette = LocalPalette.current
    val text = stringResource(title)
    Text(
        text = if (capitals) text.uppercase() else text,
        color = palette.fgDim,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.8.sp,
        modifier = modifier.padding(top = 28.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
fun AboutBody(@StringRes text: Int, modifier: Modifier = Modifier) {
    AboutBody(stringResource(text), modifier)
}

@Composable
fun AboutBody(text: String, modifier: Modifier = Modifier, dim: Boolean = false) {
    val palette = LocalPalette.current
    Text(
        text = text,
        color = if (dim) palette.fgDim else palette.fg,
        fontSize = if (dim) 13.sp else 15.sp,
        lineHeight = if (dim) 18.sp else 22.sp,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** A full-width button that names what it opens. */
@Composable
fun AboutButton(@StringRes label: Int, onClick: () -> Unit) {
    val palette = LocalPalette.current
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(bottom = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.fg),
        border = BorderStroke(1.dp, palette.fgDim),
    ) {
        Text(stringResource(label), fontSize = 15.sp)
    }
}
