# Weather

An Android weather app for any location — current conditions, the next 24 hours,
and a 7-day forecast. Search for a city or use the device's GPS.

Ported from [Copenhagen Weather](https://github.com/cocodedk/copenhagen-weather-tv),
a Samsung Tizen TV app that showed one hardcoded city. This version keeps that
app's palette, its weather icons and its day-selection model, and adds the two
things a phone needs: arbitrary locations and GPS.

## Website

- [English](https://cocodedk.github.io/weather-android/)
- [Dansk](https://cocodedk.github.io/weather-android/da/)
- [فارسی (Persian)](https://cocodedk.github.io/weather-android/fa/)

All three pages carry a live forecast panel with a working location search — the
same Open-Meteo data the app uses, so you can try the idea before installing
anything.

## Download

[**Download Weather**](https://github.com/cocodedk/weather-android/releases/latest/download/Weather.apk)
— Android 8.0 (API 26) or newer.

## Features

- **Any location.** Type-ahead search over Open-Meteo's geocoder. Country and
  region are shown, so the dozen places that share a name stay tellable apart.
- **Device GPS.** One tap resolves your coordinates and reverse-geocodes them to a
  place name, falling back to coordinates where no geocoder backend exists.
- **Saved locations.** Switch between them; the list persists. The GPS entry
  updates in place instead of stacking a new row per fix.
- **The right local time.** Each location shows its own wall clock and its own
  day/night icons, taken from the API's UTC offset — Tokyo reads as Tokyo even
  from Denmark.
- **Seven days, selectable.** Tap a day and the headline, the six stat tiles and
  the hourly strip all retarget to it.
- **Home screen widget.** Current conditions for the selected location, in the
  app's own artwork. Follows whatever place is selected in the app, refreshes
  every half hour, and taps through to the full forecast.
- **Metric or imperial.** Converted on the client, so switching needs no refetch.
- **Works offline.** The last successful response is cached per location and shown
  with a stale marker when the network is unavailable.
- **Nothing to sign up for.** No API key, no account, no analytics, no ads.

## Build from source

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

Contributors should run `./scripts/install-hooks.sh` after cloning. See
[CONTRIBUTING.md](CONTRIBUTING.md).

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
