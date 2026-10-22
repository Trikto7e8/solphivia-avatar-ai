# Voice session in alpha9 / Sessione vocale

The greeting is processed locally by Vosk. After «Hola Solphivia», Solphivia replies «Cosa posso fare per te?» and opens Android recognition for one command. Android recognition may use the network. It then returns to local wake waiting. While Solphivia speaks, wake decoding is suspended. This is not voice authentication.

Voice requests are sent automatically with recent text history, without adding vault memory or attachments. Typed requests keep memory preview and single-use approval. The editable single-phrase dictation option remains separate.

«Apri WhatsApp» launches the app locally. It does not send messages, resolve recipients or read the screen. Other app-control requests are not generally supported. Alpha5 ShareBridge is not active in this version.

Stop dialogo returns to wake waiting. Spegni microfono stops capture; reopening the app restarts it only if automatic wake is enabled. No boot receiver starts the microphone. The notification indicates microphone use.

## Italiano

Apri Solphivia, concedi overlay e microfono, attendi il saluto iniziale. Di’ «Hola Solphivia», aspetta «Cosa posso fare per te?», poi pronuncia la richiesta. Nessun tasto Invia per la sessione vocale. Le domande vanno al fornitore collegato con la cronologia testuale, senza aggiungere memoria personale e allegati. I comandi locali restano sul telefono.

Se l’ascolto viene limitato da Android, controlla l’uso in background della sola Solphivia. Su alcuni Motorola esistono anche Ottimizzazione standby app e Arresta app dopo il blocco schermo. Non occorre cambiare globalmente la gestione batteria. Comportamento a schermo bloccato, consumi prolungati e altri dispositivi richiedono ancora prove.

[Release notes and measured limits](releases/v0.9.2-alpha9.md).
