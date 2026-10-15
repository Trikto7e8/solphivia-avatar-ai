# Solphivia Mobile 0.9.2-alpha1 — aggiornamento 13 ottobre 2026

## Modifiche

- Memoria personale locale cifrata con AES-GCM e chiave Android Keystore, separata da modello, OAuth e cronologia. Editor nel menu `☰ → Memoria`, comando locale `ricorda: testo`, limite 32 KB. Il modello non può scrivere ricordi autonomamente.
- Importazione di testo UTF-8 e backup cifrati **AI Memory Vault v1**. Il backup viene decifrato sul dispositivo con la passphrase; si propone nell’editor il profilo descrittivo e solo ricordi approvati, ordinari e non scaduti. I ricordi sensibili/archiviati/suggeriti non sono inclusi. Gli asset voce/avatar non vengono importati.
- Esportazione di un **nuovo** vault cifrato, compatibile con il progetto originale. Contiene il testo dell’editor suddiviso in note e un profilo Solphivia di base con preferenze di lettura/accento. Non è una copia integrale del vault importato: non conserva ID, categorie o cronologia originali. Non sovrascrivere il backup originale con questa copia derivata.
- Nessuna sincronizzazione automatica o collegamento a un server privato: il repository stesso non implementa ancora la sincronizzazione. Il file esportato si può trasferire e importare sugli altri dispositivi.
- Anteprima modificabile della memoria per ciascuna richiesta remota, con possibilità di inviare senza memoria o annullare. Consenso monouso, scadenza di cinque minuti. Richieste annullate non vengono aggiunte alla cronologia.
- `+` e microfono ora sono sopra il campo di testo dell’overlay; prima avevano un’elevazione inferiore che poteva nasconderli. Disponibili anche nella schermata interna. Gli allegati selezionati restano in bozza fino a `➜`; pressione prolungata su `+` per rimuoverli. Limiti: 5 file, 8 MB per file, 15 MB complessivi per la conversazione inviata.
- Dettatura italiana Android modificabile prima dell’invio. Non viene avviato un ascolto continuo.
- Impostazioni del motore vocale installato, accento inglese offline e lettura ad alta voce. Preferenza per eSpeak NG se già installato e non è stata scelta un’altra impostazione. Le preferenze esplicite esistenti sono conservate. La schermata interna ora legge le risposte. Le risposte lunghe vengono suddivise per rispettare i limiti TTS.
- L’accento usa la prova fonetica già presente (`ItalianEnglishAccent`): non è una nuova voce neurale né una garanzia di timbro identico su motori differenti. Il motore libero è un’app separata, non incorporata nell’APK Solphivia.
- Radar: eliminata la richiesta WMS con scala non ammessa (errore HTTP 400 riprodotto). Il client usa le tile SRI attuali del sito DPC, con decodifica del canale rosso nella scala 0–100 mm/h e coordinate XYZ. Soglia di rilevamento ≥1 mm/h, ricerca entro 60 km. L’overlay mostra timestamp, errore o dati vecchi; toccare Radar per i dettagli. Gli avvisi indicano solo echi vicini in tre scansioni, non direzione o tempo di arrivo.
- Animazioni: tutti i 108 PNG verificati e renderizzati; ciclo del sonno riattivato sui quattro fotogrammi, proporzioni dell’immagine preservate e idle delle modalità rallentato. Il frame duplicato di `cyborg_sit` resta il ripristino intenzionale già documentato. Nuova schermata `☰ → Prova animazioni`. Gli outfit meteo/prof/codex non possiedono cicli d’azione dedicati: si usano gli sprite base/cyborg esistenti.

## Verifiche e riproducibilità

