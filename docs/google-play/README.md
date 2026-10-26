# Solphivia IA — preparazione Google Play

Materiale di proposta per 1.1.0-beta.3, aggiornato il 26 ottobre 2026. **Non ancora inviabile a Google Play.** Nessun account Play Console è stato creato, nessun pagamento effettuato e nessun modulo inviato.

## Materiale preparato

- [Testi della scheda italiana](STORE-LISTING-IT.md) e [metadati](listing-it.json).
- Icona 512×512 con un solo personaggio e feature graphic 1024×500 in [assets](assets). La grafica è promozionale, non uno screenshot.
- [Mappa Data safety](DATA-SAFETY-DRAFT.md), [permessi e servizi](PERMISSIONS-AND-SERVICES.md) e [istruzioni di firma](BUILD-AND-SIGN.md).
- [Checklist di invio](SUBMISSION-CHECKLIST.md), [guida account](ACCOUNT-SETUP-IT.md) e [rapporto tecnico](native-libraries.json).
- [Bozza privacy attuale](../../PRIVACY-IT.md). La versione definitiva richiede responsabile e recapito pubblico del progetto.

## Blocchi prima dell’invio

1. Migrare e collaudare compile/target SDK da 35 a 36; la beta GitHub conserva la build provata. [Requisito target API](https://support.google.com/googleplay/android-developer/answer/11926878).
2. Verificare librerie native, allineamento e funzionamento reale in ambiente 16 KB. Il rapporto statico non è un test runtime. [Guida Android](https://developer.android.com/guide/practices/page-sizes).
3. Generare AAB release con chiave di caricamento custodita fuori da Git e configurare Play App Signing. L’APK GitHub usa firma debug.
4. Aggiungere una segnalazione in-app dei contenuti AI che raggiunga privatamente il manutentore senza uscire dall’app; concordare il recapito o backend prima di implementare. Non usare una issue GitHub pubblica come destinazione automatica di conversazioni private. [Policy AI](https://support.google.com/googleplay/android-developer/answer/13985936).
5. Completare privacy pubblica su HTTPS, supporto, dichiarazioni Data safety/servizi, accesso revisori, target d’età e questionario contenuti sulla build finale.
6. Acquisire almeno due screenshot reali e privi di dati personali della candidata Play: le immagini promozionali non li sostituiscono.
7. Creare account e completare il collaudo richiesto. Per un nuovo account personale: almeno 12 tester iscritti continuativamente per 14 giorni prima della richiesta di accesso alla produzione. [Requisiti test](https://support.google.com/googleplay/android-developer/answer/14151465).

La scadenza esatta relativa alle pagine di memoria va ricontrollata in Play Console: le pagine ufficiali lette riportano indicazioni temporali differenti. La preparazione richiede comunque una prova di compatibilità 16 KB, senza dedurla dall’assenza di errori su un telefono 4 KB.

Controllo statico della beta: le librerie ARM64 Vosk e JNA hanno segmenti ELF allineati a 16 KB. La libreria Vosk ARMv7 riporta 4 KB; il requisito sui dispositivi a 64 bit non va dedotto da quel risultato a 32 bit. Runtime dedicato e allineamento finale del bundle restano da verificare.
