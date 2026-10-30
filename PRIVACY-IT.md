# Informativa sulla privacy — Solphivia IA

**Proposta per Google Play, aggiornata il 31 ottobre 2026. Non ancora pronta da usare come informativa definitiva:** manca il recapito pubblico approvato; vanno confermati titolare, basi giuridiche e condizioni del collegamento al fornitore AI. Il testo descrive la beta 1.1.0-beta.3 (versionCode 49), pacchetto `it.solphivia.mobile`, esaminata al commit `e0ff501429502f6416c777de3c0649891e5dd2d1`. La pagina definitiva dovrà essere pubblicamente accessibile e collegata anche nell'app.

## Sviluppatore e contatto privacy

Sviluppatore del progetto: account GitHub **Trikto7e8**, repository [solphivia-avatar-ai](https://github.com/Trikto7e8/solphivia-avatar-ai).

Titolare proposto, da confermare prima dell'uso definitivo: **Marco Iacobellis**. Recapito privacy: **[DA COMPLETARE CON UN RECAPITO PUBBLICO APPROVATO]**. Non pubblicare dati personali o richieste di cancellazione nelle issue pubbliche di GitHub.

## Finalità e scelta delle funzioni

Solphivia IA offre un avatar flottante, chat con il fornitore AI collegato, interazione vocale, memoria personale locale e funzioni meteo. Dati e permessi servono alle funzioni attivate dall'utente. Non è obbligatorio concedere microfono, posizione o overlay per il solo fatto di installare l'app; negare o revocare un permesso impedisce la funzione che ne dipende. Senza collegamento al fornitore AI non è possibile usare la conversazione AI.

Nella configurazione esaminata non risultano SDK pubblicitari o analytics nelle dipendenze dell'app. Le richieste di rete dirette sono effettuate dal telefono ai servizi indicati sotto: il codice esaminato non usa un server intermedio del manutentore per ricevere le conversazioni. Questo non significa che tutti i dati restino sul telefono.

## Basi giuridiche proposte — da confermare dal titolare

Per l'erogazione delle funzioni esplicitamente richieste, la base proposta è l'esecuzione del servizio richiesto (art. 6, par. 1, lett. b GDPR); per i trattamenti facoltativi basati sul consenso, incluso l'invio della memoria dopo l'anteprima, è il consenso revocabile (art. 6, par. 1, lett. a). Un permesso Android non sostituisce automaticamente ogni consenso o informativa richiesti. La qualificazione dei ruoli del manutentore e dei fornitori deve essere verificata rispetto al servizio effettivamente distribuito; non vengono qui dichiarati accordi di trattamento o garanzie di trasferimento non verificati.

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

## Conservazione, cancellazione e sicurezza

- **Cronologia:** al massimo 16 messaggi recenti nel contesto locale salvato. Il comando «nuova chat» rimuove la cronologia locale salvata; non cancella ciò che è già stato trasmesso a terzi.
- **Account:** token cifrati con Android Keystore; email, scadenza e metadati nelle preferenze private. Restano nei dati dell'app fino alla loro cancellazione o disinstallazione. La rimozione locale non elimina l'account OpenAI e non sostituisce la revoca lato fornitore.
- **Memoria personale:** conserva il testo scelto dall'utente fino a modifica/sostituzione o cancellazione dei dati dell'app. La memoria è cifrata con AES-GCM e chiave Android Keystore; la cronologia non ha questa cifratura aggiuntiva. Nella schermata Memoria è possibile modificare il testo e salvarlo.
- **Allegati:** la cronologia contiene riferimenti URI e metadati, non una copia persistente dei byte del file. La cancellazione dei dati dell'app non elimina gli originali nel servizio o nella cartella scelti dall'utente.
- **Audio:** l'app non conserva registrazioni del richiamo o della conversazione. Il servizio Android di riconoscimento può trattare l'audio secondo la propria informativa; il testo riconosciuto può entrare nella cronologia e nelle richieste AI.
- **Meteo e impostazioni:** località, coordinate selezionate, posizione dell'avatar e preferenze restano salvate localmente fino a sostituzione o cancellazione dei dati dell'app. Il permesso di posizione può essere revocato nelle impostazioni Android.
- **Backup:** il backup automatico Android è disabilitato nel manifest. I file esportati volontariamente dall'utente restano nella destinazione scelta anche dopo la disinstallazione: eliminarli separatamente quando non servono più.
- **Cancellazione completa sul telefono:** Impostazioni Android → App → Solphivia IA → Archiviazione → Cancella dati (i nomi possono variare). Questa operazione rimuove dati e collegamento locali. Conservare prima solo gli eventuali backup desiderati.
- **Dati già inviati:** per cancellazione, accesso e conservazione presso OpenAI o altri fornitori valgono anche le loro procedure. `store: false` non equivale a zero conservazione o a una garanzia di esclusione da ogni trattamento.

I dati dell'app sono protetti dal sandbox Android. Le comunicazioni verso i servizi elencati usano HTTPS; ciò non elimina i rischi di un telefono compromesso, di un backup esportato o dell'invio volontario di informazioni sensibili. Non viene dichiarata una cifratura end-to-end delle conversazioni AI.

## Destinatari e servizi esterni

- **OpenAI:** autorizzazione dell'account, consultazione dei modelli e generazione delle risposte attraverso `auth.openai.com` e `api.openai.com`. Riceve i dati necessari al collegamento e, per la chat, testo, contesto recente, allegati scelti e memoria quando approvata. Informativa: https://openai.com/policies/privacy-policy/ .
- **Servizi Android di riconoscimento e sintesi vocale:** il fornitore dipende dal servizio installato e scelto sul telefono. Può elaborare audio o testo anche online; consultare le impostazioni e l'informativa di quel servizio.
- **Open-Meteo:** la ricerca manuale invia la località a `geocoding-api.open-meteo.com`; le previsioni inviano coordinate a `api.open-meteo.com`, anche se ottenute dal GPS autorizzato. Informativa: https://open-meteo.com/en/terms .
- **Radar della Protezione Civile e relativo hosting AWS:** richieste ai prodotti radar e alle tile attraverso `radar-api.protezionecivile.it` e il bucket radar AWS; le tile indicano l'area visualizzata. Sito del servizio: https://radar.protezionecivile.it/ .
- **App esterne aperte su comando:** Android può aprire Maps, altre app o il compositore telefonico; l'eventuale ricerca o destinazione viene passata all'app richiesta, che opera secondo la propria informativa.

I servizi contattati possono ricevere indirizzo IP e metadati tecnici delle richieste. Le condizioni di conservazione e gli eventuali trattamenti fuori dallo Spazio economico europeo dipendono dal fornitore e dall'account usato. Prima del rilascio occorre verificare ruoli, condizioni e garanzie applicabili: non viene promessa una localizzazione esclusiva dei dati nell'UE.

## Diritti e richieste

Per i trattamenti di competenza del titolare, l'utente può chiedere accesso, rettifica, cancellazione, limitazione e portabilità, nonché opporsi nei casi previsti dal GDPR, usando il recapito privacy da completare sopra. Può revocare i consensi per il futuro senza pregiudicare la liceità dei trattamenti precedenti. Restano disponibili i controlli locali descritti in questa pagina e il diritto di presentare reclamo al [Garante per la protezione dei dati personali](https://www.garanteprivacy.it/). Il manutentore non può accedere a distanza ai dati conservati soltanto sul telefono.

Le risposte AI sono generate automaticamente e possono essere errate; il codice esaminato non le usa per assumere decisioni con effetti giuridici o analogamente significativi sull'utente. Evitare l'invio di dati sensibili propri o di terzi. Le limitazioni d'età e il pubblico dichiarato nello Store devono essere coerenti con le condizioni dei servizi collegati.

## Modifiche e completamento prima della pubblicazione

Le modifiche a dati, funzioni o fornitori richiedono l'aggiornamento dell'informativa e delle dichiarazioni Data safety. Prima del rilascio: confermare il titolare e il recapito pubblico; verificare le condizioni del fornitore AI per questo collegamento; pubblicare una pagina web definitiva accessibile senza login; collegarla nell'app; verificare il comportamento della build finale. Questa proposta non attesta la conformità né l'approvazione Google Play.

## Memoria, voce e meteo in 1.1 beta 3

La memoria personale è cifrata localmente con AES-GCM e Android Keystore, separata dalla cronologia. Prima di ogni richiesta che la include, viene mostrata un’anteprima modificabile: si può confermare, inviare senza memoria o annullare. Il consenso vale per un solo invio e scade dopo cinque minuti. Il modello non salva ricordi autonomamente.

L’importazione dei backup AI Memory Vault viene decifrata sul telefono con la password: vengono proposti il profilo e i ricordi approvati, ordinari e non scaduti. L’esportazione crea un nuovo backup cifrato portabile, senza sincronizzazione automatica. Non conserva tutti i metadati del vault originale; conservarne una copia separata.

La voce inglese viene scelta fra quelle offline disponibili; il ripiego italiano dipende dal motore Android e può usare la rete. La località meteo può essere inserita manualmente o ottenuta dal GPS autorizzato. Le richieste meteo e radar contattano Open-Meteo e Radar-DPC; coordinate e tile richieste indicano ai servizi l’area consultata. Gli avvisi sono sperimentali e funzionano mentre l’overlay è attivo.

## Chiusura nella 1.1 beta 3

Il pulsante tondo ⏻ o Chiudi Solphivia dalla notifica dell’avatar ferma servizi, lettura e microfono e chiude le attività dell’app; conserva account e dati. Richiamo riattivato solo alla successiva apertura con l’opzione automatica attiva. L’icona normale può essere trascinata dall’elenco app alla Home; non vengono create automaticamente scorciatoie con badge.

## Arresto e ripresa della lettura

Durante il parlato, il rilevatore locale ascolta le frasi di arresto abilitate. Una lettura sospesa può essere ripresa con una frase dedicata. Nessuna registrazione audio è conservata da Solphivia. Il testo letto e la posizione di ripresa restano in memoria del processo; vengono sostituiti da una nuova richiesta o rimossi dalla chiusura. Le parole brevi sono verificate localmente contro la frase completa. Non è autenticazione biometrica. La trascrizione inviata resta nella barra durante la risposta e rispetta le bozze modificate a mano.
