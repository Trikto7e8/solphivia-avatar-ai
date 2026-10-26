# Italiano naturale e confronto della pronuncia — alpha12 di prova

## Base e richiesta attuale

Base esatta: release GitHub **v0.9.2-alpha11**, commit `f9f65419a9c08459ac7bad95ef8d6bf8f6e03723`, pubblicata il 24 ottobre 2026. L’utente ha poi scelto una **voce italiana** perché quella britannica ha un accento troppo forte. La prova alpha12 (45) è locale e non sostituisce la release pubblicata.

## Comportamento

Al primo avvio dell’aggiornamento viene scelta la modalità italiana naturale una sola volta. Le successive scelte esplicite restano memorizzate. Il motore TTS selezionato resta quello già configurato; non vengono installati o preferiti automaticamente motori come eSpeak. La selezione conserva una voce della lingua richiesta prima di scegliere un ripiego; la lingua italiana non viene trasformata foneticamente.

Voce e timbro effettivi dipendono dalle voci italiane installate nel motore Android. Non viene promesso un genere, una qualità HD o un timbro particolare senza ascolto. La disponibilità offline va verificata sul telefono; il TTS italiano può usare Internet secondo la voce e il motore scelti.

La schermata **Voce** ha tre prove con la stessa frase e lo stesso motore:

1. Italiano naturale (consigliato).
2. Inglese classico: stessa trasformazione testuale della alpha11.
3. Inglese con correzione Chiara (sperimentale).

Le prove non salvano il profilo: occorre premere **Salva e prova la scelta**. Lasciare disattivato **Usa una voce inglese** conserva l’italiano. Il ritmo parte dal 100% della velocità Android, può essere variato dal 75% al 115%, e non modifica l’altezza/timbro. Si può fermare la prova o uscire dalla schermata. Nessun nuovo avatar o registratore viene avviato per ascoltare un campione.

La correzione inglese non è un regolatore dell’intensità dell’accento. È una preparazione locale del testo con sostituzioni non concatenate, indicazioni per parole frequenti e suoni italiani, lettura italiana di interi/orari/percentuali, e conservazione di marchi, sigle, indirizzi e file. È incompleta: accenti tonici, parole straniere, nomi propri, formati numerici ambigui e regole fonetiche hanno eccezioni. Nessuna equivalenza IPA/SSML o comprensibilità acustica è certificata.

La preparazione è condivisa fra chat, avatar e anteprime. Il testo visibile e la cronologia non vengono alterati dalla pronuncia. Le frasi lunghe vengono spezzate rispettando spazi e coppie Unicode, nei limiti del TTS Android. Nessuna nuova API a pagamento, modello ONNX, server, campione vocale personale, registrazione o dipendenza di sintesi è aggiunta. Un motore disponibile gratuitamente non equivale necessariamente a software libero.

## Verifiche e limiti

Test JVM controllano preservazione del profilo classico, testo italiano invariato, URL/marchi/sigle, numeri italiani, distinzione di suoni, limiti del ritmo, token lunghi e partizionamento Unicode. Sono verifiche di testo/policy, **non un ascolto o una misura della comprensibilità**.

Il Motorola non è collegato e l’utente ha chiesto di preparare la versione per provarla dopo. Nessun test Android o sintesi audio sul dispositivo è dichiarato riuscito. È presente una verifica strumentale isolata per controllare la migrazione delle preferenze quando il telefono sarà disponibile. Nessuna installazione, pubblicazione su GitHub o modifica della voce sul telefono è stata effettuata durante questa preparazione.

## Prova sul telefono, dopo

1. Installare l’APK di aggiornamento mantenendo i dati; aprire **Voce**. Se la firma non corrisponde, fermarsi: non disinstallare per aggirare il problema.
2. Lasciare lo stesso motore e il ritmo al 100%. Ascoltare **Italiano naturale** usando la frase sintetica proposta.
3. Se interessa ancora l’accento, ascoltare separatamente i due profili inglesi con lo stesso testo e ritmo. La voce inglese deve essere installata; il messaggio di stato segnala il ripiego italiano.
4. Valutare se si capiscono il saluto, «Cosa posso fare per te?», numeri e orari al primo ascolto. Annotare solo il profilo preferito e le parole poco chiare, senza registrazioni o dati privati.
5. Salvare la scelta. Provare una risposta nella chat e una dall’avatar; verificare Ferma, uscita dalla schermata e ripresa del richiamo. Facoltativamente confrontare 95% con 100% in una prova separata.

## Fonti ufficiali consultate

- Base pubblicata: https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha11
- Android TTS (lingua, voce, ritmo e limiti input): https://developer.android.com/reference/android/speech/tts/TextToSpeech
- Modelli Kokoro valutati, non integrati: https://github.com/thewh1teagle/kokoro-onnx/releases . Richiederebbero un motore/modello aggiuntivo e un confronto reale su Android; questa prova usa invece il motore già installato.

## Esito della preparazione

48 test JVM superati, zero errori/fallimenti. Build APK, compilazione dei test Android isolati e Android Lint riusciti. Nessun test strumentale eseguito sul telefono. Firma alpha12 uguale all’impronta pubblica alpha11: `7458c577288b1a5adfb9ee7251e00eab8d2d7518fb26ce1a7b0dd6cf49d323a5`. APK `SolphiviaMobile-0.9.2-alpha12-italiano.apk`, SHA256 `93cf986d5f191b573fe1e48d81d3b454f83a1504a1340ddf759eaf8ec15ab0f3`. Nessuna pubblicazione o installazione effettuata.
