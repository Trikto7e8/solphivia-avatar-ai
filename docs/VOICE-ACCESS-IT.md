# Solphivia Voice Access e ShareBridge — v0.1 Android

## Ricognizione

- `CompanionInputActivity`: dettatura esistente tramite `ACTION_RECOGNIZE_SPEECH`, con ritorno alla chat o all’overlay. Non è una parola di attivazione.
- `DeviceSpeech`, `PetService`, `SpeechVoiceSelection`, `VoicePreferences`: TTS e selezione delle voci già presenti. Voice Access riusa il selettore offline, con ciclo di vita TTS proprio e callback di fine saluto.
- `PetService`: avatar in overlay e servizio foreground special-use; Voice Access non aggiunge ascolto all’overlay né un nuovo servizio.
- Profilo OAuth e dati personali del vault non sono usati. Il nome scelto per Voice Access vive solo nelle preferenze locali `voice_access`.
- Il manifest aveva permessi overlay, rete, posizione e notifiche. Questa funzione aggiunge solo `RECORD_AUDIO`, richiesto all’avvio volontario, e visibilità mirata del riconoscitore e di WhatsApp.

## Implementato

Dalla home aprire **Voice Access** oppure, con avatar flottante attivo, **☰ → Voice Access e ShareBridge**. L’overlay viene nascosto mentre la schermata è visibile. Inserire un nome e premere una volta **Avvia e attendi il saluto**. Dopo il consenso Android al microfono, Voice Access resta in attesa di **Hola, Solphivia!**. Silenzio e frasi diverse non interrompono l’attesa: il riconoscitore viene riaperto automaticamente entro la sessione in primo piano. Quando riconosce il saluto, Solphivia dice **Hola, Marco!** (o il nome scelto) e poi ascolta un comando.

- **Apri WhatsApp**: Intent di avvio del pacchetto `com.whatsapp`.
- **Condividi Ciao Marco su WhatsApp**: ShareBridge estrae il testo e mostra un’anteprima modificabile. Solo **Apri condivisione WhatsApp** passa il testo a `ACTION_SEND`, `text/plain`, con pacchetto WhatsApp esplicito. Il destinatario e l’invio finale si scelgono in WhatsApp. Annullare non apre altre app. La conferma vocale e l’invio automatico non sono implementati.

Non sono interpretati comandi arbitrari o istruzioni di un modello. Il parser accetta soltanto queste forme, con variazioni di maiuscole, punteggiatura e “Whats App”. Il testo di condivisione è limitato a 2.000 caratteri; messaggi senza testo o comandi concatenati di apertura non vengono eseguiti. Non è supportato WhatsApp Business.

## Privacy, consumo e accessibilità

- Nessun salvataggio di audio, trascrizioni o bozze da parte del modulo, nessun log del parlato, nessun accesso a ChatGPT, contatti o AI Memory Vault. Il nome viene salvato localmente dopo Avvia; può essere sostituito nel campo o eliminato cancellando i dati dell’app.
- Preferenza per `createOnDeviceSpeechRecognizer` da Android 12 quando disponibile. Se non disponibile, nessun fallback silenzioso: occorre selezionare il consenso al riconoscitore di sistema, che potrebbe inviare audio al proprio fornitore. Il consenso non viene conservato tra nuove aperture. La disponibilità del servizio locale non garantisce l’installazione del modello italiano: eventuali errori fermano la sessione.
- TTS italiano con voce dichiarata offline; se assente, installarla nelle impostazioni Android e riaprire la schermata.
- Massimo 15 secondi per ciascun ascolto e per il saluto, nessun ciclo di tentativi, wake lock, biometria o ascolto a schermo spento. Interrompi, uscita, sospensione e rotazione annullano la sessione; il ritorno non riattiva il microfono. Le callback obsolete vengono ignorate. Un’anteprima aperta viene scartata quando la schermata perde il primo piano.
- Controlli Android nativi, nome etichettato, stato testuale annunciabile da TalkBack, pulsanti di almeno 48 dp e pagina scorrevole. La verifica manuale con TalkBack e font grandi resta necessaria.

## Prova su Android

Usare un emulatore o telefono di prova, Android 8+ (API 26), con riconoscitore italiano e voce TTS italiana offline. Per il percorso tutto locale serve Android 12+ e un servizio on-device disponibile. Compilare con JDK 17 e SDK 35:

```sh
./gradlew -PverificationBuild assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L’APK di verifica usa `it.solphivia.mobile.verification`, separato dall’installazione personale.

1. Inserire “Marco”, consentire il microfono e avviare. Dire “Hola, Solphivia!”, attendere “Hola, Marco!”, poi “Apri WhatsApp”. Verificare che il microfono si spenga prima dell’apertura e non riparta tornando indietro.
2. Ripetere con “Condividi Ci vediamo alle 18 su WhatsApp”. Modificare il testo, annullare: nessuna apertura. Ripetere, confermare: verificare testo e scelta destinatario in WhatsApp, senza inviare a persone reali.
3. Senza WhatsApp installato: errore leggibile, nessun crash o apertura del browser.
4. Negare/revocare il permesso microfono, disattivare il microfono dalle impostazioni privacy, provare senza riconoscitore/TTS: messaggio di errore e nessun ascolto nascosto.
5. Silenzio e frase errata: l’attesa del saluto continua, senza effetti. Comandi non supportati ritornano all’attesa. “Interrompi” arresta il microfono; dopo otto errori consecutivi il modulo si ferma e mostra lo stato.
6. Durante saluto e ascolto: Interrompi, Home, blocco schermo e rotazione. Verificare indicatore microfono spento, niente comando tardivo e riavvio solo manuale. Ripetere rapidamente Avvia/Interrompi per verificare callback obsolete.
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
