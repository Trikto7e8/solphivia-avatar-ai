# Data safety — mappa da validare sulla candidata Play

Bozza tecnica, non un modulo inviato. Google considera anche i trasferimenti ai fornitori e le librerie/servizi terzi: non dichiarare «nessun dato raccolto» soltanto perché il manutentore non possiede un server. [Guida ufficiale](https://support.google.com/googleplay/android-developer/answer/10787469).

| Flusso presente nel codice | Dichiarazione da verificare |
| --- | --- |
| Messaggi e cronologia recente inviati al provider AI via HTTPS | Messaggi/altri contenuti generati dall’utente; finalità funzionalità; scelta facoltativa di collegarsi. Retention e ruolo del provider da confermare. |
| File e immagini scelti e inviati | File/documenti e foto secondo il contenuto, su azione esplicita. Non includere automaticamente vault o allegati nelle richieste vocali. |
| Email e metadati account dall’OAuth | Informazioni personali/identificativi account per gestione e funzionalità; distinguere dati mantenuti solo localmente da trasferimenti. |
| Audio di comandi verso il riconoscitore Android, quando il servizio usa rete | Registrazioni/audio: verificare concretamente servizio, contratto, transito e trattamento; la funzione vocale è facoltativa. Non attribuire al riconoscitore locale il trattamento del servizio Android. |
| TTS configurato, eventualmente online | Il testo della risposta può essere trattato dal motore installato; verificare comportamento e termini di quel motore. |
| Coordinate meteo/GPS e richieste radar | Posizione approssimativa o precisa secondo percorso e dati effettivamente inviati; funzionalità facoltativa. |
| Memoria personale e backup cifrati | Elaborazione locale; includere nel censimento le sole esportazioni e gli invii scelti dall’utente. |
| Token OAuth cifrati Android Keystore | Autenticazione; non inserirli in log, screenshot o segnalazioni. |

Nessun SDK pubblicitario o di analytics individuato fra le dipendenze correnti. Questa osservazione non dimostra l’assenza di traffico dei servizi terzi Android. La cronologia locale non ha una cifratura applicativa separata, mentre memoria personale e credenziali hanno protezioni specifiche.

Da completare: classificazione collected/shared con le eccezioni applicabili, obbligatorio/facoltativo, effimero, retention, cancellazione, account deletion se applicabile e dichiarazioni di ciascun fornitore. Non spuntare automaticamente le caselle da questa tabella.
