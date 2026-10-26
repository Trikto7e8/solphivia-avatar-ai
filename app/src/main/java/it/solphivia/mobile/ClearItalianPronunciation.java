package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Experimental English-voice hints, not IPA or a guarantee of acoustic intelligibility. */
final class ClearItalianPronunciation {
    private static final Pattern TOKENS=Pattern.compile("https?://[^\\s]+|[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.-]+\\.[\\p{L}]{2,}|[\\p{L}\\p{N}_-]+\\.(?:apk|pdf|docx|zip|java|kt|json|png|jpg|wav)\\b|[\\p{L}\\p{M}]+(?:['’][\\p{L}\\p{M}]+)*|[0-9]+(?:[.,:][0-9]+)*",Pattern.CASE_INSENSITIVE);
    private static final Set<String> ENGLISH=new HashSet<>(Arrays.asList("android","google","maps","whatsapp","telegram","chrome","chatgpt","github","spotify","youtube","bluetooth","wifi","offline","online","software","hardware","codex","play","store","file","debug","link","browser","touch","screen"));
    private static final Map<String,String> WORDS=new HashMap<>();
    static {
        String[][] pairs={
            {"ciao","chow"},{"hola","oh-lah"},{"solphivia","sol-fee-vee-ah"},{"solfivia","sol-fee-vee-ah"},
            {"cosa","koh-zah"},{"posso","pohs-soh"},{"fare","fah-reh"},{"per","pehr"},{"te","teh"},
            {"io","ee-oh"},{"tu","too"},{"e","eh"},{"di","dee"},{"ti","tee"},{"mi","mee"},{"si","see"},{"vi","vee"},
            {"qui","kwee"},{"chi","kee"},{"che","keh"},{"c'è","cheh"},{"ciò","choh"},{"più","pyoo"},
            {"grazie","graht-tsyeh"},{"prego","preh-goh"},{"perché","pehr-keh"},{"perche","pehr-keh"},
            {"come","koh-meh"},{"sono","soh-noh"},{"bene","beh-neh"},{"non","nohn"},{"con","kohn"},
            {"aiuto","ah-yoo-toh"},{"aiutare","ah-yoo-tah-reh"},{"aiutarti","ah-yoo-tahr-tee"},
            {"buongiorno","bwohn-jor-noh"},{"buonasera","bwoh-nah-seh-rah"},{"buonanotte","bwoh-nah-noht-teh"},
            {"arrivederci","ahr-ree-veh-dehr-chee"},{"oggi","ohj-jee"},{"domani","doh-mah-nee"},
            {"ascolto","ahs-kohl-toh"},{"voce","voh-cheh"},{"inglese","een-gleh-zeh"},{"italiano","ee-tah-lyah-noh"},
            {"apri","ah-pree"},{"telefono","teh-leh-foh-noh"},{"chiamare","kyah-mah-reh"},
            {"numero","noo-meh-roh"},{"schermo","skehr-moh"},{"sblocca","sblohk-kah"},
            {"scegli","sheh-lyee"},{"figlio","fee-lyoh"},{"famiglia","fah-mee-lyah"},{"acqua","ahk-kwah"},
            {"scienza","shehn-tsah"},{"coscienza","koh-shehn-tsah"},{"sciare","shee-ah-reh"},
            {"glielo","lyeh-loh"},{"gli","lyee"},{"glicine","glee-chee-neh"},
            {"zero","dzeh-roh"},{"azione","ah-tsyoh-neh"},{"attenzione","aht-tehn-tsyoh-neh"},
            {"memoria","meh-moh-ryah"},{"progetto","proh-jeht-toh"},{"risposta","rees-pohs-tah"}
        };
        for(String[] pair:pairs)WORDS.put(pair[0],pair[1]);
    }
    static String prepare(String text){
        if(text==null||text.isEmpty())return text;
        String spoken=text.replaceAll("(?<=\\d)\\s*%", " per cento").replaceAll("(?<=\\d)\\s*°\\s*[cC]\\b", " gradi Celsius");
        Matcher matcher=TOKENS.matcher(spoken);StringBuffer out=new StringBuffer(text.length()*2);
        while(matcher.find()){
            String token=matcher.group(),replacement;
            if(token.startsWith("http:")||token.startsWith("https:")||token.contains("@")||token.matches(".*\\.[a-zA-Z]{2,5}$")||token.matches("[A-Z]{2,8}")||ENGLISH.contains(token.toLowerCase(Locale.ROOT)))replacement=token;
            else if(Character.isDigit(token.charAt(0))){
                String numberWords=ItalianSpeechNumbers.say(token);replacement=numberWords.equals(token)?token:words(numberWords);
            }else replacement=word(token);
            matcher.appendReplacement(out,Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);return out.toString();
    }
    private static String words(String text){StringJoiner out=new StringJoiner(" ");for(String part:text.split(" "))out.add(word(part));return out.toString();}
    private static String word(String original){
        if(original.length()>80)return original;
        String lower=original.toLowerCase(Locale.ITALIAN).replace('’','\'');
        String hint=WORDS.get(lower);if(hint!=null)return hint;
        int apostrophe=lower.indexOf('\'');
        if(apostrophe>=0)return word(lower.substring(0,apostrophe))+word(lower.substring(apostrophe+1));
        String s=Normalizer.normalize(lower,Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        // Conservative: keep non-Italian alphabets and foreign words with uncommon letters intact.
        if(!s.matches("[a-z]+")||s.matches(".*[jkwxy].*"))return original;
        StringBuilder out=new StringBuilder(s.length()*2);
        for(int i=0;i<s.length();){
            String rest=s.substring(i);char c=s.charAt(i);
            if(rest.startsWith("sch")){out.append("sk");i+=3;continue;}
            if(rest.startsWith("gli")&&(rest.length()==3||"aeou".indexOf(rest.charAt(3))>=0)){out.append("ly");i+=3;if(i==s.length())out.append("ee");continue;}
            if(rest.startsWith("gn")){out.append("ny");i+=2;continue;}
            if(rest.startsWith("sc")&&rest.length()>2&&"ei".indexOf(rest.charAt(2))>=0){out.append("sh");i+=2;if(i+1<s.length()&&s.charAt(i)=='i'&&"aeou".indexOf(s.charAt(i+1))>=0)i++;continue;}
            if(c=='c'||c=='g'){
                boolean hard=i+1<s.length()&&s.charAt(i+1)=='h';
                boolean soft=!hard&&i+1<s.length()&&"ei".indexOf(s.charAt(i+1))>=0;
                out.append(c=='c'?(soft?"ch":"k"):(soft?"j":"g"));i++;
                if(hard)i++;
                else if(soft&&i+1<s.length()&&s.charAt(i)=='i'&&"aeou".indexOf(s.charAt(i+1))>=0)i++;
                continue;
            }
            if(rest.startsWith("qu")){out.append("kw");i+=2;continue;}
            switch(c){
                case 'a':out.append("ah");break;case 'e':out.append("eh");break;case 'i':out.append("ee");break;
                case 'o':out.append("oh");break;case 'u':out.append("oo");break;
                case 'z':out.append("ts");break;
                case 's':out.append(i>0&&i+1<s.length()&&vowel(s.charAt(i-1))&&vowel(s.charAt(i+1))?'z':'s');break;
                case 'h':break;default:out.append(c);
            }
            i++;
        }
        return out.toString();
    }
    private static boolean vowel(char c){return "aeiou".indexOf(c)>=0;}
}
