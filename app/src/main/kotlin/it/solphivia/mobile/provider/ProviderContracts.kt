package it.solphivia.mobile.provider

import java.net.URI
import java.util.UUID
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

enum class ProviderId {
    OPENAI_CHATGPT,
    OPENAI_COMPATIBLE,
    OLLAMA,
    LM_STUDIO,
    CUSTOM
}

data class ProviderIdentity(
    val id: ProviderId,
    val displayName: String
) {
    init {
        require(displayName.isNotBlank()) { "Provider display name must not be blank" }
    }
}

enum class RequestMode { TYPED, VOICE }

enum class AttachmentKind { IMAGE, DOCUMENT, OTHER }

internal const val MAX_APPLICATION_ATTACHMENT_BYTES = 8L * 1024L * 1024L
internal const val MAX_APPLICATION_TOTAL_ATTACHMENT_BYTES = 15L * 1024L * 1024L

internal fun validateApplicationAttachments(messages: List<ConversationMessage>) {
    var total = 0L
    for (message in messages) {
        for (attachment in message.attachments) {
            require(attachment.sizeBytes in 1L..MAX_APPLICATION_ATTACHMENT_BYTES) {
                "Attachment is empty or exceeds the 8 MB application limit"
            }
            require(attachment.dataUri?.startsWith("data:") == true) {
                "Attachment content must be materialized before dispatch"
            }
            total += attachment.sizeBytes
            require(total <= MAX_APPLICATION_TOTAL_ATTACHMENT_BYTES) {
                "Attachments exceed the 15 MB application limit"
            }
        }
    }
}

data class ProviderCapabilities(
    val text: Boolean = true,
    val streaming: Boolean = false,
    val cancellation: Boolean = false,
    val images: Boolean = false,
    val documents: Boolean = false,
    val tools: Boolean = false,
    val maxAttachmentBytes: Long = 0L
) {
    fun supports(kind: AttachmentKind): Boolean = when (kind) {
        AttachmentKind.IMAGE -> images
        AttachmentKind.DOCUMENT -> documents
        AttachmentKind.OTHER -> false
    }

    fun validate(request: FrozenChatRequest): ProviderError? {
        if (!text) {
            return ProviderError(
                ProviderErrorCategory.UNSUPPORTED_CAPABILITY,
                "Il provider selezionato non supporta richieste testuali."
            )
        }
        for (message in request.messages) {
            for (attachment in message.attachments) {
                if (!supports(attachment.kind)) {
                    return ProviderError(
                        ProviderErrorCategory.UNSUPPORTED_CAPABILITY,
                        "Il provider selezionato non supporta allegati ${attachment.mimeType}."
                    )
                }
                if (maxAttachmentBytes > 0L && attachment.sizeBytes > maxAttachmentBytes) {
                    return ProviderError(
                        ProviderErrorCategory.PAYLOAD_TOO_LARGE,
                        "L’allegato ${attachment.fileName} supera il limite del provider."
                    )
                }
            }
        }
        return null
    }
}

data class ProviderConfiguration(
    val identity: ProviderIdentity,
    val endpoint: String,
    val model: String,
    val credentialAlias: String? = null,
    val connectTimeoutMillis: Long = 25_000L,
    val readTimeoutMillis: Long = 120_000L
) {
    init {
        val uri = runCatching { URI(endpoint) }.getOrElse {
            throw IllegalArgumentException("Provider endpoint is invalid")
        }
        require(uri.scheme == "http" || uri.scheme == "https") {
            "Provider endpoint must use HTTP or HTTPS"
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Provider endpoint must not contain credentials, query parameters or fragments"
        }
        require(model.isNotBlank()) { "Provider model must not be blank" }
        require(connectTimeoutMillis > 0 && readTimeoutMillis > 0) {
            "Provider timeouts must be positive"
        }
    }

    fun redactedEndpoint(): String = runCatching {
        val uri = URI(endpoint)
        buildString {
            append(uri.scheme).append("://").append(uri.host)
            if (uri.port >= 0) append(':').append(uri.port)
        }
    }.getOrDefault("[invalid endpoint]")

    fun stableFingerprint(): String = listOf(
        identity.id.name,
        endpoint,
        model,
        credentialAlias.orEmpty(),
        connectTimeoutMillis,
        readTimeoutMillis
    ).joinToString("|")

    override fun toString(): String = "ProviderConfiguration(provider=${identity.id}, endpoint=${redactedEndpoint()}, model=$model, credentialAlias=${credentialAlias?.let { "[configured]" } ?: "[none]"})"
}

