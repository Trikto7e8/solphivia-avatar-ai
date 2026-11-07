# Prova senza account — candidata interna API 36

## Base e versione

Sul Motorola è stata rilevata l’app `it.solphivia.mobile`, codice **54**, target API **36**, installata da `com.android.vending`. La copia del progetto usata per quel bundle contiene compileSdk 36 e targetSdk 36. La candidata usa **1.1.0-beta.6**, codice **55**, mantenendo API 36 e minSdk 26.

Il bundle 54 ha questa impronta SHA-256 del certificato di caricamento pubblico.

```text
76110773e7fa518f553f79a3b9a48aac1d5c5b45f3913d58467b7de01fc20c6a
```

La chiave di firma con cui Google distribuisce l’app può essere diversa dalla chiave di caricamento. Per verificare la chiave di caricamento si usa il bundle originale, conservato sul computer.

## Servizio HTTPS

La cartella `demo-server` contiene il servizio ASGI e la configurazione Docker Compose con HTTPS Caddy. Occorrono un server con Docker Compose, un dominio che punti al server e le porte 80 e 443 raggiungibili. Il database usa un volume persistente sullo stesso server. I due processi del servizio condividono quel database.

1. Nella piattaforma OpenAI seleziona o crea il progetto API dedicato alla demo e crea lì la chiave. Il progetto scelto determina dove comparirà l’uso. Conserva la chiave sul server.
2. Copia `demo-server` sul server. Nella cartella crea un file `.env` con permessi riservati al proprietario. Inserisci il segreto direttamente in quel file sul server.

```dotenv
DEMO_HOST=demo.example.org
OPENAI_API_KEY=INSERISCI_LA_CHIAVE_DEL_PROGETTO_SOLO_SUL_SERVER
OPENAI_MODEL=gpt-5-mini
DEMO_GLOBAL_REQUESTS=100
DEMO_GLOBAL_TOKENS=200000
```

3. Sostituisci il dominio di esempio con quello reale. Avvia il servizio.

```sh
chmod 600 .env
docker compose up -d --build
```

4. Apri `https://TUO_DOMINIO/health` e verifica `{"status":"ok"}`. Questo controlla il servizio HTTPS. La prima richiesta demo verifica il collegamento effettivo a OpenAI.
5. Crea il codice per il tuo primo collaudo. L’identificativo del tester è uno pseudonimo.

```sh
docker compose exec demo python server.py issue --tester collaudo-01 --limit 2 --days 7
```

Il comando mostra un invito casuale. Inseriscilo nel telefono in **Prova senza account**. La chiave OpenAI resta nella configurazione del server. Ogni tester deve ricevere un proprio invito. Condividere lo stesso invito significa condividere il limite.

### Revocare, ruotare e controllare

```sh
docker compose exec demo python server.py revoke --tester collaudo-01
docker compose exec demo python server.py usage
docker compose exec demo python server.py issue --tester collaudo-01 --limit 20 --days 7
```

La revoca invalida tutti i codici di quel tester. Il comando issue successivo crea un nuovo codice. Usare lo stesso identificativo conserva i consumi precedenti e aggiorna il limite del tester.

### Limiti applicati dal server

- Un messaggio di massimo 4096 byte UTF-8 e una richiesta JSON di massimo 8192 byte.
- Solo testo del messaggio corrente. Il client non può impostare modello, strumenti, memoria o cronologia da inviare.
- Massimo 512 token di output, compresi quelli di ragionamento, e 4000 caratteri di risposta visibile.
- Limite di richieste per tester e limite complessivo di richieste e token prenotati.
- Prenotazione dei consumi prima della chiamata OpenAI con transazione SQLite. I processi concorrenti condividono i contatori.
- Tentativi falliti e risposte perse restano conteggiati. Lo stesso identificativo di richiesta viene rifiutato prima di una seconda chiamata.
- Scadenza e revoca controllate a ogni invio. Un riavvio del servizio o la reinstallazione dell’app conservano i consumi registrati sul server.

Il budget token è conservativo e comprende un margine per le istruzioni. I limiti sono cumulativi per la campagna, senza ripristino automatico giornaliero. Conservare il volume `demo_data` durante aggiornamenti e riavvii. Usare più server con database separati creerebbe budget separati. Il modello e i suoi prezzi sono impostazioni del gestore da controllare prima della campagna.

Il database conserva hash degli inviti, pseudonimi, scadenze, revoche, identificativi delle richieste, prenotazioni e conteggi token. Testi inviati e risposte non sono salvati dal servizio. Gli access log HTTP sono disattivati nella configurazione fornita. OpenAI tratta il testo secondo le proprie condizioni. Il payload usa `store: false`.

## Generare l’AAB successivo

Nel progetto aggiornato, imposta l’origine HTTPS reale tramite proprietà Gradle o variabile d’ambiente. Si usa solo l’origine, senza barra finale o percorso.

```powershell
$env:SOLPHIVIA_DEMO_URL = 'https://demo.example.org'
.\gradlew.bat testReleaseUnitTest lintRelease bundleRelease
```

