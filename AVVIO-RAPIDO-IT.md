# Solphivia Avatar AI: guida rapida per Android

Questa guida descrive la versione di prova attuale. Le schermate e i nomi delle voci Android possono cambiare leggermente in base al modello del telefono.

## Prima di iniziare

- Installa Solphivia Avatar AI da un APK che hai compilato o ricevuto da una fonte fidata.
- Installa o aggiorna un browser, per esempio Chrome, e verifica di avere una connessione Internet.
- Accedi prima a [chatgpt.com](https://chatgpt.com) nel browser del telefono con l'account ChatGPT che vuoi collegare. Se il browser te lo chiede, completa lì l'accesso. Solphivia non chiede né riceve la password.
- Per il collegamento diretto occorrono un account e le autorizzazioni ChatGPT compatibili con Sign in with ChatGPT. Il servizio può cambiare disponibilità e requisiti.

## Primo collegamento

1. Apri l'app **Solphivia Avatar AI** dall'icona con il viso di Solphivia.
2. Tocca **Collega ChatGPT**. Android apre il browser; usa lo stesso browser in cui hai già effettuato l'accesso.
3. Controlla che sia selezionato l'account giusto. Leggi la schermata di consenso e approva solo se vuoi autorizzare questa app. Se compare l'opzione per usare il piano ChatGPT, è facoltativa e soggetta a disponibilità, limiti e condizioni dell'account.
4. Attendi il ritorno automatico all'app. Se la pagina mostra **Torna a Solphivia Avatar AI**, toccalo. Non scrivere manualmente l'indirizzo `127.0.0.1` nel browser: è un passaggio locale temporaneo gestito dall'app.
5. Se l'avatar non compare perché manca il permesso, torna nella schermata dell'app e tocca la riga di stato **Chat pronta · avatar flottante disattivato**. Android apre **Mostra sopra altre app**: abilita Solphivia Avatar AI e torna indietro. Questo permesso serve a disegnare il personaggio flottante sulla schermata Home e sopra le altre app. Non consente a Solphivia di leggere ciò che appare nelle altre app.
6. Conferma l'aggiunta dell'icona alla schermata Home, se il launcher Motorola lo chiede. L'icona avvia l'app; non è l'avatar flottante.
7. Quando il collegamento è completato, Solphivia compare sulla schermata Home con fumetto e riga per scrivere. Se non la vedi, torna nell'app e tocca **Collega ChatGPT** solo se risulta disconnessa; controlla anche il permesso di overlay.

## Usare l'avatar

### Voice Access nell’alpha5

Aprire l’app dall’icona avvia volontariamente l’ascolto se overlay e microfono sono autorizzati. Al primo avvio serve il Wi-Fi per scaricare il modello italiano offline (circa 48 MB). A schermo acceso e sbloccato, di’ **«Hola, Solphivia!»**, attendi il saluto e poi **«Apri WhatsApp»**. L’ascolto continua sopra le altre app. **«Fermati»** spegne il microfono; **Ferma** nella notifica chiude anche l’avatar. Il blocco schermo sospende la registrazione.

Le richieste libere dopo il saluto vengono inoltrate alla connessione ChatGPT già autorizzata. Gli invii WhatsApp richiedono anteprima e conferma. Sul Moto g35, se la vignetta non cambia mentre parli, disattiva e riattiva **Consenti utilizzo in background** nelle impostazioni batteria della sola app e riaprila: Android può ripristinare la restrizione dopo aggiornamenti debug. [Dettagli e limiti della release](docs/releases/v0.9.2-alpha5.md).

### Testo e dettatura dal pulsante

- Tocca la riga di testo sotto l'avatar, scrivi e premi la freccia di invio.
- Tocca il microfono per dettare in italiano. Il riconoscimento usa il servizio vocale configurato su Android; il trattamento audio dipende da quel servizio e dalle impostazioni del telefono.
- Usa **+** per selezionare fino a 5 file PDF, DOC/DOCX o immagini. Limite: 8 MB per file e 15 MB complessivi per invio. Il contenuto selezionato viene inviato al servizio AI per rispondere.
- Trascina Solphivia sullo schermo o chiedile un movimento supportato, per esempio «corri a destra».
- La notifica persistente di Android permette di controllare o fermare il servizio flottante.
- Per chiudere il personaggio, scrivi o detta **«spegni Solphivia»**, **«spegni l'app»** o **«chiudi Solphivia»**. Il servizio flottante si ferma.

## Icona e avatar flottante: due cose diverse

L'icona con il viso è una normale icona Android: si tocca per aprire la schermata dell'app. L'avatar flottante è un servizio separato, avviato dopo l'autorizzazione, che mostra Solphivia sopra la Home e le altre app. Puoi quindi vedere l'icona nel launcher e, dopo il primo collegamento, anche il personaggio sulla Home. Rimuovere l'icona non disinstalla l'app; fermare il servizio dalla notifica nasconde l'avatar senza rimuovere l'icona.

## Cosa aspettarsi dal collegamento ChatGPT

- La chat di Solphivia è separata dalle chat dell'app/sito ChatGPT. Solphivia non legge la memoria, le chat passate o la schermata di ChatGPT.
- La connessione invia il testo della conversazione e gli allegati selezionati a OpenAI per generare la risposta. La richiesta usa l'opzione `store: false`; consulta comunque i termini e l'informativa del fornitore per le regole di trattamento e conservazione applicabili.
- L'uso del piano ChatGPT, quando disponibile, è distinto dalla fatturazione API di OpenAI. Questa versione non richiede di incollare una chiave API.
- Se il browser è entrato nell'account sbagliato, scollega l'app dal browser/account e rifai l'autorizzazione con l'account desiderato. Non condividere codici temporanei, token o schermate di consenso pubblicamente.

## Problemi frequenti

**Il browser non torna all'app.** Lascia Solphivia aperta in background, completa l'autorizzazione e tocca il collegamento di ritorno nella pagina. Se la callback scade, riapri l'app e riprova il collegamento.

**Errore `127.0.0.1` o connessione rifiutata.** Non aprire quel link manualmente e non usare una scheda in incognito. Riapri Solphivia, avvia di nuovo **Collega ChatGPT** e completa il flusso nella stessa sessione del browser.

**Il personaggio non appare sulla Home.** In Android vai in **Impostazioni → App → Accesso speciale app → Mostra sopra altre app → Solphivia Avatar AI** e abilita il permesso. Poi riapri l'app. Le etichette possono variare sul Motorola.

**La voce o il microfono non funzionano.** Controlla il motore di sintesi vocale e il servizio di riconoscimento vocale scelti nelle impostazioni Android; la qualità e le lingue disponibili dipendono dal dispositivo e dai servizi installati.

**Non si collega o compare un limite.** Controlla Internet, account autorizzato e limiti dell'account. Le funzionalità SIWC possono cambiare e non sono garantite per tutti gli account o in tutte le aree.

## Percorso corrente alpha9

Per la voce segui [questa guida](docs/VOICE-SESSION.md): «Hola Solphivia» → attendi la risposta → richiesta. Il modello del richiamo è incluso, senza download separato. I passaggi Voice Access alpha5 eventualmente descritti sopra sono storici. ShareBridge non è attivo nella alpha9.
