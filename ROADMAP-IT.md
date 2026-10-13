# Roadmap

Questi sono obiettivi del progetto, non promesse sulle funzioni della versione attuale.

## Alpha comunitaria

- Riprovare su più dispositivi Android l'aggiornamento dell'icona del collegamento Home e il loop continuo della corsa.
- Aggiungere test Android mirati agli stati delle animazioni, all'interpretazione della chat e alla gestione degli errori dei provider.
- Aggiungere nell'app un'azione per segnalare risposte dell'IA problematiche.
- Preparare l'informativa privacy e le dichiarazioni di Play Console per i servizi foreground.

## Collegamenti ai modelli

- Definire un'interfaccia provider per testo, allegati, risposte in streaming e dichiarazione delle capacità.
- Mantenere il collegamento open source documentato a Sign in with ChatGPT come adattatore facoltativo.
- Aggiungere connessioni configurabili dall'utente a server locali LM Studio/Ollama.
- Aggiungere adattatori cloud usando i metodi di autenticazione e fatturazione supportati da ciascun fornitore.
- Valutare separatamente l'esecuzione sul dispositivo: prestazioni, memoria e licenze variano secondo dispositivo e modello.

## Avatar e voce

- Creare in Solphivia una schermata dedicata alla conversazione vocale con avatar del volto o a figura intera.
- Mantenere riconoscimento vocale, sintesi vocale e selezione del modello come componenti separati, così da poterli variare indipendentemente.
- Mantenere l'overlay Android Home esistente sotto il controllo dell'utente e con un comando visibile per fermarlo.

## Pubblicazione

- Aggiornare l'app al target API corrente di Google Play e provarla sul dispositivo.
- Completare privacy policy, dichiarazione Data safety, classificazione dei contenuti e dichiarazioni sui servizi foreground.
- Preparare un Android App Bundle firmato e svolgere test interni/chiusi prima di qualsiasi pubblicazione generale.

## iOS and community collaboration

Start with an iOS feasibility study and an in-app avatar prototype, with portable encrypted memory and independent provider adapters. Document platform limitations before committing to feature parity. See [COLLABORATE.md](COLLABORATE.md) for contribution areas.
