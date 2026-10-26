package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative phrase/stability/echo-text guard; not speaker authentication. */
final class SpeechInterruptPolicy {
    private String candidate="";private long since;private int repeats;private long last=-10000;
    static String normalized(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ITALIAN).replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();}
    static String canonical(String text){return normalized(text).replaceAll("\\b(?:solfivia|sol fibbia|sul fibbia)\\b","solphivia");}
    static boolean command(String text){return canonical(text).matches("(?:basta|fermati) solphivia");}
    static boolean quotedBySpeech(String spoken){return (" "+canonical(spoken)+" ").matches(".* (?:basta|fermati) solphivia .*");}
    void reset(){candidate="";since=0;repeats=0;}
    boolean accept(String text,boolean finalResult,long now,String spoken,boolean playing){
        if(!playing||!command(text)||quotedBySpeech(spoken)){reset();return false;}
        String value=canonical(text);
        if(!candidate.equals(value)){candidate=value;since=now;repeats=1;}else repeats++;
        if(now-last<1500)return false;
        if(!finalResult&&(repeats<3||now-since<250))return false;
        last=now;reset();return true;
    }
}
