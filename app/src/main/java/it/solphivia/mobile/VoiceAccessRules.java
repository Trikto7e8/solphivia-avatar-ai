package it.solphivia.mobile;

import java.util.Locale;

/** Exact allowlist: dictated text is never passed to a chat or arbitrary Intent. */
final class VoiceAccessRules {
    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}]+", " ").trim().replaceAll("\\s+", " ");
    }
    static boolean wake(String text) { return "hola solphivia".equals(normalize(text)); }
    static boolean openWhatsApp(String text) {
        String value = normalize(text);
        return "apri whatsapp".equals(value) || "apri whats app".equals(value);
    }
    static boolean stop(String text) {\n        String value = normalize(text);\n        return "fermati".equals(value) || "fermati solphivia".equals(value)\n                || "basta".equals(value) || "smetti".equals(value) || "stop".equals(value);\n    }\n    static String greeting(String name) { return "Hola, " + name.trim() + "!"; }
}
