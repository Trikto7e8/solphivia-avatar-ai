package it.solphivia.mobile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Limits mirror the server, which remains authoritative. */
final class DemoProtocol {
    static final int MAX_MESSAGE_BYTES = 4096;
    static final int MAX_RESPONSE_BYTES = 20000;

    /** Only recent text from this invitation's isolated conversation; never vault or file references. */
    static org.json.JSONObject payload(org.json.JSONArray history, String id) throws Exception {
        org.json.JSONObject last=history.optJSONObject(history.length()-1);
        if(last==null||!"user".equals(last.optString("role")))throw new IllegalArgumentException("Scrivi o pronuncia un messaggio.");
        String message=last.optString("content");validateMessage(message);
        org.json.JSONArray messages=new org.json.JSONArray();
        for(int i=Math.max(0,history.length()-8);i<history.length();i++) {
            org.json.JSONObject line=history.optJSONObject(i);if(line==null)continue;
            String role=line.optString("role"),text=line.optString("content");
            if((role.equals("user")||role.equals("assistant"))&&!text.trim().isEmpty()&&text.getBytes(StandardCharsets.UTF_8).length<=4096)
                messages.put(new org.json.JSONObject().put("role",role).put("content",text));
        }
        org.json.JSONObject body=new org.json.JSONObject().put("message",message).put("request_id",id).put("messages",messages);
        while(messages.length()>1&&(messages.toString().getBytes(StandardCharsets.UTF_8).length>6144||body.toString().getBytes(StandardCharsets.UTF_8).length>8192))messages.remove(0);
        if(body.toString().getBytes(StandardCharsets.UTF_8).length>8192)body.remove("messages");
        if(body.toString().getBytes(StandardCharsets.UTF_8).length>8192)throw new IllegalArgumentException("Il messaggio codificato supera il limite della prova.");
        return body;
    }

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
