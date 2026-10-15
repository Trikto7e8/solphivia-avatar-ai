package it.solphivia.mobile;

import java.util.Locale;

/** Exact allowlist: dictated text is never passed to a chat or arbitrary Intent. */
final class VoiceAccessRules {
    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}]+", " ").trim().replaceAll("\\s+", " ");
    }
    static boolean wake(String text) {
        String value = normalize(text);
        // Vosk's Italian model can transcribe the invented name phonetically.
        // Keep the activation constrained to the two-word greeting.
        switch (value) {
            case "hola solphivia": case "hola solfivia": case "hola solphibia":
            case "hola solvia": case "hola solivia": case "hola solphia":
            case "hola sofia": case "hola sophia":
            case "ola solphivia": case "ola solfivia": case "ola solphibia":
            case "ola solvia": case "ola sofia": case "ola sophia":
                return true;
            default: return false;
        }
    }
    static boolean wakeCandidate(String text) {
        String value = normalize(text);
        return value.startsWith("hola ") || value.startsWith("ola ")
                || value.startsWith("oh la ") || value.contains("solphiv")
                || value.contains("solfiv") || value.contains("sofia");
    }
    static boolean openWhatsApp(String text) {
        String value = normalize(text);
        return "apri whatsapp".equals(value) || "apri whats app".equals(value);
    }
    static boolean stop(String text) {
        String value = normalize(text);
        return "fermati".equals(value) || "fermati solphivia".equals(value)
                || "basta".equals(value) || "smetti".equals(value) || "stop".equals(value);
    }
    static String greeting(String name) { return "Hola, " + name.trim() + "!"; }
}
