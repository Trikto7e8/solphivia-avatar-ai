# Privacy: cosa fa la versione attuale

**Bozza informativa tecnica per l'alpha comunitaria, non parere legale né informativa completa per uno store.** Prima di una distribuzione pubblica stabile o su Google Play, il titolare del progetto dovrà completare questa pagina con i propri recapiti, verificare il comportamento della build effettivamente pubblicata e predisporre una pagina privacy raggiungibile sul web.

## Dati e flussi

| Dato | Dove/come viene usato |
| --- | --- |
| Password ChatGPT | Non viene chiesta né inserita in Solphivia. L'accesso avviene nella pagina di autorizzazione aperta dal browser. |
| Token OAuth | Usati per collegare ChatGPT; salvati nei dati privati dell'app come valori cifrati con una chiave Android Keystore. Eliminando i dati dell'app o disinstallandola si rimuove il token conservato localmente. |
| Email e impostazioni dell'account | L'email restituita dal consenso e alcuni metadati OAuth sono salvati nelle preferenze private dell'app per mostrare/ricordare l'account. Questi campi non sono cifrati separatamente. |
| Conversazione | Fino a 16 messaggi recenti sono salvati in SharedPreferences private dell'app per mantenere il contesto fra richieste. Il testo è in chiaro nei dati privati locali, non in una cartella pubblica. Android li protegge con il sandbox dell'app; la build non implementa cifratura specifica della cronologia. |
| Messaggi e allegati inviati | Quando invii una richiesta, la cronologia recente e gli allegati scelti vengono inviati via HTTPS al servizio OpenAI Responses per generare la risposta. La richiesta imposta `store: false`; ciò non sostituisce né modifica le regole di trattamento, sicurezza o conservazione stabilite dal fornitore. Leggi i termini e le informative di OpenAI. |
| File selezionati | Il selettore Android dà accesso solo ai file scelti. Sono ammessi fino a 5 file: PDF, DOC/DOCX o immagini; 8 MB massimo ciascuno e 15 MB complessivi. Il contenuto viene letto per l'invio e i permessi persistenti di lettura vengono rilasciati al termine della lettura. Nella cronologia locale vengono conservati il testo digitato e i nomi degli allegati, non i byte dei file. |
| Voce | Il pulsante microfono apre il riconoscitore vocale scelto/configurato in Android, in lingua italiana. Solphivia riceve il testo riconosciuto; l'eventuale trattamento dell'audio dipende dal servizio Android selezionato e dalla sua informativa. |
| Sintesi vocale | La risposta viene letta dal motore TTS installato/configurato sul telefono. Solphivia non invia l'audio TTS a un proprio server. |
| Posizione e overlay | Il personaggio usa il permesso Android “Mostra sopra altre app” per disegnare la propria interfaccia. Non acquisisce schermate, non legge ciò che mostrano altre app e non automatizza tocchi su di esse. La posizione dell'avatar sulla schermata viene salvata localmente. |

## Controlli dell'utente

- Non collegare l'account se non vuoi concedere i permessi mostrati dal browser.
- Puoi fermare l'avatar dalla notifica persistente o revocare il permesso overlay nelle impostazioni Android.
- La cronologia e i dati locali dell'app si possono eliminare cancellando i dati dell'app o disinstallandola; questo disconnette anche il token locale. Per revocare l'autorizzazione lato account, controlla le impostazioni di account ChatGPT/OpenAI.
- Evita di inviare dati sensibili o file che non vuoi trasmettere al fornitore AI.

## Prima della pubblicazione

Il manutentore deve aggiungere un indirizzo di contatto e un URL privacy pubblici, confermare la retention lato fornitore applicabile al tipo di account e alla configurazione usata, compilare le dichiarazioni Data safety dello store e ricontrollare questa descrizione sulla build finale. Nessun recapito personale è stato inventato o aggiunto a questa bozza.
