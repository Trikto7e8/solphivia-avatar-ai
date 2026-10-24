package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.*;

/** Parses explicit user text only. No provider output, URI execution or personal-data lookup. */
final class DeviceCommand {
    enum Kind { APP, MAP_SEARCH, DIRECTIONS, DIAL, HELP, INVALID }
    final Kind kind;
    final String value;
    private DeviceCommand(Kind kind,String value){this.kind=kind;this.value=value;}
    static String normalized(String value){
        return Normalizer.normalize(value.toLowerCase(Locale.ITALIAN),Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replace('’','\'').replaceAll("\\s+"," ").trim();
    }
    private static String argument(String text,String expression){
        Matcher match=Pattern.compile(expression,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(text);
        return match.matches()?match.group(1).trim():null;
    }
    static DeviceCommand parse(String text){
        if(text==null)return null;
        String clean=text.trim().replaceFirst("[.!?]+$","").trim();
        if(clean.isEmpty())return null;
        if(clean.indexOf('\n')>=0||clean.indexOf('\r')>=0)return null;
        clean=clean.replaceFirst("(?iu)^(?:puoi|potresti) +", "").replaceFirst("(?iu) +per favore$", "");
        if(normalized(clean).matches("(?:comandi|aiuto comandi|cosa sai fare)"))return new DeviceCommand(Kind.HELP,"");
        String value=argument(clean,"(?:apri|aprire|avvia|avviare) +(?:(?:l['’]app|l applicazione|l['’]applicazione|app|applicazione|la) +)?(.+)");
        if(value!=null)return new DeviceCommand(value.length()>100||value.contains(":")||value.contains("/")?Kind.INVALID:Kind.APP,value);
        value=argument(clean,"(?:portami a|portami al|portami alla|indicazioni per|percorso per|naviga verso) +(.+)");
        if(value!=null)return new DeviceCommand(value.length()>300?Kind.INVALID:Kind.DIRECTIONS,value);
        value=argument(clean,"cerca +(.+?) +su (?:google )?maps");
        if(value==null)value=argument(clean,"cerca su (?:google )?maps +(.+)");
        if(value==null)value=argument(clean,"cerca +(.+ vicino a me)");
        if(value!=null)return new DeviceCommand(value.length()>300?Kind.INVALID:Kind.MAP_SEARCH,value);
        value=argument(clean,"(?:chiama il numero|componi il numero|chiama|chiamare|componi) +(.+)");
        if(value!=null){String number=phoneNumber(value);return new DeviceCommand(number==null?Kind.INVALID:Kind.DIAL,number==null?"numero":number);}
        return null;
    }
    static String phoneNumber(String value){
        String number=normalized(value);
        if(!number.matches("[+0-9() .-]+")){
            String[] names={"zero","uno","due","tre","quattro","cinque","sei","sette","otto","nove"};
            StringBuilder digits=new StringBuilder();
            for(String token:number.split(" +")){
                if(token.equals("piu")&&digits.length()==0){digits.append('+');continue;}
                boolean found=false;
                for(int i=0;i<names.length;i++)if(token.equals(names[i])){digits.append(i);found=true;break;}
                if(!found&&token.matches("[0-9]+")){digits.append(token);found=true;}
                if(!found)return null;
            }
            number=digits.toString();
        }
        number=number.replaceAll("[() .-]","");
        return number.matches("\\+?[0-9]{3,15}")?number:null;
    }
}
