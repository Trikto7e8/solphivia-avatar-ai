# Solphivia Voice Access e ShareBridge — v0.1 Android

## Ricognizione

- `CompanionInputActivity`: dettatura esistente tramite `ACTION_RECOGNIZE_SPEECH`, con ritorno alla chat o all’overlay. Non è una parola di attivazione.
- `DeviceSpeech`, `PetService`, `SpeechVoiceSelection`, `VoicePreferences`: TTS e selezione delle voci già presenti. Voice Access riusa il selettore offline, con ciclo di vita TTS proprio e callback di fine saluto.
- `PetService`: avatar flottante e servizio foreground; usa il tipo Android `microphone` durante Voice Access e `specialUse` per il solo overlay. `VoiceAccessActivity` gestisce permessi e avvio, poi sposta il proprio task in secondo piano, escluso dai recenti. Sul Moto g35 chiuderlo immediatamente causava il declassamento del servizio e il silenziamento del microfono. Nessuna attività trasparente resta davanti alle altre app.
- Profilo OAuth e dati personali del vault non sono usati. Il nome scelto per Voice Access vive solo nelle preferenze locali `voice_access`.
- Il manifest aveva permessi overlay, rete, posizione e notifiche. Questa funzione usa il permesso `RECORD_AUDIO` richiesto da Android e visibilità mirata del riconoscitore e di WhatsApp.

## Implementato

Dall’icona si avvia il servizio vocale e appare una sola Solphivia flottante con bolla e controlli. Il riconoscitore offline attende **Hola, Solphivia!**, risponde con TTS **Hola, Marco!** usando il nome locale e il motore/accento scelti per l’avatar, poi ascolta i comandi. La vignetta mostra le trascrizioni parziali; sono accettate alcune varianti fonetiche del nome. Dopo una risposta o l’apertura di WhatsApp l’ascolto dei comandi continua anche sopra un’altra app, finché lo schermo resta acceso e sbloccato. **Fermati**, **Basta**, **Stop** o **Smetti** spengono il microfono fino al prossimo avvio. Il pulsante **Ferma** nella notifica chiude avatar e voce. A schermo spento o bloccato il microfono si spegne; allo sblocco torna in attesa del saluto. Il modello italiano Vosk (48 MB) si scarica una volta via Wi-Fi e resta nei dati privati dell’app. AI Memory Vault resta separato.

- **Apri WhatsApp**: apre l’app nel proprio task e continua ad ascoltare il comando successivo.
- **Condividi Ciao Marco su WhatsApp**: ShareBridge mostra un’anteprima modificabile. Solo dopo **Apri condivisione WhatsApp** passa il testo a WhatsApp; destinatario e invio finale si scelgono lì.
- **Cerca gatti sul web**: chiede conferma e poi apre la ricerca Android.
- **Metti un timer di 5 minuti** e **sveglia alle 7:30**: dopo conferma apre l’app Orologio con i dati proposti, lasciando all’utente il controllo finale.
- **Crea un appuntamento visita dentistica**: chiede conferma e apre il Calendario con una bozza da completare e salvare.
- Una richiesta libera viene inviata alla chat ChatGPT già collegata; Solphivia legge la risposta e torna ad ascoltare. Se ChatGPT non è collegato, il modulo lo comunica a voce.

Le azioni Android sopra sono un insieme limitato di comandi locali, con conferma prima di aprire app o inviare una ricerca. Il modello risponde alle richieste libere, ma in questa versione non può attivare funzioni o modificare il dispositivo tramite strumenti generali. Il parser tratta il testo di condivisione fino a 2.000 caratteri; WhatsApp Business non è supportato.

## Privacy, consumo e accessibilità

