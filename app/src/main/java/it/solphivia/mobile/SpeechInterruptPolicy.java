package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Conservative phrase/stability/echo-text guard; not speaker authentication. */
final class SpeechInterruptPolicy {
    private String candidate="";private long since;private int repeats;private long last=-10000;
    static String normalized(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ITALIAN).replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();}
    static String canonical(String text){return normalized(text).replaceAll("\\b(?:solfivia|sol fibbia|sul fibbia)\\b","solphivia");}
    static boolean command(String text){return canonical(text).matches("(?:basta|fermati|stop|alt)(?: solphivia)?(?: (?:basta|fermati|stop|alt)(?: solphivia)?){0,2}");}
    static boolean sameShortCommand(String command,String fullText){return command(command)&&command(fullText);}
    static boolean corroborates(String command, String fullText, String spoken) {
        if (!command(command)) return false;
        String heard = canonical(fullText);
        if (sameShortCommand(command, heard)) return true;
        String[] words = heard.split(" ");
        String source = " " + canonical(spoken) + " ";
        // During overlap the unrestricted decoder may hear only the known TTS echo.
        // Keep the more confusable "alt" and "basta" on the stricter path.
        if (canonical(command).matches("(?:stop|fermati)(?: (?:stop|fermati)){0,2}")
                && words.length >= 2 && source.contains(" " + heard + " ")) return true;
        // Italian ASR can render the overlapped "fermati" as "fermare".
        // Require the keyword decoder and known speech on both sides to agree.
        if (canonical(command).contains("fermati")) {
            for (int index = 2; index < words.length; index++) {
                if (!words[index].equals("fermare")) continue;
                String prefix = String.join(" ", java.util.Arrays.copyOfRange(words, 0, index));
                String suffix = String.join(" ", java.util.Arrays.copyOfRange(words, index + 1, words.length));
                if (!prefix.endsWith(" non") && source.contains(" " + prefix + " ")
                        && (suffix.isEmpty() || source.contains(" " + suffix + " "))) return true;
            }
        }
        for (int index = 1; index < words.length; index++) {
            String suffix = String.join(" ", java.util.Arrays.copyOfRange(words, index, words.length));
            if (!command(suffix)) continue;
            String prefix = String.join(" ", java.util.Arrays.copyOfRange(words, 0, index));
            if (prefix.equals("non") || prefix.endsWith(" non")) continue;
            if (source.contains(" " + prefix + " ")) return true;
        }
        return false;
    }

    boolean acceptRecognition(String phrase, String verified, boolean finalResult, long now, String spoken) {
        return acceptRecognition(phrase, verified, finalResult, now, spoken, spoken);
    }

    boolean acceptRecognition(String phrase, String verified, boolean finalResult, long now, String spoken, String source) {
        boolean named = canonical(phrase).endsWith(" solphivia");
        if (!named && !corroborates(phrase, verified, source)) {
            reset();
            return false;
        }
        return accept(phrase, finalResult, now, spoken, true, true);
    }
    static boolean resumeCommand(String text){return canonical(text).matches("(?:prosegui|riprendi|continua|dimmi|ti ascolto|dai|vai)(?: solphivia)?");}
    static boolean quotedBySpeech(String spoken){return (" "+canonical(spoken)+" ").matches(".* (?:(?:basta|fermati|stop|alt) solphivia|stop|alt|basta|fermati) .*");}
    void reset(){candidate="";since=0;repeats=0;}
    boolean accept(String text,boolean finalResult,long now,String spoken,boolean playing){
        return accept(text, finalResult, now, spoken, playing, false);
    }
    private boolean accept(String text,boolean finalResult,long now,String spoken,boolean playing,boolean corroborated){
        if(!playing||!command(text)||(" "+canonical(spoken)+" ").contains(" "+canonical(text)+" ")){reset();return false;}
        String value=canonical(text);
        if(!candidate.equals(value)){candidate=value;since=now;repeats=1;}else repeats++;
        if(now-last<1500)return false;
        boolean named = canonical(text).endsWith(" solphivia");
        if (!finalResult && ((!named && !corroborated) || repeats < 3 || now - since < (named ? 250 : 600))) return false;
        last=now;reset();return true;
    }
}
