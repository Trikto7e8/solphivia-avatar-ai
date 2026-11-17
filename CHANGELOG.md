# Avanzamenti di Solphivia IA

Ogni aggiornamento distingue codice preparato, prove automatiche, collaudo reale e distribuzione.

## 2.0.0-beta.2 — codice 57

- Correzione del campo invito: il pannello flottante lascia temporaneamente il posto al menu e alle schermate dei collegamenti, che possono ricevere tocchi e tastiera. Alla chiusura riappare lo stesso avatar, senza ricreare il servizio, cambiare sprite o riavviare audio e richiamo.
- Verifica aggiunta al collaudo HTTPS isolato: finestra demo in primo piano, campo invito con fuoco, tastiera effettivamente visibile e ritorno dello stesso servizio flottante. Prova superata in emulatore. Il 17 novembre il Motorola e stato aggiornato da Play al codice 57: tastiera reale visibile, attivazione invito, ritorno dell’avatar e risposta OpenAI dal fumetto verificati senza browser. Il collaudo vocale dell’utente prosegue.
- Bundle 57 firmato con la chiave di caricamento esistente e pubblicato nel canale interno Google Play il 17 novembre 2026. API 36 e dispositivi supportati conservati; controlli GitHub superati.
- Icona ufficiale singola 512x512 caricata e salvata nella bozza della scheda Google Play. La scheda pubblica richiede ancora completamento e revisione; il nome temporaneo del test interno non viene dichiarato risolto.

## 2.0.0-beta.1 — candidata interna, 16 novembre 2026

Rispetto alla beta 5 pubblica e alla preparazione locale della demo:

- Versione **2.0 beta**, codice Android **56**, compile/target **API 36**. Il codice va ricontrollato contro Play Console prima di ogni caricamento.
- Servizio Solphivia con invito: accetta anche richieste vocali già trascritte, usando riconoscimento e voce Android esistenti. Stessa quota per tastiera e voce, senza un limite artificiale alla sola modalità scritta.
- Quota persistente per tester, tetti globali di richieste/token, inviti revocabili/scadenti e deduplicazione. Il consumo del credito di progetto riguarda solo questa modalità.
- Collegamenti opzionali con chiavi API personali: OpenAI, Gemini, Claude, endpoint compatibili; connessioni a Ollama/LM Studio sul PC, inclusi modelli come Qwen. Il login consumer non è automaticamente un'autorizzazione API.
- Credenziali personali cifrate in Android Keystore, conferma della destinazione, nessun invio automatico durante la configurazione, nessun fallback a un altro fornitore.
- Cronologia separata per collegamento; contesto recente e limitato per il singolo invito. Nessuna migrazione automatica della chat precedente verso un'altra IA.
- Identità/prompt e parser dei tag del personaggio condivisi; voce Android indipendente dal modello. Nessuna modifica ai file di sprite, icone, animazioni, immagini o video promozionali.
- Percorso di consenso monouso riutilizzato per i provider personali. Nella voce il vault e gli allegati restano esclusi; il servizio a invito non riceve il vault.

**Prove:** 85 test JVM debug e 85 release superati; server Python 14 test e Worker Cloudflare 15 test superati. In emulatore, protocolli sintetici OpenAI/Gemini/Claude, modello locale, cifratura, isolamento, consenso, cambio destinazione e annullamento verificati. Un secondo collaudo HTTPS sintetico conferma invito errato rifiutato, chat scritta e richiesta vocale trascritta dal fumetto nella stessa quota e terza richiesta rifiutata, oltre al passaggio finestra/overlay. Non sono prove della qualità audio o della voce umana.

**Distribuzione:** bundle 56 firmato con la chiave di caricamento esistente e pubblicato nel canale interno Google Play il 16 novembre 2026. API 36 conservata. Non e una pubblicazione in produzione o un'approvazione dello Store. Server HTTPS e invito configurati: risposta OpenAI reale e utilizzo API verificati. Corretta incompatibilita Cloudflare nel rifiuto dei redirect, senza seguire destinazioni alternative. Aggiornamento sul Motorola e collaudo nell'app ancora da verificare. Nessuna installazione debug sopra la copia Play.

**Limiti conservati/espliciti:** Stop resta da risolvere come documentato in beta 5; Fermati/Riprendi erano stati confermati dall'utente. TalkBack e prove prolungate restano da verificare. I nuovi adattatori supportano testo e voce; gli allegati rimangono sul percorso ChatGPT preesistente. HTTP locale è facoltativo e non cifrato: ammessi soltanto IP IPv4 privati esplicitamente scelti, senza chiavi; nessuna abilitazione globale del cleartext nell'app.

[Dettagli e collaudo](docs/CONNESSIONI-IA-2.0-IT.md) · [Preparazione release](docs/releases/v2.0.0-beta.1.md)

## 1.1.0-beta.5 — base precedente

Continuità del dialogo e trascrizioni, recupero del riconoscimento locale, gestione dell'eco durante l'interruzione. Confermato sul Motorola Fermati → Riprendi → Fermati; Stop non confermato e registrato come problema aperto. [Note della release](docs/releases/v1.1.0-beta.5.md).
