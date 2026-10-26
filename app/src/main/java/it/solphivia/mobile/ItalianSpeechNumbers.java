package it.solphivia.mobile;

/** Bounded local number verbalization; no service, recording or locale-dependent network data. */
final class ItalianSpeechNumbers {
    private static final String[] SMALL={"zero","uno","due","tre","quattro","cinque","sei","sette","otto","nove","dieci","undici","dodici","tredici","quattordici","quindici","sedici","diciassette","diciotto","diciannove"};
    private static final String[] TENS={"","","venti","trenta","quaranta","cinquanta","sessanta","settanta","ottanta","novanta"};
    static String integer(int number){
        if(number<0||number>999999)throw new IllegalArgumentException("Number out of range");
        if(number<20)return SMALL[number];
        if(number<100){String tens=TENS[number/10];int unit=number%10;if(unit==1||unit==8)tens=tens.substring(0,tens.length()-1);return tens+(unit==0?"":SMALL[unit]);}
        if(number<1000){int rest=number%100;String hundreds=number<200?"cento":SMALL[number/100]+"cento";if(rest>=80&&rest<90)hundreds=hundreds.substring(0,hundreds.length()-1);return hundreds+(rest==0?"":integer(rest));}
        int thousands=number/1000,rest=number%1000;
        return (thousands==1?"mille":integer(thousands)+"mila")+(rest==0?"":integer(rest));
    }
    static String digits(String value){StringBuilder out=new StringBuilder();for(char c:value.toCharArray()){if(out.length()>0)out.append(' ');out.append(SMALL[c-'0']);}return out.toString();}
    static String say(String token){
        if(token.matches("[0-9]{1,2}:[0-9]{2}")){
            String[] parts=token.split(":");int h=Integer.parseInt(parts[0]),m=Integer.parseInt(parts[1]);
            if(h<24&&m<60)return integer(h)+(m==0?" in punto":" e "+integer(m));
            return token;
        }
        if(token.matches("[0-9]{1,3}(?:\\.[0-9]{3})+"))return say(token.replace(".",""));
        if(token.matches("[0-9]+,[0-9]+")){String[] p=token.split(",");return say(p[0])+" virgola "+digits(p[1]);}
        if(!token.matches("[0-9]+"))return token;
        if(token.length()>6||(token.length()>1&&token.startsWith("0")))return digits(token);
        return integer(Integer.parseInt(token));
    }
}
