> **1.1.0-beta.2 — experimental development candidate.** Say “Basta Solphivia” to interrupt reading, or use “Ferma la voce”. Stop/resume commands and retained transcripts passed synthetic tests; the new commands still need live validation. [Notes and limits](docs/releases/v1.1.0-beta.2.md). The 1.0 review remains separate.

<p align="center"><img src="docs/media/solphivia-cover.svg" alt="Solphivia: animated AI companion for Android, with chat, voice and portable memory" width="100%" /></p>

# Meet Solphivia IA

**New in 1.1 beta 2 — experimental voice interruption.** Natural Italian, clear shutdown controls and a Home icon remain available. While reading, local recognition can listen for “Basta Solphivia” or “Fermati Solphivia”. [Test status and limits](docs/releases/v1.1.0-beta.2.md).

An animated companion on your Android screen. Talk, type, attach a file, and keep your assistant's personal memory under your control.

**[Try the Android beta](docs/CURRENT_RELEASE.md)** · **[Help build Solphivia](COLLABORATE.md)** · [Leggi in italiano](README-IT.md)

> Community beta · Android available for testing · Google Play and iOS are work ahead.

## A character that moves with the conversation

<table><tr>
<td align="center"><img src="app/src/main/assets/solphivia/idle/frame_00.png" width="170" alt="Solphivia in her everyday outfit" /><br/><b>Everyday companion</b></td>
<td align="center"><img src="app/src/main/assets/solphivia/mode_vj/frame_00.png" width="170" alt="Solphivia wearing the cyan and purple cyborg suit" /><br/><b>Cyborg mode</b></td>
<td align="center"><img src="app/src/main/assets/solphivia/listen/frame_00.png" width="170" alt="Solphivia in her listening animation" /><br/><b>Ready to listen</b></td>
</tr></table>

Actual character frames from the Android app. Artwork is reserved under [ASSETS-LICENSE.md](ASSETS-LICENSE.md).

| Make it yours | Keep control | Build together |
| --- | --- | --- |
| Draggable avatar, animations, chat and Android speech. | Local encrypted memory, editable sharing previews and encrypted Memory Vault export. | Help improve Android, accessibility and testing; design local-model adapters and iOS. |


Promotional illustrations generated from the original character sprites; these are not app screenshots. The frames in the table above are actual app assets.

## Start in a few minutes

1. Get the APK from the [alpha release](docs/CURRENT_RELEASE.md). This is a debug test build, not a store release.
2. Follow the [Italian setup guide](AVVIO-RAPIDO-IT.md), enable the overlay and choose an installed Android speech engine.
3. Try the avatar and share a [reproducible test report](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/6) using synthetic examples.

**Want to contribute?** Start with [testing](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/6), [Android release readiness](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/3), [iOS feasibility](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/4) or [model providers](https://github.com/Trikto7e8/solphivia-avatar-ai/issues/5). Small, focused contributions are welcome.

**Explore:** [Collaboration](COLLABORATE.md) · [Privacy](PRIVACY-IT.md) · [Contributing](CONTRIBUTING.md) · [AI Memory Vault](https://github.com/Trikto7e8/AI-memory-vault)

## What it does today

- Shows Solphivia as a draggable Android overlay with a speech bubble and one-line chat input.
- Includes idle, run, bounce, sit, sleep, and cyborg animation states.
- Supports Android speech, transcript preview in the composer and five seconds from microphone readiness to start a follow-up after each response.
- Opens apps by exact name, Maps/directions and dialer preparation on explicit user requests; calls still need manual confirmation.
- Includes encrypted local memory, vault import/export, single-use consent and document/image attachments.
- Connects to ChatGPT through OpenAI's Sign in with ChatGPT flow and eligible Responses API usage. It does not read the user's ChatGPT conversations or memory.
- Stores the local conversation on the device and encrypts OAuth credentials with Android Keystore.

The ChatGPT connection is optional and depends on account eligibility and user consent. ChatGPT plan usage is distinct from OpenAI API-key billing. See [OpenAI's documentation](https://developers.openai.com/siwc/token-sharing-open-source) and [privacy details](https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites).

## Project direction

The goal is to keep Solphivia's avatar and interaction layer independent from any one model provider. Planned work includes:

1. Extend the delivered Stage A provider interface with additional adapters and capability settings.
2. Local connections to a model server such as LM Studio or Ollama.
3. Additional cloud provider adapters, configured with each user's own account or credentials.
4. A later on-device model option, subject to phone memory, performance, and model licenses.
5. Better multilingual voice, wake accuracy and battery measurements across devices.

Stage A is already included in 1.0 beta 1; the remaining items are roadmap work, not current features. Each provider will need its own adapter; compatibility with every LLM cannot be promised.

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

## Current verification — 1.0 beta 1

**1.0.0-beta.1, code 46, 25 October 2026.** 52 JVM tests pass; build and Lint complete (0 errors, 136 warnings). The user confirmed natural Italian on the preceding alpha12 trial; beta-specific close/icon checks are tracked separately in the [review notes](docs/releases/v1.0.0-beta.1.md). No store approval or completed 1.1 voice interruption is claimed.

Locked-screen/background sessions, prolonged use, network recovery and other devices still need testing. Voice does not automatically add vault memory or attachments; editable memory consent remains in typed chat.

[Current version and downloads](docs/CURRENT_RELEASE.md) · [Historical archive](docs/RELEASE_HISTORY.md)
