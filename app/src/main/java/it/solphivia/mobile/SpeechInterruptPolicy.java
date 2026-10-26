package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative phrase/stability/echo-text guard; not speaker authentication. */
final class SpeechInterruptPolicy {
    private String candidate="";private long since;private int repeats;private long last=-10000;
    static String normalized(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ITALIAN).replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();}
    static String canonical(String text){return normalized(text).replaceAll("\\b(?:solfivia|sol fibbia|sul fibbia)\\b","solphivia");}
    static boolean command(String text){return canonical(text).matches("(?:(?:basta|fermati|stop|alt) solphivia|stop|alt|basta|fermati)");}
    static boolean sameShortCommand(String command,String fullText){return command(command)&&canonical(command).equals(canonical(fullText));}
    static boolean resumeCommand(String text){return canonical(text).matches("(?:prosegui|riprendi|continua|dimmi|ti ascolto|dai|vai)(?: solphivia)?");}
    static boolean quotedBySpeech(String spoken){return (" "+canonical(spoken)+" ").matches(".* (?:(?:basta|fermati|stop|alt) solphivia|stop|alt|basta|fermati) .*");}
    void reset(){candidate="";since=0;repeats=0;}
    boolean accept(String text,boolean finalResult,long now,String spoken,boolean playing){
        if(!playing||!command(text)||(" "+canonical(spoken)+" ").contains(" "+canonical(text)+" ")){reset();return false;}
        String value=canonical(text);
        if(!candidate.equals(value)){candidate=value;since=now;repeats=1;}else repeats++;
        if(now-last<1500)return false;
        if(!finalResult&&(!canonical(text).endsWith(" solphivia")||repeats<3||now-since<250))return false;
        last=now;reset();return true;
    }
}
