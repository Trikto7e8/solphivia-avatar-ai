package it.solphivia.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpeechInterruptPolicyTest {
 @Test public void onlyNamedWholeCommands(){for(String text:new String[]{"Basta Solphivia!","fermati solfivia","basta sol fibbia","fermati sul fibbia"})assertTrue(text,SpeechInterruptPolicy.command(text));for(String text:new String[]{"basta","basta così","non basta solphivia","basta sofia","fermati silvia","ho detto basta solphivia ieri","Cosa posso fare per te?"})assertFalse(text,SpeechInterruptPolicy.command(text));}
 @Test public void noInterruptionWhenNotReading(){assertFalse(new SpeechInterruptPolicy().accept("basta sol fibbia",true,1000,"test",false));}
 @Test public void ownSpokenCommandIsSuppressed(){SpeechInterruptPolicy p=new SpeechInterruptPolicy();assertFalse(p.accept("basta sol fibbia",true,1000,"Puoi dire: Basta Solphivia, per fermarmi.",true));assertFalse(p.accept("fermati sul fibbia",true,1000,"Dì fermati Solphivia.",true));}
 @Test public void partialNeedsStability(){SpeechInterruptPolicy p=new SpeechInterruptPolicy();assertFalse(p.accept("basta sol fibbia",false,1000,"una prova",true));assertFalse(p.accept("basta sol fibbia",false,1100,"una prova",true));assertFalse(p.accept("basta sol fibbia",false,1200,"una prova",true));assertTrue(p.accept("basta sol fibbia",false,1300,"una prova",true));}
 @Test public void wrongOrChangedWordsResetStability(){SpeechInterruptPolicy p=new SpeechInterruptPolicy();p.accept("basta sol fibbia",false,1000,"test",true);p.accept("basta sofia",false,1300,"test",true);assertFalse(p.accept("basta sol fibbia",false,1400,"test",true));}
 @Test public void finalPhraseAndCooldown(){SpeechInterruptPolicy p=new SpeechInterruptPolicy();assertTrue(p.accept("basta sol fibbia",true,1000,"test",true));assertFalse(p.accept("basta sol fibbia",true,1200,"test",true));assertTrue(p.accept("basta sol fibbia",true,2600,"test",true));}
}
