# Solphivia IA

<p align="center"><img src="docs/media/solphivia-icon.png" width="160" alt="One Solphivia character, the current app icon" /></p>

**1.1.0-beta.3 · Android community beta · Italian voice.**

An animated companion that stays on your Android screen. Talk, type, attach documents and keep personal memory under your control.

**[Download the current APK](https://github.com/Trikto7e8/solphivia-avatar-ai/releases/tag/v1.1.0-beta.3)** · [Italian guide](AVVIO-RAPIDO-IT.md) · [Leggi in italiano](README-IT.md) · [Google Play preparation](docs/google-play/README.md)

## Current features

- Floating avatar, contextual animations and outfits, chat and file attachments.
- Natural Italian from the installed Android speech engine; opening greeting explains the main commands.
- Local “Hola Solphivia” wake listening. After a reply, start the next command within five seconds; silence returns to wake waiting.
- Stop/Alt to interrupt reading; Riprendi/Prosegui to resume a suspended reading. Voice transcripts remain in the composer during the answer.
- One-character Solphivia IA icon. Round power button and menu sit to the left of the avatar. Power closes avatar, speech and microphone while preserving data.
- Explicit app/Maps commands and dialer preparation. Phone calls require confirmation in the dialer.
- Encrypted local personal memory and encrypted Memory Vault import/export. Optional ChatGPT connection; no import of your ChatGPT conversations or memory.

This is a debug-signed test APK, not a store release. Speech recognition/TTS and an optional AI provider may use the network. The wake phrase and reading-control detection run locally. It is not speaker authentication.

## Tests and limits

65 JVM tests passed on the final source; synthetic device trials recognized 13/13 stop/resume phrases with 0/15 activations in negative trials. The maintainer confirmed voice interruption, continued commands, transcript visibility and full shutdown. Beta 3 adds the opening command summary and left-side controls; its build/Lint and Home icon/control placement were verified. These results do not replace long-session, battery, noise, headphone, screen-lock and multi-device tests. See [release notes](docs/releases/v1.1.0-beta.3.md).

## Build and contribute

Use JDK 17, Android SDK 35, Gradle 8.11.1 and AGP 8.9.1. [Build instructions](BUILD-E-GITHUB-IT.md) · [Contribute](CONTRIBUTING.md) · [Collaboration](COLLABORATE.md).

Google Play preparation is separate: API 36 migration, native-library checks, release signing, privacy/contact completion and in-app AI reporting remain gates before submission. Local LM Studio/Ollama adapters and iOS are roadmap work.

[Privacy](PRIVACY-IT.md) · [Roadmap](ROADMAP.md) · [Current status](STATUS.md) · [Old versions and references](old/README.md) · [AI Memory Vault](https://github.com/Trikto7e8/AI-memory-vault)

Code: MIT. Character artwork, animations and promotional assets: [reserved rights](ASSETS-LICENSE.md). Solphivia IA is independent and not affiliated with OpenAI or Google.
