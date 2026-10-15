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
| File selezionati | Fino a 5 allegati selezionati con il selettore Android; 8 MB ciascuno e 15 MB complessivi per la conversazione inviata. Sono supportati documenti, immagini e file di testo. I riferimenti URI e i permessi persistenti consentono di rileggere gli allegati nei turni successivi. La cronologia conserva riferimenti e metadati, non i byte; questi vengono letti e trasmessi quando necessari alla richiesta. |
| Voce | Il pulsante microfono apre il riconoscitore vocale scelto/configurato in Android, in lingua italiana. Solphivia riceve il testo riconosciuto; l'eventuale trattamento dell'audio dipende dal servizio Android selezionato e dalla sua informativa. | La trascrizione resta in bozza fino all’invio esplicito.
| Sintesi vocale | La risposta viene letta dal motore TTS installato/configurato sul telefono. Solphivia non invia l'audio TTS a un proprio server. |
| Posizione e overlay | Il personaggio usa il permesso Android “Mostra sopra altre app” per disegnare la propria interfaccia. Non acquisisce schermate, non legge ciò che mostrano altre app e non automatizza tocchi su di esse. La posizione dell'avatar sulla schermata viene salvata localmente. |

## Controlli dell'utente

### Voice Access e ShareBridge (modulo v0.1)

La schermata Voice Access richiede il microfono solo dopo Avvia. Due ascolti limitati a 15 secondi riconoscono il saluto e un comando; uscita o sospensione interrompono la sessione. Nessun ascolto continuo, identificazione biometrica o accesso ad AI Memory Vault. Il nome scelto viene conservato in preferenze private separate; audio, trascrizioni e anteprime non vengono salvati dal modulo.

Il riconoscimento locale è preferito sui dispositivi compatibili. L’uso del riconoscitore di sistema richiede una scelta esplicita e può comportare elaborazione online da parte del suo fornitore. Il saluto usa una voce italiana dichiarata offline. ShareBridge mostra il testo modificabile e passa quel testo a WhatsApp soltanto dopo la conferma “Apri condivisione WhatsApp”; destinatario e invio finale vengono scelti dentro WhatsApp, secondo la sua informativa. Nessun invio automatico. [Dettagli e prove](docs/VOICE-ACCESS-IT.md).

### Controlli generali

- Non collegare l'account se non vuoi concedere i permessi mostrati dal browser.
- Puoi fermare l'avatar dalla notifica persistente o revocare il permesso overlay nelle impostazioni Android.
- La cronologia e i dati locali dell'app si possono eliminare cancellando i dati dell'app o disinstallandola; questo disconnette anche il token locale. Per revocare l'autorizzazione lato account, controlla le impostazioni di account ChatGPT/OpenAI.
- Evita di inviare dati sensibili o file che non vuoi trasmettere al fornitore AI.

## Prima della pubblicazione

Il manutentore deve aggiungere un indirizzo di contatto e un URL privacy pubblici, confermare la retention lato fornitore applicabile al tipo di account e alla configurazione usata, compilare le dichiarazioni Data safety dello store e ricontrollare questa descrizione sulla build finale. Nessun recapito personale è stato inventato o aggiunto a questa bozza.

## Memoria, voce e meteo nella 0.9.2-alpha1

La memoria personale è cifrata localmente con AES-GCM e Android Keystore, separata dalla cronologia. Prima di ogni richiesta che la include, viene mostrata un’anteprima modificabile: si può confermare, inviare senza memoria o annullare. Il consenso vale per un solo invio e scade dopo cinque minuti. Il modello non salva ricordi autonomamente.

L’importazione dei backup AI Memory Vault viene decifrata sul telefono con la password: vengono proposti il profilo e i ricordi approvati, ordinari e non scaduti. L’esportazione crea un nuovo backup cifrato portabile, senza sincronizzazione automatica. Non conserva tutti i metadati del vault originale; conservarne una copia separata.

La voce inglese viene scelta fra quelle offline disponibili; il ripiego italiano dipende dal motore Android e può usare la rete. La località meteo può essere inserita manualmente o ottenuta dal GPS autorizzato. Le richieste meteo e radar contattano Open-Meteo e Radar-DPC; coordinate e tile richieste indicano ai servizi l’area consultata. Gli avvisi sono sperimentali e funzionano mentre l’overlay è attivo.
