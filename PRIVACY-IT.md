# Privacy: cosa fa la versione attuale

**Bozza informativa tecnica per la beta comunitaria, non parere legale né informativa completa per uno store.** Prima di una distribuzione pubblica stabile o su Google Play, il titolare del progetto dovrà completare questa pagina con i propri recapiti, verificare il comportamento della build effettivamente pubblicata e predisporre una pagina privacy raggiungibile sul web.

## Dati e flussi

| Dato | Dove/come viene usato |
| --- | --- |
| Password ChatGPT | Non viene chiesta né inserita in Solphivia. L'accesso avviene nella pagina di autorizzazione aperta dal browser. |
| Token OAuth | Usati per collegare ChatGPT; salvati nei dati privati dell'app come valori cifrati con una chiave Android Keystore. Eliminando i dati dell'app o disinstallandola si rimuove il token conservato localmente. |
| Email e impostazioni dell'account | L'email restituita dal consenso e alcuni metadati OAuth sono salvati nelle preferenze private dell'app per mostrare/ricordare l'account. Questi campi non sono cifrati separatamente. |
| Conversazione | Fino a 16 messaggi recenti sono salvati in SharedPreferences private dell'app per mantenere il contesto fra richieste. Il testo è in chiaro nei dati privati locali, non in una cartella pubblica. Android li protegge con il sandbox dell'app; la build non implementa cifratura specifica della cronologia. |
| Messaggi e allegati inviati | Quando invii una richiesta, la cronologia recente e gli allegati scelti vengono inviati via HTTPS al servizio OpenAI Responses per generare la risposta. La richiesta imposta `store: false`; ciò non sostituisce né modifica le regole di trattamento, sicurezza o conservazione stabilite dal fornitore. Leggi i termini e le informative di OpenAI. |
| File selezionati | Fino a 5 allegati selezionati con il selettore Android; 8 MB ciascuno e 15 MB complessivi per la conversazione inviata. Sono supportati documenti, immagini e file di testo. I riferimenti URI e i permessi persistenti consentono di rileggere gli allegati nei turni successivi. La cronologia conserva riferimenti e metadati, non i byte; questi vengono letti e trasmessi quando necessari alla richiesta. |
| Voce e trascrizione | Il richiamo è elaborato localmente. Il riconoscitore Android dei comandi può usare Internet e restituisce testo mostrato nel rigo di scrittura; la conversazione vocale invia automaticamente il risultato con la cronologia recente, senza memoria del vault né allegati. Dopo ogni risposta si riapre una finestra di cinque secondi dal microfono pronto. La dettatura singola esplicitamente modificabile resta una bozza. Il trattamento audio dipende dal servizio Android configurato. |
| Sintesi vocale | La risposta viene letta dal motore TTS installato/configurato sul telefono. Solphivia non invia l'audio TTS a un proprio server. |
| Posizione e overlay | Il personaggio usa il permesso Android “Mostra sopra altre app” per disegnare la propria interfaccia. Non acquisisce schermate, non legge ciò che mostrano altre app e non automatizza tocchi su di esse. La posizione dell'avatar sulla schermata viene salvata localmente. |

## Controlli dell'utente

### Conversazione vocale 1.1 beta 3

Aprire Solphivia avvia l’attesa locale del richiamo se overlay, microfono e richiamo automatico sono abilitati. La notifica indica il microfono attivo. Stop dialogo torna al richiamo, Spegni microfono arresta la cattura. Nessuna identificazione biometrica. Schermo bloccato e consumi prolungati restano da verificare nella 1.1 beta 3.

