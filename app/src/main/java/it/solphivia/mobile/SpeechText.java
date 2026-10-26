package it.solphivia.mobile;

import java.util.*;

/** Shared by overlay, chat and A/B preview; displayed chat text is never changed. */
final class SpeechText {
    static String prepare(String text,boolean english,boolean clear){
        if(text==null)return "";
        if(!english)return text;
        return clear?ClearItalianPronunciation.prepare(text):ItalianEnglishAccent.respell(text);
    }
    static int boundedRate(int percent){return Math.max(75,Math.min(115,percent));}
    static float rate(int systemPercent,int localPercent){return Math.max(.1f,systemPercent/100f)*boundedRate(localPercent)/100f;}
    static List<String> chunks(String text,int limit){
        if(limit<2)throw new IllegalArgumentException("Speech limit too small");
        List<String> parts=new ArrayList<>();
        for(int start=0;start<text.length();){
            int end=Math.min(start+limit,text.length());
            if(end<text.length()){
                if(Character.isLowSurrogate(text.charAt(end)))end--;
                int boundary=end;
                while(boundary>start&&!Character.isWhitespace(text.charAt(boundary-1)))boundary--;
                if(boundary>start)end=boundary;
            }
            parts.add(text.substring(start,end));start=end;
        }
        return parts;
    }
}
