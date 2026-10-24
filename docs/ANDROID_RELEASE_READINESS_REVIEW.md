# Android Release Readiness — Initial Review

**Historical initial review.** This inspection describes commit `47be7626601a9c08dbea74c1372a78c879197b59`, not current alpha11. For the current build/device results use [alpha11 notes](releases/v0.9.2-alpha11.md) and [current version](CURRENT_RELEASE.md). Current source also has a microphone foreground service and local command/follow-up/transcript paths. The original unchecked checklist below is preserved as evidence of that inspection; it does not negate later build/device results or establish store readiness.

Related issue: #3

## Review scope and evidence

- Repository commit: `47be7626601a9c08dbea74c1372a78c879197b59`
- Platform: MacBook Air M4, macOS
- Review type: static source and configuration inspection
- Reviewed: Gradle configuration, AndroidManifest.xml, MainActivity.java, PetService.java, OAuthKeepAliveService.java, OpenAiChatClient.java, README.md
- Android build: NOT RUN — Android SDK unavailable locally
- Emulator/device testing: NOT RUN
- Security assessment: preliminary; no verified vulnerability claimed

## Findings and priorities

### P0 — Android API migration

`app/build.gradle` specifies `compileSdk 35` and `targetSdk 35`.

Proposed next step:
1. Confirm the applicable Google Play target API requirement.
2. Update compile/target SDK in a separate PR.
3. Compile, run Android Lint, and test behavioral changes.
4. Document any required dependency/toolchain upgrades.

Changing the SDK version alone does not establish Play readiness.

### P0 — Foreground services and overlay lifecycle

The manifest declares `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE_SPECIAL_USE`, and two `specialUse` services.

`PetService` checks overlay permission, starts a foreground notification, and has a shutdown path. `OAuthKeepAliveService` also enters foreground mode.

Required verification:
- Overlay permission denied or revoked
- Notification permission denied
- User closes overlay or stops service
- App backgrounding and process recreation
- Android foreground-service start restrictions
- Justification and Play declarations for both special-use services

These are review risks, not confirmed defects.

### P1 — OAuth lifecycle and credential handling

`OpenAiChatClient` implements a loopback OAuth callback, PKCE challenge generation, and encrypted token storage using Android Keystore / AES-GCM.

Cleanup includes calls to `stopService` on completion/error paths.

Required verification:
- Browser cancellation and callback timeout
- Activity destruction during authorization
- Worker failure or rejected execution
- Interrupted process and service cleanup
- Credential revocation, token refresh, and error handling

No live credentials were used in this review.

### P1 — Google Play release preparation

Required follow-up work:
- Reproducible signed AAB build instructions; keys outside Git
- Privacy policy and accurate Data safety disclosures
- AI-response reporting/flagging mechanism
- Permission and foreground-service declarations
- Accessibility evaluation
- Store listing, content rating, and review evidence
- Testing requirements appropriate to the developer account

### P1 — Release testing matrix

Proposed checks:
- Supported minimum Android version (API 26)
- Android 13, 14, 15, and 16
- Overlay permission granted, denied, revoked
- Notifications enabled and disabled
- Foreground/background transitions
- OAuth success, cancellation, timeout, network loss
- Speech, attachments, memory, and accessibility
- Debug APK and signed release AAB verification

## Verification checklist

- [x] Pinned local and upstream commit inspected
- [x] Gradle and SDK settings inspected
- [x] Manifest permission/service declarations inspected
- [x] Foreground-service call sites inspected
- [x] OAuth lifecycle and Keystore implementation inspected
- [ ] JDK 17 environment verified
- [ ] Android debug build executed
- [ ] Android Lint executed
- [ ] Instrumentation tests executed
- [ ] Physical-device testing performed
- [ ] Signed AAB generated or verified
- [ ] Google Play submission requirements validated

Unchecked items must not be interpreted as passing.

## Proposed follow-up sequence

1. Independently reproduce the documented build and establish a clean test baseline.
2. Migrate to the applicable target SDK and run compatibility tests.
3. Address verified overlay/foreground-service lifecycle failures.
4. Add reproducible release signing instructions without credentials.
5. Complete privacy, accessibility, AI reporting, and Play declarations.
6. Test release candidates on a documented device matrix.

Each implementation change should be discussed on Issue #3 and submitted as a focused PR.

## Limitations

This report is an initial static review, not a certification, penetration test, build verification, or evidence of Google Play approval. Findings must be updated after executable tests and maintainer review.
