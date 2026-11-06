package it.solphivia.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceSessionStateTest {
    @Test public void repeatedReadingStopsKeepTheConversationAvailable() {
        VoiceSessionState session = new VoiceSessionState();
        for (int cycle = 0; cycle < 3; cycle++) {
            assertEquals(VoiceSessionState.SEND, session.accept("Raccontami una storia").action);
            assertEquals(VoiceSessionState.SEND, session.accept("stop").action);
            assertFalse(session.paused);
            assertEquals(VoiceSessionState.SEND, session.accept("riprendi").action);
        }
    }

    @Test public void onlyExplicitDialoguePauseRequiresAnotherWake() {
        VoiceSessionState session = new VoiceSessionState();
        assertEquals(VoiceSessionState.PAUSE, session.accept("stop dialogo").action);
        assertEquals(VoiceSessionState.IGNORE, session.accept("una frase nella stanza").action);
        assertEquals(VoiceSessionState.SEND, session.accept("Hola Solphivia").action);
        assertFalse(session.paused);
        assertEquals(VoiceSessionState.OFF, session.accept("spegni microfono").action);
    }
}
