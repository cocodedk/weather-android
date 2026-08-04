# Contributing to Weather

## Local setup

1. Install **JDK 17** and the **Android SDK** (platform 37, build-tools 36).
   Platform 37 is not optional — `compileSdk` is 37 and the build will not
   configure without it.
2. Point the build at them:
   ```bash
   export JAVA_HOME=/path/to/jdk-17
   export ANDROID_HOME=/path/to/android-sdk
   echo "sdk.dir=$ANDROID_HOME" > local.properties
   ```
3. Install the git hooks — they are not active in a fresh clone until you do:
   ```bash
   ./scripts/install-hooks.sh
   ```

## Recommended git config

Run these once after cloning:

```bash
git config pull.rebase true          # rebase on pull instead of a merge commit
git config core.autocrlf input       # normalize CRLF to LF on commit (macOS/Linux)
git config push.autoSetupRemote true # push without needing -u the first time
```

Windows contributors: use `core.autocrlf true`.

## Build and test

```bash
./gradlew buildSmoke      # what CI runs and what pre-push enforces
./gradlew assembleDebug   # debug APK
./gradlew lintDebug       # lint only (what pre-commit runs)
```

Install and launch on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dk.cocode.weather.debug/dk.cocode.weather.MainActivity
```

## The website

`website/` is plain HTML, CSS and JS with no build step. Serve it locally:

```bash
python3 -m http.server -d website 8099
```

`tools/selftest.html` exercises `website/js/wx.js` against the live Open-Meteo API
— open it through the same server (`http://localhost:8099/../tools/selftest.html`
will not work; copy it into `website/` temporarily, or serve the repo root).

Check every change at **360px, 768px and 1280px in all three languages**. Persian
is RTL, so it mirrors margins and padding, not just text — it needs its own look.

## What the hooks enforce

| Hook | Runs |
|---|---|
| `pre-commit` | `./gradlew lintDebug` — fast, so the commit loop stays quick |
| `commit-msg` | Conventional Commits format |
| `pre-push` | Owner-lock to `github.com/cocodedk`, force-push guard on `main`, then the full `buildSmoke` |

`git push --no-verify` bypasses the pre-push hook by design. It stops accidents,
not malice. Note that `core.hooksPath` is per-checkout and is not committed —
a fresh clone has no hooks until `./scripts/install-hooks.sh` is run.

## Branch naming

Kebab-case, prefixed to match the Conventional Commit type:

| Prefix | Commit type | Example |
|---|---|---|
| `feature/` | `feat:` | `feature/add-hourly-wind` |
| `fix/` | `fix:` | `fix/night-icon-at-midnight` |
| `chore/` | `chore:` | `chore/bump-compose-bom` |
| `docs/` | `docs:` | `docs/clarify-gps-fallback` |
| `refactor/` | `refactor:` | `refactor/extract-day-window` |
| `ci/` | `ci:` | `ci/cache-gradle` |

Never commit directly to `main` — open a PR.

## Coding conventions

Read `CLAUDE.md` first. The two rules that catch people out:

- **Wall-clock times are parsed as digits, never as instants.** Open-Meteo returns
  times already localised to the requested place; putting them through
  `Instant`/`Date` re-reads them in the phone's zone and silently shifts every
  forecast for a city you don't live in.
- **Missing data renders as `--`, never as a number.** Use the helpers in
  `data/Json.kt`; `optDouble` returns `NaN` where the API meant "no value".

Files stay under 200 lines. Comments explain *why*.

## PR checklist

- [ ] `./gradlew buildSmoke` passes
- [ ] Change checked by eye on a device or in a browser — a green build has twice
      failed to catch a layout bug in this project
- [ ] Docs updated if behaviour changed
