<p align="center"><img src="docs/media/solphivia-cover.svg" alt="Solphivia: avatar IA per Android con chat, voce e memoria portabile" width="100%" /></p>

# Ti presento Solphivia

**Novità alpha5 — «Hola, Solphivia!»** Saluto a voce, apertura di WhatsApp e altri comandi con un solo avatar flottante. Riconoscimento offline, a schermo acceso e sbloccato. [Novità, installazione e limiti](docs/releases/v0.9.2-alpha5.md). Cerchiamo tester Android!

Una compagna animata sullo schermo Android. Puoi parlarle, scriverle, allegare un file e personalizzare la memoria dell'assistente mantenendone il controllo.

**[Prova l’alpha Android](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha5)** · **[Aiutaci a farla crescere](COLLABORATE.md)** · [Read in English](README.md)

> Alpha comunitaria · Android disponibile per le prove · Google Play e iOS sono obiettivi da realizzare.

## Un personaggio che accompagna la conversazione

<table><tr>
<td align="center"><img src="app/src/main/assets/solphivia/idle/frame_00.png" width="170" alt="Solphivia con il suo abito quotidiano" /><br/><b>Compagna quotidiana</b></td>
<td align="center"><img src="app/src/main/assets/solphivia/mode_vj/frame_00.png" width="170" alt="Solphivia nel costume cyborg ciano e viola" /><br/><b>Modalità cyborg</b></td>
<td align="center"><img src="app/src/main/assets/solphivia/listen/frame_00.png" width="170" alt="Solphivia nell’animazione di ascolto" /><br/><b>Pronta ad ascoltare</b></td>
</tr></table>

Fotogrammi reali del personaggio nell’app Android. Le illustrazioni restano riservate secondo la [licenza degli asset](LICENZA-ASSET-IT.md).

| Personalizzala | Mantieni il controllo | Costruiamola insieme |
| --- | --- | --- |
| Avatar trascinabile, animazioni, chat e voce Android. | Memoria locale cifrata, anteprima modificabile prima della condivisione ed export cifrato Memory Vault. | Miglioriamo Android, accessibilità e test; progettiamo i collegamenti ai modelli locali e la versione iOS. |

## Comincia in pochi minuti

1. Scarica l’APK dalla [release alpha](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha5). È una build debug di prova, non una versione degli store.
2. Segui la [guida al primo avvio](AVVIO-RAPIDO-IT.md), attiva l’avatar flottante e scegli un motore vocale Android installato.
3. Prova l’avatar e condividi un [test riproducibile](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/6) con esempi inventati.

**Vuoi partecipare?** Puoi iniziare dai [test](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/6), dalla [preparazione Android](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/3), dallo [studio iOS](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/4) o dai [fornitori di modelli](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/5). Anche un piccolo contributo concreto ci aiuta.

