package dk.cocode.weather.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import dk.cocode.weather.R
import dk.cocode.weather.ui.theme.LocalPalette

/**
 * The About page, sections in the cocode-apps standard's order: name and version,
 * what the app does, privacy, links, credits and licenses, made by Cocode, support.
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    // The language the app's own strings use, so the website and privacy pages match it.
    val language = LocalConfiguration.current.locales[0].language
    var noBrowser by rememberSaveable { mutableStateOf(false) }
    val open = { link: AboutLink -> aboutUrl(link, language)?.let { noBrowser = !openLink(context, it) } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.about_back),
                    tint = palette.fg,
                )
            }
            Text(
                text = stringResource(R.string.about_title),
                color = palette.fg,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
        }

        // Fixed under the title, not in the scrolling part, so it is seen whichever button was tapped.
        if (noBrowser) {
            AboutBody(R.string.about_no_browser, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            AboutHeading(R.string.about_name_title, Modifier.padding(top = 12.dp))
            Text(
                stringResource(R.string.app_name),
                color = palette.fg,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            AboutBody(stringResource(R.string.about_version, rememberVersionLine()))
            AboutButton(R.string.about_check_updates) { open(AboutLink.Updates) }
            AboutBody(stringResource(R.string.about_check_updates_note), dim = true)

            AboutHeading(R.string.about_what_title)
            AboutBody(R.string.about_what)

            AboutHeading(R.string.about_privacy_title)
            AboutBody(R.string.about_privacy)
            // Left out if the privacy policy has no page to link to (aboutUrl returns null).
            if (aboutUrl(AboutLink.Privacy, language) != null) {
                AboutButton(R.string.about_privacy_link) { open(AboutLink.Privacy) }
            }

            AboutHeading(R.string.about_links_title)
            AboutButton(R.string.about_website) { open(AboutLink.Website) }
            AboutButton(R.string.about_source) { open(AboutLink.Source) }
            AboutButton(R.string.about_report) { open(AboutLink.Issues) }

            AboutHeading(R.string.about_credits)
            AboutBody(R.string.about_credits_data)
            AboutBody(R.string.about_credits_license)

            AboutHeading(R.string.about_made_by, capitals = false)

            // Support slot: the standard's section 7, deliberately empty until the Support
            // phase (cocode-apps standard/support.md). Nothing is drawn for it.

            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun rememberVersionLine(): String {
    val context = LocalContext.current
    return remember {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        versionLine(info.versionName.orEmpty(), PackageInfoCompat.getLongVersionCode(info))
    }
}

/** False when no app on the phone can open web links; the page then says so. */
private fun openLink(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (e: ActivityNotFoundException) {
    false
}
