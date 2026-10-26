# Compilare Solphivia IA 1.1 beta 3

JDK 17, Android SDK Platform 35 / Build Tools 35.0.0, Gradle 8.11.1 e AGP 8.9.1. Imposta JAVA_HOME e ANDROID_HOME. Non pubblicare local.properties, chiavi o credenziali.

```text
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug
```

APK principale: app/build/outputs/apk/debug/app-debug.apk, it.solphivia.mobile, codice 49. Per un aggiornamento che conserva i dati serve la stessa firma; non disinstallare l’app per forzare l’installazione.

## Collaudo isolato

```text
./gradlew --no-daemon -PverificationBuild testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb shell am instrument -w it.solphivia.mobile.v11.verification.test/it.solphivia.mobile.TranscriptComposerInstrumentation
adb shell am instrument -w it.solphivia.mobile.v11.verification.test/it.solphivia.mobile.InterruptAcousticInstrumentation
```

La variante PROVA usa it.solphivia.mobile.v11.verification. Le prove che manipolano impostazioni e memoria vanno eseguite solo su dati sintetici senza account personali. Le prove dei comandi usano TTS sintetico locale: non certificano ascolto reale o cancellazione dell’eco. Gli altri runner sono dichiarati nel manifest AndroidTest.

## Pubblicazione

Tag v1.1.0-beta.3, prerelease beta con APK debug firmato e checksum. Sorgenti ottenuti da git archive, senza cartelle private, dati del telefono o chiavi. [Risultati](docs/releases/v1.1.0-beta.3.md).

Per Google Play usare un AAB release firmato con chiave privata esterna al repository. Il pacchetto attuale target API 35 non va inviato come pronto allo store: [preparazione e blocchi](docs/google-play/README.md).