**Esplora:** [Collaborazione](COLLABORATE.md) · [Privacy](PRIVACY-IT.md) · [Come contribuire](CONTRIBUTI-IT.md) · [AI Memory Vault](https://github.com/Trikto7e8/AI-memory-vault)

## Funzioni attuali

- Mostra Solphivia come avatar Android flottante, spostabile, con fumetto e riga per scrivere.
- Include animazioni di riposo, corsa, salto, seduta, sonno e costume cyborg.
- Supporta la sintesi vocale, il riconoscimento vocale Android e gli allegati di documenti e immagini.
- Si collega a ChatGPT tramite Sign in with ChatGPT di OpenAI e, per gli account idonei, all'uso di Responses. Non legge le conversazioni né la memoria dell'utente su ChatGPT.
- Salva la cronologia locale sul dispositivo e cifra le credenziali OAuth con Android Keystore.

Il collegamento a ChatGPT è facoltativo e dipende dall'idoneità dell'account e dal consenso dell'utente. L'uso del piano ChatGPT è distinto dalla fatturazione delle API OpenAI. Consulta la [documentazione OpenAI](https://developers.openai.com/siwc/token-sharing-open-source) e le [informazioni sull'uso del piano ChatGPT in app e siti esterni](https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites).

## Direzione del progetto

L'obiettivo è mantenere l'avatar e il livello di interazione di Solphivia indipendenti da un singolo fornitore di modelli. I lavori pianificati includono:

1. Una piccola interfaccia comune per chat e capacità dei modelli.
2. Connessioni locali a server di modelli come LM Studio o Ollama.
3. Altri adattatori per servizi cloud, configurati con l'account o le credenziali dell'utente.
4. Una possibile opzione per modelli eseguiti sul dispositivo, subordinata a memoria, prestazioni e licenze dei modelli.
5. Una modalità vocale con il volto o l'avatar a figura intera di Solphivia mostrato nell'app stessa.

Questi sono obiettivi futuri, non funzioni già disponibili. Ogni fornitore richiederà un proprio adattatore; non si può garantire la compatibilità con qualsiasi LLM.

## Compilare il prototipo attuale

- Android Studio con Android SDK 35 e JDK 17
- Gradle 8.11.1 / Android Gradle Plugin 8.9.1

Apri questa cartella in Android Studio e compila `:app:assembleDebug`. L'APK debug viene creato in `app/build/outputs/apk/debug/app-debug.apk`.

Il progetto al momento usa API 35 come target. Prima di inviarlo a Google Play occorre aggiornarlo e provarlo secondo i requisiti API correnti dello store.

## Preparazione per Google Play

Prima di una pubblicazione su Play occorre almeno:

- Portare il target ad API 36 e creare un Android App Bundle firmato.
- Preparare una privacy policy e una dichiarazione Data safety accurata, relativa a dati dell'account, testo della chat, allegati e funzioni vocali.
- Aggiungere nell'app una funzione per segnalare o contrassegnare risposte dell'IA problematiche.
- Completare le dichiarazioni di Play Console e la revisione dei servizi foreground per overlay e `specialUse`.
- Eseguire test sui dispositivi, preparare gli asset dello store e la classificazione dei contenuti; svolgere un test chiuso se richiesto per l'account sviluppatore.

## Privacy e affiliazione

Quando è collegata a un fornitore di IA, l'app invia a quel fornitore i prompt e gli allegati selezionati per generare la risposta. Prima di configurare un collegamento, consulta i termini del servizio corrispondente. Solphivia è un progetto indipendente, non sponsorizzato né affiliato a OpenAI. OpenAI, ChatGPT e i relativi nomi sono marchi dei rispettivi titolari e sono citati solo per identificare un'integrazione facoltativa.

Per i dettagli su archiviazione locale, voce, allegati e overlay del prototipo attuale, consulta [PRIVACY-IT.md](PRIVACY-IT.md). È una bozza tecnica, non sostituisce l'informativa pubblica completa richiesta per la distribuzione sugli store.

## Licenze

- Codice sorgente: MIT, vedi `LICENSE` e la [traduzione italiana di cortesia](LICENZA-CODICE-IT.md).
- Personaggio Solphivia, volto, sprite delle animazioni, costumi, illustrazioni dell'icona e identità visiva: diritti riservati, vedi `ASSETS-LICENSE.md` e [la sua versione italiana](LICENZA-ASSET-IT.md).

## Contributi

I contributi al codice sono benvenuti. Leggi prima `CONTRIBUTING.md` o [la versione italiana](CONTRIBUTI-IT.md). Non inserire credenziali, token di accesso, contenuti personali o schermate private nelle segnalazioni o nelle pull request. Contribuire al codice non concede diritti di riutilizzo del personaggio o delle illustrazioni di Solphivia.

## Stato delle verifiche

La versione **0.9.2-alpha4** (codice 33) è stata compilata, verificata con Android Lint e installata sul Motorola. I test automatici nella copia isolata sono passati e l’aggiornamento alpha4 è stato verificato e installato il 14 ottobre 2026. Restano miglioramenti successivi e la valutazione della voce: il timbro dipende dal motore Android scelto e dalle voci installate. Vedere [rapporto di verifica](VERIFICA-0.9.2-alpha4.md).

## Novità nell’alpha4

- Una Solphivia alla volta: la finestra flottante si nasconde nella chat interna e torna quando esci.
- Rispetta la voce selezionata nel motore Android, senza sostituirla con un’altra variante equivalente.
- Abiti contestuali Prof, VJ, Codex e Meteorina, con priorità ai comandi manuali e almeno 20 secondi tra cambi automatici.
- Tre gesti brevi: cenno, inclinazione di spiegazione e piccolo rimbalzo di gioia, animando gli sprite esistenti. Opzione Movimenti ridotti nell’anteprima.
- Seduta e sonno conservati; riflessione attiva mentre si prepara la risposta.

Scrivi `scegli tu` o `modalità auto` per la scelta contestuale. I comandi espliciti `prof`, `vj`, `codex` o `meteorina` fissano l’abito. Il contesto viene dalla conversazione, non dall’osservazione delle altre app. Vedi [le note sul comportamento](docs/CONTEXTUAL-AVATAR-DECISIONS.md).

## 0.9.2-alpha2

- Memoria personale cifrata sul dispositivo, editor e comando `ricorda:`; import/export cifrato compatibile con AI Memory Vault v1. Anteprima modificabile per ogni invio remoto. Nessuna sincronizzazione automatica e nessun nuovo adattatore LLM.
- Pulsanti `+` e microfono visibili anche nella schermata interna; dettatura modificabile prima dell’invio e allegati mantenuti nei turni successivi.
- Scelta del motore TTS e accento inglese con voce offline, quando disponibile. eSpeak NG è separato e non incluso nell’APK.
- Radar DPC aggiornato: dati, timestamp ed errori visibili. Segnala echi vicini, senza stimare direzione o tempo di arrivo.
- Sonno animato, proporzioni corrette e schermata `☰ → Prova animazioni`; 108 sprite e 1248 combinazioni verificate.

Scarica l’APK dalla [pre-release v0.9.2-alpha5](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha5). È una build debug per test, non una distribuzione Google Play. Il profilo iniziale è generico e può essere personalizzato localmente nell’editor Memoria.

## Voice Access e ShareBridge (MVP del modulo v0.1)

Saluto “Hola, Solphivia!”, risposta personalizzata, apertura di WhatsApp e condivisione con anteprima e conferma. Vedi [funzioni, privacy, limiti e test Android](docs/VOICE-ACCESS-IT.md). La numerazione 0.1 riguarda il modulo, non sostituisce la versione dell’app.
