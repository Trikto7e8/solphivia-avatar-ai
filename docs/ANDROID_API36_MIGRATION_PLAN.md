# Android API 36 Migration Plan

Related issue: #3

## Scope

Propose a small migration from compileSdk/targetSdk 35 to 36.
This document is a plan, not evidence of API 36 compatibility.

No application source changes, permissions, or service types are
modified by this proposal.

## Current baseline

- Version: 0.9.2-alpha2
- minSdk: 26
- compileSdk: 35
- targetSdk: 35
- Android Gradle Plugin: 8.9.1
- Gradle wrapper: 8.11.1
- Java: 17
- API 35 debug compilation: verified successful
- API 36 SDK platform and Build Tools: installed locally
- API 36 compilation and runtime behavior: not yet tested

## Proposed build migration

1. Keep minSdk 26 and Java 17 unchanged.
2. Update compileSdk and targetSdk to 36.
3. Review and update Android Gradle Plugin compatibility, initially
   evaluating AGP 8.10.x with the existing Gradle wrapper.
4. Run a clean debug build and Android lint.
5. Inspect the generated APK metadata and compare with API 35.
6. Separate build-tool warnings from actual compilation failures.

Exact AGP and dependency changes should be validated before merging.

## Source-inspection findings

### Floating avatar

PetService creates a TYPE_APPLICATION_OVERLAY window after checking
Settings.canDrawOverlays(). It starts a specialUse foreground service
and shows an ongoing notification.

Its onDestroy() removes callbacks, stops animations and workers,
releases TextToSpeech, removes the overlay view, and stops foreground
operation.

Test permission denial and revocation, start/stop behavior, overlay
dragging, screen rotation, app backgrounding, and process recreation.

### Foreground services

AndroidManifest.xml declares FOREGROUND_SERVICE and
FOREGROUND_SERVICE_SPECIAL_USE.

PetService and OAuthKeepAliveService are non-exported and use
foregroundServiceType=specialUse with documented subtypes.

Verify foreground-service launch eligibility, notification visibility,
service startup and shutdown, and Android 16 restrictions.

### OAuth authorization

OpenAiChatClient starts OAuthKeepAliveService while the browser
authorization and local loopback callback are active. The normal
worker completion and caught startup failure paths call stopService().

The callback socket has a 900-second timeout. Test success, user
cancellation, failure, timeout, app backgrounding, and process death.
Do not claim all exceptional paths are verified from source review.

### Voice

CompanionInputActivity launches external Android speech recognition
using RecognizerIntent.ACTION_RECOGNIZE_SPEECH.

PetService and other classes use Android TextToSpeech. No direct
AudioRecord or MediaRecorder use was found in the reviewed search.

Do not add RECORD_AUDIO without confirming a direct microphone
capture requirement. Test recognition availability, cancellation,
returned text, speech playback, and TTS engine shutdown.

### Permissions

The manifest requests SYSTEM_ALERT_WINDOW, INTERNET, location,
foreground-service permissions, and POST_NOTIFICATIONS.

Test permission grant/denial paths and notification behavior.
Review whether each requested permission remains justified.

## Validation matrix

- [ ] Build API 36 debug APK using JDK 17
- [ ] Run Gradle lint and review findings
- [ ] Verify APK compile/target SDK metadata
- [ ] Install and launch on Android 16 physical device or emulator
- [ ] Test overlay permission denial, grant, and revocation
- [ ] Test foreground-service start, stop, and notifications
- [ ] Test app backgrounding and process recreation
- [ ] Test OAuth success, cancellation, failure, and timeout
- [ ] Test speech recognition and TextToSpeech
- [ ] Test file picker and location-permission flows
- [ ] Record device model, Android version, time zone, and results

## Out of scope for this initial migration

- Full foreground-service architecture rewrite
- OAuth protocol redesign
- New voice-recording implementation
- Release signing keys or production credentials
- Google Play approval claims

## Proposed small PR sequence

1. Agree on this migration and device-test scope.
2. Make minimal SDK/build-tool version changes in a separate PR.
3. Run build and lint; fix only demonstrated compatibility failures.
4. Test on devices and document actual results.
5. Handle release AAB, policy disclosures, and store readiness
   as separate follow-up work.

No implementation or runtime compatibility is claimed here.
