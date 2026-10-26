# Animazioni e abiti contestuali — decisioni del 14 ottobre 2026

**Stato corrente: 1.0 beta 1 (44).** Le decisioni datate qui sotto documentano l’origine delle funzioni contestuali mantenute. Per versione, download e prove attuali usa [la pagina corrente](../../docs/CURRENT_RELEASE.md).

L'utente chiede di conservare e verificare seduta, sonno e gli altri stati esistenti, e di aggiungere anche nuove animazioni. Solphivia deve scegliere gesti e abiti dal contesto, rispettando le scelte esplicite dell'utente.

- Prof: spiegazioni e insegnamento; VJ: musica, visual e performance; Codex: programmazione e debug; Meteorina: meteo e radar.
- La scelta automatica usa soltanto i tag consentiti della risposta e non osserva altre app. Non si deducono emozioni private o dati meteo dal testo.
- Gli abiti scelti manualmente restano fissi fino al comando `modalità auto` o `scegli tu`. Il costume cyborg manuale ha la precedenza.
- Cambi automatici di abito distanziati almeno 20 secondi; tag invalidi ignorati. La preferenza automatica non richiede nuovi permessi.
- Il look meteo appare nella modalità Meteorina; usa le condizioni già ottenute dal modulo meteo, quando attivo. Senza dati, usa il look base senza inventare condizioni.
- Nuove animazioni `nod`, `explain`, `celebrate`: cenno, leggera inclinazione durante una spiegazione e breve rimbalzo di gioia. Sono movimenti del personaggio esistente, senza nuovi sprite disegnati e senza spostare l'intera finestra.
- `sit` è anche un tag contestuale consentito; il sonno rimane determinato dall'inattività. L'animazione di riflessione resta attiva mentre si prepara la risposta.
- I nuovi gesti sono brevi e i prompt chiedono di non ripeterli a ogni turno. L'anteprima include gli stati nuovi e l'opzione Movimenti ridotti, che elimina le nuove trasformazioni e l'oscillazione idle, senza eliminare le animazioni esistenti.

I tag del modello sono suggerimenti visivi, non comandi per controllare il dispositivo. La qualità delle decisioni del modello richiede anche una prova nelle conversazioni reali.
