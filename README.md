# ScreenBrightness

An Android application for managing screen brightness.

## Description

This application allows users to control their screen brightness, schedule brightness changes, and apply color filters.

## Features

*   **Brightness Control:** Adjust the screen brightness using a slider.
*   **Scheduling:** Schedule automatic brightness changes at specific times.
*   **Color Filters:** Apply color filters to the screen.
*   **Overlay Service:** Uses an overlay service to apply brightness and color changes.
*   **Ads:** Includes ad support.

## Visuel

<img src="media/exemple.gif" width="254">

## Building

The project needs JDK 17 and an Android SDK with API 36 installed. The Gradle
wrapper takes care of Gradle itself.

```bash
./gradlew assembleDebug
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

Without this file the build stops on `:app:processDebugGoogleServices`.

## Technologies Used

*   Kotlin
*   Android SDK
*   Jetpack Compose
*   Firebase Crashlytics

## License

[License](LICENSE-2.0.txt)
