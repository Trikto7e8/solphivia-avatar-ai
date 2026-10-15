package it.solphivia.mobile;

/** Pure Java checks: command allowlist, wake phrase and ShareBridge parsing. */
public final class VoiceAccessRulesTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(VoiceAccessRules.wake("  HOLA, Solphivia!  "));
        check(!VoiceAccessRules.wake("hola"));
        check(!VoiceAccessRules.wake("non dire hola solphivia"));
        check(!VoiceAccessRules.wake("hola solphivia apri whatsapp"));
        check(!VoiceAccessRules.wake(null));
        check(VoiceAccessRules.openWhatsApp("Apri WhatsApp!"));
        check(VoiceAccessRules.openWhatsApp("apri whats app"));
        check(!VoiceAccessRules.openWhatsApp("non apri whatsapp"));
        check(!VoiceAccessRules.openWhatsApp("apri whatsapp e invia ciao"));
        check("Hola, Marco!".equals(VoiceAccessRules.greeting(" Marco ")));
        check("Ciao, Marco!".equals(ShareBridge.preview("Condividi Ciao, Marco! su WhatsApp")));
        check("Ciao".equals(ShareBridge.preview("condividi Ciao su whats app.")));
        check(ShareBridge.preview("invia Ciao su WhatsApp") == null);
        check(ShareBridge.preview("non condividi Ciao su WhatsApp") == null);
        check(ShareBridge.preview("condividi   su WhatsApp") == null);
        check(ShareBridge.preview("condividi " + "x".repeat(2001) + " su WhatsApp") == null);
        check(ShareBridge.preview(null) == null);
        System.out.println("PASS: 17 Voice Access / ShareBridge checks");
    }
}