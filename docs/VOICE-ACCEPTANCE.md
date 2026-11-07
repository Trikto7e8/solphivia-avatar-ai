# Continuità di Solphivia e prove prima di ogni aggiornamento

Requisiti riletti nelle conversazioni originali il 6 novembre 2026. Base pubblica `v1.1.0-beta.4`, commit applicativo `3244cad`, documentazione successiva `bfd24d9`. La versione locale di prova è `1.1.0-beta.5-test`, codice 51.

## Richieste e comportamenti da conservare

| Evoluzione | Richiesta dell’utente e riscontro | Vincolo per gli aggiornamenti |
|---|---|---|
| Alpha Voice Access | Apertura, saluto, richiamo Hola Solphivia; eliminazione del ciclo di suoni di avvio/chiusura microfono | Attesa locale del richiamo e un solo proprietario della cattura |
| Alpha9–11 | Risposta alla frase successiva al saluto e invio vocale automatico | Il flusso vocale ordinario non deve fermarsi nella conferma memoria |
| Beta 1.0 italiana | Voce italiana accettata, avatar flottante e chiusura completa | Conservare voce selezionata, asset, servizio e dati dell’app |
| Beta 1.1 iniziale | Stop mentre parla, ripresa successiva | Fermare la lettura mantenendo disponibile il dialogo |
| Beta 2 | Stop/Alt/Basta/Fermati, Riprendi/Prosegui/Continua/Dimmi/Ti ascolto/Dai/Vai | Verificare più cicli, non una sola interruzione |
| Beta 2–3 | Trascrizione e seconda frase, senza cancellare bozze manuali | Ricevuta vocale distinta dalla bozza; proteggere le modifiche manuali |
| Beta 3 | Icona con un solo personaggio, menu e power tondo a sinistra, saluto con comandi | Non ricreare scorciatoie con badge duplicato o coprire lo sprite |
| Beta 4 | Collegamento ChatGPT dal menu e browser, overlay disponibile prima del login | Conservare il percorso OAuth e il ritorno all’avatar |
| Richiesta del 6 novembre | Ultima frase visibile, conversazione disponibile senza continui richiami, Stop ripetuto | Mantenere il testo fino alla frase successiva; dopo il richiamo continuare il dialogo fino a pausa esplicita |
| Solfivia Reader | Lettura fedele, pausa, discussione, ripresa e segnalibro; uso con disabilità visiva e mani libere | Accessibilità e TalkBack inclusi nei criteri di accettazione |

La beta pubblica precedente tornava al richiamo dopo una finestra di cinque secondi e cancellava il testo a risposta conclusa. Erano comportamenti codificati e documentati, ora aggiornati nella prova locale secondo la nuova richiesta. Alcuni riscontri reali erano positivi ma intermittenti. Non vanno presentati come garanzia su tutte le revisioni.

## Prova locale del 6 novembre

- Nessuna modifica ad asset, sprite, voce TTS, icone, power, percorso OAuth o controlli delle app esterne.
- Primo comando tramite riconoscitore Android; dopo silenzio/errore temporaneo il dialogo passa al modello italiano locale già incluso, senza un ciclo di riavvii del riconoscitore Android. La qualità delle trascrizioni locali richiede prova reale.
- Stop interrompe la lettura; Stop dialogo torna all’attesa del richiamo; Spegni microfono e power arrestano il microfono.
- Accettate anche sequenze composte soltanto da parole di arresto, come Stop fermati. Frasi come Stop alla stazione restano escluse dal controllo breve.
- Gli errori temporanei della cattura locale avviano un ripristino. I permessi mancanti richiedono comunque l’intervento dell’utente.
- Quando il motore TTS fornisce intervalli reali, il filtro contro l’eco considera il frammento attualmente pronunciato. Una parola Stop citata altrove nella risposta non rende più tutta la risposta non interrompibile. Con motori senza intervalli precisi rimane la protezione conservativa precedente.
- I contatori diagnostici della voce contengono numeri e categorie di errore, senza registrazioni o testo dettato.

## Accettazione sul dispositivo