In 1.1 beta 3 l’attesa del richiamo usa Vosk locale con il modello italiano incluso nell’APK. Non salva registrazioni audio. Dopo il richiamo, il comando viene trascritto dal riconoscitore Android, che può usare Internet: il trattamento dell’audio dipende dal servizio configurato. Le richieste vocali libere vengono inviate automaticamente al fornitore collegato con la cronologia testuale recente, senza aggiungere memoria personale del vault né allegati; la chat scritta mantiene l’anteprima della memoria e il consenso monouso. La dettatura singola esplicitamente modificabile resta una bozza. Il TTS usa il motore Android selezionato e può usare la rete. I comandi locali aprono app, Maps o il compositore telefonico: nessuna lettura o invio di messaggi, nessuna lettura della rubrica e nessuna chiamata senza conferma manuale. ShareBridge resta inattivo in 1.1 beta 3. Stop dialogo torna al richiamo; Spegni microfono arresta la cattura. Il microfono riparte all’apertura se l’opzione automatica è attiva, non al riavvio del telefono. [Dettagli](docs/VOICE-SESSION.md).

Nella 1.1 beta 3 le risposte vocali successive sono ammesse per cinque secondi dal microfono pronto dopo ogni risposta; la trascrizione appare nel rigo della chat. Le anteprime vocali non sovrascrivono una bozza scritta o modificata a mano. Il consenso memoria è monouso ed è legato anche al digest SHA-256 dei contenuti materializzati degli allegati.

### Controlli generali

- Non collegare l'account se non vuoi concedere i permessi mostrati dal browser.
- Puoi fermare l'avatar dalla notifica persistente o revocare il permesso overlay nelle impostazioni Android.
- La cronologia e i dati locali dell'app si possono eliminare cancellando i dati dell'app o disinstallandola; questo disconnette anche il token locale. Per revocare l'autorizzazione lato account, controlla le impostazioni di account ChatGPT/OpenAI.
- Evita di inviare dati sensibili o file che non vuoi trasmettere al fornitore AI.

## Prima della pubblicazione

Il manutentore deve aggiungere un indirizzo di contatto e un URL privacy pubblici, confermare la retention lato fornitore applicabile al tipo di account e alla configurazione usata, compilare le dichiarazioni Data safety dello store e ricontrollare questa descrizione sulla build finale. Nessun recapito personale è stato inventato o aggiunto a questa bozza.

## Memoria, voce e meteo in 1.1 beta 3

La memoria personale è cifrata localmente con AES-GCM e Android Keystore, separata dalla cronologia. Prima di ogni richiesta che la include, viene mostrata un’anteprima modificabile: si può confermare, inviare senza memoria o annullare. Il consenso vale per un solo invio e scade dopo cinque minuti. Il modello non salva ricordi autonomamente.

L’importazione dei backup AI Memory Vault viene decifrata sul telefono con la password: vengono proposti il profilo e i ricordi approvati, ordinari e non scaduti. L’esportazione crea un nuovo backup cifrato portabile, senza sincronizzazione automatica. Non conserva tutti i metadati del vault originale; conservarne una copia separata.

La voce inglese viene scelta fra quelle offline disponibili; il ripiego italiano dipende dal motore Android e può usare la rete. La località meteo può essere inserita manualmente o ottenuta dal GPS autorizzato. Le richieste meteo e radar contattano Open-Meteo e Radar-DPC; coordinate e tile richieste indicano ai servizi l’area consultata. Gli avvisi sono sperimentali e funzionano mentre l’overlay è attivo.

## Chiusura nella 1.1 beta 3

Il pulsante tondo ⏻ o Chiudi Solphivia dalla notifica dell’avatar ferma servizi, lettura e microfono e chiude le attività dell’app; conserva account e dati. Richiamo riattivato solo alla successiva apertura con l’opzione automatica attiva. L’icona normale può essere trascinata dall’elenco app alla Home; non vengono create automaticamente scorciatoie con badge.

## Arresto e ripresa della lettura

Durante il parlato, il rilevatore locale ascolta le frasi di arresto abilitate. Una lettura sospesa può essere ripresa con una frase dedicata. Nessuna registrazione audio è conservata da Solphivia. Il testo letto e la posizione di ripresa restano in memoria del processo; vengono sostituiti da una nuova richiesta o rimossi dalla chiusura. Le parole brevi sono verificate localmente contro la frase completa. Non è autenticazione biometrica. La trascrizione inviata resta nella barra durante la risposta e rispetta le bozze modificate a mano.
