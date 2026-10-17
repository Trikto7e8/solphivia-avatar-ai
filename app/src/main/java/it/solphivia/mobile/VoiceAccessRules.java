package it.solphivia.mobile;

import java.util.Locale;

/** Exact allowlist for activation and local app-opening commands. */
final class VoiceAccessRules {
    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}]+", " ").trim().replaceAll("\\s+", " ");
    }
    static boolean wake(String text) {
        String value = normalize(text);
        // Vosk's Italian model can transcribe the invented name phonetically.
        // Accept complete phrases only, including the transcription observed on Moto g35.
        switch (value) {
            case "hola solphivia": case "hola solfivia": case "hola solphibia":
            case "hola solvia": case "hola solivia": case "hola solphia":
            case "hola sofia": case "hola sophia": case "hola solfibia":
            case "ola solphivia": case "ola solfivia": case "ola solphibia":
            case "ola solvia": case "ola sofia": case "ola sophia": case "ola solfibia":
            case "o la solfivia": case "o la solfibia": case "oh la solfivia":
            case "o la sua figlia":
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
