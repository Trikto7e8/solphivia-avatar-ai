# Solphivia iOS feasibility study — Issue #4

**Status:** evidence-scoped feasibility and migration study only; **no iOS app, prototype, Xcode build, device test, App Store/TestFlight submission, or security interoperability test performed**.  
**Contributor:** @sunny-kavixo. **AI assistance:** ChatGPT (GPT-6) assisted with source review, research and drafting; observations are differentiated from proposals.  
**Reviewed basis:** repository Android Java sources on default `main` and linked Apple Developer documentation (accessed October 2026). This is not a substitute for runtime verification, iOS developer-account entitlements, or Apple's App Review decision.

## 1. Recommendation / smallest useful first milestone

**Recommend a native SwiftUI iOS shell** with in-app animated placeholder avatar, local text chat, editable speech transcription, speech playback, explicit document picker, and provider-neutral model interface. Use synthetic chat/memory fixtures first. Prefer a small proof of architecture over a port of the Android service.

Do **not** attempt Android's draggable **system-wide** `SYSTEM_ALERT_WINDOW` overlay, a general foreground service, permanent background listening, or out-of-app autonomous avatar actions: these are **not normal third-party iOS app capabilities**. No background-audio tricks, accessibility abuse or private APIs as substitutes. An avatar **inside Solphivia's own foreground scene** is appropriate.

## 2. Evidence from existing Android source

