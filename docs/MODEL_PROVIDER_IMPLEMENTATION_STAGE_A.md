# Model-provider architecture — Stage A implementation

**Status:** implementation candidate for maintainer review; Ollama, LM Studio and provider settings are intentionally not included.

This slice turns the Issue #5 design proposal into a dependency-light Kotlin boundary while preserving the existing Java callers and ChatGPT OAuth path.

## Implemented boundary

- `ProviderIdentity`, `ProviderConfiguration`, `ProviderCapabilities` and typed `ProviderError` categories.
- Immutable `FrozenChatRequest` snapshots containing the selected provider configuration, model, request mode, recent history, materialized attachments and approved memory text.
- `RequestSnapshotFactory` and a legacy JSON bridge for the existing `JSONArray` history format.
- `ModelProvider`, streaming listeners, cancellation handles and `RequestLifecycle` with one terminal outcome.
- `SingleUseMemoryConsent`, which binds approval to provider configuration and attachment metadata, expires after five minutes by default and cannot be consumed twice.
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

The suite intentionally uses synthetic prompts, files and responses only.

## Deliberate follow-up gates

- No provider settings screen or persisted local-provider configuration is exposed yet.
- No Ollama native adapter is started before maintainer review of this Stage A implementation.
- No LM Studio adapter is included; it remains a separate contribution.
- The current ChatGPT transport remains pinned to the existing OpenAI OAuth/Responses endpoints; configurable endpoint selection belongs to the provider-settings/local-adapter stage and must not be inferred from this adapter.
- Android instrumentation and a real authenticated ChatGPT request still require a device/account and are not replaced by the JVM fixtures.
