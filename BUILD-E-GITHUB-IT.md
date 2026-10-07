# Compilare e preparare la pubblicazione

Questa procedura serve a produrre sul tuo computer l'APK di prova e a caricare manualmente i sorgenti su GitHub. Non caricare password, token OAuth, chiavi API, keystore o password del keystore.

## A. Aprire il progetto

1. Estrai lo ZIP in una cartella locale facile da trovare.
2. In Android Studio scegli **Open** e seleziona la cartella `solphivia-avatar-ai` (quella che contiene `settings.gradle`).
3. Attendi **Gradle Sync**. Se Android Studio chiede di installare Android SDK Platform 35 o Build Tools, accetta l'installazione.
4. Se la sincronizzazione fallisce, copia il messaggio completo dell'errore; non aggiungere chiavi o password nei file del progetto.

## B. Creare un APK di prova

1. Dal menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Attendi il messaggio **APK(s) generated successfully**.
3. Premi **Locate** nel messaggio di Android Studio. Il file previsto è `app/build/outputs/apk/debug/app-debug.apk`.
4. Trasferisci l'APK sul Motorola, installalo e prova avvio, icona, consenso browser, overlay, chat, audio e allegati.

Questo APK debug è adatto a test personali; non è la release firmata per Google Play. Prima di una pubblicazione stabile servono revisione privacy, target API richiesto da Play, verifiche delle policy e test su dispositivi. La configurazione sorgente attuale è API 35: non inviare ancora a Play senza aggiornamento e verifica dei requisiti correnti.

## C. Preparare i sorgenti per GitHub

1. Crea su GitHub un repository pubblico chiamato **Solphivia Avatar AI** (o un nome disponibile), senza generare file iniziali se vuoi caricare la cartella già pronta.
2. In Android Studio verifica la lista dei file: non includere `local.properties`, cartelle `.gradle/` e `build/`, APK/AAB, log, token, chiavi o configurazioni personali. Il `.gitignore` del progetto esclude gli elementi di build e i segreti comuni.
3. Carica solo la cartella di progetto `solphivia-avatar-ai` e i file di documentazione. Puoi usare il sito GitHub con **Add file → Upload files**, oppure Git da terminale. Non selezionare mai `.jks`/`.keystore`, `.env`, `local.properties` o `app/build/`.
4. Prima di confermare, controlla la schermata dei file selezionati e fai una ricerca nel repository per email personali, percorsi del tuo computer, password, `sk-`, `ghp_`, `Bearer ` e token.
5. In **About** imposta la descrizione breve contenuta in `PRESENTAZIONE-IT.md` e aggiungi i topic suggeriti.
6. Rileggi `README.md`, `PRIVACY-IT.md` e `ASSETS-LICENSE.md` dalla pagina pubblica appena creato il repository.

## D. APK nelle release GitHub (opzionale)

Quando hai verificato l'APK sul tuo telefono, puoi allegare `app-debug.apk` a una GitHub Release marcata chiaramente come **alpha/debug, solo test**. Un APK debug non è un pacchetto Play Store. Per Google Play si prepara in seguito un Android App Bundle firmato (`.aab`) con una chiave privata conservata in modo sicuro e mai caricata nel repository.

## Verifiche prima del caricamento

- Controlla che nome e icona visualizzati siano quelli voluti.
- Prova il flusso con il tuo account ma non pubblicare screenshot che mostrino email, nomi o contenuti personali.
- Se cambi sorgenti dopo la build, ricrea l'APK: il file già prodotto non contiene le modifiche successive.
- Il manutentore deve fornire un recapito e una privacy policy web prima di distribuire l'app al pubblico o candidarla a uno store.
