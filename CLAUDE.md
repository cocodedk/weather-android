# CLAUDE.md — Weather (Android)

## Project overview

An Android weather app: current conditions, a 24-hour outlook and a 7-day
forecast, for **any location** — searched by name or taken from the device GPS.

Ported from the `../weather` Samsung Tizen TV app. The visual language, the WMO
icon set, the stat tiles and the day-selection model all come from there; the
location picker, the GPS path and the touch layout are new.

- **Language**: Kotlin, Jetpack Compose (Material 3)
- **Platform**: Android 8.0+ (minSdk 26), compiled and targeted at SDK 35
- **Data**: Open-Meteo forecast + geocoding, no API key, CORS-open
- **Application id**: `dk.cocode.weather` (`.debug` suffix on debug builds)
- **Repo**: `cocodedk/weather-android` · site at `cocodedk.github.io/weather-android/`

---

## Required skills — always invoke these

| Situation | Skill |
|-----------|-------|
| Before any new feature or screen | `superpowers:brainstorming` |
| Planning multi-step changes | `superpowers:writing-plans` |
| Writing or fixing core logic | `superpowers:test-driven-development` |
| First sign of a bug or failure | `superpowers:systematic-debugging` |
| Working on UI or the website | `frontend-design:frontend-design` |
| Website copy, before it ships | `humanizer` (EN), `humanizer-da` (DA) |
| Before completing a feature branch | `superpowers:requesting-code-review` |
| Before claiming any task done | `superpowers:verification-before-completion` |
| After implementing — reviewing quality | `simplify` |

---

## What came from the Tizen app, and what changed

| Tizen (`../weather`) | Here |
|---|---|
| `js/wmo.js` | `domain/Wmo.kt` — same codes, same icon ids |
| `js/units.js` | `domain/Units.kt` — same conversions; day/month names now localised |
| `js/api.js` | `data/ForecastApi.kt` — lat/lon are **arguments**, not constants |
| inline SVG sprite in `index.html` | `ui/icons/` — same 64×64 geometry, drawn to a Canvas |
| `js/nav.js` (D-pad focus engine) | **dropped** — touch needs no focus model |
| `js/render.js` | `ui/components/` — one composable per section |
| hardcoded Copenhagen | `data/GeocodingApi.kt` + `data/DeviceLocation.kt` |

The Tizen project's **Chromium 69 engine ceiling does not apply here.** That rule
exists because a TV's WebView is frozen to its firmware. Nothing in this project
runs in a WebView.

---

## The rules that matter here

### 1. Wall-clock times are parsed as digits, never as instants

Open-Meteo returns local wall-clock strings (`"2026-07-26T22:15"`) already in the
requested location's timezone. `Units.parseLocal` reads the digits directly rather
than going through `Instant`/`Date`.

This was worth doing on the TV; here it is **load-bearing**. The user can select a
city in any timezone, and its forecast must read in *that city's* local time on a
phone that never leaves Denmark. `PlaceHeader` likewise derives the location's
clock from the API's `utc_offset_seconds`, not from the device zone.

Verified: Tokyo showed 18:41 with moon icons while the phone's status bar read
11:41.

### 2. Missing data must render as `--`, never as a number

`org.json` returns `NaN`/`0` where the API means "no value". `data/Json.kt`
collapses every one of those to a Kotlin `null` so the formatters print `--`. Use
those helpers — never `optDouble`/`optInt` directly.

### 3. Location is always optional

The app must stay fully usable with location permission denied, location services
switched off, or no fix available. Search is the always-available path;
`DeviceLocation` degrades through: fresh fix → stale cached fix → helpful error.

### 4. The widget draws the app's icons, it does not copy them

A widget cannot host a Composable and RemoteViews only takes bitmaps or
drawables. Rather than maintain a parallel set of vector drawables that would
quietly drift, `WidgetIcons` runs the app's own `drawWeatherIcon` through a
`CanvasDrawScope` off-screen and hands the pixels to `setImageViewBitmap`.

That is why `drawWeatherIcon` is `internal` rather than `private`. If you make it
private again, the widget loses its artwork.

### 5. `website/js/wx.js` and `domain/` must be kept in step by hand

The Tizen project could copy its shared modules into the site verbatim, because
both sides were JavaScript. Across Kotlin and JS that is impossible, so
`website/js/wx.js` is a deliberate re-implementation of `domain/Wmo.kt`,
`domain/Units.kt`, `data/ForecastApi.kt` and `data/GeocodingApi.kt`.

**Change a condition label, a rounding rule or a request parameter on one side and
you must change the other.** `tools/selftest.html` runs the JS half against the
live API and will catch a broken request, but nothing catches a *divergence* —
only care does.

---

## Architecture

