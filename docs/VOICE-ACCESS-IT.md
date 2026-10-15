# Solphivia Voice Access e ShareBridge — v0.1 Android

## Ricognizione

- `CompanionInputActivity`: dettatura esistente tramite `ACTION_RECOGNIZE_SPEECH`, con ritorno alla chat o all’overlay. Non è una parola di attivazione.
- `DeviceSpeech`, `PetService`, `SpeechVoiceSelection`, `VoicePreferences`: TTS e selezione delle voci già presenti. Voice Access riusa il selettore offline, con ciclo di vita TTS proprio e callback di fine saluto.
- `PetService`: avatar in overlay e servizio foreground special-use; Voice Access non aggiunge ascolto all’overlay né un nuovo servizio.
- Profilo OAuth e dati personali del vault non sono usati. Il nome scelto per Voice Access vive solo nelle preferenze locali `voice_access`.
- Il manifest aveva permessi overlay, rete, posizione e notifiche. Questa funzione aggiunge solo `RECORD_AUDIO`, richiesto all’avvio volontario, e visibilità mirata del riconoscitore e di WhatsApp.

## Implementato

Dalla home aprire **Voice Access** oppure, con avatar flottante attivo, **☰ → Voice Access e ShareBridge**. Inserire un nome e premere **Avvia e attendi il saluto**. Dopo il consenso Android al microfono, la schermata di configurazione scompare e resta visibile l’avatar flottante esistente, seduto mentre attende **Hola, Solphivia!**. Al saluto risponde con TTS **Hola, Marco!** (o il nome scelto), si anima e ascolta il comando. Dopo una risposta di Solphivia torna in ascolto per il comando successivo. Dire **Fermati** (anche **Basta**, **Stop** o **Smetti**) arresta il microfono e chiude la sessione; per riattivarla si usa di nuovo **Voice Access** dal menu dell’avatar. L’app deve restare in primo piano e il telefono sbloccato.

- **Apri WhatsApp**: apre l’app.
- **Condividi Ciao Marco su WhatsApp**: ShareBridge mostra un’anteprima modificabile. Solo dopo **Apri condivisione WhatsApp** passa il testo a WhatsApp; destinatario e invio finale si scelgono lì.
- **Cerca gatti sul web**: chiede conferma e poi apre la ricerca Android.
- **Metti un timer di 5 minuti** e **sveglia alle 7:30**: dopo conferma apre l’app Orologio con i dati proposti, lasciando all’utente il controllo finale.
- **Crea un appuntamento visita dentistica**: chiede conferma e apre il Calendario con una bozza da completare e salvare.
- Una richiesta libera viene inviata alla chat ChatGPT già collegata; Solphivia legge la risposta e torna ad ascoltare. Se ChatGPT non è collegato, il modulo lo comunica a voce.

Le azioni Android sopra sono un insieme limitato di comandi locali, con conferma prima di aprire app o inviare una ricerca. Il modello risponde alle richieste libere, ma in questa versione non può attivare funzioni o modificare il dispositivo tramite strumenti generali. Il parser tratta il testo di condivisione fino a 2.000 caratteri; WhatsApp Business non è supportato.

## Privacy, consumo e accessibilità

- Audio e comandi locali non sono salvati dal modulo. Se chiedi una risposta libera, la trascrizione e la risposta vengono aggiunte alla cronologia chat già esistente e inviate tramite il collegamento ChatGPT dell’app; il testo resta nelle preferenze locali della chat. Il nome è salvato separatamente nelle preferenze `voice_access`. AI Memory Vault e contatti non vengono consultati.
- Preferenza per `createOnDeviceSpeechRecognizer` da Android 12 quando disponibile. Se non disponibile, nessun fallback silenzioso: occorre selezionare il consenso al riconoscitore di sistema, che potrebbe inviare audio al proprio fornitore. Il consenso non viene conservato tra nuove aperture. La disponibilità del servizio locale non garantisce l’installazione del modello italiano: eventuali errori fermano la sessione.
- TTS italiano con voce dichiarata offline; se assente, installarla nelle impostazioni Android e riaprire la schermata.
- Massimo 15 secondi per ciascun ascolto e per il saluto, niente wake lock, biometria o ascolto a schermo spento. La frase di attivazione resta in attesa mentre l’app è aperta; errori ripetuti interrompono la sessione. **Fermati**, uscita, sospensione e rotazione annullano la sessione; il ritorno non riattiva il microfono. Le callback obsolete vengono ignorate. Un’anteprima aperta viene scartata quando la schermata perde il primo piano.
- Controlli Android nativi, nome etichettato, stato testuale annunciabile da TalkBack, pulsanti di almeno 48 dp e pagina scorrevole. La verifica manuale con TalkBack e font grandi resta necessaria.

