# Voice conversation in 1.1 beta 2 / Conversazione vocale

## Interruzione sperimentale / Experimental interruption

Con il microfono già attivo, durante la lettura puoi provare **«Basta Solphivia»** o **«Fermati Solphivia»**. Attendi il segnale prima del nuovo comando. **Ferma la voce** è disponibile anche nel menu e nella notifica; non riaccende il microfono spento. Le sezioni successive descrivono il normale percorso dei comandi: nella 1.1 solo il riconoscitore locale delle frasi di interruzione resta attivo durante il parlato, se abilitato. [Stato delle prove e limiti](releases/v1.1.0-beta.2.md).

With the microphone already active, try **“Basta Solphivia”** or **“Fermati Solphivia”** during reading. Wait for the microphone cue before the next command. “Ferma la voce” also provides a manual control. Regular command capture remains suspended during output; only the enabled local interruption recognizer listens. Live acoustic validation is pending.

## Stop, ripresa e trascrizione

Puoi anche dire **Stop**, **Alt**, **Basta** o **Fermati**, come frasi complete. Dopo l’arresto, **Prosegui**, **Riprendi**, **Continua**, **Dimmi**, **Ti ascolto**, **Dai** e **Vai** riprendono la lettura sospesa. Restano attivi solo se il microfono è acceso; le frasi di ripresa hanno questo significato locale solo quando c’è una lettura sospesa. La ripresa usa la posizione comunicata dal motore: senza aggiornamenti precisi può ripetere il blocco in corso.

La trascrizione inviata resta nella barra durante la risposta; si libera al termine della risposta o viene sostituita dalla frase successiva. Una bozza modificata a mano non viene cancellata dalla pulizia automatica. Il pulsante ⏻ chiude tutta la sessione; le voci di chiusura sono state rimosse dal menu.

## English

Open Solphivia, grant overlay and microphone permissions, and finish the opening greeting. Say **“Hola Solphivia”**, wait for **“Cosa posso fare per te?”**, then speak a question, an app command or an ordinary conversational reply.

After every response finishes, Android command recognition reopens. You have **five seconds from microphone readiness** to start speaking without another wake phrase. Speech started in time may finish after the window. Silence returns to local wake waiting. Recognition retries do not extend the same window; microphone/service errors can interrupt it. The timer is checked every 250 ms. While generation or speech output is busy, command capture is suspended.

Recognized words appear in the same input used for typing, then the final result is sent automatically. Progressive preview depends on the Android recognition service providing partial results. The final preview is briefly shown even when partial results are unavailable. A pre-existing typed draft or a manually edited preview is preserved instead of being overwritten or automatically submitted by voice.

Wake waiting uses the bundled Italian Vosk model. Android command recognition and the selected TTS engine may use the network. Solphivia does not save wake audio recordings. Voice requests include recent text history, without adding vault memories or attachments. Typed requests retain editable memory preview and single-use consent. Single-phrase editable dictation remains a separate option.

Local commands include opening an installed app by its exact name, Maps searches/directions, camera/phone opening and preparing a number in the dialer. The call still needs manual confirmation; there is no contact lookup or direct call permission. Only explicit user commands are executed, never model output. ShareBridge remains inactive; configurable Ollama/LM Studio adapters remain future work.

**“Stop dialogo”** returns to wake waiting. **“Spegni microfono”** stops capture. Opening the app can restart it if automatic wake is enabled; there is no microphone start at phone boot. The notification shows microphone use. This is not voice authentication.

## Italiano

Apri Solphivia, concedi overlay e microfono e attendi la fine del saluto iniziale. Di’ **«Hola Solphivia»**, aspetta **«Cosa posso fare per te?»**, poi pronuncia una domanda, un comando o una normale risposta a una sua domanda.

Dopo ogni risposta il microfono si riapre: hai **cinque secondi da quando è pronto** per iniziare a parlare senza ripetere il richiamo. Se inizi in tempo puoi finire la frase. In silenzio torna al riposo fino a «Hola Solphivia». Gli errori del servizio Android possono interrompere l’acquisizione. Durante generazione e riproduzione della risposta l’ascolto del comando è sospeso.

Le parole riconosciute compaiono nel rigo dove puoi scrivere a mano; il risultato finale viene inviato automaticamente. L’anteprima progressiva dipende dal riconoscitore Android. Se esiste una bozza scritta o modifichi a mano il testo, la bozza viene conservata e non sovrascritta o inviata dal percorso vocale.

L’attesa del richiamo usa Vosk locale; riconoscimento dei comandi e TTS Android possono usare Internet. La voce invia la cronologia testuale recente senza aggiungere ricordi del vault o allegati. La chat scritta conserva la conferma modificabile della memoria. La dettatura singola da modificare resta separata.

Puoi dire «apri Maps», «apri» seguito dal nome completo dell’app, «cerca una farmacia su Maps», «portami a Roma», «chiama» seguito da un numero. Per chiamare confermi nel telefono; la rubrica non è supportata. «Comandi» mostra un riepilogo. «Stop dialogo» torna al richiamo; «Spegni microfono» ferma la cattura.

Se Android limita l’ascolto, controlla le impostazioni batteria della sola Solphivia. Non occorre modificare globalmente la gestione batteria. Consumi prolungati, blocco schermo e altri dispositivi richiedono ulteriori prove.

[Release notes and measured limits / Note e limiti](releases/v1.0.0-beta.1.md)

## Chiudere tutta la sessione

Pulsante tondo ⏻ sotto l’avatar, o il comando Chiudi Solphivia dopo il richiamo. Si fermano avatar, lettura e microfono. Come faccio a chiuderti? spiega i controlli senza chiudere. Stop dialogo lascia il richiamo, Spegni microfono lascia l’avatar. Ferma la voce interrompe solo la lettura.
