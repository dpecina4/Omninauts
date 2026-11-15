# Cavernauts Android project

Cavernauts is a native Android application built with Kotlin and Jetpack Compose. It is not a web app, so Capacitor is not used: the existing Android app is built directly with Gradle. Firebase configuration and the existing Android project structure are preserved.

## Build locally

Prerequisites: Java 21 and the Android SDK Platform 36.1 / Build Tools 36.1.0.

1. Open this folder in Android Studio, or use a terminal.
2. Accept Android SDK licenses and install the SDK packages listed above.
3. Run `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`). The included launcher uses an installed Gradle 9.3.1 when available, or downloads and verifies the pinned Gradle distribution.
4. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions workflow creates a temporary root-level debug signing key. For a local build, create that file once with `keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"`. The key is ignored by Git; do not commit a personal signing key.

## GitHub Actions

The workflow at `.github/workflows/android.yml` runs on pushes and pull requests to `main` / `master`, and can also be started manually from the Actions tab. It installs Java and Android SDK dependencies, runs `./gradlew assembleDebug`, and uploads `app-debug.apk` as the `cavernauts-debug-apk` artifact for 14 days.

## Prepare Squad interaction

The sector card now uses the explicit **Prepare Squad** button as its launch target, and the squad selection screen is displayed in a platform dialog. A Compose UI regression test checks that the button opens deployment and that confirming the squad invokes the expedition callback.
