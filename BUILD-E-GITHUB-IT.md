# Compilare e verificare 1.0 beta 1

Servono JDK 17 e Android SDK Platform 35 / Build Tools 35.0.0. Imposta `JAVA_HOME` e `ANDROID_HOME`, oppure configura Android Studio e un `local.properties` locale che non va pubblicato.

## Build principale

```text
./gradlew --no-daemon testDebugUnitTest assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`, pacchetto `it.solphivia.mobile`, 1.0 beta 1 / codice 46. È una build debug per le prove, non il pacchetto Google Play. Per aggiornare dati esistenti occorrono la stessa firma e un codice versione compatibile; non disinstallare per forzare un aggiornamento.

## Collaudo isolato

```text
./gradlew --no-daemon -PverificationBuild testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
```

Pacchetto `it.solphivia.mobile.beta1.verification`, nome launcher «Solphivia 1.0 beta 1 · PROVA». Le suite che manipolano memoria e impostazioni vanno eseguite solo su un’installazione di test senza dati/account dell’utente.

Dopo installazione app e APK test, concedi i permessi necessari della sola variante isolata. Esegui il runner desiderato:

```text
adb shell am instrument -w it.solphivia.mobile.beta1.verification.test/it.solphivia.mobile.RegressionInstrumentation
adb shell am instrument -w it.solphivia.mobile.beta1.verification.test/it.solphivia.mobile.VoiceCommandInstrumentation
adb shell am instrument -w it.solphivia.mobile.beta1.verification.test/it.solphivia.mobile.FollowUpInstrumentation
adb shell am instrument -w it.solphivia.mobile.beta1.verification.test/it.solphivia.mobile.TranscriptComposerInstrumentation
```

I test generali richiedono overlay; alcuni controlli usano dati DPC via rete. I test dei comandi intercettano gli intent e non effettuano chiamate. Le sonde tempo/trascrizione non usano il microfono reale. Login, qualità del parlato e comportamento reale si verificano separatamente con l’utente.

## Pubblicazione

Mantieni APK, keystore, chiavi, preferenze, token, log privati e backup memoria fuori dal codice. Pubblica l’APK verificato e i sorgenti come allegati di una GitHub prerelease marcata alpha/debug, indicando hash e certificato. Il tag deve riferirsi al commit dei sorgenti pubblicati. Le immagini promozionali sono illustrazioni; non vanno presentate come screenshot.

[Note 1.0 beta 1 e risultati](docs/releases/v1.0.0-beta.1.md). La pubblicazione Google Play, gli adattatori Ollama/LM Studio e iOS restano lavori separati.
