# Avanzamenti di Solphivia IA

Ogni aggiornamento distingue codice preparato, prove automatiche, collaudo reale e distribuzione.

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

**Distribuzione:** candidata, non ancora approvazione Play né aggiornamento installato sul dispositivo personale. Server HTTPS e invito configurati: una risposta OpenAI reale verificata il 16 novembre 2026. Corretta incompatibilita Cloudflare nel rifiuto dei redirect, senza seguire destinazioni alternative. Richiede firma con la chiave di caricamento esistente e pubblicazione sul canale interno; seguono prova reale sul Motorola e riscontro utilizzo API. Nessuna installazione debug sopra la copia Play.

**Limiti conservati/espliciti:** Stop resta da risolvere come documentato in beta 5; Fermati/Riprendi erano stati confermati dall'utente. TalkBack e prove prolungate restano da verificare. I nuovi adattatori supportano testo e voce; gli allegati rimangono sul percorso ChatGPT preesistente. HTTP locale è facoltativo e non cifrato: ammessi soltanto IP IPv4 privati esplicitamente scelti, senza chiavi; nessuna abilitazione globale del cleartext nell'app.

[Dettagli e collaudo](docs/CONNESSIONI-IA-2.0-IT.md) · [Preparazione release](docs/releases/v2.0.0-beta.1.md)

## 1.1.0-beta.5 — base precedente

Continuità del dialogo e trascrizioni, recupero del riconoscimento locale, gestione dell'eco durante l'interruzione. Confermato sul Motorola Fermati → Riprendi → Fermati; Stop non confermato e registrato come problema aperto. [Note della release](docs/releases/v1.1.0-beta.5.md).
