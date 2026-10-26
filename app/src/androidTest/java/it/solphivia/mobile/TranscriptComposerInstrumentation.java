package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;

/** Synthetic EditText checks; no real microphone, provider request or stored user data. */
public final class TranscriptComposerInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            check(getTargetContext().getPackageName().equals("it.solphivia.mobile.v11.verification"));
            check(VoicePhrases.recognitionIntent().getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false));
            runOnMainSync(()->{
                EditText field=new EditText(getTargetContext());
                VoiceTranscriptComposer p=new VoiceTranscriptComposer(field);
                p.show("Preferisco");check(field.getText().toString().equals("Preferisco"));
                p.show("Preferisco la seconda");check(field.getText().toString().equals("Preferisco la seconda"));
                p.show("Preferisco la seconda opzione.");check(field.getSelectionStart()==field.length());
                check(p.canSubmit());p.finish();check(!p.canSubmit());
                field.setText("");p=new VoiceTranscriptComposer(field);p.show("Frase incompleta");p.cancel();check(field.length()==0);
                field.setText("Bozza scritta");p=new VoiceTranscriptComposer(field);p.show("Voce");check(!p.canSubmit());p.cancel();check(field.getText().toString().equals("Bozza scritta"));
                field.setText("");p=new VoiceTranscriptComposer(field);p.show("Sì");field.setText("No, preferisco altro");p.show("Sì grazie");check(!p.canSubmit());p.cancel();check(field.getText().toString().equals("No, preferisco altro"));
                VoiceSessionState session=new VoiceSessionState();
                for(String text:new String[]{"Sì, grazie.","No, preferisco la seconda.","Questa è una frase di prova.","Perché me lo chiedi?"}){
                    VoiceSessionState.Decision d=session.accept(text);check(d.action==VoiceSessionState.SEND);check(d.text.equals(text));
                }
                try{
                    field.setText("");
                    VoiceSessionService service=new VoiceSessionService();
                    java.lang.reflect.Field listening=VoiceSessionService.class.getDeclaredField("listening");listening.setAccessible(true);listening.setBoolean(service,true);
                    java.lang.reflect.Field preview=VoiceSessionService.class.getDeclaredField("transcript");preview.setAccessible(true);preview.set(service,new VoiceTranscriptComposer(field));
                    Bundle partial=new Bundle();partial.putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION,new java.util.ArrayList<>(java.util.Arrays.asList("Preferisco la seconda")));
                    service.onPartialResults(partial);check(field.getText().toString().equals("Preferisco la seconda"));
                    listening.setBoolean(service,false);
                    partial.putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION,new java.util.ArrayList<>(java.util.Arrays.asList("Risultato tardivo")));
                    service.onPartialResults(partial);check(field.getText().toString().equals("Preferisco la seconda"));
                }catch(ReflectiveOperationException e){throw new AssertionError(e);}
            });
            VoiceFollowUpWindow window=new VoiceFollowUpWindow();window.replyStarted();window.replyFinished();window.captureReady(10000);
            check(!window.expired(14999));check(window.expired(15000));
            result.putString("stream","PASS: partial-results enabled; synthetic EditText partial/final transcript and cursor; actual service partial callback updates field and ignores late results; cancelled preview cleanup; existing draft and manual edits preserved; natural answers forwarded unchanged; five-second window retained. No live ASR/network/account/vault used.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
    }
    private static void check(boolean ok){if(!ok)throw new AssertionError("transcript UI or natural reply policy");}
}