```
domain/          pure Kotlin, no Android imports, no DOM equivalent
  Wmo.kt         WMO code -> label + icon + UV band
  Units.kt       metric/imperial conversion, wall-clock parsing, date formatting

data/            IO and persistence
  Models.kt          Place, Current, HourRow, DayRow, Forecast
  Http.kt            HttpURLConnection GET (no networking library)
  Json.kt            null-safe org.json accessors — see rule 2
  ForecastApi.kt     Open-Meteo forecast; column arrays -> row objects
  GeocodingApi.kt    place search
  DeviceLocation.kt  LocationManager + reverse geocode, all failures typed
  WeatherStore.kt    DataStore: saved places, prefs, per-place response cache
  PlaceJson.kt       explicit (non-reflective) Place serialisation
  ForecastRepository.kt  fetch, else fall back to that place's cache

ui/
  WeatherViewModel.kt  state, selection, refresh, permission signalling
  PlaceSearch.kt       debounced type-ahead, isolated from forecast state
  WeatherUiState.kt    one immutable snapshot the screen draws
  WeatherScreen.kt     scaffold + body
  components/          Hero, StatsGrid, HourlyStrip, DailyList, LocationSheet, …
  icons/               the Tizen SVG sprite, as Canvas draw calls
  theme/Theme.kt       the two Tizen palettes + gradient

widget/            home screen widget
  WeatherWidgetProvider.kt  AppWidgetProvider; goAsync() + coroutine to fetch
  WidgetViews.kt            builds the RemoteViews (pure presentation)
  WidgetIcons.kt            drives the app's drawWeatherIcon into a Bitmap

website/           GitHub Pages site — plain HTML/CSS/JS, no build step
  index.html         English; da/ and fa/ are the other two languages
  styles.css         one stylesheet for all three, with [dir="rtl"] overrides
  js/wx.js           JS mirror of domain/ + the API clients — see rule 5
  js/live.js         the live panel, location search, and page atmosphere
  js/sprite.js       the icon sprite, injected so one copy serves every page
  js/i18n/{da,fa}.js translated strings; English is what wx.js already speaks

tools/selftest.html  runs wx.js against the live API — not deployed
scripts/             install-hooks.sh, setup-repo.sh, setup-signing.sh
```

**The site's signature behaviour**: the background gradient follows the searched
location's day/night and the accent glow follows its condition. It is the product's
claim made literal, so keep it — a change that makes the page static loses the
point of the page.

**Layer rules**
- `domain/` has no Android imports. Keep it that way — `Units` takes `use24Hour`
  as a parameter rather than reaching for `DateFormat`.
- The ViewModel never shows UI. It raises `permissionRequest`; `MainActivity`
  owns the result contract and launches the dialog.
- `ui/icons/` knows nothing about weather codes; `domain/Wmo` knows nothing about
  drawing.

---

## Coding conventions

- [ ] Files stay under **200 lines**; extract when one grows past it.
- [ ] Comments say *why*. Several record an API or platform quirk and will look
      like mistakes without the explanation.
- [ ] No dependency that is not already in the local Gradle cache without a
      deliberate decision — the build currently resolves offline.
- [ ] DRY / SOLID / KISS / YAGNI. Delete dead code immediately.
- [ ] Never widen the permission set. Coarse location is enough for a forecast.

---

## Commands

The environment needs both of these; there is no system JDK on PATH.

```bash
export JAVA_HOME=/home/agent/jdk/jdk-17.0.19+10
export ANDROID_HOME=/home/agent/android-sdk
ADB=$ANDROID_HOME/platform-tools/adb

./gradlew buildSmoke                    # build + tests + lint — CI and pre-push
./gradlew assembleDebug                 # build only
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell am start -n dk.cocode.weather.debug/dk.cocode.weather.MainActivity
$ADB exec-out screencap -p > shot.png   # verify visually — do this

python3 -m http.server -d website 8099  # the site, at localhost:8099
./scripts/install-hooks.sh              # once per clone; hooks are not committed
```

Re-render the OG image after any change to `website/og-image.html`:

```bash
google-chrome-stable --headless --disable-gpu --no-sandbox \
  --window-size=1200,630 --virtual-time-budget=8000 \
  --screenshot=website/og.png "file://$PWD/website/og-image.html"
```

## Key files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | This file — conventions and session startup |
| `.github/workflows/ci.yml` | `buildSmoke` on every PR and branch; job name `verify` |
| `.github/workflows/release-apk.yml` | Manual signed release; version comes from the latest `v*` tag |
| `.github/workflows/deploy-pages.yml` | Deploys `website/` on push to main |
| `.githooks/pre-push` | Owner-lock to `cocodedk`, force-push guard, full `buildSmoke` |
| `scripts/setup-repo.sh` | Branch protection — run once after the first CI run |
| `scripts/setup-signing.sh` | Generates the keystore and uploads the 4 signing secrets |

## Verifying a change

There is no test suite yet, so **screenshot the running app** rather than assuming
a green build means a correct screen. Two defects in the first build compiled
perfectly and were only visible in a screenshot: the header sat under the system
status bar, and the hero icon tinted a rain cloud gold.

Check GPS with `adb shell settings get secure location_mode` first — `0` means
location is off device-wide and the GPS path will (correctly) show its error.
