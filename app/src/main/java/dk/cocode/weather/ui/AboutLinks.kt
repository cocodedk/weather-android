package dk.cocode.weather.ui

/** The places the About screen can send the reader to. */
enum class AboutLink { Updates, Privacy, Website, Source, Issues }

/** A fact about the app's publication that changes over time. See cocode-apps' apps.yml. */
object AboutTargets {
    /** Becomes true once the app is live on F-Droid; the update button then opens its F-Droid page. */
    const val LIVE_ON_FDROID = false
}

// The release id, not BuildConfig/packageName: debug builds add a ".debug" suffix.
private const val APPLICATION_ID = "dk.cocode.weather"
private const val REPO = "https://github.com/cocodedk/weather-android"
private const val SITE = "https://weather.cocode.dk/"

/**
 * Languages the site has both a home page and a privacy page for, at `<site>/<code>/` and
 * `<site>/<code>/privacy/`. Persian has a home page but no privacy page of its own, so Persian
 * stays on the English pages.
 */
private val SITE_LANGUAGES = setOf("da")

private fun sitePage(language: String, path: String = ""): String =
    if (language in SITE_LANGUAGES) "$SITE$language/$path" else "$SITE$path"

/**
 * Where each About link goes, or null when there is nothing to link to (no privacy page yet).
 * The website and privacy links follow [language] (a code such as "da" from the app's current
 * locale) and open the English pages when the site has none in that language. If [privacyUrl] is
 * null, the About screen leaves the privacy link out.
 * The update button only opens a web page; the app never asks the network about updates itself.
 */
fun aboutUrl(
    link: AboutLink,
    language: String,
    liveOnFdroid: Boolean = AboutTargets.LIVE_ON_FDROID,
    privacyUrl: String? = sitePage(language, "privacy/"),
): String? = when (link) {
    AboutLink.Updates ->
        if (liveOnFdroid) "https://f-droid.org/packages/$APPLICATION_ID/" else "$REPO/releases/latest"
    AboutLink.Privacy -> privacyUrl
    AboutLink.Website -> sitePage(language)
    AboutLink.Source -> REPO
    AboutLink.Issues -> "$REPO/issues"
}

fun versionLine(name: String, code: Long): String = "$name ($code)"
