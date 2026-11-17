# Collegamenti IA nella 2.0 beta

## Cosa cambia per l'utente

L'app resta **Solphivia IA**, pacchetto `it.solphivia.mobile`. L'utente sceglie chi produce le risposte dal menu **Collegamenti IA · personali o locali**, disponibile anche nella finestra normale. La configurazione è facoltativa e non sostituisce il launcher, non cambia il motore vocale e non richiede nuovi permessi microfono.

| Scelta | Credenziali | Utilizzo |
| --- | --- | --- |
| Servizio Solphivia | Codice d'invito revocabile | Credito del progetto, quota personale per tester e tetto complessivo |
| ChatGPT | Collegamento preesistente | Modalità account già presente, conservata |
| OpenAI / Gemini / Claude API | Chiave API personale inserita sul telefono | Quote/costi del proprio fornitore; nessun consumo del credito Solphivia |
| API compatibile HTTPS | Indirizzo, modello e chiave personale | Richieste direttamente alla destinazione confermata |
| Ollama / LM Studio | Indirizzo del PC e nome modello | Modello eseguito dal server sul PC; il telefono non scarica automaticamente Qwen o altri pesi |

Le chiavi non sono password della chat. Un abbonamento consumer non implica accesso alle API di un'altra app. La voce dell'avatar rimane quella Android scelta dall'utente, indipendentemente dal modello che scrive la risposta.

## Quote della demo, anche a voce

Una richiesta pronunciata viene trascritta dal riconoscimento esistente e passa allo stesso server della richiesta scritta. Consuma una richiesta e la relativa prenotazione di budget. Stop/Riprendi della lettura e comandi locali non diventano chiamate al modello per il solo fatto che la demo è attiva.

Il server conserva solo inviti in forma di hash, pseudonimo tester, conteggi/prenotazioni, esito e dati di utilizzo. Richieste fallite con risultato incerto restano conteggiate, evitando spese non contabilizzate. Ruotare un invito dello stesso tester non azzera la quota. Ogni nuovo tester deve avere un identificativo/invito distinto; condividere il codice significa condividere il limite.

Il servizio ammette fino a 4096 byte UTF-8 per il messaggio, fino a otto messaggi recenti entro 6144 byte di contesto e 8192 byte totali di richiesta. L'output è limitato a 512 token. Il server applica anche i tetti globali di richieste e token prenotati. Non sono una promessa di un importo esatto in dollari: controllare anche credito e condizioni del fornitore. Nessuna ricarica o acquisto viene attivato dal codice.

La conversazione di ciascuna attivazione dell'invito resta separata dalla cronologia ChatGPT e dai collegamenti personali. Il contesto inviato non contiene vault, URI o allegati. L'autorizzazione Android e il codice d'invito non sono autenticazione biometrica di chi parla.

## Chiavi personali e modelli locali

Ogni nuova configurazione inizia con chiave vuota. Non vengono scaricati cataloghi o eseguiti test autenticati mentre si digita. L'utente conferma il servizio e il modello prima del salvataggio; il profilo completo viene cifrato AES-GCM con chiave in Android Keystore. Cambiando profilo nasce una cronologia separata. Un profilo guasto non provoca fallback a ChatGPT o al credito del progetto.

HTTPS usa la verifica TLS standard e rifiuta redirect. Per le connessioni HTTP a Ollama/LM Studio occorre un consenso distinto: si accettano solo indirizzi IPv4 privati/loopback in forma numerica canonica e nessuna chiave. Il trasporto LAN dedicato connette direttamente l'indirizzo validato, senza DNS, proxy o redirect; non abilita il cleartext globalmente nell'app. Le risposte HTTP sono limitate e la connessione è chiusa su annullamento/scadenza. HTTP in LAN rimane non cifrato: per chiavi o reti non fidate usare HTTPS.

Un indirizzo come `127.0.0.1` sul telefono indica il telefono stesso. Per il PC nella stessa rete usare il suo IP privato, per esempio `http://192.168.1.50:1234` per LM Studio o porta `11434` per Ollama, con server avviato e modello caricato. La configurazione di rete/firewall del PC va effettuata consapevolmente, senza esporre il server a Internet.

Gli adattatori nuovi gestiscono testo e voce trascritta. Gli allegati vengono rifiutati prima di leggerne il contenuto e rimangono disponibili nel percorso ChatGPT. Il consenso memoria esistente resta monouso e vincolato al provider: se la destinazione cambia dopo l'approvazione, nessuno dei due endpoint riceve la richiesta.

## Personalità e Memory Vault

Gli adattatori riutilizzano lo stesso prompt attuale e lo stesso parser di umore/movimento/modalità. Non sono state cambiate le istruzioni della personalità né la voce. Modelli diversi possono comunque rispondere con stile o capacità differenti: un prompt non garantisce comportamento identico.

Il futuro profilo portatile potrà contenere preferenze di tono, lingua, modalità e riferimento alla voce disponibile. Non dovrà contenere chiavi API né un consenso permanente all'invio del vault. Le regole di sicurezza, di quota e di azione sul dispositivo vanno applicate dal codice, non affidate alla personalità del modello. Questa estensione del formato Memory Vault non è implementata in questa candidata.

## Stato e prove

I test automatici del server verificano inviti, scadenza/revoca, deduplicazione, quote persistenti e concorrenti, contesto limitato e rifiuto di ruoli system o allegati nelle richieste demo. Le nuove prove Android verificano cifratura del profilo, nessuna eredità di cronologia, formati cloud sintetici, modello locale, memoria vocale esclusa, consenso scritto, destinazione cambiata e annullamento.

La fixture HTTPS locale verifica una risposta dalla finestra e una dal percorso vocale del fumetto, con rifiuto alla terza richiesta di un tester limitato a due. Questi sono dati sintetici, senza chiavi cloud personali o consumo API: non attestano qualità audio, TalkBack o riconoscimento della voce umana.

Prima della distribuzione: configurare il segreto sul server, creare un invito fresco, generare il bundle con la stessa chiave di caricamento, controllare il codice massimo in Console, caricare nel canale interno. Sul Motorola usare quella distribuzione per conservare firma/dati della copia Play. La prima risposta reale deve essere riscontrata nel progetto API. Non ripetere da zero le prove di microfono/voce già acquisite; verificare sul telefono le integrazioni nuove e gli eventuali problemi ancora aperti.

## Fonti dei protocolli

[Gemini compatibile](https://ai.google.dev/gemini-api/docs/openai) · [Claude Messages](https://platform.claude.com/docs/en/api/messages) · [LM Studio](https://lmstudio.ai/docs/developer/openai-compat) · [Ollama](https://docs.ollama.com/api/openai-compatibility).