object ProviderConfigurations {
    @JvmStatic
    fun chatGpt(model: String): ProviderConfiguration = ProviderConfiguration(
        identity = ProviderIdentity(ProviderId.OPENAI_CHATGPT, "OpenAI / ChatGPT"),
        endpoint = "https://api.openai.com/v1/responses",
        model = model,
        credentialAlias = "chatgpt-oauth"
    )
}

data class AttachmentSnapshot(
    val sourceUri: String?,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dataUri: String?
) {
    val kind: AttachmentKind
        get() = when {
            mimeType.lowercase().startsWith("image/") -> AttachmentKind.IMAGE
            mimeType.lowercase() in setOf(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "text/markdown",
                "text/csv"
            ) -> AttachmentKind.DOCUMENT
            else -> AttachmentKind.OTHER
        }

    init {
        require(fileName.isNotBlank()) { "Attachment filename must not be blank" }
        require(mimeType.isNotBlank()) { "Attachment MIME type must not be blank" }
        require(sizeBytes >= 0L) { "Attachment size must not be negative" }
        if (dataUri != null) require(dataUri.startsWith("data:")) {
            "Materialized attachment content must be a data URI"
        }
    }

    override fun toString(): String = "AttachmentSnapshot(fileName=$fileName, mimeType=$mimeType, sizeBytes=$sizeBytes, materialized=${dataUri != null})"
}

data class ConversationMessage(
    val role: String,
    val content: String,
    val attachments: List<AttachmentSnapshot> = emptyList()
) {
    init {
        require(role == "user" || role == "assistant") { "Unsupported conversation role" }
    }
}

class FrozenChatRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val configuration: ProviderConfiguration,
    val mode: RequestMode,
    messages: List<ConversationMessage>,
    val approvedMemory: String = "",
    val memoryConsentId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val messages: List<ConversationMessage> = Collections.unmodifiableList(messages.map { message ->
        message.copy(attachments = Collections.unmodifiableList(message.attachments.map { it.copy() }))
    })

    init {
        require(requestId.isNotBlank()) { "Request id must not be blank" }
        require(this.messages.isNotEmpty()) { "A chat request needs at least one message" }
        if (mode == RequestMode.VOICE) {
            require(approvedMemory.isEmpty()) { "Voice requests cannot include vault memory" }
            require(this.messages.all { it.attachments.isEmpty() }) {
                "Voice requests cannot include attachment payloads or references"
            }
        }
        for (message in this.messages) {
            for (attachment in message.attachments) {
                require(attachment.dataUri != null) {
                    "Provider requests must contain materialized attachment content"
                }
            }
        }
        validateApplicationAttachments(this.messages)
    }

    fun textHistory(): List<ConversationMessage> = messages.map {
        it.copy(attachments = emptyList())
    }

    override fun toString(): String = "FrozenChatRequest(requestId=$requestId, provider=${configuration.identity.id}, endpoint=${configuration.redactedEndpoint()}, model=${configuration.model}, mode=$mode, messages=${messages.size}, approvedMemory=${approvedMemory.isNotEmpty()})"
}

enum class ProviderErrorCategory {
    AUTH_REQUIRED,
    UNREACHABLE,
    TIMEOUT,
    RATE_LIMITED,
    UNSUPPORTED_CAPABILITY,
    PAYLOAD_TOO_LARGE,
    MALFORMED_RESPONSE,
    CANCELLED,
    CONFIGURATION,
    PROVIDER_FAILURE
}

