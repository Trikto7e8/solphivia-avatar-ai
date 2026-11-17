package it.solphivia.mobile;

import org.junit.Test;
import static org.junit.Assert.*;

public class DemoProtocolTest {
    @Test public void httpsOriginOnly() {
        assertEquals("https://demo.example.org", DemoProtocol.origin("https://demo.example.org"));
        for (String value : new String[]{"", "http://example.org", "https://user:code@example.org", "https://example.org/redirect", "https://example.org?invite=code", "https://example.org#fragment"}) {
            try { DemoProtocol.origin(value); fail(value); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void sizeIsUtf8Bytes() {
        DemoProtocol.validateMessage("a".repeat(4096));
        DemoProtocol.validateMessage("é".repeat(2048));
        for (String message : new String[]{"  ", "a".repeat(4097), "é".repeat(2049)}) {
            try { DemoProtocol.validateMessage(message); fail("oversized or blank accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void errorsAreActionableWithoutOAuthFallback() {
        assertTrue(DemoProtocol.error("invalid_invite").contains("invito"));
        assertTrue(DemoProtocol.error("tester_limit").contains("tester"));
        assertTrue(DemoProtocol.error("global_limit").contains("complessivo"));
        assertFalse(DemoProtocol.error("private-provider-secret").contains("private-provider-secret"));
    }
}
