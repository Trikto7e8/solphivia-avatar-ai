# Model-provider boundary proposal — Issue #5

**Status:** design proposal only; no adapter implemented, no Android build/device test run.
**Scope:** original Android alpha2 source review; API contracts proposed for maintainer review.
**Provenance:** Prepared by @sunny-kavixo with ChatGPT (GPT-6) assisting source inspection and drafting. Source inspection via GitHub; architectural recommendations are proposals, not verified functionality.

## Maintainer update — 22 October 2026, alpha9

This is a design document, not a working local-model feature. The original inspection describes the earlier alpha2 request path. Alpha9 is the current runtime baseline: [release notes](releases/v0.9.2-alpha9.md), [voice behavior](VOICE-SESSION.md).

Voice requests already send automatically with recent text history, **no added vault memory and no attachment payloads or historical file references**. Typed requests retain editable, single-use memory approval. An adapter must preserve both paths without adding a manual memory screen to every voice command. Pending typed attachments stay selected for a separate typed send. Local app commands are handled before provider dispatch and never execute model output.

Agreed first implementation: dependency-light contracts and mock/state tests, followed by an OpenAI compatibility wrapper. Keep `ChatCallback` as a shim and settings in a separate screen. Ollama native chat is the first planned text-only local adapter **after** that slice is reviewed; LM Studio follows separately. These are approved directions for focused PRs, not implemented capabilities.

## 1. Original behavior and coupling (historical source inspection)

| Component | Verified source behavior | Design implication |
| --- | --- | --- |
| `MainActivity.java` | Composes chat turns; saves shared-preference `chat/history`; handles inline attachments with `AttachmentReader`; invokes `OpenAiChatClient.respond(...)`. | Keep UI/history/animation separate from provider transport. |
| `PetService.java` | Overlay independently invokes the same `OpenAiChatClient.respond(...)` with selected attachments. | One provider-facing orchestration entrypoint must cover both callers. |
| `OpenAiChatClient.java` | Combines ChatGPT OAuth token management, request construction, OpenAI Responses API HTTP/SSE, assistant-text parsing, and `ChatCallback` delivery. POST `/v1/responses` with `store:false`, `stream:true`; handles delta/completed/failed/error events but buffers text before one final callback. | Extract protocol/auth behind adapter; distinguish HTTP streaming transport from incremental UI delivery. |
| `OpenAiChatClient.buildInput` | Takes last 16 history items; re-materializes prior attachment URI references; sends image attachments as `input_image`, other files as `input_file`. | Do not assume an Ollama/LM Studio endpoint supports these OpenAI-specific formats. |
| `AttachmentReader.java` | Reads Android document URIs into in-memory data URIs (base64); limits each file to 8 MiB and aggregate conversation attachments to 15 MiB. UI selection cap is up to five pending URIs. | Validate before any provider request; capability-gate MIME, size, URI access and modality, including historic references. |
| `MemoryConsentActivity.java` | Freezes preview/memory and offers edited, omitted, or cancelled memory; grant expires after five minutes, removed on consume. Text currently says destination OpenAI/ChatGPT. | Consent must name **selected destination** and remain per-request, even when changing provider. |
| `AssistantMemory.java` | Local memory encrypted with Android Keystore AES-GCM; 32 KiB max; explicit `contextForModel(approvedText)` function. | Keep local memory provider-independent; do not give adapters direct memory-file access. |

**Inspection boundary:** static source review only; provider API compatibility, runtime cancellation, persisted URI permissions, OAuth eligibility, and build behavior were **not** exercised.

## 2. Smallest proposed interface

Prefer one provider-neutral request/response contract plus two adapters; preserve existing OpenAI path as baseline before introducing local providers.

```java
interface ModelProvider {
    ProviderCapabilities capabilities(ProviderConfig config);
    void generate(FrozenChatRequest request, GenerationListener listener,
                  CancellationToken cancellation);
}
interface GenerationListener {
    void onDelta(String text);            // optional incremental display
    void onCompleted(String fullText);    // exactly once if successful
    void onError(ProviderError error);    // exactly once if failed
}
record ProviderCapabilities(
    boolean text, boolean images, boolean documents,
    boolean tools, boolean streaming, long maxAttachmentBytes
) {}
record FrozenChatRequest(
    String requestId, ProviderSnapshot destination, InputPolicy inputPolicy,
    String systemInstructions,
    java.util.List<ChatTurn> turns, java.util.List<AttachmentRef> attachments,
    String approvedMemory // approved by user for THIS request only
) {}
```