| Android component | Source-observed behavior | Proposed iOS mapping / divergence |
| --- | --- | --- |
| `MainActivity.java`, `PetView`, `PetService.java` | In-app avatar and draggable window overlay; bubble, animations, chat composer. Overlay service also sends AI requests. | SwiftUI `View` + `Canvas` / `TimelineView` or sprite animation for **in-app** only. No global always-on-top `PetService` analogue. Use **placeholder** art unless asset owner approves use. |
| `OpenAiChatClient.java` | Current ChatGPT OAuth and Responses API; frozen history, SSE events collected into final callback; assistant-tag parsing controls a constrained avatar. | Retain provider-neutral request/response types (coordinate with Issue #5). iOS URLSession streaming as appropriate; authenticate using platform-approved browser flows and securely store tokens; do not promise parity without integration tests. Maintain a whitelist for avatar actions. |
| `AttachmentReader.java` | Content URI input, 8 MiB/file and 15 MiB aggregate attachment limits; image/file payload mapping. | SwiftUI `fileImporter` / document picker, security-scoped URLs when needed, read/copy bounded content; keep equivalent limits by default and reject unsupported provider modalities. |
| `DeviceSpeech.java` and speech interactions | Android TextToSpeech and speech-recognition interactions; editable dictation before send. | `AVSpeechSynthesizer` and Speech framework with explicit speech/microphone permission prompts; editable transcription, foreground use. Model availability/locale/offline support require testing. |
| `AssistantMemory.java` | Encrypted local 32 KiB UTF-8 memory; Android Keystore AES-GCM; user-editable text. | iOS app-container protected file + CryptoKit authenticated encryption, with encryption key material stored using Keychain. AndroidKeyStore alias/key cannot be copied to iOS. |
| `MemoryConsentActivity.java` | Editable/omit/cancel remote-memory preview, single-use approval, 5-minute expiry. Current UI labels destination OpenAI/ChatGPT. | Present iOS sheet before every send with memory, provider destination and attached-file summary; preserve a **single-use, expiring, request-frozen grant**. No auto memory synchronization. |
| `MemoryActivity.java`, `VaultBackup.java` | Explicit encrypted AI Memory Vault v1 import/export; user selects material. | Preserve external **vault schema and cryptographic envelope** via a documented compatibility fixture; implement separate native import/export. This cannot simply decode Android Keystore internal storage. |

Evidence locations: [MainActivity](../app/src/main/java/it/solphivia/mobile/MainActivity.java), [PetService](../app/src/main/java/it/solphivia/mobile/PetService.java), [OpenAiChatClient](../app/src/main/java/it/solphivia/mobile/OpenAiChatClient.java), [AttachmentReader](../app/src/main/java/it/solphivia/mobile/AttachmentReader.java), [DeviceSpeech](../app/src/main/java/it/solphivia/mobile/DeviceSpeech.java), [AssistantMemory](../app/src/main/java/it/solphivia/mobile/AssistantMemory.java), [MemoryConsentActivity](../app/src/main/java/it/solphivia/mobile/MemoryConsentActivity.java), [VaultBackup](../app/src/main/java/it/solphivia/mobile/VaultBackup.java).

## 3. Technology comparison (pre-implementation evaluation)

| Criterion | Native SwiftUI + Apple frameworks | Flutter | React Native |
| --- | --- | --- | --- |
| Native speech, file coordination, privacy prompts, Keychain | Direct Apple APIs, good fit | Plugins/platform-channel integration needed | Native modules/bridge integration needed |
| Avatar animation in-app | SwiftUI/Canvas or SpriteKit | Flutter animation engine | React Native animation ecosystem/native layer |
| Sharing Android Java implementation | No direct code reuse; share protocols/schema | No direct Java UI reuse | No direct Java UI reuse |
| Maintenance/size | Single-platform design; Swift expertise required | Cross-platform UI possible, extra runtime/deps | Cross-platform UI possible, native integration/deps |
| System-wide Android overlay parity | **No** | **No** | **No** |

**Recommendation:** Native SwiftUI for a focused iOS-specific product; postpone a shared-framework commitment until code/test evidence warrants it. Framework choice cannot bypass iOS system permissions or App Review rules.

## 4. Proposed minimal iOS architecture

```text
SwiftUI SolphiviaApp
  ├── ChatScreen: composer + scrollable transcript + in-app avatar
  ├── VoiceController: Speech recognition / AVSpeechSynthesizer
  ├── FilePicker: permission-scoped user-chosen documents
  ├── ChatCoordinator: immutable request + one terminal response
  │     ├── MemoryApprovalSheet: editable/omit/cancel, per-request expiry
  │     └── ModelProvider: abstract send/stream/cancel + capabilities
  ├── ChatStore: local-only persistence, explicit user deletion
  ├── MemoryVault: CryptoKit + Keychain; opted-in import/export
  └── AvatarStateMachine: validate restricted mood/motion tags
```

The request coordinator should freeze selected destination, model, message/history, memory and attachments before dispatch. Treat cancellation or a provider switch as invalidating approval. Degrade gracefully if Speech permission is denied, using text-only chat. Never send memory or attachments to a different provider without explicit renewed authorization.

### Suggested prototype acceptance checks

1. Launch as an **ordinary foreground app**; render placeholder avatar with idle/happy/look, tap animation, accessibility labels, Reduce Motion support.
2. Compose and locally display text; use synthetic mock-provider responses (no account, secrets or personal data).
3. Start/stop speech recognition after permission prompt; offer editable transcript before submit. Speak a synthetic response with Stop control; text mode always works.
4. Import synthetic Markdown/text via Files picker; show name/size/type and explicit attach/remove. Limit payload size and avoid copying unrelated files.
5. Create a sample encrypted vault, export via share/file picker and re-import; verify wrong passphrase/tampered ciphertext fails closed. Do not claim Android interoperability until *Android-produced* compatibility fixtures pass.
6. On a single synthetic request, display a memory consent sheet; approve modified content, send without, cancel, timeout and provider-change paths. Each request must use fresh consent.
7. Verify VoiceOver focus order, Dynamic Type, Reduce Motion, appropriate permission rationale and deletion flow.

## 5. Memory Vault v1 interoperability — exact items to validate

Source review of `VaultBackup.java` shows `memoria-vault-encrypted` / `schemaVersion:1` envelope with PBKDF2-HMAC-SHA256 **600,000 iterations**, 16-byte salt, RSA OAEP SHA-256 key-wrapping, and AES-GCM payload/private-key envelope with 12-byte IVs. The exported plaintext snapshot uses `memoria-vault` schema v1 and selected memories; Android `AssistantMemory` private storage is distinct. **This is a source-derived format sketch, not independently verified cross-platform compatibility.**

Native Swift implementation needs a precisely specified test-vector protocol: JSON canonical field expectations, Base64 encoding/decoding, RSA OAEP interoperability, PBKDF2 parameters, GCM IV/tag handling, memory expiry and validation limits. CryptoKit alone does **not** supply every operation (notably RSA OAEP and PBKDF2) directly: identify vetted Security/CommonCrypto or other supported primitives and thoroughly test interop. Use synthetic fixtures and no real passwords or vaults. Never change Android export format silently.

## 6. Platform limitations and policy

- **Overlay:** iOS third-party apps cannot present an unrestricted always-on-top avatar over arbitrary other apps using Android's overlay pattern. Restrict to Solphivia itself. Widgets, Live Activities and extensions have constrained, system-owned surfaces; they are **not** a generic overlay substitute.
- **Background:** Ordinary apps may be suspended; permitted background tasks are limited and scheduler-controlled, not persistent interactive agents. Do not propose artificial audio playback to obtain continuous execution or background mic access. Resume gracefully when foregrounded; evaluate explicit supported capabilities only for a real justified use case.
- **Privacy/permissions:** Provide meaningful `NSSpeechRecognitionUsageDescription` and `NSMicrophoneUsageDescription` when required by actual APIs. Request at time of use; handle denial. Document why each attachment, voice input, and external-provider transfer occurs.
- **Keychain:** Store tokens and appropriately protected encryption keys using Keychain Services; keep large content as authenticated-encrypted files with appropriate file protection. Never commit secrets, private user screenshots, vault files or signing material.
- **Accessibility:** VoiceOver labels/hints, keyboard/focus ordering, Dynamic Type, contrast, Reduce Motion; do not use accessibility privileges for screen scraping or cross-app avatar behavior.
- **App Review:** Prepare truthful privacy disclosures, account/data deletion if applicable, safety/reporting controls for AI output, working demo/review access, accurate screenshots and rights to artwork. Approval cannot be promised.
- **TestFlight:** An Apple Developer team and signing/provisioning are needed for distribution; internal/external tester processes and possible external beta review apply. TestFlight and App Store approval remain maintainer-owned decisions.

## 7. Apple primary references (checked October 2026)

- SwiftUI: https://developer.apple.com/documentation/swiftui
- Speech framework: https://developer.apple.com/documentation/speech
- AVSpeechSynthesizer: https://developer.apple.com/documentation/avfaudio/avspeechsynthesizer
- SwiftUI fileImporter: https://developer.apple.com/documentation/swiftui/view/fileimporter(ispresented:allowedcontenttypes:allowsmultipleselection:oncompletion:)
- Keychain Services: https://developer.apple.com/documentation/security/keychain-services
- CryptoKit: https://developer.apple.com/documentation/cryptokit
- Background Tasks: https://developer.apple.com/documentation/backgroundtasks
- Accessibility: https://developer.apple.com/accessibility/
- App Review Guidelines: https://developer.apple.com/app-store/review/guidelines/
- TestFlight Overview: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/

These references establish platform API/policy scope; device and OS-version behavior must still be checked. Some Apple API docs are JS-rendered; exact compatibility and entitlements require verification in Xcode against the target deployment.

## 8. Phased next steps and maintainer questions

**Slice A — agreed MVP specification:** confirm minimum iOS deployment target, iPhone/iPad scope, animation placeholder and mock provider UI; separate iOS feature list from impossible Android overlays.

**Slice B — minimal SwiftUI spike:** build a foreground in-app avatar and text chat with deterministic mock replies and reproducible Xcode setup; report actual toolchain/device/simulator results, not claims of parity.

**Slice C — permission-bound voice & files:** integrate Speech, AVSpeechSynthesizer, fileImporter with cancellation, error UI and synthetic tests.

**Slice D — memory/security interoperability:** Keychain + encrypted local storage and Memory Vault v1 import/export backed by shared synthetic compatibility vectors, explicit user preview and transfer.

**Slice E — provider integration / distribution readiness:** align with Issue #5, test API connectivity only with the user's configuration; prepare review/checklists for accessibility, TestFlight and App Store privacy.

**Questions for @Trikto7e8:** Which iOS deployment minimum, device targets and animation assets (if any) may we use? Is SwiftUI with mock-provider text/placeholder-avatar the approved first prototype? Can we create a shared synthetic `VaultBackup` test vector before cryptographic implementation? Who will run Xcode/device tests and manage TestFlight/App Store signing?

**Deliverable:** This initial *documentation-only* feasibility report for Issue #4; no code changes and no larger implementation promised without follow-up review.
