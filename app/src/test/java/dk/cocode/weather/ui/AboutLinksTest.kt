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
            aboutUrl(AboutLink.Updates, "en", liveOnFdroid = false, privacyUrl = null),
        )
    }

    @Test
    fun updateButtonOpensTheFdroidPageOnceLive() {
        assertEquals(
            "https://f-droid.org/packages/dk.cocode.weather/",
            aboutUrl(AboutLink.Updates, "en", liveOnFdroid = true, privacyUrl = null),
        )
    }

    @Test
    fun privacyLinkIsPresentOnlyWhenThereIsAPolicyPage() {
        assertEquals(privacy, aboutUrl(AboutLink.Privacy, "en", liveOnFdroid = false, privacyUrl = privacy))
        assertNull(aboutUrl(AboutLink.Privacy, "en", liveOnFdroid = false, privacyUrl = null))
    }

    @Test
    fun englishOpensTheEnglishPages() {
        assertEquals("https://weather.cocode.dk/", aboutUrl(AboutLink.Website, "en"))
        assertEquals(privacy, aboutUrl(AboutLink.Privacy, "en"))
    }

    @Test
    fun danishOpensTheDanishPages() {
        assertEquals("https://weather.cocode.dk/da/", aboutUrl(AboutLink.Website, "da"))
        assertEquals("https://weather.cocode.dk/da/privacy/", aboutUrl(AboutLink.Privacy, "da"))
    }

    @Test
    fun aLanguageTheSiteLacksOpensTheEnglishPages() {
        // The site has a Persian home page but no Persian privacy page, so Persian stays on English;
        // German has no pages at all.
        for (language in listOf("fa", "de")) {
            assertEquals("https://weather.cocode.dk/", aboutUrl(AboutLink.Website, language))
            assertEquals(privacy, aboutUrl(AboutLink.Privacy, language))
        }
    }

    @Test
    fun sourceAndIssuesDoNotDependOnTheOtherSettings() {
        for (language in listOf("en", "da", "fa")) {
            for (live in listOf(false, true)) {
                for (policy in listOf(null, privacy)) {
                    assertEquals(
                        "https://github.com/cocodedk/weather-android",
                        aboutUrl(AboutLink.Source, language, live, policy),
                    )
                    assertEquals(
                        "https://github.com/cocodedk/weather-android/issues",
                        aboutUrl(AboutLink.Issues, language, live, policy),
                    )
                }
            }
        }
    }

    @Test
    fun versionLineShowsNameAndCode() {
        assertEquals("0.1.7 (1008)", versionLine("0.1.7", 1008))
    }
}
