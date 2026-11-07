package it.solphivia.mobile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Limits mirror the server, which remains authoritative. */
final class DemoProtocol {
    static final int MAX_MESSAGE_BYTES = 4096;
    static final int MAX_RESPONSE_BYTES = 20000;

    static String origin(String value) {
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) {
            throw new IllegalArgumentException("Servizio demo HTTPS non configurato.");
        }
        return value;
    }

    static void validateMessage(String message) {
        if (message.trim().isEmpty() || message.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new IllegalArgumentException("La demo accetta un messaggio di testo fino a 4096 byte.");
        }
    }

    static String error(String code) {
        switch (code) {
            case "invalid_invite": return "Codice d’invito non valido, scaduto o revocato. Controllalo in Prova senza account.";
            case "tester_limit": return "Hai raggiunto il limite di richieste previsto per questo tester.";
            case "global_limit": return "La demo ha raggiunto il limite complessivo. Riprova quando il gestore la riattiva.";
            case "request_in_progress": return "C’è già una richiesta demo in corso per questo codice.";
            case "duplicate_request": return "Questa richiesta è già stata elaborata. Non viene inviata di nuovo.";
            case "message_size": return "Il messaggio supera il limite della demo.";
            default: return "Servizio demo temporaneamente non disponibile. Il tentativo può aver consumato una richiesta.";
        }
    }
}
