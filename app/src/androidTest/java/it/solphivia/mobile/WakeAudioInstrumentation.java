package it.solphivia.mobile;

import android.app.*;
import android.os.Bundle;
import android.speech.tts.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

/** Actual waveform-to-keyword test, with synthetic speech only; no microphone capture. */
public final class WakeAudioInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private int detect(File wav,Model model,float leadingSeconds)throws Exception{
        byte[] bytes=java.nio.file.Files.readAllBytes(wav.toPath());ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int rate=0,offset=0,length=0;
        for(int p=12;p+8<=bytes.length;){int size=b.getInt(p+4);String id=new String(bytes,p,4,java.nio.charset.StandardCharsets.US_ASCII);
            if(id.equals("fmt ")){RevisionChecks.check(b.getShort(p+8)==1&&b.getShort(p+10)==1&&b.getShort(p+22)==16,"mono PCM16");rate=b.getInt(p+12);}
            if(id.equals("data")){offset=p+8;length=Math.min(size,bytes.length-offset);break;}p+=8+size+(size%2);}
        RevisionChecks.check(rate>0&&length>0,"WAV header");int found=0;
        try(Recognizer decoder=LocalWakeDetector.recognizer(model,rate,"")){
            int lead=(int)(leadingSeconds*rate),speech=length/2;
            for(int pos=0;pos<lead+speech+rate*2;pos+=1600){
                int n=Math.min(1600,lead+speech+rate*2-pos);short[] samples=new short[n];
                for(int j=0;j<n;j++){int k=pos+j-lead;if(k>=0&&k<speech)samples[j]=b.getShort(offset+2*k);}
                if(decoder.acceptWaveForm(samples,n)){
                    String text=LocalWakeDetector.text(decoder.getResult());
                    if(LocalWakeDetector.matches(text,""))found++;
                    Bundle report=new Bundle();report.putString("stream","SYNTHETIC ITALIAN "+wav.getName()+": "+text+"\n");sendStatus(0,report);
                }
            }
            if(LocalWakeDetector.matches(LocalWakeDetector.text(decoder.getFinalResult()),""))found++;
        }
        return found;
    }
    @Override public void onStart(){Bundle result=new Bundle();TextToSpeech[] tts={null};Model detector=null;
        try{
            RevisionChecks.check(getTargetContext().getPackageName().endsWith(".verification"),"isolated test only");
            CountDownLatch init=new CountDownLatch(1);int[] status={-1};
            runOnMainSync(()->tts[0]=new TextToSpeech(getTargetContext(),code->{status[0]=code;init.countDown();},"com.google.android.tts"));
            RevisionChecks.check(init.await(15,TimeUnit.SECONDS)&&status[0]==TextToSpeech.SUCCESS,"TTS ready");
            detector=LocalWakeDetector.model(getTargetContext());int positive=0,negative=0,positiveTrials=0;
            StringBuilder details=new StringBuilder();
            int iteration=0;
            for(String text:new String[]{"Hola Solphivia","Hola Solphivia","Hola Solphivia","Hola Solphivia","Oggi guardiamo insieme il mare e poi prepariamo un caffè.","Ho scattato un selfie.","Hola Sofia.","Ciao Silvia.","Cosa posso fare per te?","Solphivia.","Hola.","Spegni il microfono.","Ho incontrato sua figlia ieri.","La sua figlia sta studiando.","Paola è andata al mare.","Oggi è una bella giornata.","Alza il volume.","Hola Sofia, come stai?", "Ciao Silvia, raccontami una storia."}){
                RevisionChecks.check(tts[0].setLanguage(java.util.Locale.ITALIAN)>=0,"Italian speech available");
                tts[0].setSpeechRate(iteration==1?0.9f:iteration==2?1.1f:1f);
                CountDownLatch done=new CountDownLatch(1);boolean[] ok={false};tts[0].setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){ok[0]=true;done.countDown();}public void onError(String id){done.countDown();}});
                File wav=new File(getTargetContext().getExternalFilesDir(null),"wake-fixture-"+iteration+".wav");
                RevisionChecks.check(tts[0].synthesizeToFile(text,null,wav,"sample")==TextToSpeech.SUCCESS&&done.await(20,TimeUnit.SECONDS)&&ok[0],"synthetic sample ready");
                int count=detect(wav,detector,iteration==1?1.3f:iteration==2?1.45f:iteration==3?5f:0f);if(iteration>0&&iteration<4){positiveTrials++;if(count>0)positive++;}else if(iteration>=4)negative+=count;iteration++;
            }
            result.putString("stream","LOCAL KWS AUDIO: positive="+positive+"/"+positiveTrials+", negative="+negative+"\n");
            RevisionChecks.check(positive==positiveTrials,"Italian greeting detected at 0.9x, 1.1x and 1x after silence");RevisionChecks.check(negative==0,"negative sentences do not wake");
            result.putString("stream",result.getString("stream")+"PASS: real local acoustic detection on synthetic Italian greeting; negative speech ignored; no microphone or user voice captured\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",result.getString("stream","")+"FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally{if(tts[0]!=null)tts[0].shutdown();if(detector!=null)detector.close();}
    }
}
