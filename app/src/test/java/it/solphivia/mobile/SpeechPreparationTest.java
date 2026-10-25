package it.solphivia.mobile;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/** Text correctness and integration policy, not a test of acoustic intelligibility. */
public class SpeechPreparationTest {
    @Test public void legacyProfileIsExactlyThePublishedTransformation(){
        for(String sample:corpus())assertEquals(ItalianEnglishAccent.respell(sample),SpeechText.prepare(sample,true,false));
    }
    @Test public void plainItalianAndDisplayedInputAreUntouched(){
        String original="Perché c’è il ghiaccio? 14:30, 23°C. 😀";
        assertEquals(original,SpeechText.prepare(original,false,true));
        String copy=original;SpeechText.prepare(original,true,true);assertEquals(copy,original);
    }
    @Test public void foreignBrandsAndTechnicalTokensKeepTheirSpelling(){
        String input="Apri WhatsApp, Google Maps, ChatGPT e Android. ONNX API LLM GitHub https://example.org/a?q=ciao test@example.org file.pdf";
        String result=SpeechText.prepare(input,true,true);
        for(String token:new String[]{"WhatsApp","Google","Maps","ChatGPT","Android","ONNX","API","LLM","GitHub","https://example.org/a?q=ciao","test@example.org","file.pdf"})assertTrue(token,result.contains(token));
    }
    @Test public void exactHintsCoverCorePhrasesAndAvoidCascadingReplacements(){
        assertEquals("koh-zah pohs-soh fah-reh pehr teh?",SpeechText.prepare("Cosa posso fare per te?",true,true));
        assertEquals("ah-yoo-tah-reh",SpeechText.prepare("aiutare",true,true));
        assertEquals("voh-cheh",SpeechText.prepare("voce",true,true));
        assertEquals("cheh",SpeechText.prepare("c’è",true,true));
    }
    @Test public void difficultSoundGroupsRemainDistinct(){
        assertEquals("shee",SpeechText.prepare("sci",true,true));
        assertEquals("nyohkkoh",SpeechText.prepare("gnocco",true,true));
        assertEquals("keeahveh",SpeechText.prepare("chiave",true,true));
        assertEquals("jeelahtoh",SpeechText.prepare("gilato",true,true));
    }
    @Test public void italianNumbersHaveDefinedBoundsAndElisions(){
        String[][] examples={{"0","zero"},{"18","diciotto"},{"21","ventuno"},{"28","ventotto"},{"80","ottanta"},{"180","centottanta"},{"2000","duemila"},{"2026","duemilaventisei"},{"999999","novecentonovantanovemilanovecentonovantanove"},{"007","zero zero sette"},{"12,05","dodici virgola zero cinque"},{"1.000","mille"},{"14:30","quattordici e trenta"},{"08:00","otto in punto"},{"25:99","25:99"}};
        for(String[] pair:examples)assertEquals(pair[0],pair[1],ItalianSpeechNumbers.say(pair[0]));
        for(int i=0;i<=999999;i+=97)assertTrue(ItalianSpeechNumbers.integer(i).matches("[a-z]+"));
    }
    @Test public void clockAndPercentDoNotLeaveEnglishNumbersForTheEngine(){
        String result=SpeechText.prepare("Alle 14:30 ci sono 2 messaggi e il 25% di batteria.",true,true);
        assertFalse(result.matches(".*[0-9].*"));assertFalse(result.contains("%"));assertTrue(result.contains("chehntoh"));
    }
    @Test public void chunkingPreservesAllCharactersAndUnicode(){
        for(int limit=2;limit<=90;limit++)for(String sample:corpus()){
            String prepared=SpeechText.prepare(sample,true,true);
            List<String> parts=SpeechText.chunks(prepared,limit);assertEquals(prepared,String.join("",parts));
            for(String part:parts){assertTrue(part.length()<=limit);assertFalse(part.isEmpty());assertFalse(Character.isLowSurrogate(part.charAt(0)));assertFalse(Character.isHighSurrogate(part.charAt(part.length()-1)));}
        }
        assertEquals(Arrays.asList("ciao ","mondo"),SpeechText.chunks("ciao mondo",7));
    }
    @Test public void localRateKeepsSystemSettingAndPitchOutsideItsScope(){
        assertEquals(1.2f,SpeechText.rate(120,100),.001f);assertEquals(.9f,SpeechText.rate(100,90),.001f);
        assertEquals(75,SpeechText.boundedRate(-1));assertEquals(115,SpeechText.boundedRate(900));
    }
    @Test public void emptyAndLongTokensAreBounded(){
        assertEquals("",SpeechText.prepare(null,true,true));assertEquals("",SpeechText.prepare("",true,true));
        String word=String.join("",Collections.nCopies(2000,"a"));assertEquals(word,SpeechText.prepare(word,true,true));
    }
    private static List<String> corpus(){return Arrays.asList(
        "Ciao, sono Solphivia. Cosa posso fare per te?", "Oggi sono le 14:30 e ci sono 23 gradi.",
        "Scegli la voce inglese. Ti ascolto e posso aiutarti.", "La famiglia sceglie gli gnocchi, il ghiaccio e l’acqua.",
        "Apri WhatsApp, Google Maps, ChatGPT e Android. 😀", "Sono pronti 2 messaggi: grazie!", "0123456789", "", "東京", "a😀b");}
}
