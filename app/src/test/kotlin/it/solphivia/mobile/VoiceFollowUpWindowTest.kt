package it.solphivia.mobile

import org.junit.Assert.*
import org.junit.Test

class VoiceFollowUpWindowTest {
    @Test fun replyAndMicrophoneStartupDoNotConsumeTheWindow() {
        val window = VoiceFollowUpWindow()
        window.replyStarted()
        window.captureReady(100L)
        assertTrue(window.awaitingReply())
        assertFalse(window.expired(90000L))
        window.replyFinished()
        window.captureReady(100000L)
        assertFalse(window.expired(104999L))
        assertTrue(window.expired(105000L))
    }
    @Test fun speechStartedInTimeCanFinishAfterFiveSeconds() {
        val window = VoiceFollowUpWindow()
        window.captureReady(100L)
        assertTrue(window.speechStarted(5099L))
        assertFalse(window.expired(12000L))
        assertFalse(window.canRetry(12000L))
    }
    @Test fun speechAtTheDeadlineIsTooLate() {
        val window = VoiceFollowUpWindow()
        window.captureReady(100L)
        assertFalse(window.speechStarted(5100L))
        assertTrue(window.expired(5100L))
    }
    @Test fun recognizerRetryNeverExtendsTheDeadline() {
        val window = VoiceFollowUpWindow()
        window.captureReady(100L)
        assertTrue(window.canRetry(2000L))
        window.captureReady(2500L)
        assertTrue(window.expired(5100L))
        assertFalse(window.canRetry(5100L))
    }
    @Test fun eachNewReplyCreatesANewWindow() {
        val window = VoiceFollowUpWindow()
        window.captureReady(100L)
        window.speechStarted(1000L)
        window.replyStarted()
        assertTrue(window.awaitingReply())
        window.replyFinished()
        window.captureReady(20000L)
        assertFalse(window.expired(24999L))
        assertTrue(window.expired(25000L))
    }
    @Test fun pauseAndOffRemoveTheFollowUp() {
        val window = VoiceFollowUpWindow()
        window.captureReady(100L)
        window.rest()
        assertFalse(window.canRetry(101L))
        assertFalse(window.expired(10000L))
        assertFalse(window.speechStarted(101L))
    }
}
