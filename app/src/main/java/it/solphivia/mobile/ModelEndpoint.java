package it.solphivia.mobile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Pure connection policy. HTTP is confined to explicitly selected private IPv4 literals. */
final class ModelEndpoint {
    enum Kind { OPENAI, GEMINI, CLAUDE, OLLAMA, LM_STUDIO, COMPATIBLE }
    static final int MAX_INPUT_BYTES = 32768, MAX_RESPONSE_BYTES = 65536, MAX_REPLY_CHARS = 12000;
    static String label(Kind kind) {
        switch(kind) {
            case OPENAI: return "OpenAI API personale";
            case GEMINI: return "Gemini API personale";
            case CLAUDE: return "Claude API personale";
            case OLLAMA: return "Ollama sul tuo PC";
            case LM_STUDIO: return "LM Studio sul tuo PC";
            default: return "API compatibile";
        }
    }
    static boolean local(Kind kind) { return kind == Kind.OLLAMA || kind == Kind.LM_STUDIO; }
    static String defaultBase(Kind kind) {
        switch(kind) {
            case OPENAI: return "https://api.openai.com/v1";
            case GEMINI: return "https://generativelanguage.googleapis.com/v1beta/openai";
            case CLAUDE: return "https://api.anthropic.com/v1";
            default: return "";
        }
    }
    static String endpoint(Kind kind, String value, boolean allowLanHttp, boolean hasKey) {
        String base = value == null ? "" : value.trim();
        if (!defaultBase(kind).isEmpty()) {
            if (!base.isEmpty() && !base.equals(defaultBase(kind))) throw new IllegalArgumentException("Il servizio selezionato usa il proprio indirizzo ufficiale.");
            base=defaultBase(kind);
        }
        while(base.endsWith("/"))base=base.substring(0,base.length()-1);
        URI uri;
        try { uri=URI.create(base); } catch(Exception e) { throw new IllegalArgumentException("Indirizzo del servizio non valido."); }
        if(uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getRawQuery()!=null || uri.getRawFragment()!=null
                || uri.getPort()==0 || uri.getPort()>65535 || base.length()>512 || !uri.normalize().equals(uri))
            throw new IllegalArgumentException("Usa un indirizzo senza credenziali, parametri o percorsi relativi.");
        if("http".equals(uri.getScheme())) {
            if(!local(kind) || !allowLanHttp || !privateIpv4(uri.getHost()) || hasKey)
                throw new IllegalArgumentException("HTTP consentito solo per un IP privato del tuo PC, dopo conferma e senza chiavi. Per le credenziali usa HTTPS.");
        } else if(!"https".equals(uri.getScheme())) throw new IllegalArgumentException("I servizi cloud richiedono HTTPS.");
        if(kind==Kind.GEMINI)return base+"/chat/completions";
        if(!base.endsWith("/v1"))base+="/v1";
        return base+(kind==Kind.CLAUDE?"/messages":"/chat/completions");
    }
    static boolean privateIpv4(String host) {
        String[] parts=host.split("\\.",-1);
        if(parts.length!=4)return false;
        int[] p=new int[4];
        for(int i=0;i<4;i++) {
            if(!parts[i].matches("0|[1-9][0-9]{0,2}"))return false;
            p[i]=Integer.parseInt(parts[i]);if(p[i]>255)return false;
        }
        return p[0]==10 || p[0]==127 || (p[0]==192&&p[1]==168) || (p[0]==172&&p[1]>=16&&p[1]<=31);
    }
    static void validateModel(String model) {
        if(model==null || !model.matches("[a-zA-Z0-9][a-zA-Z0-9._:/@+-]{0,159}"))throw new IllegalArgumentException("Inserisci l’identificativo del modello fornito dal servizio.");
    }
    static void checkInput(String text) {
        if(text.getBytes(StandardCharsets.UTF_8).length>MAX_INPUT_BYTES)throw new IllegalArgumentException("Conversazione troppo lunga: inizia una nuova chat per questo collegamento.");
    }
    private ModelEndpoint() {}
}
