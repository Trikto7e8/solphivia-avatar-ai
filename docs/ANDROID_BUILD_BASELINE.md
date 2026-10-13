# Android Debug Build Baseline

Related issue: #3

## Verified environment

- Date: 2026-10-14
- Approximate APK generation time: 00:39 IST (UTC+05:30); this is a file timestamp, not an independently recorded build start time
- Source commit: 71409324733826d5a5e03bc992ab1e06b075bc15
- Host: Apple Silicon Mac, macOS 27.0.1, aarch64
- JDK: Eclipse Temurin 17.0.20.1
- Gradle: 8.11.1
- Android SDK Platform: 35
- Build Tools: 35.0.0
- Platform Tools: 37.0.1

## Reproduce

Install JDK 17 and Android SDK Platform 35 with Build Tools 35.0.0.

From the repository root, run:

    JAVA_HOME=$(/usr/libexec/java_home -v 17) ANDROID_HOME="$HOME/Library/Android/sdk" ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" bash ./gradlew :app:assembleDebug --no-daemon

## Observed result

- BUILD SUCCESSFUL in 1m 10s
- 32 actionable tasks executed
- Generated APK: app/build/outputs/apk/debug/app-debug.apk
- Approximate APK size: 27 MB
- Package: it.solphivia.mobile
- Version: 0.9.2-alpha2 (versionCode 31)
- minSdk: 26
- targetSdk: 35
- compileSdk: 35

APK SHA-256:

    68041d0efabe150b869329e093732058a6fc3a1b4de15fad97c058b196e056ec

APK metadata was inspected using Android Build Tools aapt.

## Troubleshooting

The initial Gradle 8.11.1 distribution download failed with a 10-second timeout. Temporarily increasing networkTimeout in gradle/wrapper/gradle-wrapper.properties from 10000 to 120000 allowed the download and subsequent build to succeed. The configuration change was reverted afterward.

The checked-in Gradle wrapper lacked execute permission on the test machine. Running bash ./gradlew worked without modifying repository permissions.

Non-blocking warnings included an SDK XML parser compatibility warning and deprecated Java API usage.

## Not verified

- Installation and runtime behavior on an Android device
- Floating avatar and overlay lifecycle
- Foreground service and background restrictions
- Microphone permissions and voice features
- Instrumentation and end-to-end tests
- Signed release AAB and Google Play readiness

This report confirms a debug compilation baseline, not release readiness.

## Next checkpoint

Agree on an API 36 migration scope and perform build, lint, and device testing before making production release claims.
