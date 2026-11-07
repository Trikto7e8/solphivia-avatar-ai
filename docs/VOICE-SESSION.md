# Conversazione vocale nella 1.1 beta 5 / Voice conversation

## Parlare e continuare il dialogo

Apri Solphivia IA, concedi overlay e microfono e attendi il saluto. Di’ **Hola Solphivia**, aspetta **Cosa posso fare per te?**, poi pronuncia una domanda o un comando.

Dopo la risposta il dialogo resta disponibile. Il servizio Android viene usato nella finestra iniziale di cinque secondi. Se la finestra scade o il servizio incontra errori temporanei, subentra il riconoscimento locale continuo. Per tornare al solo richiamo di’ **Stop dialogo**. **Spegni microfono** arresta la cattura. La notifica indica il microfono attivo.

Le frasi riconosciute nel dialogo attivo sono inviate automaticamente al fornitore collegato. Anche altre persone vicine possono essere trascritte. Richiamo, riconoscimento locale di continuità e controlli della lettura sono elaborati sul telefono. Il riconoscitore Android e il motore TTS possono usare la rete. Solphivia non conserva registrazioni audio.

## Fermare e riprendere la lettura

Durante il parlato di’ **Fermati**, poi **Riprendi** per continuare. Il 7 novembre 2026 l’utente ha confermato **Fermati → Riprendi → Fermati** sul Motorola con la build di prova codice 52. La beta pubblica codice 53 usa lo stesso codice applicativo.

**Stop ha fallito nella stessa prova e rimane un problema aperto.** Alt, Basta e i comandi con il nome Solphivia sono implementati e richiedono ulteriori prove reali. I test sintetici favorevoli non garantiscono il comportamento con voce, eco e ambiente reali.

Quando esiste una lettura sospesa sono implementati anche Prosegui, Continua, Dimmi, Ti ascolto, Dai e Vai. La prova reale di questa revisione ha confermato Riprendi. La posizione di ripresa dipende dagli aggiornamenti del motore TTS e può ripetere il frammento corrente.

Durante il parlato il riconoscitore locale dei controlli resta disponibile, mentre l’invio dei normali comandi è sospeso. I controlli richiedono il microfono attivo. Il pulsante tondo **⏻** chiude avatar, voce e microfono conservando i dati.

## Trascrizione e bozze

L’ultima frase riconosciuta rimane nel rigo durante e dopo la risposta, fino alla frase successiva o alla modifica manuale. Anteprime identiche non aggiornano ripetutamente il campo. Una bozza scritta o modificata a mano resta protetta.

La voce invia la cronologia testuale recente senza aggiungere ricordi del vault o allegati. La chat scritta mantiene la conferma modificabile della memoria e il consenso monouso. La dettatura singola modificabile resta disponibile separatamente.

## Accessibilità e limiti

TalkBack è il lettore schermo Android che pronuncia pulsanti e contenuti. L’arresto vocale di Solphivia è una funzione distinta. La convivenza con TalkBack, compresa la possibilità che la sua voce attivi comandi, richiede ancora una prova dedicata. È un criterio di accettazione per l’uso con disabilità visiva e a mani libere.

Sono aperte le prove di rumore, cuffie, consumo, sessioni prolungate, blocco schermo e altri dispositivi. Il riconoscimento non autentica chi parla.

## English

Open Solphivia, grant overlay and microphone permissions, wait for the greeting and say **Hola Solphivia**. After **Cosa posso fare per te?**, speak your request. Active dialogue switches to local continuous recognition after the initial Android window or temporary recognizer errors. **Stop dialogo** returns to wake waiting. **Spegni microfono** stops capture.

During speech use **Fermati**, then **Riprendi**. The device user confirmed two interruptions with a resume between them. **Stop failed in that live trial** and remains a known issue. The last transcript stays until replaced by new speech or manual editing. Manually edited drafts remain protected. Nearby speech can also be recognized and submitted during active dialogue.

TalkBack coexistence and broader device/environment testing remain outstanding. Local wake and interruption detection do not identify the speaker. Android recognition, TTS and the connected AI provider may use the network.

[Release notes / Note e verifiche](releases/v1.1.0-beta.5.md) · [Privacy](../PRIVACY-IT.md) · [Guida](../AVVIO-RAPIDO-IT.md).