- Il riconoscimento del saluto e dei comandi è elaborato sul telefono da Vosk; i campioni audio non vengono inviati né salvati. Il modello si scarica una volta via Wi-Fi e rimane nei dati privati dell’app. Se chiedi una risposta libera, la trascrizione e la risposta vengono aggiunte alla cronologia chat già esistente e inviate tramite il collegamento ChatGPT dell’app; il testo resta nelle preferenze locali della chat. Il nome è salvato separatamente nelle preferenze `voice_access`. AI Memory Vault e contatti non vengono consultati.
- Il microfono usa un’unica registrazione `AudioRecord` nel servizio Android foreground di tipo `microphone`, avviato volontariamente dall’icona dopo il permesso `RECORD_AUDIO`. La notifica persistente offre **Ferma**. Il modello italiano piccolo è Apache-2.0; il download è consentito solo su Wi-Fi. Senza Wi-Fi al primo avvio l’avatar attende la connessione; un errore non provoca riavvii in loop.
- Il TTS usa lo stesso motore e l’accento selezionato nelle impostazioni dell’avatar; se la voce scelta non è disponibile, Android deve offrire una voce italiana compatibile.
- Sul Moto g35 è stata osservata la trascrizione “o la sua figlia” per il saluto. Questa variante esatta è accettata soltanto a frase conclusa, evitando di attivare una frase più lunga dalla sola trascrizione parziale.
- Durante il TTS di Voice Access o dell’avatar flottante, il riconoscitore non elabora l’audio. Dopo la fine della voce attende 500 ms e azzera il riconoscitore prima di riprendere, per scartare la coda acustica. La registrazione rimane aperta senza riavvii. La sintesi segue il comportamento di rete del motore e della voce Android selezionati; il riconoscimento del microfono resta locale.
- L’ascolto continua durante l’uso di altre app a schermo acceso e sbloccato. Nessun wake lock o biometria. I broadcast di spegnimento e sblocco sospendono/riprendono la sessione; le risposte IA divenute obsolete vengono ignorate. Durante TTS e attesa ChatGPT il flusso non viene passato al riconoscitore. Anteprime e conferme chiudono temporaneamente il microfono; il blocco schermo le scarta. Le conferme sono dialoghi Android di tipo overlay, senza un’attività trasparente permanente.
- Android deve consentire l’attività in background: nelle impostazioni Batteria dell’app attivare **Consenti l’utilizzo in background**. L’app tenta l’avvio dall’attività visibile e lascia ad Android la verifica dei permessi effettivi, senza bloccare preventivamente su uno stato batteria memorizzato. Sul Moto g35 l’impostazione va ricontrollata dopo gli aggiornamenti APK di debug.
- La sessione aggiorna l’unico avatar flottante, la sua bolla e i suoi controlli già presenti; le conferme per effetti sul dispositivo restano dialoghi Android nativi. La verifica manuale con TalkBack e font grandi resta necessaria.

## Prova su Android

Usare un emulatore o telefono di prova Android 8+ (API 26), con Wi-Fi disponibile al primo avvio e voce TTS italiana offline. Compilare con JDK 17+ e SDK 35:

