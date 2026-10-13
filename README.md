# Solphivia Avatar AI

## Contributors wanted: Android, iOS and local AI

Help take Solphivia from a working Android alpha to a robust Google Play release, a native iOS experience and selectable local/cloud models. Android engineers, SwiftUI developers, testers and accessibility contributors are welcome.

**[Join the collaboration](COLLABORATE.md)** · [Help wanted](https://github.com/Trikto7e8/solphivia-avatar-ai/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22) · [Download the alpha](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha2)


**An animated Android AI assistant with a chat bubble, touch controls, speech, and a provider-based AI roadmap.**

**Leggi in italiano:** [README-IT.md](README-IT.md) · [Italian quick start](AVVIO-RAPIDO-IT.md)

> Community alpha. This is an evolving project, not a production-ready release. The current source has not completed Google Play review.

**Guide in Italian:** [first-use and ChatGPT connection](AVVIO-RAPIDO-IT.md) · [privacy details](PRIVACY-IT.md) · [build and GitHub upload](BUILD-E-GITHUB-IT.md) · [project presentation](PRESENTAZIONE-IT.md)

## What it does today

- Shows Solphivia as a draggable Android overlay with a speech bubble and one-line chat input.
- Includes idle, run, bounce, sit, sleep, and cyborg animation states.
- Supports speech output, Android speech recognition, and document/image attachments.
- Connects to ChatGPT through OpenAI's Sign in with ChatGPT flow and eligible Responses API usage. It does not read the user's ChatGPT conversations or memory.
- Stores the local conversation on the device and encrypts OAuth credentials with Android Keystore.

The ChatGPT connection is optional and depends on account eligibility and user consent. ChatGPT plan usage is distinct from OpenAI API-key billing. See [OpenAI's documentation](https://developers.openai.com/siwc/token-sharing-open-source) and [privacy details](https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites).

## Project direction

The goal is to keep Solphivia's avatar and interaction layer independent from any one model provider. Planned work includes:

1. A small provider interface for chat and model capabilities.
2. Local connections to a model server such as LM Studio or Ollama.
3. Additional cloud provider adapters, configured with each user's own account or credentials.
4. A later on-device model option, subject to phone memory, performance, and model licenses.
5. A voice conversation mode that renders Solphivia's face or full-body avatar in Solphivia itself.

These are roadmap items, not current features. Each provider will need its own adapter; compatibility with every LLM cannot be promised.

## Build the current prototype

- Android Studio with Android SDK 35 and JDK 17
- Gradle 8.11.1 / Android Gradle Plugin 8.9.1

Open this folder in Android Studio and build `:app:assembleDebug`. The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

The project currently targets API 35. A Play Store submission needs migration and testing against the current Play target API requirements before it can be submitted.

## Google Play preparation

Before a Play release, the project needs (at minimum):

- Target API 36 and a signed Android App Bundle.
- A privacy policy and an accurate Play Data safety declaration for account data, chat text, attachments, and speech features.
- An in-app way to report or flag problematic AI responses.
- Play Console declarations and review for the overlay and `specialUse` foreground services.
- Device testing, store assets, content rating, and a closed test if the developer account is subject to that requirement.

## Privacy and affiliation

When connected to an AI provider, prompts and selected attachments are sent to that provider to answer the request. Review each provider's terms and configure its connection deliberately. Solphivia is an independent project and is not sponsored by or affiliated with OpenAI. OpenAI, ChatGPT, and related names are trademarks of their respective owner; they are used only to identify an optional integration.

For the current Android prototype's local storage, voice, attachment, and overlay behavior, see [PRIVACY-IT.md](PRIVACY-IT.md). It is a technical draft, not a substitute for the complete public privacy policy required for a store release.

## Licenses

- Source code: MIT, see `LICENSE`.
- Solphivia character, face, animation sprites, costume designs, launcher artwork, and visual identity: reserved; see `ASSETS-LICENSE.md` or [the Italian translation](LICENZA-ASSET-IT.md).

## Contributing

Code contributions are welcome. Please read `CONTRIBUTING.md` first. Do not post credentials, access tokens, private user content, or private screenshots in issues or pull requests. Contributions to code do not grant rights to reuse the Solphivia character or artwork.

## Current verification status

Version **0.9.2-alpha2** (code 31) was built, passed Android Lint and isolated Motorola regression tests, and was installed on the Motorola. On 13 October 2026 the user confirmed it works and approved this alpha. Further improvements and listening evaluation remain; The user has confirmed that eSpeak works on the phone. See the [verification report](VERIFICA-0.9.2-alpha2.md).

## 0.9.2-alpha2

- Memoria personale cifrata sul dispositivo, editor e comando `ricorda:`; import/export cifrato compatibile con AI Memory Vault v1. Anteprima modificabile per ogni invio remoto. Nessuna sincronizzazione automatica e nessun nuovo adattatore LLM.
- Pulsanti `+` e microfono visibili anche nella schermata interna; dettatura modificabile prima dell’invio e allegati mantenuti nei turni successivi.
- Scelta del motore TTS e accento inglese con voce offline, quando disponibile. eSpeak NG è separato e non incluso nell’APK.
- Radar DPC aggiornato: dati, timestamp ed errori visibili. Segnala echi vicini, senza stimare direzione o tempo di arrivo.
- Sonno animato, proporzioni corrette e schermata `☰ → Prova animazioni`; 108 sprite e 1248 combinazioni verificate.

Scarica l’APK dalla [pre-release v0.9.2-alpha2](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v0.9.2-alpha2). È una build debug per test, non una distribuzione Google Play. Il profilo iniziale è generico e può essere personalizzato localmente nell’editor Memoria.
