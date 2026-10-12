# Solphivia Avatar AI

**Un avatar animato per Android che dà forma a Solphivia, assistente IA e collaboratrice creativa in sviluppo, con fumetto, comandi touch, voce e una roadmap per diversi fornitori di IA.**

> Alpha comunitaria. Il progetto è in evoluzione e non è pronto per una distribuzione stabile. La versione attuale non ha completato la revisione Google Play.

**Guide:** [primo avvio e collegamento a ChatGPT](AVVIO-RAPIDO-IT.md) · [privacy](PRIVACY-IT.md) · [compilazione e caricamento su GitHub](BUILD-E-GITHUB-IT.md) · [presentazione del progetto](PRESENTAZIONE-IT.md)

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

Il sorgente 0.8.9 include una correzione pensata per aggiornare l'icona di un collegamento Home già esistente e imposta il nome dell'app su **Solphivia Avatar AI**. Questa versione non è ancora stata ricompilata né provata su un dispositivo fisico: considerala sperimentale. Per lo stato e i prossimi passi vedi [STATO-IT.md](STATO-IT.md) e [ROADMAP-IT.md](ROADMAP-IT.md).