The proposed `ProviderSnapshot` must freeze provider identifier, validated endpoint, selected model, configuration revision and an opaque credential reference (never credential values in logs or DTO debug output). `InputPolicy` must distinguish typed/reviewed requests from voice/text-only requests.

Pseudocode above illustrates the boundary; Android minimum runtime, Java syntax compatibility, class placement, DTOs and lifecycle ownership require implementation review. **Do not pass mutable activity/URI state across asynchronous dispatch**; resolve and validate content under an explicit bounded policy. Keep provider capabilities as conservative configured claims; server-advertised capabilities may be incomplete.

Suggested pipeline:

1. UI/overlay freezes input + selected provider identifier and config (before memory approval).
2. Prepare a redacted preview listing text and attachment names, expected destination and whether local traffic can leave the device/LAN.
3. For a typed request including vault memory, `MemoryConsentActivity` obtains one expiring approval for edited/omitted memory or aborts the **entire** send. Voice requests instead freeze empty approved memory and no file payload/references; speech is the explicit send action under the already selected destination. Do not silently change provider or attach pending typed files.
4. Validate provider capabilities, URI permissions, allowed MIME/byte limits and configured endpoint **before transport**. Unsupported features produce explicit actionable errors; never discard attachments silently.
5. Adapter performs one request with explicit cancellation handle and bounded connect/read timeouts. Stream deltas through listener if feasible; emit one terminal completion/error, ignoring stale callbacks after cancel or Activity/Service destruction.
6. Shared response parser validates Solphivia movement/mood/mode tags against allowed values; defaults safely when missing. Providers return text only; no platform controls, arbitrary tool execution, or UI side-effects.
7. Persist only the intended history, after successful response, according to existing semantics. Avoid serializing approved memory or auth tokens into chat logs.

## 3. Provider-specific mapping

| Provider | Transport/auth | Supported initial scope | Do not silently support |
| --- | --- | --- | --- |
| Current ChatGPT adapter | Preserve existing OAuth refresh and OpenAI Responses API mapping; do not route through personal API keys automatically. | Text, existing supported images/files; maintain existing storage/consent semantics. | Do not imply other ChatGPT app history/memory access, or treat ChatGPT plan as ordinary API billing. |
| Ollama | Explicit configurable `http(s)://<host>:11434` reachable from phone; adapter to native Ollama chat API or explicitly selected compatible API; validate selected model. | **Text-only milestone**; optional streaming after tests. | Images, documents, tools, embeddings are unsupported in milestone 1 unless capabilities and serialization are explicitly tested. |
| LM Studio | Explicit configured reachable computer-hosted server, often OpenAI-compatible endpoint; document exact selected endpoint/path and model. | **Text-only milestone**; streaming only after parser tests. | Assume no parity with OpenAI Responses API; do not send `input_file` or `input_image` to a chat-completions endpoint without mapped, tested support. |
| Configurable cloud API | Strictly selected HTTPS provider and known API protocol adapter; user-owned secrets in secure storage, never Git. | Text-only initially if adapter is explicitly implemented. | Arbitrary REST URL auto-detection, invisible fallback, or logging request bodies/tokens. |

**Network boundaries:** On Android, `localhost` and `127.0.0.1` refer to the **phone**, not the Mac running Ollama/LM Studio. User must configure a reachable LAN host or approved secure tunnel. Do not silently expose local server to the internet; explicit warning before cleartext HTTP to a LAN server, avoid sending sensitive memories over insecure networks, and verify Android cleartext network policy before implementation. Reject the assumption that phone loopback reaches a computer. Loopback is valid for an explicitly configured server actually running on the Android device. Reject accidental cloud fallback and unsupported protocols with useful errors.

**Authentication and credentials:** preserve existing OAuth logic for ChatGPT; separate API-key credentials per provider; never include secrets in DTO debug logs, export, or intent extras. Provider switching never reuses another provider's credentials.

## 4. Failure/cancellation semantics

