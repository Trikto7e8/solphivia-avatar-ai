package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

/** Pure timing checks on Android; does not open a microphone or touch account/vault data. */
public final class FollowUpInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            check(getTargetContext().getPackageName().equals("it.solphivia.mobile.alpha11.verification"));
            VoiceFollowUpWindow w=new VoiceFollowUpWindow();
            w.replyStarted();w.captureReady(100);check(!w.expired(90000));
            w.replyFinished();w.captureReady(100000);check(!w.expired(104999));check(w.expired(105000));
            w.replyStarted();w.replyFinished();w.captureReady(200000);
            check(w.canRetry(202000));w.captureReady(203000);check(w.expired(205000));
            w.replyStarted();w.replyFinished();w.captureReady(300000);
            check(w.speechStarted(304999));check(!w.expired(320000));
            w.rest();check(!w.canRetry(320001));check(!w.speechStarted(320001));
            VoiceSessionState session=new VoiceSessionState();
            check(session.accept("Quanto fa due piu due").action==VoiceSessionState.SEND);
            session.paused=true;
            check(session.accept("Quanto fa tre piu tre").action==VoiceSessionState.IGNORE);
            check(session.accept("Hola Solphivia").action==VoiceSessionState.SEND);
            check(!session.paused);
            check(session.accept("stop dialogo").action==VoiceSessionState.PAUSE);
            check(session.accept("spegni microfono").action==VoiceSessionState.OFF);
            result.putString("stream","PASS: Android five-second boundary, reply wait excluded, retries preserve deadline, speech may finish beyond deadline, rest/pause/off and wake decisions. No microphone, account or vault accessed.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
    private static void check(boolean ok){if(!ok)throw new AssertionError("follow-up timing or voice policy");}
}
