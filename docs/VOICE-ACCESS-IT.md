# Solphivia Voice Access e ShareBridge — v0.1 Android

## Ricognizione

- `CompanionInputActivity`: dettatura esistente tramite `ACTION_RECOGNIZE_SPEECH`, con ritorno alla chat o all’overlay. Non è una parola di attivazione.
- `DeviceSpeech`, `PetService`, `SpeechVoiceSelection`, `VoicePreferences`: TTS e selezione delle voci già presenti. Voice Access riusa il selettore offline, con ciclo di vita TTS proprio e callback di fine saluto.
- `PetService`: avatar in overlay e servizio foreground special-use; Voice Access non aggiunge ascolto all’overlay né un nuovo servizio.
- Profilo OAuth e dati personali del vault non sono usati. Il nome scelto per Voice Access vive solo nelle preferenze locali `voice_access`.
- Il manifest aveva permessi overlay, rete, posizione e notifiche. Questa funzione usa il permesso `RECORD_AUDIO` richiesto da Android e visibilità mirata del riconoscitore e di WhatsApp.

## Implementato

Dall’avvio di Solphivia, l’avatar resta visibile nella schermata principale e il riconoscitore locale attende **Hola, Solphivia!** senza una pagina o un pulsante Voice Access. L’avatar flottante viene nascosto durante la sessione, così resta visibile una sola Solphivia. Al primo uso Android chiede il normale permesso microfono. Al saluto Solphivia si anima e risponde con TTS **Hola, Marco!** usando il nome locale già salvato (ripiego: “Marco”), poi ascolta i comandi. Dopo una risposta torna ad ascoltare il comando successivo. Dire **Fermati** (anche **Basta**, **Stop** o **Smetti**) interrompe la sessione fino al prossimo avvio di Solphivia. Uscendo dall’app o bloccando il telefono il microfono si spegne; quando Solphivia torna in primo piano, l’attesa riparte.

- **Apri WhatsApp**: apre l’app.
- **Condividi Ciao Marco su WhatsApp**: ShareBridge mostra un’anteprima modificabile. Solo dopo **Apri condivisione WhatsApp** passa il testo a WhatsApp; destinatario e invio finale si scelgono lì.
- **Cerca gatti sul web**: chiede conferma e poi apre la ricerca Android.
- **Metti un timer di 5 minuti** e **sveglia alle 7:30**: dopo conferma apre l’app Orologio con i dati proposti, lasciando all’utente il controllo finale.
- **Crea un appuntamento visita dentistica**: chiede conferma e apre il Calendario con una bozza da completare e salvare.
- Una richiesta libera viene inviata alla chat ChatGPT già collegata; Solphivia legge la risposta e torna ad ascoltare. Se ChatGPT non è collegato, il modulo lo comunica a voce.

Le azioni Android sopra sono un insieme limitato di comandi locali, con conferma prima di aprire app o inviare una ricerca. Il modello risponde alle richieste libere, ma in questa versione non può attivare funzioni o modificare il dispositivo tramite strumenti generali. Il parser tratta il testo di condivisione fino a 2.000 caratteri; WhatsApp Business non è supportato.

## Privacy, consumo e accessibilità

- Audio e comandi locali non sono salvati dal modulo. Se chiedi una risposta libera, la trascrizione e la risposta vengono aggiunte alla cronologia chat già esistente e inviate tramite il collegamento ChatGPT dell’app; il testo resta nelle preferenze locali della chat. Il nome è salvato separatamente nelle preferenze `voice_access`. AI Memory Vault e contatti non vengono consultati.
- Usa solo `createOnDeviceSpeechRecognizer` quando Android lo supporta; non c’è fallback online nella v0.1. Su Android 13+ la frase di attivazione richiede una sessione segmentata, che mantiene un’unica sessione di ascolto fra le pause quando il servizio vocale la supporta. Sui servizi che ignorano questa opzione o su Android 12 la sessione può terminare e ripartire con i normali suoni Android. Se il riconoscitore locale o il modello italiano non è disponibile, Solphivia mostra l’errore nella bolla e chiude la sessione. Android 8–11 non è supportato per il percorso vocale locale.
- TTS italiano con voce dichiarata offline; se assente, installarla nelle impostazioni Android e riaprire Solphivia.
- L’attesa della frase di attivazione e dei comandi resta attiva finché Solphivia è in primo piano; gli errori del riconoscitore usano un’attesa crescente e si fermano dopo cinque errori consecutivi, evitando riavvii acustici senza fine. Nessun wake lock, biometria o ascolto a schermo spento. **Fermati** sospende l’ascolto fino al successivo avvio dell’app. Uscita, blocco e rotazione fermano il microfono; al ritorno in primo piano l’attesa riparte. Le callback obsolete vengono ignorate. Un’anteprima aperta viene scartata quando l’app perde il primo piano.
- La sessione usa l’avatar e la bolla già presenti; le conferme per effetti sul dispositivo restano dialoghi Android nativi. La verifica manuale con TalkBack e font grandi resta necessaria.

