# Verifica locale alpha4 — 14 ottobre 2026

> Rapporto storico: date, versione e risultati qui sotto restano quelli originali. La versione corrente è [alpha11 (44)](docs/CURRENT_RELEASE.md); [download consigliato](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha11). Non usare questo rapporto come collaudo dell’ultima versione.

APK debug 0.9.2-alpha4, codice 33, installato sul Motorola senza cancellazione dati.

- Compilazione e Android Lint: riusciti.
- Regressioni isolate: memoria/Vault cifrati e filtri consenso, allegati e limiti, costume manuale, schermata connessione, tre cicli servizio: riusciti.
- 108 sprite decodificati; 2048 combinazioni di modalità/abito/stato/tempo renderizzate. Seduta e sonno conservati; avanzamento del sonno verificato.
- Modalità Meteorina automatica, precedenza della scelta manuale, tag modalità invalido ignorato, ritorno ad auto e intervallo minimo tra cambi: verificati.
- Nuovi gesti nod/explain/celebrate tornano a idle entro il tempo previsto; thinking resta attivo finché il controller non cambia stato.
- Installazione con aggiornamento sul dispositivo riuscita, senza cancellazione dati.

I tre nuovi gesti animano gli sprite esistenti: non sono nuovi disegni dei movimenti di mani/braccia. Qualità delle scelte contestuali e frequenza nelle conversazioni reali richiedono valutazione dell'utente; non è stato simulato un colloquio remoto con dati personali.

SHA-256 APK: `f178e4a0d8304e6f6611e18a7e5bec2430d9be62a2da8fa8359583da22464695`.

Decisioni conservate in `docs/CONTEXTUAL-AVATAR-DECISIONS.md`. Release comunitaria di prova; nessuna approvazione degli store.
