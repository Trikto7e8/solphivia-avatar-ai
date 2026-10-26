# Permessi e servizi — evidenze da proporre

- SYSTEM_ALERT_WINDOW: avatar flottante richiesto dall’utente. Non è lettura schermo, AccessibilityService o automazione dei tocchi.
- RECORD_AUDIO + FOREGROUND_SERVICE_MICROPHONE: richiamo locale persistente mentre l’app è attiva, arresto/ripresa del TTS e sessione vocale. Notifica del microfono e controllo di arresto visibili; non si avvia al boot.
- FOREGROUND_SERVICE_SPECIAL_USE, PetService: sovrapposizione animata avviata dall’utente, notificata e chiudibile.
- FOREGROUND_SERVICE_SPECIAL_USE, OAuthKeepAliveService: mantiene la callback di autorizzazione durante il passaggio al browser; verificare eccezioni, timeout e arresto. La sola dichiarazione nel manifest non implica accettazione Play.
- POST_NOTIFICATIONS: stato e controlli di avatar e microfono.
- INTERNET: provider AI e funzioni online; wake e controlli locali non richiedono invio audio a un server del progetto.
- Posizione precisa/approssimativa: percorso meteo facoltativo; città manuale alternativa. Non è richiesta per chat o richiamo.
- Allegati: selettore documenti Android con URI concessi, nessun accesso indiscriminato a tutti i file.

Preparare video reale per Play Console: aprire app e consenso → avatar/notifica → richiamo → comando e trascrizione → Stop/Riprendi → spegnimento microfono → chiusura completa. Usare dati sintetici. Il manutentore deve approvare le finalità delle dichiarazioni sulla candidata firmata.

Non aggiungere permessi SMS, rubrica o chiamata diretta: non servono alle funzioni attuali. Le chiamate passano da ACTION_DIAL e richiedono conferma dell’utente.
