package it.solphivia.mobile;

import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.*;
import java.io.File;
import java.util.concurrent.*;

public final class VoiceInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();final TextToSpeech[] holder={null};
        try{
            RevisionChecks.check(getTargetContext().getPackageName().endsWith(".verification"),"isolated package required");
            VoicePreferences.initialize(getTargetContext());
            String engine=getTargetContext().getSharedPreferences("pet",0).getString("tts_engine","");
            CountDownLatch init=new CountDownLatch(1);int[] status={-1};
            runOnMainSync(()->holder[0]=new TextToSpeech(getTargetContext(),code->{status[0]=code;init.countDown();},engine.isEmpty()?null:engine));
            RevisionChecks.check(init.await(20,TimeUnit.SECONDS)&&status[0]==TextToSpeech.SUCCESS,"TTS initialization");
            TextToSpeech tts=holder[0];Voice selected=null;
            for(Voice voice:tts.getVoices())if(voice.getLocale().getLanguage().equals("en")&&!voice.isNetworkConnectionRequired()){
                if(selected==null||voice.getLocale().getCountry().equals("GB"))selected=voice;
            }
            RevisionChecks.check(selected!=null,"offline English voice installed");
            RevisionChecks.check(tts.setVoice(selected)==TextToSpeech.SUCCESS,"offline voice selection");
            CountDownLatch spoken=new CountDownLatch(1);boolean[] success={false};
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){success[0]=true;spoken.countDown();}public void onError(String id){spoken.countDown();}});
            File output=new File(getTargetContext().getExternalFilesDir(null),"english-accent-test.wav");
            int code=tts.synthesizeToFile(ItalianEnglishAccent.respell("Ciao utente, sono Solphivia. Come stai oggi?"),null,output,"test");
            RevisionChecks.check(code==TextToSpeech.SUCCESS&&spoken.await(30,TimeUnit.SECONDS)&&success[0]&&output.length()>44,"offline speech synthesis produces audio");
            result.putString("stream","PASS: offline English TTS engine="+engine+", voice="+selected.getName()+", WAV "+output.length()+" bytes\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally{if(holder[0]!=null)holder[0].shutdown();}
    }
}
