package it.solphivia.mobile.provider

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val MAX_ATTACHMENT_FILE_BYTES = 8L * 1024L * 1024L
private const val MAX_ATTACHMENT_TOTAL_BYTES = 15L * 1024L * 1024L

data class MemoryConsentGrant(
    val id: String,
    val providerFingerprint: String,
    val attachmentFingerprint: String,
    val approvedText: String,
    val expiresAtMillis: Long
)

sealed interface MemoryConsentResult {
    data class Approved(val text: String) : MemoryConsentResult
    data class Rejected(val error: ProviderError) : MemoryConsentResult
}

/** In-memory, single-use approval that is bound to provider configuration and attachments. */
class SingleUseMemoryConsent @JvmOverloads constructor(
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val grants = ConcurrentHashMap<String, MemoryConsentGrant>()

    fun issue(
        configuration: ProviderConfiguration,
        attachments: List<AttachmentSnapshot>,
        approvedText: String,
        ttlMillis: Long = 300_000L
    ): MemoryConsentGrant {
        require(ttlMillis > 0) { "Consent lifetime must be positive" }
        val grant = MemoryConsentGrant(
            id = UUID.randomUUID().toString(),
            providerFingerprint = configuration.stableFingerprint(),
            attachmentFingerprint = fingerprintAttachments(attachments),
            approvedText = approvedText,
            expiresAtMillis = clockMillis() + ttlMillis
        )
        grants[grant.id] = grant
        return grant
    }

    fun consume(
        grantId: String,
        configuration: ProviderConfiguration,
        attachments: List<AttachmentSnapshot>,
        approvedText: String
    ): MemoryConsentResult {
        val grant = grants.remove(grantId) ?: return MemoryConsentResult.Rejected(
            ProviderError(ProviderErrorCategory.CONFIGURATION, "Il consenso memoria è già stato usato o annullato.")
        )
        if (clockMillis() >= grant.expiresAtMillis) return MemoryConsentResult.Rejected(
            ProviderError(ProviderErrorCategory.CONFIGURATION, "Il consenso memoria è scaduto.")
        )
        if (grant.providerFingerprint != configuration.stableFingerprint() ||
            grant.attachmentFingerprint != fingerprintAttachments(attachments)
        ) {
            return MemoryConsentResult.Rejected(
                ProviderError(ProviderErrorCategory.CONFIGURATION, "La destinazione o gli allegati sono cambiati: approva di nuovo questa richiesta.")
            )
        }
        return MemoryConsentResult.Approved(approvedText)
    }

    fun cancel(grantId: String): Boolean = grants.remove(grantId) != null

    fun pendingCount(): Int = grants.size

    private fun fingerprintAttachments(attachments: List<AttachmentSnapshot>): String =
        sha256(attachments.joinToString("|") { "${it.sourceUri}|${it.fileName}|${it.mimeType}|${it.sizeBytes}" })

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

object RequestSnapshotFactory {
    @JvmStatic
    fun freeze(
        messages: List<ConversationMessage>,
        mode: RequestMode,
        configuration: ProviderConfiguration,
        approvedMemory: String = "",
        memoryConsentId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): FrozenChatRequest {
        require(messages.isNotEmpty()) { "A chat request needs at least one message" }
        if (mode == RequestMode.VOICE) require(approvedMemory.isEmpty()) {
            "Voice requests cannot include vault memory"
        }
        val recent = messages.takeLast(16)
        val frozen = if (mode == RequestMode.VOICE) {
            recent.map { it.copy(attachments = emptyList()) }
        } else {
            recent.map { it.copy(attachments = it.attachments.toList()) }
        }
        return FrozenChatRequest(
            configuration = configuration,
            mode = mode,
            messages = frozen,
            approvedMemory = if (mode == RequestMode.VOICE) "" else approvedMemory,
            memoryConsentId = memoryConsentId,
            createdAtMillis = nowMillis
        )
    }
}

object ProviderRequestFactory {
    /** Converts the legacy JSON history to a fully materialized immutable request snapshot. */
    @JvmStatic
    fun freezeLegacy(
        history: JSONArray,
        currentAttachments: JSONArray,
        mode: RequestMode,
        configuration: ProviderConfiguration,
        approvedMemory: String,
        nowMillis: Long = System.currentTimeMillis(),
        memoryConsentId: String? = null
    ): FrozenChatRequest {
        val copy = JSONArray(history.toString())
        val messages = ArrayList<ConversationMessage>()
        val aggregateBytes = longArrayOf(0L)
        val start = maxOf(0, copy.length() - 16)
        for (index in start until copy.length()) {
            val line = copy.optJSONObject(index) ?: continue
            val role = line.optString("role")
            if (role != "user" && role != "assistant") continue
            val attachments = if (mode == RequestMode.VOICE) {
                emptyList()
            } else {
                val source = if (index == copy.length() - 1 && currentAttachments.length() > 0) {
                    currentAttachments
                } else {
                    line.optJSONArray("attachments") ?: JSONArray()
                }
                parseAttachments(source, aggregateBytes)
            }
            messages += ConversationMessage(role, line.optString("content"), attachments)
        }
        if (messages.isEmpty()) throw IllegalArgumentException("A chat request needs at least one message")
        if (mode == RequestMode.VOICE && approvedMemory.isNotEmpty()) {
            throw IllegalArgumentException("Voice requests cannot include vault memory")
        }
        return FrozenChatRequest(
            requestId = UUID.randomUUID().toString(),
            configuration = configuration,
            mode = mode,
            messages = messages,
            approvedMemory = if (mode == RequestMode.VOICE) "" else approvedMemory,
            memoryConsentId = memoryConsentId,
            createdAtMillis = nowMillis
        )
    }

    @JvmStatic
    fun materializedAttachments(history: JSONArray): List<AttachmentSnapshot> {
        val result = ArrayList<AttachmentSnapshot>()
        val aggregateBytes = longArrayOf(0L)
        val start = maxOf(0, history.length() - 16)
        for (index in start until history.length()) {
            val line = history.optJSONObject(index) ?: continue
            val source = line.optJSONArray("attachments") ?: continue
            result += parseAttachments(source, aggregateBytes)
        }
        return result
    }

    private fun parseAttachments(source: JSONArray, aggregateBytes: LongArray): List<AttachmentSnapshot> {
        val result = ArrayList<AttachmentSnapshot>()
        for (index in 0 until source.length()) {
            val item = source.optJSONObject(index)
                ?: throw IllegalArgumentException("Allegato storico non valido.")
            val name = item.optString("filename", "allegato")
            val mime = item.optString("mime_type", "application/octet-stream")
            val dataUri = item.optString("data_uri", "").ifEmpty { null }
            if (dataUri == null) {
                throw IllegalArgumentException("Allegato non più accessibile: $name")
            }
            val size = item.optLong("size", -1L)
            require(size in 1L..MAX_ATTACHMENT_FILE_BYTES) {
                "Allegato oltre 8 MB o vuoto: $name"
            }
            require(dataUri.startsWith("data:") && dataUri.contains(";base64,")) {
                "Contenuto allegato non valido: $name"
            }
            val encoded = dataUri.substringAfter(";base64,")
            val padding = encoded.takeLastWhile { it == '=' }.length
            val decodedSize = (encoded.length * 3L / 4L) - padding
            require(decodedSize == size) { "Dimensione allegato non verificabile: $name" }
            aggregateBytes[0] += size
            require(aggregateBytes[0] <= MAX_ATTACHMENT_TOTAL_BYTES) {
                "Allegati della conversazione oltre 15 MB. Inizia una nuova chat."
            }
            result += AttachmentSnapshot(
                sourceUri = item.optString("uri", "").ifEmpty { null },
                fileName = name,
                mimeType = mime,
                sizeBytes = size,
                dataUri = dataUri
            )
        }
        return result
    }
}