## Prova su Android

Usare un emulatore o telefono di prova Android 12+ (API 31), con modello italiano on-device e voce TTS italiana offline. Compilare con JDK 17 e SDK 35:

```sh
./gradlew -PverificationBuild assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L’APK di verifica usa `it.solphivia.mobile.verification`, separato dall’installazione personale.

1. Aprire Solphivia e, al primo uso, consentire il microfono ad Android. Verificare che non compaia una pagina di configurazione, che l’indicatore microfono Android sia attivo, che si veda un solo avatar e che il riconoscitore non si riavvii a ogni pausa sul dispositivo in prova. Dire “Hola, Solphivia!”, verificare risposta TTS e animazione, poi “Apri WhatsApp”. Verificare che il microfono si spenga prima dell’apertura e riparta tornando a Solphivia.
2. Ripetere e dire “Cerca gatti sul web”: annullare e ripetere confermando. Provare poi “Metti un timer di 5 minuti”, “Sveglia alle 7:30” e “Crea un appuntamento visita dentistica”; controllare che ciascuna azione chieda conferma e lasci l’ultima modifica all’app Android.
3. Con ChatGPT collegato, dire una richiesta libera, verificare risposta TTS e che Solphivia torni ad ascoltare. Verificare la cronologia chat locale e il collegamento ChatGPT. Senza collegamento, il modulo deve spiegare il requisito a voce.
4. Ripetere con “Condividi Ci vediamo alle 18 su WhatsApp”. Modificare il testo, annullare: nessuna apertura. Ripetere, confermare: verificare testo e scelta destinatario in WhatsApp, senza inviare a persone reali.
5. Senza WhatsApp installato: errore leggibile, nessun crash o apertura del browser.
6. Negare/revocare il permesso microfono, disattivare il microfono dalle impostazioni privacy, provare senza riconoscitore/TTS: messaggio di errore e nessun ascolto nascosto.
7. Silenzio e frase errata: l’attesa del saluto continua, senza effetti. “Fermati” arresta il microfono.
8. Durante attesa e ascolto, provare Home, blocco schermo e rotazione. Verificare indicatore microfono spento, niente comando tardivo e ripartenza solo tornando in Solphivia.
9. Android 12+: provare con il riconoscitore on-device e modello italiano disponibili e poi assenti. Provare in modalità aereo con i modelli locali installati.
10. TalkBack, caratteri grandi e orientamento orizzontale: verificare etichette, anteprima e conferma. Provare anche con overlay preesistente attivo: eventuale audio di altre funzioni non deve produrre un invio.

La CI verifica parser, compilazione degli APK e lint. Non simula un motore vocale reale né certifica il comportamento di WhatsApp: i passaggi sopra sono collaudo manuale richiesto prima del rilascio.

### Verifiche locali

- PASS: build APK debug, APK strumentale debug e `lintDebug` con JDK 17 e SDK 35.
- PASS: 17 controlli puri del parser di wake phrase, allowlist WhatsApp e ShareBridge.
- PASS: build APK debug, APK strumentale debug e `lintDebug` dopo l’integrazione dell’ascolto nella `MainActivity`.
- PASS: APK di verifica installato sul Moto g35 5G (Android 16); `MainActivity` è l’attività in primo piano, la schermata mostra un solo avatar e l’app ha il permesso microfono attivo in primo piano. La prova parlata della frase di attivazione e dei comandi resta da confermare.
- NOT RUN: il test strumentale overlay non ha completato l’esecuzione nel runner Android personalizzato; la prima esecuzione rilevava un riferimento di test obsoleto (`homeShortcutAttempted`), che è stato rimosso. La seconda esecuzione ha restituito un bundle vuoto prima di qualsiasi risultato.

## Roadmap separata

- **0.1 (questa PR):** modulo Android Voice Access + ShareBridge, saluto e condivisione controllata. Il piccolo parser è già Java puro, ma non è una libreria distribuita.
- **0.2 (previsto):** estrarre un componente open source con API pubblica e test di contratto utilizzabile da ChatGPT, Qwen e altri modelli; mantenere il consenso e gli effetti sul dispositivo nel livello Android.
- **0.3 (previsto):** plugin MCP sperimentale, documentazione, test, informativa privacy e valutazione della candidatura alla directory OpenAI. Nessuna candidatura o integrazione MCP è inclusa o promessa come approvata.
- **AI Memory Vault:** lavoro distinto; nessuna migrazione o lettura/scrittura introdotta.

Riferimenti: [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [fine TTS](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener), [condivisione Android](https://developer.android.com/training/sharing/send).
