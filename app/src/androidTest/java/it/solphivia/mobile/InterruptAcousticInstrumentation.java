package it.solphivia.mobile;

import android.app.*;
import android.os.Bundle;
import android.speech.tts.*;
import org.vosk.*;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/** Synthetic files only, no microphone or user recording. Fixtures deleted after this test. */
public final class InterruptAcousticInstrumentation extends Instrumentation {
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private boolean detect(File wav,Model model,String spoken)throws Exception{
  byte[] bytes=java.nio.file.Files.readAllBytes(wav.toPath());ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  int rate=0,offset=0,length=0;
  for(int p=12;p+8<=bytes.length;){int size=b.getInt(p+4);String id=new String(bytes,p,4,java.nio.charset.StandardCharsets.US_ASCII);if(id.equals("fmt ")){if(b.getShort(p+8)!=1||b.getShort(p+10)!=1||b.getShort(p+22)!=16)throw new AssertionError("PCM16 mono required");rate=b.getInt(p+12);}if(id.equals("data")){offset=p+8;length=Math.min(size,bytes.length-offset);break;}p+=8+size+(size%2);}
  if(rate<=0||length==0)throw new AssertionError("Invalid synthetic WAV");
  SpeechInterruptPolicy policy=new SpeechInterruptPolicy();
  try(Recognizer decoder=LocalWakeDetector.interruptRecognizer(model,rate)){
   int total=length/2+rate;
   for(int p=0;p<total;p+=1600){int n=Math.min(1600,total-p);short[] pcm=new short[n];for(int j=0;j<n;j++)if(p+j<length/2)pcm[j]=b.getShort(offset+2*(p+j));boolean end=decoder.acceptWaveForm(pcm,n);String value=end?LocalWakeDetector.text(decoder.getResult()):LocalWakeDetector.partial(decoder.getPartialResult());if(policy.accept(value,end,10000+(p+n)*1000L/rate,spoken,true))return true;}
   String value=LocalWakeDetector.text(decoder.getFinalResult());Bundle report=new Bundle();report.putString("stream","Synthetic final: "+value+"\n");sendStatus(0,report);
   return policy.accept(value,true,10000+total*1000L/rate,spoken,true);
  }
 }
 @Override public void onStart(){Bundle result=new Bundle();TextToSpeech[] tts={null};Model model=null;List<File> fixtures=new ArrayList<>();
  try{
   if(!getTargetContext().getPackageName().equals("it.solphivia.mobile.v11.verification"))throw new AssertionError("isolated only");
   CountDownLatch ready=new CountDownLatch(1);int[] status={-1};runOnMainSync(()->tts[0]=new TextToSpeech(getTargetContext(),code->{status[0]=code;ready.countDown();},"com.google.android.tts"));
   if(!ready.await(20,TimeUnit.SECONDS)||status[0]!=TextToSpeech.SUCCESS)throw new AssertionError("TTS unavailable");
   if(tts[0].setLanguage(Locale.ITALIAN)<0)throw new AssertionError("Italian voice unavailable");
   Voice voice=SpeechVoiceSelection.select(tts[0],Locale.ITALIAN,true);if(voice==null||tts[0].setVoice(voice)!=TextToSpeech.SUCCESS)throw new AssertionError("Offline Italian voice required");
   model=LocalWakeDetector.model(getTargetContext());
   String[] samples={"Basta Solphivia","Fermati Solphivia","Basta Solphivia","Cosa posso fare per te?","Oggi possiamo parlare di idee e progetti.","Basta così.","Basta Sofia.","Fermati Silvia.","Sono qui per aiutarti, continua pure.","Puoi aprire Maps e cercare una farmacia."};
   int positive=0,negative=0;
   for(int i=0;i<samples.length;i++){
    CountDownLatch done=new CountDownLatch(1);boolean[] ok={false};tts[0].setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){ok[0]=true;done.countDown();}public void onError(String id){done.countDown();}});
    File file=new File(getTargetContext().getCacheDir(),"interrupt-synthetic-"+i+".wav");fixtures.add(file);
    if(tts[0].synthesizeToFile(samples[i],null,file,"fixture")!=TextToSpeech.SUCCESS||!done.await(25,TimeUnit.SECONDS)||!ok[0])throw new AssertionError("Synthetic fixture failed");
    boolean hit=detect(file,model,i==2?"Per interrompermi puoi dire Basta Solphivia.":"Una risposta di prova da leggere.");
    Bundle report=new Bundle();report.putString("stream","Synthetic sample "+i+": hit="+hit+"\n");sendStatus(0,report);
    if(i<2&&hit)positive++;if(i>=2&&hit)negative++;
   }
   if(positive!=2||negative!=0)throw new AssertionError("Acoustic counts: positives="+positive+"/2, negatives="+negative);
   result.putString("stream","PASS: 2/2 synthetic Italian interruption phrases, 0/8 negative/echo-text trials; no microphone/user audio. Not a live echo-cancellation test.\n");finish(Activity.RESULT_OK,result);
  }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
  finally{if(tts[0]!=null)tts[0].shutdown();if(model!=null)model.close();for(File f:fixtures)f.delete();}
 }
}