```sh
./gradlew -PverificationBuild assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L’APK di verifica usa `it.solphivia.mobile.verification`, separato dall’installazione personale.

1. Al primo avvio consentire il microfono e attendere il download del modello (solo Wi-Fi; circa 48 MB). Verificare che l’avatar mostri l’avanzamento, poi che compaia una sola Solphivia flottante senza schermata chat o seconda finestra. Lasciare l’app in silenzio per almeno un minuto: l’indicatore microfono deve restare stabile, senza riaperture o suoni ripetuti. Dire “Hola, Solphivia!”, attendere la risposta TTS e l’animazione, poi “Apri WhatsApp”. Se non si attiva, leggere dalla vignetta la frase riconosciuta e aggiungerla ai test delle varianti fonetiche.
2. Ripetere e dire “Cerca gatti sul web”: annullare e ripetere confermando. Provare poi “Metti un timer di 5 minuti”, “Sveglia alle 7:30” e “Crea un appuntamento visita dentistica”; controllare che ciascuna azione chieda conferma e lasci l’ultima modifica all’app Android.
3. Con ChatGPT collegato, dire una richiesta libera, verificare risposta TTS e che Solphivia torni ad ascoltare. Verificare la cronologia chat locale e il collegamento ChatGPT. Senza collegamento, il modulo deve spiegare il requisito a voce.
4. Ripetere con “Condividi Ci vediamo alle 18 su WhatsApp”. Modificare il testo, annullare: nessuna apertura. Ripetere, confermare: verificare testo e scelta destinatario in WhatsApp, senza inviare a persone reali.
5. Senza WhatsApp installato: errore leggibile, nessun crash o apertura del browser.
6. Negare/revocare il permesso microfono, disattivare il microfono dalle impostazioni privacy, provare senza riconoscitore/TTS: messaggio di errore e nessun ascolto nascosto.
7. Silenzio e frase errata: l’attesa del saluto continua, senza effetti. “Fermati” arresta il microfono.
8. Provare Home e WhatsApp: il microfono deve restare attivo e non silenziato, e un secondo comando deve funzionare. Spegnere/bloccare lo schermo: il microfono deve spegnersi e nessun comando tardivo deve essere eseguito. Allo sblocco deve tornare l’attesa del saluto. Provare **Fermati** e **Ferma** nella notifica: l’ascolto deve terminare.
9. Dopo il download, disattivare Wi-Fi e ripetere il saluto: il riconoscimento deve funzionare offline. Al primo avvio senza Wi-Fi, verificare che non parta alcun download su rete mobile e che l’avatar spieghi cosa manca.
10. TalkBack, caratteri grandi e orientamento orizzontale: verificare etichette, anteprima e conferma. Provare anche con overlay preesistente attivo: eventuale audio di altre funzioni non deve produrre un invio.

La CI verifica parser, compilazione degli APK e lint. Non simula un motore vocale reale né certifica il comportamento di WhatsApp: i passaggi sopra sono collaudo manuale richiesto prima del rilascio.

### Verifiche locali

- PASS: build APK debug, APK strumentale debug e `lintDebug` con JDK 17 e SDK 35.
- PASS: 33 controlli puri del parser di wake phrase, allowlist WhatsApp e ShareBridge.
- PASS: build APK debug, APK strumentale debug e `lintDebug` dopo l’integrazione dell’ascolto nella `MainActivity`.
- PASS sul Moto g35 (Android 16): servizio foreground di tipo microfono attivo, cattura non silenziata con WhatsApp davanti, registrazione interrotta a schermo spento/bloccato.
- PASS utente sulla revisione del servizio: dopo lo sblocco, saluto TTS, WhatsApp visibile senza schermata nera e comando successivo **Fermati** riconosciuto. Verificato anche lo spegnimento effettivo del microfono.
- PASS utente del 17 ottobre 2026: saluto «Hola, Marco!», apertura WhatsApp e nuovo richiamo durante la sessione. La correzione accetta la trascrizione esatta «o la sua figlia» anche quando stabile per 900 ms, annulla l'attivazione se il testo cambia e riconosce nuovamente il saluto durante la sessione comandi. Non usa corrispondenze generiche con frasi comuni. Stabilità dopo riavvio e su altri dispositivi ancora da verificare.
- NOT RUN: il test strumentale overlay non ha completato l’esecuzione nel runner Android personalizzato; la prima esecuzione rilevava un riferimento di test obsoleto (`homeShortcutAttempted`), che è stato rimosso. La seconda esecuzione ha restituito un bundle vuoto prima di qualsiasi risultato.

## Roadmap separata

- **0.1 (questa PR):** modulo Android Voice Access + ShareBridge, saluto e condivisione controllata. Il piccolo parser è già Java puro, ma non è una libreria distribuita.
- **0.2 (previsto):** estrarre un componente open source con API pubblica e test di contratto utilizzabile da ChatGPT, Qwen e altri modelli; mantenere il consenso e gli effetti sul dispositivo nel livello Android.
- **0.3 (previsto):** plugin MCP sperimentale, documentazione, test, informativa privacy e valutazione della candidatura alla directory OpenAI. Nessuna candidatura o integrazione MCP è inclusa o promessa come approvata.
- **AI Memory Vault:** lavoro distinto; nessuna migrazione o lettura/scrittura introdotta.

Riferimenti: [Vosk Android](https://alphacephei.com/vosk/android), [modello italiano Vosk e licenza](https://alphacephei.com/vosk/models), [libreria Vosk Android](https://github.com/alphacep/vosk-api), [fine TTS](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener), [condivisione Android](https://developer.android.com/training/sharing/send).
