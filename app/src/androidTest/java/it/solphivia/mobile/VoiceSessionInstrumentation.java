package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import java.util.Arrays;

public final class VoiceSessionInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            RevisionChecks.check(getTargetContext().getPackageName().endsWith(".verification"),"isolated tests only");
            RevisionChecks.check(VoicePhrases.greeting("Hola Solphivia!"),"named greeting");
            RevisionChecks.check(VoicePhrases.greeting("Hola Solphivia?"),"question punctuation greeting");
            RevisionChecks.check(VoicePhrases.greeting("o la sua figlia"),"observed short homophone");
            RevisionChecks.check(!VoicePhrases.greeting("Ho incontrato la sua figlia ieri"),"ordinary sentence unchanged");
            RevisionChecks.check(VoicePhrases.wakeRequest("o la sua figlia studia matematica")==null,"homophone not rewritten inside ordinary speech");
            RevisionChecks.check("Hola Solphivia".equals(VoicePhrases.best(Arrays.asList("ola solfivia","Hola Solphivia"))),"alternative recognition greeting");
            VoiceSessionState session=new VoiceSessionState();
            RevisionChecks.check(session.accept("Raccontami una storia").action==VoiceSessionState.SEND,"active conversation accepts speech");
            RevisionChecks.check(session.accept("stop dialogo").action==VoiceSessionState.PAUSE && session.paused,"pause keeps wake state");
            RevisionChecks.check(session.accept("Questa frase non va inviata").action==VoiceSessionState.IGNORE,"paused speech not sent");
            VoiceSessionState.Decision wake=session.accept("Hola Solphivia, scrivi x = 3 + 2.");
            RevisionChecks.check(wake.action==VoiceSessionState.SEND && !session.paused && wake.text.equals("scrivi x = 3 + 2."),"wake resumes and preserves punctuation");
            RevisionChecks.check(session.accept("stop").action==VoiceSessionState.PAUSE,"spoken stop pauses");
            RevisionChecks.check(session.accept("o la sua figlia").text.equals("Hola Solphivia"),"homophone wakes paused session");
            RevisionChecks.check(session.accept("spegni microfono").action==VoiceSessionState.OFF,"microphone off is local");
            RevisionChecks.check(session.accept(" ").action==VoiceSessionState.IGNORE,"empty results ignored");
            RevisionChecks.check(!VoiceSessionService.running(),"tests never start recording");
            result.putString("stream","PASS: greeting and narrow ASR aliases, paused speech ignored, wake resumes dialog, request punctuation preserved, local pause/off decisions; no microphone opened\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
    }
}