1. Aprire l’app dall’icona ordinaria e osservare una sola Solphivia, saluto e controlli accessibili.
2. Hola Solphivia, domanda, risposta e trascrizione della seconda frase senza invio manuale.
3. Storia, Stop, Riprendi, Fermati, nuova domanda. Ripetere almeno tre cicli e registrare separatamente primo/secondo/terzo esito.
4. Dopo 15 e 60 secondi di silenzio, nel dialogo già attivato, pronunciare Comandi senza un nuovo richiamo.
5. Ultimo messaggio visibile durante e dopo la risposta; frase successiva lo sostituisce. Una bozza modificata a mano deve restare protetta.
6. Stop dialogo lascia soltanto il richiamo; power ferma avatar e microfono. Riapertura senza perdere dati.
7. TalkBack attivo, navigazione senza guardare lo schermo, nomi dei pulsanti comprensibili e nessun comando involontario causato dalla voce del lettore schermo.
8. Per Reader, Leggi, Pausa, Riprendi, Ripeti e Segnalibro con una fonte autorizzata; distinguere la lettura dai commenti. Il plugin attuale contiene istruzioni, mentre motore autonomo e persistenza richiedono implementazione.

Il 6 novembre TalkBack risultava disattivato sul telefono. La compatibilità live non è dichiarata collaudata. I test sintetici e quelli del cursore non sostituiscono prove con voce dell’utente, altoparlante, cuffie, rumore e lettore schermo.

## Risultati già osservati in questa lavorazione

- 69 test JVM superati, APK e APK strumentale compilati, Lint senza errori.
- Prova sintetica sul dispositivo nella copia isolata, con 15/15 comandi positivi e 0/15 falsi controlli nei casi negativi e di eco testuale. Nessuna registrazione dell’utente.
- Prova strumentale del campo di trascrizione superata, inclusi aggiornamenti parziali, sostituzione della ricevuta, annullamento e protezione della bozza.
- Build principale codice 51 installata come aggiornamento. Verifica ripetuta con la voce dell’utente ancora da completare; il telefono si è bloccato durante i controlli successivi.

La prova non viene pubblicata come nuova release finché il ciclo reale ripetuto non è stato valutato.

## Correzione Stop, prova 2 del 6 novembre

- La prova reale della build 51 ha fallito. I contatori mostravano 6 candidati, 0 conferme e nessuna nuova interruzione rispetto al valore iniziale di 5. Le precedenti prove sintetiche con una sola voce non coprivano questa condizione.
- Il riconoscitore libero può sentire soltanto la voce del TTS mentre quello dedicato rileva Stop. Ora Stop e Fermati possono superare il controllo quando il testo del secondo riconoscitore corrisponde all'eco conosciuta. Alt e Basta mantengono il controllo più restrittivo.
- Sono gestiti i comandi preceduti da testo effettivamente letto, i parziali confermati e stabili per almeno 600 ms e la resa sintetica osservata di Fermati come fermare durante la sovrapposizione. I frammenti estranei e le negazioni restano esclusi nei casi provati.
- Il testo completo della lettura serve a riconoscere l'eco accumulata. La protezione dai comandi pronunciati da Solphivia usa ancora il frammento corrente.
- 74 test JVM superati. Compilazione e Lint completati senza errori.
- Prova strumentale isolata superata con 15/15 comandi, 0/20 interruzioni nei casi negativi e di sola voce sintetica, 3/3 comandi con voce sintetica sovrapposta, rispettivamente Stop, Fermati e Stop fermati. Il mix usa voce di sfondo al 18% e comando all'80%, senza simulare la cancellazione acustica del microfono. I file temporanei vengono eliminati.
- Build 52, 1.1.0-beta.5-test.2, installata nell'app principale. La copia isolata è disabilitata conservandone i dati. Richiesta la verifica reale di Stop, Riprendi, Stop. Tale verifica e la compatibilità con TalkBack rimangono da completare.

## Riscontro reale del 7 novembre 2026

L’utente conferma Fermati, Riprendi e un secondo Fermati riusciti nella build 52. Stop ha fallito e rimane un problema noto. La verifica TalkBack resta da eseguire. Autorizzata la pubblicazione della beta 5, codice 53, con gli stessi sorgenti applicativi e metadati aggiornati, distinguendo i risultati reali da quelli sintetici.
