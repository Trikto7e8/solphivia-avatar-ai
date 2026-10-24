package it.solphivia.mobile.provider

import org.junit.Assert.*
import org.junit.Test

class ReviewEdgeCasesTest {
    @Test fun changedAttachmentBytesMustInvalidateConsent() {
        val config = ProviderConfigurations.chatGpt("synthetic-model")
        val store = SingleUseMemoryConsent { 1000L }
        val original = AttachmentSnapshot("content://synthetic/file", "note.txt", "text/plain", 4, "data:text/plain;base64,dGVzdA==")
        val modified = original.copy(dataUri = "data:text/plain;base64,ZXZpbA==")
        val grant = store.issue(config, listOf(original), "synthetic memory")
        assertTrue("Changed bytes must require fresh consent", store.consume(grant.id, config, listOf(modified), "synthetic memory") is MemoryConsentResult.Rejected)
    }

    @Test fun malformedSingleClosingBracketMustNotLeakIntoFinalResponse() {
        val response = SolphiviaResponseParser.parse("Ciao [[SOLPHIVIA:nod]")
        assertFalse("Malformed control leaked: ${response.text}", response.text.contains("SOLPHIVIA", ignoreCase = true))
    }
}
