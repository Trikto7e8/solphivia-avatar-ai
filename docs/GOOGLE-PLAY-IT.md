# Preparazione Google Play — 30 ottobre 2026

Base esaminata: 1.1.0-beta.3, versionCode 49, applicationId `it.solphivia.mobile`.
Questa preparazione non equivale ad approvazione Google né a pubblicazione.

## Compilazione

Il workflow `Google Play release readiness` esegue test JVM release, Lint release,
build AAB e controllo ELF delle librerie native a 64 bit. L'artefatto CI è
**unsigned e non va caricato nella Console**. Il controllo ELF non certifica
l'allineamento ZIP degli APK distribuiti né sostituisce la prova su dispositivo
con pagine da 16 KB.

Per firmare la release sul computer fidato che custodisce la chiave di upload,
impostare privatamente queste variabili, senza copiarne i valori in chat o log:

- `SOLPHIVIA_UPLOAD_STORE_FILE`: percorso del keystore;
- `SOLPHIVIA_UPLOAD_STORE_PASSWORD`;
- `SOLPHIVIA_UPLOAD_KEY_ALIAS`;
- `SOLPHIVIA_UPLOAD_KEY_PASSWORD`.

Eseguire `gradlew.bat --no-daemon testReleaseUnitTest lintRelease bundleRelease`.
Output: `app/build/outputs/bundle/release/app-release.aab`.
Una configurazione parziale di firma causa un errore; senza variabili la build
resta esplicitamente non firmata, senza ripiegare sulla chiave debug.

Conservare il keystore fuori dal repository con backup privato. Verificare
certificato e AAB firmato prima dell'upload. Incrementare versionCode rispetto
a qualsiasi versione già caricata in Console solo quando si prepara la build
finale. Non cambiare applicationId senza una decisione esplicita: gli aggiornamenti
Android dipendono dall'identità dell'app e dalla firma.

## Punti ancora da chiudere

1. Accedere alla Play Console dell'account sviluppatore e verificare lo stato
   reale della bozza, package, tracce di test e requisiti indicati dall'account.
2. Verificare il requisito target API vigente alla data di invio. Questa base
   usa target/compile API 35: qui non viene dichiarata conforme senza verifica.
3. Verificare l'esito ELF di Vosk e JNA. Se fallisce, aggiornare o ricompilare
   la dipendenza responsabile; non disattivare il controllo per pubblicare.
4. Completare privacy pubblica con titolare, recapito approvato e URL accessibile.
   `PRIVACY-IT.md` è stata completata con titolare e recapito approvati il 31 ottobre 2026; verificare la pagina e la coerenza con la build finale.
5. Dichiarazioni Data safety coerenti con codice e provider: chat, allegati,
   trascrizioni, cronologia, memoria, posizione/meteo, servizi Android STT/TTS.
   Non dichiarare genericamente che tutti i dati restano sul dispositivo.
6. Valutare informative evidenti e consenso prima del primo ascolto in background,
   stato microfono/notifica e arresto, incluso avvio automatico all'apertura.
7. Dichiarare e documentare i foreground service `microphone` e `specialUse`
   e la funzione overlay con i materiali richiesti dalla Console.
8. Verificare il collegamento OAuth e l'accesso del revisore con le regole
   applicabili del fornitore. Non inserire account personali nei materiali.
9. Verificare le regole applicabili ai contenuti IA, il meccanismo di segnalazione
   dei contenuti e l'accesso del revisore alle funzioni rilevanti.
10. Collaudare release su Motorola: avvio, overlay, conversazione, microfono,
    arresto, allegati, memoria, revoca permessi e aggiornamento da installazioni
    esistenti. Non disinstallare l'app con dati personali per forzare un upgrade.
11. Preparare scheda store e materiali reali; avviare la traccia di test ammessa
    dalla Console. L'eventuale requisito di 12 tester per 14 giorni riguarda il
    test chiuso per accesso alla produzione, non il test interno.

## Limiti della verifica

Al 31 ottobre 2026 i workflow sul commit `e0ff501429502f6416c777de3c0649891e5dd2d1` risultano completati con successo: `Google Play release readiness` (run 41109985165) e `Solphivia IA beta verification` (run 41109985245). Sono passati anche lo step test/Lint/bundle release e il controllo ELF. L'artefatto resta UNSIGNED e non è caricabile come release Play. Questa modifica
prepara la release e controlla un blocco tecnico concreto; non certifica privacy,
conformità alle policy, target API vigente o accettazione Google Play.


## Verifica Console e privacy — 31 ottobre 2026

Accesso in lettura e controllo della scheda autenticata verificati. App visibile: **Solphivia IA**; pagina Norme sulla privacy con URL ancora vuoto. Non è stato salvato un URL provvisorio.

L'informativa in [PRIVACY-IT.md](../PRIVACY-IT.md) include dati, destinatari, conservazione, cancellazione, controlli locali e diritti. Marco ha confermato titolare **Marco Iacobellis** e pubblicazione del recapito **solphivia.privacy@gmail.com**. Il documento è pubblicato sul ramo main. Il menu dell'app include ora «Informativa sulla privacy» con apertura della pagina pubblica e messaggio alternativo se manca un browser. Questo collegamento sarà disponibile solo nelle build che includono la modifica. Non è stata dichiarata la conformità del flusso OAuth né verificata l'operatività della casella email mediante invio di messaggi.

Evidenze tecniche sul commit esaminato:

| Fonte | Riscontro |
| --- | --- |
| AndroidManifest.xml | Microfono, overlay, posizione fine/coarse, servizi foreground; backup Android disabilitato |
| OpenAiChatClient.java | OAuth e Responses OpenAI, store:false, token cifrati, contesto e consenso memoria |
| MainActivity.java | Cronologia limitata a 16 messaggi; comando nuova chat |
| AssistantMemory.java / MemoryActivity.java | Memoria AES-GCM, modifica e import/export locali |
| CompanionInputActivity.java / PetService.java | Posizione su richiesta, geocoding e previsioni Open-Meteo; coordinate nelle preferenze locali |
| RadarClient.java | Servizio DPC e hosting tile AWS |
| VoiceSessionService.java | Riconoscimento Android e stato microfono; richiamo locale separato |
| app/build.gradle | Dipendenze JNA e Vosk; nessun SDK pubblicitario o analytics dichiarato |

Fonti delle regole consultate: [Google Play, dati utente e privacy](https://support.google.com/googleplay/android-developer/answer/10144311?hl=it), [GDPR, art. 13](https://eur-lex.europa.eu/eli/reg/2016/679/oj). Non dedurre la conformità dei fornitori dal solo esito CI.


Pagina privacy pubblica: https://github.com/Trikto7e8/solphivia-avatar-ai/blob/main/PRIVACY-IT.md . Documento pubblicato separatamente sul ramo main senza modificare il codice della release installata. La Console può registrare il link prima del successivo caricamento della build; il collegamento interno richiede la nuova build. Le verifiche su provider, informative microfono, target API, segnalazione contenuti IA e firma restano necessarie prima dell'invio della release.