Per firmare, apri questo progetto aggiornato in Android Studio. Scegli **Build → Generate Signed App Bundle / APK → Android App Bundle**, seleziona la **stessa chiave e lo stesso alias usati per il bundle 54** e inserisci le password nei campi locali di Android Studio. Usa la variante release. Non creare una nuova chiave per questo aggiornamento.

In alternativa la firma Gradle legge queste variabili dal processo locale.

```text
SOLPHIVIA_UPLOAD_STORE_FILE
SOLPHIVIA_UPLOAD_STORE_PASSWORD
SOLPHIVIA_UPLOAD_KEY_ALIAS
SOLPHIVIA_UPLOAD_KEY_PASSWORD
```

Le password vanno inserite localmente tramite un gestore di segreti o il terminale protetto. Una configurazione parziale fallisce. Con queste quattro variabili il progetto verifica il certificato rispetto all’impronta del bundle 54. Senza configurazione di firma, Gradle produce un bundle unsigned di sola compilazione. La procedura guidata Android Studio può applicare direttamente la firma selezionata.

Il bundle generato da Gradle si trova in `app/build/outputs/bundle/release/app-release.aab`. Verificare firma e configurazione prima del caricamento.

```sh
python tools/verify_play_bundle.py app/build/outputs/bundle/release/app-release.aab --jdk-bin "PERCORSO_JDK/bin"
```

Caricare la candidata **55** nel canale interno. Qualora Play Console contenga nel frattempo un codice superiore, incrementare ulteriormente versionCode prima di compilare. L’app installata da Play va aggiornata tramite il canale interno, perché la sua firma di distribuzione può differire dalla firma locale di caricamento.

## Collaudo sullo stesso Motorola

1. Aggiorna dal canale interno e verifica codice 55 e target 36.
2. Con il collegamento ChatGPT assente, scegli **Prova senza account**, inserisci l’invito e attiva la demo. Il messaggio viene verificato dal server al primo invio.
3. Invia un testo sintetico, ad esempio «Scrivi una frase di benvenuto». Deve arrivare una risposta senza browser. Nella piattaforma OpenAI controlla l’uso selezionando il progetto della chiave dedicata e l’intervallo temporale corretto.
4. Prova un invito casuale errato, poi quello revocato. Il server deve rifiutarli senza chiamare OpenAI.
5. Con l’invito limitato a due richieste, la terza deve mostrare il limite del tester. Le prove automatiche controllano anche il limite complessivo e la concorrenza.
6. Con permesso overlay negato, la finestra deve restare utilizzabile e mostrare **Attiva avatar flottante · Mostra sopra altre app**. Concedi il permesso e torna all’app. Dopo le eventuali richieste Android, deve comparire il fumetto.
7. Invia un testo dal fumetto e verifica che usi la demo. Prova separatamente **Collega ChatGPT** per il percorso preesistente.
8. Ripeti i comandi vocali abituali nella modalità account, compresi Fermati e Riprendi. La demo iniziale accetta testo e segnala esplicitamente voce e allegati non supportati.

Prima del canale interno, pubblicare l’informativa aggiornata relativa al server demo all’URL privacy dichiarato nella Console e verificare le dichiarazioni dei dati applicabili alla nuova modalità.

## Prove locali

Il 7 novembre 2026 sono passate le prove server su inviti, revoca, scadenza, limiti persistenti e richieste concorrenti. Sul Motorola, nella copia isolata, è passata una prova HTTPS con provider sintetico che comprende rifiuto dell’invito errato, risposta dalla chat principale, ritorno all’avatar dopo concessione del permesso, risposta dal fumetto e rifiuto al limite del tester. Il test controlla anche l’assenza di una sessione ChatGPT e di un’autorizzazione browser avviata dalla demo.

La prova ha evidenziato una dipendenza dell’avvio dall’evento iniziale di focus della finestra. La candidata tenta ora il passaggio all’overlay anche quando l’attività riprende, dopo i controlli dei permessi e degli stati in corso. La schermata normale resta disponibile con overlay negato.

Le prove Android usano una fixture HTTPS locale, senza richieste API a pagamento. Hosting pubblico, inserimento del segreto del progetto e primo riscontro nel pannello di utilizzo OpenAI restano attività di configurazione e verifica reale. Il bundle di sola compilazione privo di firma e indirizzo demo richiede queste configurazioni prima del caricamento interno.

```sh
python -m unittest discover -s demo-server -v
```

`DemoInstrumentation` è riservato a `it.solphivia.mobile.v11.verification` e all’origine `https://localhost:18443`. Usa il server sintetico `device_fixture.py`, un certificato di prova esplicitamente fidato nel processo isolato e parametri di collaudo. Il controllo del nome host resta attivo. Il file della fixture è escluso dall’immagine Docker di distribuzione. Queste prove non attestano una richiesta reale al progetto OpenAI.

Fonti tecniche consultate il 7 novembre 2026. [Responses e generazione del testo](https://developers.openai.com/api/docs/guides/text), [modello GPT-5 mini](https://developers.openai.com/api/docs/models/gpt-5-mini).
