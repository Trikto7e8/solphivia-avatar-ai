# Android API 36 migration plan

Related issue: #3. Original proposal by @sunny-kavixo; refreshed by the maintainer on 24 October 2026 for alpha11.

**Plan only.** This document authorizes a focused migration proposal; no SDK, permission, dependency or application source is changed here. No API 36 build/runtime compatibility or Google Play readiness is claimed.

## Current baseline

- Alpha11 (44), release source commit `f9f65419a9c08459ac7bad95ef8d6bf8f6e03723`; [current release](CURRENT_RELEASE.md).
- minSdk 26; compileSdk/targetSdk 35; JDK 17; AGP 8.9.1; Gradle 8.11.1.
- Vosk 0.3.75 and JNA 5.18.1 native dependencies; bundled Italian wake model; ARM64/ARMv7 builds.
- Build, 38 JVM tests and four isolated Android suites passed; Lint has 0 errors and 118 warnings. The maintainer confirms transcript preview and continued voice conversation on Moto g35 5G / Android 16, **with targetSdk 35**. That is not proof of targetSdk 36 behavior.
- The older contributor's Mac build is retained as a [historical alpha2 baseline](ANDROID_BUILD_BASELINE.md).

## Proposed minimal build slice

1. Preserve minSdk 26, JDK 17, application ID and the tested alpha11 behavior.
2. Propose exact AGP/SDK versions, with compileSdk/targetSdk 36. The official [AGP 8.10 compatibility table](https://developer.android.com/build/releases/agp-8-10-0-release-notes) supports API 36 with Gradle 8.11.1 and JDK 17. Choose a specific compatible patch version in the implementation PR and validate it; this is not an instruction to upgrade blindly to a newest version.
3. Build the isolated APK/instrumentation package, run Lint, inspect generated manifest/APK SDK/ABI/version metadata, and compare against alpha11.
4. Verify native Vosk/JNA ELF and APK alignment and run on a 16 KB environment under the official [16 KB page-size guide](https://developer.android.com/guide/practices/page-sizes). A successful ordinary 4 KB-device run cannot establish native compatibility.
5. Fix only demonstrated migration failures in this slice. Keep signing keys, production AAB and store declarations separate.

## Runtime areas requiring migration checks

### Floating avatar and foreground services

`PetService` is a non-exported `specialUse` foreground overlay service. `OAuthKeepAliveService` is a separate non-exported `specialUse` service. `VoiceSessionService` is a non-exported **microphone** foreground service, using `RECORD_AUDIO` and `FOREGROUND_SERVICE_MICROPHONE` in addition to the shared foreground permission.

Test foreground launch eligibility, notification visibility, background restrictions, teardown and process recreation. Check overlay grant/denial/revocation, dragging, rotation and internal-chat ownership: there must be one visible avatar, not overlapping internal/floating copies. Test actual Android 16 target changes, not just API 35 behavior on an Android 16 phone.

### Voice and privacy

Alpha11 uses direct `AudioRecord` for continuous local wake decoding. The existing microphone permission is therefore justified; do not remove it based on the earlier alpha2 external-dictation-only inspection. After the greeting, Android `SpeechRecognizer` transcribes questions and natural replies, including the five-second follow-up window after each response, and may use the network. Partial results appear in the composer when supported. `CompanionInputActivity` still provides a separate external editable dictation option. TTS uses the selected Android engine.

Check permission denial/revocation, local wake, spoken reply, command handoff, TTS pause/resume, recognition errors/silence, explicit Stop dialogo and Spegni microfono, reopen behavior, and no automatic recording at boot. Check process-death recovery separately from explicit stop. Screen lock, noisy environments and long battery sessions remain unverified and must be reported as such until tested. Do not reintroduce repeated Android recognition starts during local wake waiting.

Voice requests automatically send recent text history to the configured provider, without adding vault memory or file payloads/references. Typed requests retain editable single-use memory consent; pending typed attachments remain selected. Preserve both app and overlay callers. Explicit app/Maps/dialer commands are handled locally; calls require manual confirmation; do not add message sending or screen reading as part of this migration.

### OAuth, files and location

The OAuth helper keeps the browser/loopback callback alive; the socket timeout is 900 seconds. Check success, cancellation, failure, timeout, app backgrounding and process death. Existing normal completion paths do not prove every exceptional path correct.

Verify file-picker grant/cancel, persistent URI failures and byte limits, location grant/deny, notifications and optional weather/radar flows without collecting private data. Keep unrelated architecture changes out of scope.

## Acceptance matrix for the implementation PR

- [ ] Exact AGP/SDK versions and official compatibility basis recorded.
- [ ] API 36 isolated debug APK and instrumentation APK build.
- [ ] Lint completed and findings assessed.
- [ ] Generated SDK/ABI/permission metadata inspected.
- [ ] Native dependencies and APK alignment checked; 16 KB runtime tested or explicitly blocked.
- [ ] Physical-device/emulator targetSdk 36 behavior documented with OS, model, date/time/timezone.
- [ ] Overlay ownership, permissions, background/foreground service and notification tests.
- [ ] Wake → spoken reply → transcript/command → follow-up window → local wait; TTS/error/silence and manual-draft protection cases.
- [ ] Stop dialogo, microphone-off, reopen, process-death recovery and no boot recording.
- [ ] Voice omission of vault memory/files; typed consent/attachment behavior unchanged.
- [ ] OAuth success/cancel/failure/timeout; file and location permission flows.
- [ ] Screen-lock and battery results explicitly measured or marked not tested.

## Agreed next step and limits

A contributor may prepare one small implementation PR for SDK/toolchain changes and this validation plan, based on current main. Discuss additional refactors separately. Existing alpha11 remains the working reference until the migration has passed its applicable checks.

Release signing, AAB packaging, Play policy/privacy forms, iOS and additional model providers remain separate tasks. No credentials, personal recordings or private device screenshots are needed.