- Integrità, trasparenza e fotogrammi: `python verification/check_assets.py`; riepilogo `verification/assets-report.json`, tavola `verification/all-animations.png`.
- Compilazione e Android Lint: JDK 17 portabile in `.tools/jdk17/jdk-17.0.20.1+1` (download Adoptium con checksum verificato). Il JDK 25 dell’Android Studio installato non è compatibile con Gradle 8.11.1. Usare JDK 17 per questo progetto.
- Test Android **solo sulla variante isolata**: `gradlew -PverificationBuild :app:assembleDebug :app:assembleDebugAndroidTest`. L’ID è `it.solphivia.mobile.verification`, diverso dall’app personale. I test rifiutano il package di produzione.
- Sul Motorola, prima dello scollegamento, superati: memoria cifrata/UTF-8, limiti, decifratura del backup generato dal core JavaScript originale, rifiuto password errata e manomissioni, filtri dei ricordi, esportazione portabile, caricamento dei 108 sprite e 1248 combinazioni di disegno, avanzamento del sonno, lettura radar DPC dal vivo per coordinate fittizie di Roma, allegati mantenuti nei turni successivi e tre cicli di avvio/arresto overlay.
- Il backup fittizio prodotto su Android è stato decifrato e validato anche dal codice originale AI Memory Vault: `node verification/vault_interop.mjs validate verification/vault-android.json`.
- Controlli successivi nell’emulatore temporaneo: risultati riassunti sotto; log diagnostici conservati localmente.

## Limiti della verifica sul telefono

Il Motorola è stato ricollegato. I test completi nella variante isolata sono passati, inclusi annullamento, invio senza memoria e consenso monouso. Build finale e Android Lint riusciti (rapporto conservato nel workspace locale). APK personale installato e avviato il 13 ottobre 2026: package `it.solphivia.mobile`, versione `0.9.2-alpha1`, codice 30. Aggiornamento con `adb install -r`, senza cancellazione dei dati.

APK: `releases/SolphiviaMobile-0.9.2-alpha1.apk`. SHA-256: `3B4BADC86CAA17C9AD7B60AA2EA9233E3C7E930D52884C905CF8D95BE898DB53`.

Test UI nell’emulatore passato. La regressione completa nell’emulatore è stata interrotta dopo un’attesa bloccata (il messaggio “Process crashed” nel log deriva dall’arresto manuale); la stessa regressione è poi passata sul Motorola. Il test vocale sull’emulatore non ha trovato una voce inglese offline utilizzabile: non è una prova superata.

Android ha nuovamente rifiutato l’installazione dell’APK ufficiale eSpeak NG con `INSTALL_FAILED_VERIFICATION_FAILURE`; le protezioni non sono state disabilitate. La voce libera e la valutazione all’ascolto dell’accento restano da completare. La dettatura reale richiede una prova con la voce dell’utente. L’utente conferma che la versione funziona e autorizza la pubblicazione come alpha il 13 ottobre 2026; ulteriori miglioramenti sono rinviati.

Il radar è informativo e dipende da disponibilità, copertura e qualità DPC. Mancanza di pixel sopra soglia non significa certezza di assenza di pioggia. Gli avvisi funzionano mentre l’overlay è attivo, non come servizio affidabile a telefono/app chiusi.

## Fonti e componenti di test

- [AI Memory Vault](https://github.com/Trikto7e8/AI-memory-vault), schemi e core originali consultati il 12 ottobre 2026. Le copie del validatore e della crittografia in `verification/` servono esclusivamente ai test di interoperabilità; vedere la licenza allegata.
- [API Radar-DPC](https://dpc-radar.readthedocs.io/it/latest/api.html) e [applicazione ufficiale](https://radar.protezionecivile.it/). Fonte dati: Radar-DPC, CC BY-SA; le eventuali elaborazioni dei dati mantengono le relative condizioni.
- [eSpeak NG, release ufficiale 1.52.0](https://github.com/espeak-ng/espeak-ng/releases/tag/1.52.0). Sintetizzatore libero separato, GPL; non incluso nei sorgenti applicativi. APK di prova conservato solo nel workspace locale, SHA-256 `741b46abf84d843f9b0cce80136f60ffee9b9fe591601b9701aa2a9ff99c9ddf`.