- Define one terminal outcome per request; avoid double callbacks after network failure, token refresh, cancellation, or destroyed UI.
- Explicit errors: `AUTH_REQUIRED`, `UNREACHABLE`, `TIMEOUT`, `RATE_LIMITED`, `UNSUPPORTED_CAPABILITY`, `PAYLOAD_TOO_LARGE`, `MALFORMED_RESPONSE`, `CANCELLED`.
- Provider's partial streamed text is **not** success. An incomplete stream must never persist a completed assistant response.
- Cancel button and lifecycle disposal should cancel connection/read and suppress stale UI callbacks; serial executor cannot by itself cancel a blocking request.
- No fallback to OpenAI when local server or selected API fails. Ask user before any explicit provider switch.
- Unknown model capability must fail closed for attachments/tools; no automatic upload of personal memory or files to another destination.

## 5. Memory and attachments must remain user-directed

The existing `MemoryConsentActivity` shows OpenAI/ChatGPT as the destination. Generalize its UI to show the **frozen selected provider and endpoint label**. Approval must be single-use and 5-minute expiry, with a safe default of no memory. A provider switch or changed attachment set invalidates the pending approval; offer explicit cancellation and retry. Local providers are not exempt from permission and privacy controls.

The existing 8 MiB/file and 15 MiB aggregate limits should initially remain unchanged even when a provider accepts larger payloads; size needs to account for base64 expansion and any provider-specific request limits. For typed requests, reuse stored URI references only when accessible; if unavailable, report the attachment as missing instead of silently omitting. Voice requests intentionally exclude file payloads/references as described above; do not erase those references from persisted history. Capability checks must apply to past attachments reloaded from history.

## 6. Agreed sequence for separate, reviewable implementation PRs

1. **Contract + tests:** dependency-light request DTOs, capability errors, cancellation/terminal state tests; no behavior change to current OpenAI path.
2. **OpenAI adapter migration:** wrap existing `OpenAiChatClient` transport and consent flow with parity checks for regular and overlay chat, history, image/file requests, invalid OAuth, streaming completion.
3. **Settings + explicit selection:** selected provider/model, endpoint validation, distinct credential stores, clear warning for LAN/HTTP; preserve chat with deliberate migration policy.
4. **Local text adapter:** add **either** Ollama **or** LM Studio first, selected by maintainer after protocol discussion; synthetic-only smoke tests on real phone ↔ computer network.
5. **Opt-in extended capabilities:** images/docs/streamed UI only if adapter + model supports and has device integration tests.

No Android code, manifest, Gradle, secrets, provider routing or credentials are changed by this documentation PR.

## 7. Verification checklist and risks

- [x] Static inspect: `OpenAiChatClient`, `MainActivity`, `PetService`, `AttachmentReader`, `MemoryConsentActivity`, `AssistantMemory`, manifest and README.
- [ ] Compile/build/lint and regression tests on JDK 17 / Android SDK — **not run**.
- [ ] Confirm behavior for 16-turn history trimming and old inaccessible URI attachments.
- [ ] Verify ChatGPT OAuth still authorizes and `store:false` is maintained.
- [ ] Verify synthetic image/file request parity and limits against eligible server.
- [ ] Verify cancellation, partial SSE response, HTTP failure, retry, and stale callback handling.
- [ ] Verify Ollama/LM Studio phone-to-host reachability and cleartext policy on actual Android device.
- [ ] Verify no cross-provider memory/secret leakage, no unauthorized fallback, and required per-request consent.

## 8. Maintainer review questions

1. Should first implementation target **Ollama native chat** or a pinned **LM Studio OpenAI-compatible** API? I recommend supporting exactly one text-only local adapter initially.
2. Is preserving the existing `ChatCallback` as compatibility shim appropriate while introducing `GenerationListener`?
3. Should provider settings initially reside in a separate screen, avoiding `MainActivity` / `PetService` UI churn?
4. Should historical attachments be capability-checked on every send (recommended), or should switching providers initiate a fresh conversation by default?
5. Can we agree on an Android device test matrix and mock-server fixture before the first implementation is merged?

**Scope requested:** review this document as an **Issue #5 design proposal**, then agree on one small adapter implementation. No runtime claims or model capability claims are made.

## Official protocol reference

[Ollama native chat API](https://docs.ollama.com/api/chat): use a pinned, tested `/api/chat` mapping for the first local text adapter. The current Android build has no such adapter.
