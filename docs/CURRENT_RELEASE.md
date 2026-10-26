# Solphivia IA — versione corrente

**1.1.0-beta.3 · codice 49 · beta Android comunitaria.**

[Scarica APK e sorgenti](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v1.1.0-beta.3) · [Note e verifiche](releases/v1.1.0-beta.3.md) · [Guida](../AVVIO-RAPIDO-IT.md) · [Conversazione vocale](VOICE-SESSION.md).

Voce italiana, arresto e ripresa della lettura, trascrizione visibile, saluto con comandi e controlli a sinistra. L’utente ha confermato il dialogo successivo al saluto, arresto/ripresa e chiusura. Verificati sul telefono icona normale con un solo personaggio e apertura dalla Home.

65 test JVM superati, build APK/test Android e Lint riusciti nell’ultima verifica dei sorgenti. Le prove acustiche sintetiche restano distinte: 13/13 frasi positive e 0/15 attivazioni nei casi negativi. Non certificano ogni ambiente o durata.

Pacchetto principale it.solphivia.mobile; variante isolata it.solphivia.mobile.v11.verification. minSdk26, compileSdk/targetSdk35, Java17. L’APK pubblico mantiene la firma debug già usata nelle prove, con checksum nelle note. Nessuna versione Play è stata caricata.

Il richiamo resta disponibile finché il microfono non viene spento esplicitamente. Dopo cinque secondi senza iniziare il comando successivo si torna all’attesa locale di Hola Solphivia. Stop/Alt interrompono la lettura; ⏻ chiude avatar, voce e microfono conservando i dati.

Per la Home usa l’icona normale dall’elenco app. Il menu mostra istruzioni e non crea altre scorciatoie con badge duplicato. · [Tutti i vecchi file e riferimenti](../old/README.md).