## Prova su Android

Usare un emulatore o telefono di prova, Android 8+ (API 26), con riconoscitore italiano e voce TTS italiana offline. Per il percorso tutto locale serve Android 12+ e un servizio on-device disponibile. Compilare con JDK 17 e SDK 35:

```sh
./gradlew -PverificationBuild assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L’APK di verifica usa `it.solphivia.mobile.verification`, separato dall’installazione personale.

1. Inserire “Marco”, consentire il microfono e avviare. Verificare che la schermata si nasconda e resti l’avatar flottante seduto. Dire “Hola, Solphivia!”, verificare risposta TTS e animazione, poi “Apri WhatsApp”. Verificare che il microfono si spenga prima dell’apertura.
2. Ripetere e dire “Cerca gatti sul web”: annullare e ripetere confermando. Provare poi “Metti un timer di 5 minuti”, “Sveglia alle 7:30” e “Crea un appuntamento visita dentistica”; controllare che ciascuna azione chieda conferma e lasci l’ultima modifica all’app Android.
3. Con ChatGPT collegato, dire una richiesta libera, verificare risposta TTS e che Solphivia torni ad ascoltare. Verificare la cronologia chat locale e il collegamento ChatGPT. Senza collegamento, il modulo deve spiegare il requisito a voce.
4. Ripetere con “Condividi Ci vediamo alle 18 su WhatsApp”. Modificare il testo, annullare: nessuna apertura. Ripetere, confermare: verificare testo e scelta destinatario in WhatsApp, senza inviare a persone reali.
3. Senza WhatsApp installato: errore leggibile, nessun crash o apertura del browser.
4. Negare/revocare il permesso microfono, disattivare il microfono dalle impostazioni privacy, provare senza riconoscitore/TTS: messaggio di errore e nessun ascolto nascosto.
6. Silenzio e frase errata: l’attesa del saluto continua, senza effetti. “Interrompi” arresta il microfono; dopo otto errori consecutivi il modulo si ferma e mostra lo stato.
6. Durante attesa e ascolto: dire “Fermati”, poi provare Home, blocco schermo e rotazione. Verificare indicatore microfono spento, niente comando tardivo e riavvio solo dal menu Voice Access. Ripetere rapidamente Avvia/Fermati per verificare callback obsolete.
7. Android 8–11: consenso al servizio di sistema obbligatorio; Android 12+: testare sia locale disponibile sia assente. Provare in modalità aereo con modelli locali installati.
8. TalkBack, caratteri grandi e orientamento orizzontale: verificare etichette, scorrimento, anteprima e conferma. Provare anche con overlay preesistente attivo: eventuale audio di altre funzioni non deve produrre un invio.

La CI verifica parser, compilazione degli APK e lint. Non simula un motore vocale reale né certifica il comportamento di WhatsApp: i passaggi sopra sono collaudo manuale richiesto prima del rilascio.

### Verifiche locali del 15 ottobre 2026

- PASS: 17 controlli del parser eseguiti con JDK 17; `git diff --check` pulito.
- Build APK e test strumentali tentata con Gradle 8.11.1 e SDK 35. Il Java 25 installato è incompatibile; con JDK 17 locale la compilazione incontra `AccessDeniedException` alla chiusura dei JAR SDK nell’ambiente Windows limitato, anche usando una copia dell’SDK nella cartella di lavoro. Nessun APK validato localmente; lint non completato in questo tentativo.
- ADB non inizializza la propria cartella utente nell’ambiente: prove su dispositivo e test strumentali non eseguiti. Il workflow della PR offre una verifica separata su Linux/JDK 17; consultarne l’esito effettivo, senza dedurlo da questi test del parser.

## Roadmap separata

- **0.1 (questa PR):** modulo Android Voice Access + ShareBridge, saluto e condivisione controllata. Il piccolo parser è già Java puro, ma non è una libreria distribuita.
- **0.2 (previsto):** estrarre un componente open source con API pubblica e test di contratto utilizzabile da ChatGPT, Qwen e altri modelli; mantenere il consenso e gli effetti sul dispositivo nel livello Android.
- **0.3 (previsto):** plugin MCP sperimentale, documentazione, test, informativa privacy e valutazione della candidatura alla directory OpenAI. Nessuna candidatura o integrazione MCP è inclusa o promessa come approvata.
- **AI Memory Vault:** lavoro distinto; nessuna migrazione o lettura/scrittura introdotta.

Riferimenti: [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [fine TTS](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener), [condivisione Android](https://developer.android.com/training/sharing/send).
