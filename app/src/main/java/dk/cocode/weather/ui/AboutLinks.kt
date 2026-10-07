package dk.cocode.weather.ui

/** The places the About screen can send the reader to. */
enum class AboutLink { Updates, Privacy, Website, Source, Issues }

/** Two facts about the app's publication that change over time. See cocode-apps' apps.yml. */
object AboutTargets {
    /** Becomes true once the app is live on F-Droid; the update button then opens its F-Droid page. */
    const val LIVE_ON_FDROID = false

    /** The privacy policy page. If it is ever null, the About screen leaves the link out. */
    val PRIVACY_URL: String? = "https://weather.cocode.dk/privacy/"
}

// The release id, not BuildConfig/packageName: debug builds add a ".debug" suffix.
private const val APPLICATION_ID = "dk.cocode.weather"
private const val REPO = "https://github.com/cocodedk/weather-android"

/**
 * Where each About link goes, or null when there is nothing to link to (no privacy page yet).
 * The update button only opens a web page; the app never asks the network about updates itself.
 */
fun aboutUrl(
    link: AboutLink,
    liveOnFdroid: Boolean = AboutTargets.LIVE_ON_FDROID,
    privacyUrl: String? = AboutTargets.PRIVACY_URL,
): String? = when (link) {
    AboutLink.Updates ->
        if (liveOnFdroid) "https://f-droid.org/packages/$APPLICATION_ID/" else "$REPO/releases/latest"
    AboutLink.Privacy -> privacyUrl
    AboutLink.Website -> "https://weather.cocode.dk/"
    AboutLink.Source -> REPO
    AboutLink.Issues -> "$REPO/issues"
}

fun versionLine(name: String, code: Long): String = "$name ($code)"
