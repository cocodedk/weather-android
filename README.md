# Weather

An Android weather app for any location — current conditions, the next 24 hours,
and a 7-day forecast. Search for a city or use the device's GPS.

Ported from [Copenhagen Weather](https://github.com/cocodedk/copenhagen-weather-tv),
a Samsung Tizen TV app that showed one hardcoded city. This version keeps that
app's palette, its weather icons and its day-selection model, and adds the two
things a phone needs: arbitrary locations and GPS.

## Website

- [English](https://weather.cocode.dk/)
- [Dansk](https://weather.cocode.dk/da/)
- [فارسی (Persian)](https://weather.cocode.dk/fa/)

All three pages carry a live forecast panel with a working location search — the
same Open-Meteo data the app uses, so you can try the idea before installing
anything.

## Download

<!-- cocode-apps:install:start -->
- Coming to F-Droid
- [Download the APK from GitHub](https://github.com/cocodedk/weather-android/releases/latest/download/Weather.apk)
- [Auto-update the GitHub APK with Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/cocodedk/weather-android)
<!-- cocode-apps:install:end -->

Android 8.0 (API 26) or newer.

## Features

- **Any location.** Start typing a city name to see matching places. Country and
  region are shown, so you can tell places with the same name apart.
- **Your location.** One tap finds your coordinates and asks Android for a place name.
  If Android has no naming service on your phone, or it fails, the app shows the
  coordinates instead. Location access is optional, and search works without it.
- **Saved locations.** Switch between them; the list is kept on your phone. The entry
  for your own location updates in place instead of adding a new row each time.
- **The right local time.** Each location shows its own local time, with weather icons
  that show daytime or nighttime there. Tokyo reads as Tokyo even from Denmark.
- **Seven days, selectable.** Tap a day and the headline, the six stat tiles and
  the hourly strip all switch to it.
- **Home screen widget.** Current conditions for the selected location, in the
  app's own artwork. Follows whatever place is selected in the app, refreshes
  every half hour, and taps through to the full forecast.
- **Metric or imperial.** Units are converted on your device. Changing units also
  refreshes any installed widgets.
- **Works without a connection.** If the forecast cannot be updated, the app shows
  the last forecast it saved for that place and says when it is from.
- **English and Danish.** The app follows your phone's language.
- **Nothing to sign up for.** No API key, no account, no analytics, no ads.

## Privacy

Weather has no account, no analytics, no ads and no server of its own. What leaves
the phone, as the code stands:

- **Forecasts.** For the selected place (searched, saved or your device location),
  in the app and in the home screen widget, the latitude and longitude, to four
  decimals, go in a plain HTTPS `GET` request to `api.open-meteo.com`.
- **Search.** What you type in the search box (two characters or more) and the
  phone's language code go to `geocoding-api.open-meteo.com`.
- **Naming your location.** Location permission is optional and coarse only; search
  works without it. With it, the app asks Android's location manager for a fix, then
  passes the fix's latitude and longitude to Android's built-in `Geocoder` to get a
  place name. That service is provided by the device, not by the app. The code notes
  that it needs a backend service some devices and ROMs lack, and the app cannot say
  whether or where that service sends the coordinates. If it is missing or fails, the
  app uses the coordinates as the name.
- **Links.** The About screen has buttons that open web pages in your browser: the
  website, the privacy policy, the source code, the issue tracker and the page with
  the latest version. A page opens only when you tap its button, and your browser, not the app,
  contacts it. The app never checks for updates by itself.

Saved places, the selected place, the unit and theme preferences and the last forecast
per place are stored in the app's private storage on the device. The manifest allows
Android's own device backup (`allowBackup`), so if backup is on for the phone, that data
may be included in it.

## Build

Requires JDK 17 and the Android SDK (platform 37, build-tools 36).

```bash
git clone https://github.com/cocodedk/weather-android.git
cd weather-android

export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
./gradlew buildSmoke   # build + unit tests + lint — what CI runs
```

## Contributing

Run `./scripts/install-hooks.sh` after cloning. Setup, branch naming, coding
conventions and the PR checklist are in [CONTRIBUTING.md](CONTRIBUTING.md).
Security reports go through [SECURITY.md](SECURITY.md).

## Architecture

```
app/src/main/java/dk/cocode/weather/
├── domain/     pure Kotlin — WMO codes, units, wall-clock parsing (no Android imports)
├── data/       Open-Meteo clients, device location, DataStore, per-place cache
├── ui/         Compose screen, ViewModel, and the icon set drawn to a Canvas
└── widget/     home screen widget (RemoteViews), reusing the app's icon geometry
website/        GitHub Pages site — plain HTML/CSS/JS, no build step
tools/          selftest.html — exercises the site's JS against the live API
```

| Concern | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Declarative, and the app is one screen with a sheet |
| Networking | `HttpURLConnection` | Ships with Android; the app makes plain GET requests |
| JSON | `org.json` | Ships with Android; no reflection, no codegen |
| Location | `LocationManager` | No Play Services dependency, so it runs on any device |
| Storage | DataStore Preferences | Saved places, unit preference, cached responses |

Two design rules govern most of the code:

**Wall-clock times are parsed as digits, not instants.** Open-Meteo returns times
already localised to the requested place. Parsing them through `Instant`/`Date`
would re-interpret them in the phone's timezone and quietly shift every forecast
for any city you don't live in.

**Missing data renders as `--`, never as a number.** `org.json` returns `NaN` and
`0` where the API means "no value", so `data/Json.kt` collapses those to `null`
before a formatter can turn them into a plausible-looking reading.

## Data

Forecasts and geocoding from [Open-Meteo](https://open-meteo.com), used under
their free non-commercial terms. No API key is required.

## Author

**Babak Bandpey** — [cocode.dk](https://cocode.dk) | [LinkedIn](https://linkedin.com/in/babakbandpey) | [GitHub](https://github.com/cocodedk)

## License

Apache-2.0 | © 2026 [Cocode](https://cocode.dk) | Created by [Babak Bandpey](https://linkedin.com/in/babakbandpey)
