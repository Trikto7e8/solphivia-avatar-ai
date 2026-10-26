# Model-provider architecture — Stage A implementation

**Aggiornamento 1.0 beta 1:** la candidata corrente è descritta in [CURRENT_RELEASE](../../docs/CURRENT_RELEASE.md). I riferimenti alpha11/alpha2 e le relative misure qui sotto sono snapshot storici, non esiti attribuiti alla beta. Ogni nuova implementazione deve partire dalla candidata corrente.
**Status:** merged in PR #16 and released in alpha11 (44); [current release and evidence](../../docs/CURRENT_RELEASE.md). Ollama, LM Studio and provider settings are intentionally not included.

This slice turns the Issue #5 design proposal into a dependency-light Kotlin boundary while preserving the existing Java callers and ChatGPT OAuth path.

## Implemented boundary

- `ProviderIdentity`, `ProviderConfiguration`, `ProviderCapabilities` and typed `ProviderError` categories.
- Immutable `FrozenChatRequest` snapshots containing the selected provider configuration, model, request mode, recent history, materialized attachments and approved memory text.
- `RequestSnapshotFactory` and a legacy JSON bridge for the existing `JSONArray` history format.
- `ModelProvider`, streaming listeners, cancellation handles and `RequestLifecycle` with one terminal outcome.
- `SingleUseMemoryConsent`, which binds approval to provider configuration, canonical attachment metadata and SHA-256 digests of materialized attachment bytes, expires after five minutes by default and cannot be consumed twice.
- `OpenAiCompatibilityProvider`, an injectable OpenAI-compatible streaming boundary used by the existing `OpenAiChatClient` and JVM mock-server tests.
- `StreamingTagFilter` and `SolphiviaResponseParser`, which allow-list avatar controls and prevent complete, split or malformed controls from reaching incremental display.

## Existing app compatibility

- `MainActivity` and `PetService` continue to call the existing `OpenAiChatClient.respond(...)` and `ChatCallback` signatures.
- The OpenAI Responses request still uses `store:false`, `stream:true`, the existing system instructions, OAuth token storage and current attachment mapping.
- Typed requests freeze configuration and materialize the bounded 16-message request window before memory approval; the consent destination is shown as `OpenAI / ChatGPT`.
- Voice requests retain recent text history but remove memory and all attachment references/payloads.
- Pending consent and active provider work are cancelled when the activity or overlay service is destroyed.
- Historical attachments are revalidated through the existing `AttachmentReader`; inaccessible files fail explicitly instead of being discarded.

## Verification

The JVM suite contains deterministic tests for:

- typed versus voice snapshots;
- provider/model configuration freezing;
- unsupported attachment capabilities;
- missing and oversized attachment rejection;
- edited, omitted, cancelled, expired and reused memory consent;
- split, lowercase, malformed and terminal avatar controls;
- partial streams, provider failures, cancellation before connection, cancellation during streaming, stale callbacks and duplicate terminal events;
- a local `ServerSocket` SSE fixture exercising the OpenAI-compatible response boundary.

The suite intentionally uses synthetic prompts, files and responses only. The final alpha11 includes 38 JVM tests and four passing isolated Android suites. The maintainer also reports working transcript preview and continued conversation on the final app; this is separate manual evidence, not a JVM result. See [release notes](../releases/v0.9.2-alpha11.md) for exact scope, 0 lint errors / 118 warnings and outstanding limits.

## Deliberate follow-up gates

- No provider settings screen or persisted local-provider configuration is exposed yet.
- No Ollama native adapter is included; it remains a separately scoped follow-up after the completed Stage A review.
- No LM Studio adapter is included; it remains a separate contribution.
- The current ChatGPT transport remains pinned to the existing OpenAI OAuth/Responses endpoints; configurable endpoint selection belongs to the provider-settings/local-adapter stage and must not be inferred from this adapter.
- Android instrumentation has been run on an isolated Moto g35 5G installation and the user confirmed the final alpha11 conversation flow. JVM fixtures still do not independently certify account eligibility, every authenticated transport path or long-running/background/network recovery.
