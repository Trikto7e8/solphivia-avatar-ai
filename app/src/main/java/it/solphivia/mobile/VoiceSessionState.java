package it.solphivia.mobile;

/** Control decisions are local and do not send pause/off phrases to the model. */
final class VoiceSessionState {
    static final int IGNORE=0,SEND=1,PAUSE=2,OFF=3;
    boolean paused;
    static final class Decision {
        final int action;final String text;
        Decision(int action,String text){this.action=action;this.text=text;}
    }
    Decision accept(String text){
        String value=VoicePhrases.normalized(text);
        if(value.isEmpty())return new Decision(IGNORE,"");
        if(value.equals("spegni microfono")||value.equals("disattiva microfono"))return new Decision(OFF,"");
        if(value.equals("stop")||value.equals("stop dialogo")||value.equals("pausa dialogo")){paused=true;return new Decision(PAUSE,"");}
        String request=VoicePhrases.wakeRequest(text);
        if(paused && request==null)return new Decision(IGNORE,"");
        if(request!=null){paused=false;return new Decision(SEND,request.isEmpty()?"Hola Solphivia":request);}
        return new Decision(SEND,text);
    }
}