data class ProviderError(
    val category: ProviderErrorCategory,
    val message: String,
    val retryable: Boolean = false
) {
    init { require(message.isNotBlank()) }

    override fun toString(): String = "${category.name}: $message"

    companion object {
        @JvmStatic
        fun fromThrowable(error: Throwable): ProviderError {
            val message = safeMessage(error)
            val lower = message.lowercase()
            val category = when (error) {
                is java.util.concurrent.CancellationException -> ProviderErrorCategory.CANCELLED
                is java.net.SocketTimeoutException -> ProviderErrorCategory.TIMEOUT
                is java.net.ConnectException,
                is java.net.UnknownHostException -> ProviderErrorCategory.UNREACHABLE
                else -> when {
                    lower.contains("http 401") || lower.contains("accesso scaduto") || lower.contains("autentic") -> ProviderErrorCategory.AUTH_REQUIRED
                    lower.contains("http 429") || lower.contains("rate limit") -> ProviderErrorCategory.RATE_LIMITED
                    lower.contains("oltre 15 mb") || lower.contains("payload too large") -> ProviderErrorCategory.PAYLOAD_TOO_LARGE
                    else -> ProviderErrorCategory.PROVIDER_FAILURE
                }
            }
            return ProviderError(category, message, category == ProviderErrorCategory.TIMEOUT)
        }

        private fun safeMessage(error: Throwable): String {
            val raw = error.message?.trim().orEmpty()
            if (raw.isEmpty()) return "Errore imprevisto del provider."
            return raw
                .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._~-]+"), "Bearer [redacted]")
                .replace(Regex("(?i)(api[-_ ]?key|token|secret)=?\\s*[^\\s,;]+"), "$1=[redacted]")
                .take(500)
        }
    }
}

data class ProviderResponse(
    val text: String,
    val animation: String = "idle",
    val motion: String = "stay",
    val mode: String = ""
)

interface GenerationListener {
    fun onDelta(text: String)
    fun onCompleted(response: ProviderResponse)
    fun onError(error: ProviderError)
}

interface RequestCancellation {
    val isCancelled: Boolean
    val isTerminated: Boolean
    fun cancel(): Boolean
    fun onCancel(action: () -> Unit)
}

interface ModelProvider {
    val identity: ProviderIdentity
    fun capabilities(configuration: ProviderConfiguration): ProviderCapabilities
    fun generate(request: FrozenChatRequest, listener: GenerationListener): RequestCancellation
}

interface OpenAiStreamListener {
    fun onDelta(text: String)
    fun onCompleted(fullText: String)
    fun onError(error: ProviderError)
}

class RequestLifecycle(private val listener: GenerationListener) : RequestCancellation, OpenAiStreamListener {
    private val stateLock = Any()
    private val terminal = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private val cancelActions = CopyOnWriteArrayList<() -> Unit>()

    override val isCancelled: Boolean
        get() = cancelled.get()

    override val isTerminated: Boolean
        get() = terminal.get()

    override fun cancel(): Boolean {
        val actions: List<() -> Unit>
        synchronized(stateLock) {
            if (!terminal.compareAndSet(false, true)) return false
            cancelled.set(true)
            actions = cancelActions.toList()
            cancelActions.clear()
        }
        actions.forEach { action -> runCatching { action() } }
        listener.onError(ProviderError(ProviderErrorCategory.CANCELLED, "Richiesta annullata."))
        return true
    }

    override fun onCancel(action: () -> Unit) {
        var runNow = false
        synchronized(stateLock) {
            if (!terminal.get()) {
                cancelActions.add(action)
            } else if (cancelled.get()) {
                runNow = true
            }
        }
        if (runNow) runCatching { action() }
    }

    override fun onDelta(text: String) {
        if (text.isEmpty()) return
        synchronized(stateLock) {
            if (!terminal.get()) listener.onDelta(text)
        }
    }

    override fun onCompleted(fullText: String) {
        synchronized(stateLock) {
            if (!terminal.compareAndSet(false, true)) return
            cancelActions.clear()
            listener.onCompleted(SolphiviaResponseParser.parse(fullText))
        }
    }

    override fun onError(error: ProviderError) {
        synchronized(stateLock) {
            if (!terminal.compareAndSet(false, true)) return
            cancelActions.clear()
            listener.onError(error)
        }
    }

    fun fail(error: ProviderError) = onError(error)
}
