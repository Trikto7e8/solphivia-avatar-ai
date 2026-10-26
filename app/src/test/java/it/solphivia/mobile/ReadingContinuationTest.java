package it.solphivia.mobile;
import org.junit.Test;
import static org.junit.Assert.*;
public class ReadingContinuationTest {
 @Test public void pauseResumesAtReportedWord(){ReadingContinuation.clear();long token=ReadingContinuation.begin("Prima frase. Seconda frase.");ReadingContinuation.progress(token,13);ReadingContinuation.pause();assertEquals("Seconda frase.",ReadingContinuation.take());assertFalse(ReadingContinuation.available());}
 @Test public void lateCallbacksCannotOverwritePausedPosition(){ReadingContinuation.clear();long token=ReadingContinuation.begin("Uno due tre");ReadingContinuation.progress(token,4);ReadingContinuation.pause();ReadingContinuation.progress(token,8);ReadingContinuation.complete(token);assertEquals("due tre",ReadingContinuation.take());}
 @Test public void newAnswerReplacesPreviousReading(){ReadingContinuation.clear();long stale=ReadingContinuation.begin("Vecchia");ReadingContinuation.pause();long current=ReadingContinuation.begin("Nuova risposta");ReadingContinuation.progress(stale,6);ReadingContinuation.complete(stale);ReadingContinuation.pause();assertEquals("Nuova risposta",ReadingContinuation.take());ReadingContinuation.complete(current);assertFalse(ReadingContinuation.available());}
 @Test public void completedAndClosedReadingsCannotResume(){ReadingContinuation.clear();long token=ReadingContinuation.begin("Finita");ReadingContinuation.complete(token);ReadingContinuation.pause();assertFalse(ReadingContinuation.available());ReadingContinuation.begin("In corso");ReadingContinuation.pause();ReadingContinuation.clear();assertEquals("",ReadingContinuation.take());}
}
