# Solphivia IA — versione corrente

**1.1.0-beta.5 · codice 53 · beta Android comunitaria · 7 novembre 2026.**

[APK e sorgenti](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v1.1.0-beta.5) · [Note e verifiche](releases/v1.1.0-beta.5.md) · [Guida](../AVVIO-RAPIDO-IT.md) · [Conversazione vocale](VOICE-SESSION.md).

L’utente ha confermato sul Motorola il ciclo **Fermati → Riprendi → Fermati** nella build funzionale di prova, codice 52. La beta pubblica 53 usa lo stesso codice applicativo con numero e nome di versione aggiornati. **Stop ha fallito nella stessa prova**. Per interrompere la lettura usare Fermati.

Il dialogo attivato passa al riconoscimento locale dopo la finestra iniziale Android e resta disponibile fino alla pausa esplicita. L’ultima trascrizione resta nel rigo fino a una nuova frase o modifica manuale. Le bozze manuali restano protette.

74 test JVM superati. Prove sintetiche sul dispositivo con 15/15 comandi, 0/20 attivazioni negative o di sola voce TTS, 3/3 comandi con parlato sovrapposto. I risultati sintetici sono distinti dalla prova reale. TalkBack, sessioni lunghe, cuffie, rumore, batteria e altri dispositivi restano da verificare.

Pacchetto principale `it.solphivia.mobile`, variante isolata `it.solphivia.mobile.v11.verification`, Android 8+, ARM64/ARMv7, Java 17. APK beta con firma debug compatibile con le beta precedenti e checksum SHA-256. Aggiornare sopra l’installazione esistente per conservare i dati.

[Privacy](../PRIVACY-IT.md) · [Storia delle versioni](RELEASE_HISTORY.md) · [Archivio old](../old/README.md).
