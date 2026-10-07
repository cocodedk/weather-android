package dk.cocode.weather.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutLinksTest {
    private val privacy = "https://weather.cocode.dk/privacy/"

    @Test
    fun updateButtonOpensTheLatestGithubReleaseUntilLiveOnFdroid() {
        assertEquals(
            "https://github.com/cocodedk/weather-android/releases/latest",
            aboutUrl(AboutLink.Updates, liveOnFdroid = false, privacyUrl = null),
        )
    }

    @Test
    fun updateButtonOpensTheFdroidPageOnceLive() {
        assertEquals(
            "https://f-droid.org/packages/dk.cocode.weather/",
            aboutUrl(AboutLink.Updates, liveOnFdroid = true, privacyUrl = null),
        )
    }

    @Test
    fun privacyLinkIsPresentOnlyWhenThereIsAPolicyPage() {
        assertEquals(privacy, aboutUrl(AboutLink.Privacy, liveOnFdroid = false, privacyUrl = privacy))
        assertNull(aboutUrl(AboutLink.Privacy, liveOnFdroid = false, privacyUrl = null))
    }

    @Test
    fun theAppShipsWithTheSitesPrivacyPage() {
        assertEquals(privacy, aboutUrl(AboutLink.Privacy))
    }

    @Test
    fun websiteSourceAndIssuesDoNotDependOnTheOtherSettings() {
        for (live in listOf(false, true)) {
            for (policy in listOf(null, privacy)) {
                assertEquals("https://weather.cocode.dk/", aboutUrl(AboutLink.Website, live, policy))
                assertEquals(
                    "https://github.com/cocodedk/weather-android",
                    aboutUrl(AboutLink.Source, live, policy),
                )
                assertEquals(
                    "https://github.com/cocodedk/weather-android/issues",
                    aboutUrl(AboutLink.Issues, live, policy),
                )
            }
        }
    }

    @Test
    fun versionLineShowsNameAndCode() {
        assertEquals("0.1.7 (1008)", versionLine("0.1.7", 1008))
    }
}
