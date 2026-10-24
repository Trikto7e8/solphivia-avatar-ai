package it.solphivia.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VoiceStatusStateTest {
    @Test
    fun labelsFollowCaptureTranscriptionGenerationPlaybackAndIdleTransitions() {
        val state = VoiceStatusState()

        state.captureStarted()
        assertEquals(VoiceStatusState.Phase.CAPTURING, state.phase())
        assertEquals(VoiceStatusState.LISTENING_LABEL, state.label())

        state.transcriptionStarted()
        assertEquals(VoiceStatusState.TRANSCRIBING_LABEL, state.label())

        state.generationStarted()
        assertEquals(VoiceStatusState.Phase.GENERATING, state.phase())
        assertEquals(VoiceStatusState.THINKING_LABEL, state.label())
        assertNotEquals(VoiceStatusState.LISTENING_LABEL, state.label())

        state.playbackStarted()
        assertEquals(VoiceStatusState.PLAYING_LABEL, state.label())

        state.idle()
        assertEquals(VoiceStatusState.IDLE_LABEL, state.label())
    }

    @Test
    fun errorsDoNotAppearAsListeningOrThinking() {
        val state = VoiceStatusState()

        state.error()

        assertEquals(VoiceStatusState.Phase.ERROR, state.phase())
        assertEquals(VoiceStatusState.ERROR_LABEL, state.label())
        assertNotEquals(VoiceStatusState.LISTENING_LABEL, state.label())
        assertNotEquals(VoiceStatusState.THINKING_LABEL, state.label())
    }
}
