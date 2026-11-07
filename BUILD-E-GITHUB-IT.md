# Compilare Solphivia IA 1.1 beta 5

JDK 17, Android SDK Platform 35 / Build Tools 35.0.0, Gradle 8.11.1 e AGP 8.9.1. Imposta JAVA_HOME e ANDROID_HOME. Non pubblicare local.properties, chiavi o credenziali.

```text
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug
```

APK principale: app/build/outputs/apk/debug/app-debug.apk, it.solphivia.mobile, codice 53. Per un aggiornamento che conserva i dati serve la stessa firma; non disinstallare l’app per forzare l’installazione.

## Collaudo isolato

```text
./gradlew --no-daemon -PverificationBuild -PtestRunner=it.solphivia.mobile.InterruptAcousticInstrumentation testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w it.solphivia.mobile.v11.verification.test/it.solphivia.mobile.InterruptAcousticInstrumentation
```

La variante PROVA usa it.solphivia.mobile.v11.verification. Le prove che manipolano impostazioni e memoria vanno eseguite solo su dati sintetici senza account personali. Le prove dei comandi usano TTS sintetico locale: non certificano ascolto reale o cancellazione dell’eco. Per il campo di trascrizione ricompilare l’APK AndroidTest con -PtestRunner=it.solphivia.mobile.TranscriptComposerInstrumentation, installarlo ed eseguire quel runner. Il parametro testRunner seleziona un runner per build.

## Pubblicazione

Tag v1.1.0-beta.5, prerelease beta con APK debug firmato e checksum. Sorgenti ottenuti da git archive, senza cartelle private, dati del telefono o chiavi. [Risultati](docs/releases/v1.1.0-beta.5.md).
