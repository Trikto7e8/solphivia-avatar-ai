package it.solphivia.mobile.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ProviderPolicyTest {
    private val config = ProviderConfigurations.chatGpt("mock-model")
    private val image = AttachmentSnapshot(
        sourceUri = "content://synthetic/image",
        fileName = "synthetic.png",
        mimeType = "image/png",
        sizeBytes = 1,
        dataUri = "data:image/png;base64,AA=="
    )

    @Test
    fun voiceKeepsRecentTextHistoryButDropsMemoryAndAttachments() {
        val messages = (0 until 17).map { index ->
            ConversationMessage(
                role = if (index % 2 == 0) "user" else "assistant",
                content = "synthetic-$index",
                attachments = if (index == 16) listOf(image) else emptyList()
            )
        }

        val request = RequestSnapshotFactory.freeze(messages, RequestMode.VOICE, config)

        assertEquals(16, request.messages.size)
        assertEquals("synthetic-1", request.messages.first().content)
        assertTrue(request.messages.all { it.attachments.isEmpty() })
        assertEquals("", request.approvedMemory)
        assertEquals("mock-model", request.configuration.model)
    }

    @Test
    fun typedRequestPreservesApprovedMemoryAndMaterializedAttachments() {
        val messages = listOf(ConversationMessage("user", "read this", listOf(image)))

        val request = RequestSnapshotFactory.freeze(
            messages = messages,
            mode = RequestMode.TYPED,
            configuration = config,
            approvedMemory = "synthetic memory",
            memoryConsentId = "grant-1"
        )

        assertEquals("synthetic memory", request.approvedMemory)
        assertEquals("grant-1", request.memoryConsentId)
        assertEquals(1, request.messages.single().attachments.size)
        assertEquals("data:image/png;base64,AA==", request.messages.single().attachments.single().dataUri)
    }

    @Test
    fun requestSnapshotDoesNotFollowMutableInputListsOrConfigurationSelection() {
        val mutableMessages = mutableListOf(ConversationMessage("user", "before", listOf(image)))
        val request = RequestSnapshotFactory.freeze(mutableMessages, RequestMode.TYPED, config, "memory")

        mutableMessages[0] = ConversationMessage("user", "after")

        assertEquals("before", request.messages.single().content)
        assertEquals(1, request.messages.single().attachments.size)
        assertEquals("mock-model", request.configuration.model)
        assertNotEquals(request.requestId, RequestSnapshotFactory.freeze(mutableMessages, RequestMode.TYPED, config).requestId)
        assertThrows(UnsupportedOperationException::class.java) {
            (request.messages as MutableList).clear()
        }
        assertThrows(UnsupportedOperationException::class.java) {
            (request.messages.single().attachments as MutableList).clear()
        }
    }

    @Test
    fun capabilitiesFailClosedForUnsupportedAttachments() {
        val request = RequestSnapshotFactory.freeze(
            listOf(ConversationMessage("user", "read", listOf(image))),
            RequestMode.TYPED,
            config
        )
        val textOnly = ProviderCapabilities(text = true, streaming = true, cancellation = true)

        val error = textOnly.validate(request)

        assertEquals(ProviderErrorCategory.UNSUPPORTED_CAPABILITY, error?.category)
        assertTrue(error?.message?.contains("image/png") == true)
    }

    @Test
    fun requestRejectsHistoricalAttachmentThatWasNotValidatedAndMaterialized() {
        val inaccessible = AttachmentSnapshot(
            sourceUri = "content://synthetic/missing",
            fileName = "missing.pdf",
            mimeType = "application/pdf",
            sizeBytes = 0,
            dataUri = null
        )

        assertThrows(IllegalArgumentException::class.java) {
            FrozenChatRequest(
                configuration = config,
                mode = RequestMode.TYPED,
                messages = listOf(ConversationMessage("user", "read", listOf(inaccessible)))
            )
        }
    }

    @Test
    fun snapshotRejectsAnAttachmentAboveTheApplicationLimit() {
        val oversized = image.copy(sizeBytes = 16L * 1024L * 1024L)
        assertThrows(IllegalArgumentException::class.java) {
            RequestSnapshotFactory.freeze(
                listOf(ConversationMessage("user", "large", listOf(oversized))),
                RequestMode.TYPED,
                config
            )
        }
    }

    @Test
    fun providerConfigurationRejectsEmbeddedCredentialsAndUnstableEndpointParts() {
        val credentialEndpoint = runCatching {
            ProviderConfiguration(config.identity, "https://user:secret@example.test/v1", "model")
        }
        val queryEndpoint = runCatching {
            ProviderConfiguration(config.identity, "https://example.test/v1?key=secret", "model")
        }
        val pathSecret = ProviderConfiguration(config.identity, "https://example.test/secret-token/v1", "model")

        assertTrue(credentialEndpoint.isFailure)
        assertTrue(queryEndpoint.isFailure)
        assertFalse(config.redactedEndpoint().contains("secret"))
        assertFalse(pathSecret.redactedEndpoint().contains("secret-token"))
    }
}

