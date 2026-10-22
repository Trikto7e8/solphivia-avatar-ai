package it.solphivia.mobile;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Isolated tests: never calls a provider, opens WhatsApp, or starts the microphone. */
public final class VoiceCommandInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            Context context=getTargetContext();
            RevisionChecks.check(context.getPackageName().endsWith(".verification"),"isolated tests only");
            for(String text:new String[]{"apri WhatsApp", "Puoi aprire WhatsApp?", "apri whats app per favore", "avvia WhatsApp!"})
                RevisionChecks.check(LocalAppCommands.isWhatsApp(text),"explicit app command");
            for(String text:new String[]{"non aprire whatsapp", "come aprire whatsapp", "apri whatsapp e manda un messaggio", "racconta una storia su whatsapp", "apri whatsapp://send"})
                RevisionChecks.check(!LocalAppCommands.isWhatsApp(text),"no accidental command: "+text);
            AtomicReference<Intent> launch=new AtomicReference<>();
            Context capture=new ContextWrapper(context){@Override public void startActivity(Intent intent){launch.set(intent);}};
            RevisionChecks.check("Apro WhatsApp.".equals(LocalAppCommands.handle(capture,"apri whatsapp")),"installed WhatsApp resolves");
            Intent intent=launch.get();
            RevisionChecks.check(intent!=null&&intent.getComponent()!=null&&"com.whatsapp".equals(intent.getComponent().getPackageName()),"opens only launcher");
            RevisionChecks.check(intent.getData()==null&&!intent.hasExtra(Intent.EXTRA_TEXT),"no message content or recipient");
            JSONArray original=new JSONArray().put(new JSONObject().put("role","user").put("content","test").put("attachments",new JSONArray().put(new JSONObject().put("filename","test.txt"))));
            RevisionChecks.check(!OpenAiChatClient.requestHistory(original,true).getJSONObject(0).has("attachments"),"voice excludes file references");
            RevisionChecks.check(original.getJSONObject(0).has("attachments")&&OpenAiChatClient.requestHistory(original,false).getJSONObject(0).has("attachments"),"original and typed references preserved");
            final int[] scheduled={0};
            ExecutorService captureWorker=new AbstractExecutorService(){
                public void shutdown(){} public List<Runnable> shutdownNow(){return Collections.emptyList();}
                public boolean isShutdown(){return false;} public boolean isTerminated(){return false;}
                public boolean awaitTermination(long timeout,TimeUnit unit){return false;}
                public void execute(Runnable task){scheduled[0]++;}
            };
            runOnMainSync(()->OpenAiChatClient.respond(context,captureWorker,original,new JSONArray(),true,new OpenAiChatClient.ChatCallback(){
                public void onSuccess(String text,String mood,String motion,String mode){throw new AssertionError("no network in tests");}
                public void onError(String message){throw new AssertionError(message);}
            }));
            RevisionChecks.check(scheduled[0]==1,"voice schedules request without manual memory consent");
            ConsentChecks.run(this);
            result.putString("stream","PASS: explicit WhatsApp launcher without messaging; voice sends automatically without file references; typed memory consent remains single-use and cancellable. No provider request, WhatsApp launch, or microphone used.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
    }
}
