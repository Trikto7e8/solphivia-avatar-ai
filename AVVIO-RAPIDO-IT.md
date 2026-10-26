# Solphivia 1.0 beta 1: guida rapida Android

## Installare o aggiornare

Scarica l’APK dalla [release 1.0 beta 1](docs/CURRENT_RELEASE.md). È una build debug alpha per le prove, non una versione Google Play. Richiede Android 8 o successivo e ARM64/ARMv7.

Se aggiorni un’installazione con lo stesso certificato, installa sopra quella esistente: non disinstallare e non cancellare i dati per aggiornare. I dati cifrati dipendono anche dal Keystore dell’installazione. In caso di firma diversa, fermati e conserva l’app originale prima di decidere come trasferire i dati.

## Primo collegamento

1. Apri **Solphivia Avatar AI**. Le installazioni isolate di collaudo hanno il nome **PROVA**: non sono l’app principale.
2. Tocca **Collega ChatGPT** e completa personalmente l’autorizzazione nel browser con l’account desiderato. Solphivia non chiede la password. Disponibilità e limiti dipendono dall’account e dal fornitore.
3. Attendi il ritorno all’app. Non aprire manualmente l’indirizzo locale temporaneo `127.0.0.1`.
4. Concedi **Mostra sopra altre app** per il personaggio flottante e il permesso microfono per la voce. La notifica Android indica quando il microfono è attivo.
5. Se Android propone il collegamento Home, puoi confermarlo. Icona launcher e avatar flottante sono elementi diversi; chiudere l’avatar non disinstalla l’app.

## Parlare o scrivere

Di’ **«Hola Solphivia»**, attendi **«Cosa posso fare per te?»**, poi pronuncia la domanda o una normale risposta. Le parole riconosciute compaiono nel rigo di scrittura e vengono inviate automaticamente. L’anteprima progressiva dipende dal servizio Android. Una bozza scritta o modificata a mano viene conservata.

Dopo ogni risposta hai **cinque secondi dal microfono pronto** per iniziare a parlare ancora senza richiamo. Se inizi in tempo puoi finire la frase. In silenzio torna al riposo; per riprendere di’ «Hola Solphivia». Il servizio Android può interrompersi per errori: non è una garanzia di ascolto in ogni condizione. «Stop dialogo» torna al richiamo; «Spegni microfono» ferma la cattura.

Puoi sempre scrivere nel rigo e premere Invia. La dettatura singola da modificare resta disponibile nella schermata Voce. [Guida vocale completa](docs/VOICE-SESSION.md).

## Comandi locali

«Apri Maps», «apri» seguito dal nome completo dell’app, «cerca una farmacia su Maps», «portami a Roma», «apri fotocamera», «chiama» seguito da un numero. La chiamata va confermata nel telefono: nessuna chiamata automatica o lettura della rubrica. «Comandi» mostra il riepilogo. Questi comandi non leggono lo schermo e non inviano messaggi in altre app.

## Memoria e allegati

La voce invia la cronologia testuale recente senza aggiungere ricordi personali del vault o allegati. Per condividere memoria usa la chat scritta e la conferma modificabile, monouso. Gli allegati scelti vengono letti per la richiesta e il consenso è legato anche al digest del contenuto. Limiti: fino a 5 file, 8 MB per file e 15 MB complessivi nella conversazione inviata.

La chat di Solphivia è separata da quella di ChatGPT: non importa chat o memoria dell’app/sito ChatGPT. [Privacy tecnica](PRIVACY-IT.md).

## Se qualcosa non funziona

- Controlla di usare l’installazione giusta: il campo delle versioni di test contiene «PROVA».
- Se non compare l’avatar, controlla il permesso overlay della sola Solphivia.
- Se non ascolta, controlla permesso microfono, servizio di riconoscimento e impostazioni batteria della sola app. Il richiamo locale usa il modello incluso; riconoscimento dei comandi e TTS possono usare Internet.
- Se il browser non torna all’app, lascia Solphivia aperta in background, completa il consenso e usa il collegamento di ritorno. Non condividere token o codici temporanei.
- Per problemi di account o limiti, controlla il messaggio e le impostazioni del fornitore.

[Note di rilascio, prove e limiti](docs/releases/v1.0.0-beta.1.md). Blocco schermo, sessioni prolungate, recupero rete e altri dispositivi richiedono ancora collaudo.

## Chiudere e ritrovare Solphivia

Il pulsante tondo **⏻** sotto l’avatar ferma avatar e microfono e chiude le schermate, conservando i dati. Puoi chiederle **Come faccio a chiuderti?** e, dopo il richiamo, pronunciare **Chiudi Solphivia**. **Spegni microfono** ferma solo l’ascolto. **Aggiungi icona sulla Home** nel menu apre la richiesta Android: conferma Aggiungi. Se l’icona esiste già non viene duplicata.