class MemoryConsentTest {
    private val config = ProviderConfigurations.chatGpt("mock-model")
    private val file = AttachmentSnapshot("content://synthetic/file", "note.txt", "text/plain", 4, "data:text/plain;base64,dGVzdA==")

    @Test
    fun approvalIsSingleUseAndBoundToProviderAndAttachments() {
        var now = 1_000L
        val store = SingleUseMemoryConsent { now }
        val grant = store.issue(config, listOf(file), "edited synthetic memory")

        assertEquals(
            MemoryConsentResult.Approved("edited synthetic memory"),
            store.consume(grant.id, config, listOf(file), "edited synthetic memory")
        )
        val reused = store.consume(grant.id, config, listOf(file), "edited synthetic memory") as MemoryConsentResult.Rejected
        assertEquals(ProviderErrorCategory.CONFIGURATION, reused.error.category)

        val changed = store.issue(config, listOf(file), "memory")
        val otherConfig = ProviderConfigurations.chatGpt("different-model")
        val rejected = store.consume(changed.id, otherConfig, listOf(file), "memory") as MemoryConsentResult.Rejected
        assertEquals(ProviderErrorCategory.CONFIGURATION, rejected.error.category)
        now += 301_000L
        val expired = store.issue(config, listOf(file), "expired")
        now += 301_000L
        val expiryResult = store.consume(expired.id, config, listOf(file), "expired") as MemoryConsentResult.Rejected
        assertTrue(expiryResult.error.message.contains("scaduto"))
    }

    @Test
    fun cancelAndOmittedMemoryDoNotCreateReusableApproval() {
        val store = SingleUseMemoryConsent { 10L }
        val grant = store.issue(config, listOf(file), "memory")

        assertTrue(store.cancel(grant.id))
        assertFalse(store.cancel(grant.id))
        val result = store.consume(grant.id, config, listOf(file), "memory") as MemoryConsentResult.Rejected
        assertEquals(ProviderErrorCategory.CONFIGURATION, result.error.category)

        val omitted = store.issue(config, emptyList(), "")
        assertEquals(MemoryConsentResult.Approved(""), store.consume(omitted.id, config, emptyList(), ""))
    }
}

class ResponseParserTest {
    @Test
    fun parserRemovesControlsAndKeepsLastRecognizedValues() {
        val parsed = SolphiviaResponseParser.parse(
            "Ciao [[SOLPHIVIA:nod]] mondo [[SOLPHIVIA:mode-codex]] [[SOLPHIVIA:run-left]]"
        )

        assertEquals("Ciao  mondo", parsed.text)
        assertEquals("nod", parsed.animation)
        assertEquals("run-left", parsed.motion)
        assertEquals("codex", parsed.mode)
    }

    @Test
    fun streamingFilterHoldsSplitTagsAndDropsMalformedControls() {
        val filter = StreamingTagFilter()

        val first = filter.feed("Hello [[SOLPH")
        val second = filter.feed("IVIA:nod]] world [[SOLPHIVIA:unknown]]")
        val final = filter.finish()

        assertEquals("Hello ", first)
        assertEquals(" world ", second)
        assertEquals("", final)
    }

    @Test
    fun incompleteControlTagIsNeverExposedAtEndOfStream() {
        val filter = StreamingTagFilter()

        val output = filter.feed("Visible [[SOLPHIVIA:mode-") + filter.finish()

        assertEquals("Visible ", output)
    }

    @Test
    fun streamingFilterIsCaseInsensitiveAcrossSplitControlTags() {
        val filter = StreamingTagFilter()

        val output = filter.feed("Safe [[solph") + filter.feed("ivia:nod]] text") + filter.finish()

        assertEquals("Safe  text", output)
    }

    @Test
    fun incompleteLowercaseControlTagIsDroppedAtEndOfStream() {
        val filter = StreamingTagFilter()

        val output = filter.feed("Visible [[solphivia:nod") + filter.finish()

        assertEquals("Visible ", output)
    }
}
