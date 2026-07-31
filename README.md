# ScreenBrightness

[![CI](https://github.com/elfefe/ScreenBrightness/actions/workflows/ci.yml/badge.svg)](https://github.com/elfefe/ScreenBrightness/actions/workflows/ci.yml)

An Android application that dims the screen below the system minimum, using a
colour overlay drawn on top of everything else.

The system brightness slider stops at a level that is still too bright in a dark
room. This app goes further by drawing a translucent, tinted layer over the
screen — so it can also warm the colours to cut blue light.

<img src="media/exemple.gif" width="254">

## Features

*   **Dimming** — adjust how dark the overlay is, from a slider or from the
    notification.
*   **Colour filters** — pick a tint from a colour wheel, or one of the
    presets (beige, warm, cool, soft green, dark grey).
*   **Scheduling** — turn the overlay on and off at a chosen time, on chosen
    days of the week.
*   **Persistent notification** — dim, brighten or toggle the overlay without
    opening the app.

Available in English and French.

## Requirements

The overlay needs the **"Display over other apps"** permission
(`SYSTEM_ALERT_WINDOW`), which Android only grants from its settings screen —
the app opens it on first launch. Scheduling additionally needs the
**alarms & reminders** permission on Android 12 and above.

Minimum supported version is Android 8.0 (API 26).

## Building

You need **JDK 17** and an Android SDK with **API 37** installed. The Gradle
wrapper takes care of Gradle itself.

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. To install it on a connected
device:

```bash
./gradlew installDebug
```

### Firebase configuration

Crash reporting uses Firebase Crashlytics, so the build needs a
`google-services.json` file. It is deliberately **not** committed — it is listed
in `.gitignore`.

To obtain it:

1.  Open the [Firebase console](https://console.firebase.google.com/) and select
    the `screenbrightness-67c1c` project.
2.  Add an Android app with the package name `com.elfefe.screenbrightness`, or
    open the existing one.
3.  Download `google-services.json` and place it in `app/`.

Without this file the build stops on `:app:processDebugGoogleServices` with
`File google-services.json is missing`.

### AdMob identifiers

The app can show a **voluntary** rewarded ad, from the *Watch an ad* entry in
the top-right menu. Nothing is ever shown on its own, and consent is collected
first through Google's User Messaging Platform.

Debug builds always use Google's public demo identifiers, so you can never
click your own live ads while developing — that is a common way to get an
AdMob account suspended.

Release builds read the real identifiers from `local.properties`, which is not
tracked by git:

```properties
admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.rewardedId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

If those keys are absent, the release build falls back to the demo identifiers
too, so the project always compiles.

## Continuous integration

Every push and pull request runs `assembleDebug`, `testDebugUnitTest` and
`lint` on GitHub Actions. Reports are attached to each run as artifacts, kept
for 14 days — including when the build fails, which is when you need them.

The workflow needs a repository secret named **`GOOGLE_SERVICES_JSON`**, holding
the base64 of `app/google-services.json`:

```bash
base64 -w0 app/google-services.json | gh secret set GOOGLE_SERVICES_JSON
```

A fork without that secret fails on the *Configuration Firebase* step, with a
message pointing back here.

## Project layout

Everything lives in a single `app` module.

| Path | What it holds |
|---|---|
| `MainActivity.kt` | permissions, app entry point |
| `OverlayService.kt` | the foreground service drawing the overlay |
| `Color.kt` | colour model and its packed `Long` encoding |
| `AlarmScheduler.kt`, `AlarmReceiver.kt`, `BootReceiver.kt` | scheduling |
| `views/` | the Compose screens |

`DIAGNOSTIC.md` records the current state of the project, including the parts
that are known not to work yet.

## Technologies

*   Kotlin
*   Jetpack Compose, Material 3
*   Firebase Crashlytics

## Privacy

See [privacy.md](privacy.md).

## License

Apache License 2.0 — see [LICENSE-2.0.txt](LICENSE-2.0.txt).
