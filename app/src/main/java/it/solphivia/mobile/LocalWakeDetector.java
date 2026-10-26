package it.solphivia.mobile;

import android.content.Context;
import android.media.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.LibVosk;
import org.json.JSONObject;
import java.io.*;

/** Continuous Italian recognition in memory; waiting audio never leaves the device. */
final class LocalWakeDetector implements AutoCloseable {
    private static final java.util.concurrent.ExecutorService CAPTURE=java.util.concurrent.Executors.newSingleThreadExecutor(r->new Thread(r,"solphivia-local-wake"));
    private static Model sessionModel;
    static void releaseSessionModel(){CAPTURE.execute(()->{if(sessionModel!=null){sessionModel.close();sessionModel=null;}});}
    interface Listener {void wake();void failed(String reason);void samples(long count);default void interrupted(){} default void echo(boolean available,boolean enabled){}}
    private final Context context;
    private final Listener listener;
    private volatile boolean running;
    private volatile boolean suspended;
    private volatile boolean interruptMode;
    private volatile String spoken="";
    private final SpeechInterruptPolicy interruptPolicy=new SpeechInterruptPolicy();
    private volatile AudioRecord recorder;
    LocalWakeDetector(Context context,Listener listener){this.context=context.getApplicationContext();this.listener=listener;}
    static synchronized Model model(Context context)throws IOException{
        LibVosk.setLogLevel(org.vosk.LogLevel.WARNINGS);
        File root=new File(context.getNoBackupFilesDir(),"wake-it-0.22");
        File ready=new File(root,"complete-v1");
        if(!ready.exists()){
            copyAssets(context,"wake-it",root);
            try(FileOutputStream out=new FileOutputStream(ready)){out.write(1);}
        }
        return new Model(root.getAbsolutePath());
    }
    private static void copyAssets(Context context,String path,File target)throws IOException{
        String[] names=context.getAssets().list(path);
        if(names!=null&&names.length>0){
            if(!target.isDirectory()&&!target.mkdirs())throw new IOException("Model directory unavailable");
            for(String name:names)copyAssets(context,path+"/"+name,new File(target,name));
        }else try(InputStream in=context.getAssets().open(path);OutputStream out=new FileOutputStream(target)){
            byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
        }
    }
    static String text(String json){try{return new JSONObject(json).optString("text","");}catch(Exception ignored){return "";}}
    static Recognizer recognizer(Model model,float rate,String learned)throws IOException{
        org.json.JSONArray phrases=new org.json.JSONArray();
        // The Italian dictionary spells the new name with existing, similar sounds.
        // Keep confusable names and an unknown-speech path so unrelated speech can be rejected.
        for(String phrase:new String[]{"o la sol fibbia","o la sul fibbia","o la sua figlia",
                "o la sofia","ciao silvia","ho scattato un selfie","[unk]"})phrases.put(phrase);
        if(learned!=null)for(String phrase:learned.split("\n"))if(!phrase.isEmpty())phrases.put(phrase);
        return new Recognizer(model,rate,phrases.toString());
    }
    static String partial(String json){try{return new JSONObject(json).optString("partial","");}catch(Exception ignored){return "";}}
    static boolean matches(String text,String learned){
        String value=VoicePhrases.normalized(text);
        if(value.matches("(?:hola|ola|ciao|ehi|hey) (?:solphivia|solfivia)"))return true;
        // Whole-utterance Italian ASR renderings observed on synthetic greetings.
        if(value.equals("o la sul fibbia")||value.equals("o la sol fibbia"))return true;
        if(learned!=null)for(String example:learned.split("\n"))if(!example.isEmpty()&&value.equals(example))return true;
        return false;
    }
    void start(){running=true;CAPTURE.execute(this::capture);}
    void suspend(boolean value){suspended=value;}
    void interruptDuring(String text){spoken=text==null?"":text;interruptMode=true;suspended=false;}
    void waitForWake(){interruptMode=false;suspended=false;}
    static Recognizer interruptRecognizer(Model model,float rate)throws IOException{
        return new Recognizer(model,rate,"[\"basta sol fibbia\",\"basta sul fibbia\",\"fermati sol fibbia\",\"fermati sul fibbia\",\"basta sofia\",\"fermati silvia\",\"basta cosi\",\"[unk]\"]");
    }
    private void capture(){
        Model model=null;Recognizer decoder=null;AudioRecord input=null;
        boolean detected=false,interrupted=false;String failure=null;
        android.media.audiofx.AcousticEchoCanceler echo=null;
        try{
            if(!running)return;
            String learned=context.getSharedPreferences("pet",0).getString("wake_italian_phrases","");
            if(sessionModel==null)sessionModel=model(context);model=sessionModel;
            boolean mode=interruptMode;decoder=mode?interruptRecognizer(model,16000):recognizer(model,16000,learned);
            if(context.checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microfono non autorizzato");
            int size=Math.max(6400,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
            input=new AudioRecord(context.getSharedPreferences("pet",0).getBoolean("voice_interrupt",true)?MediaRecorder.AudioSource.VOICE_COMMUNICATION:MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
            recorder=input;if(!running)return;
            if(input.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microfono locale non disponibile");
            boolean available=android.media.audiofx.AcousticEchoCanceler.isAvailable(),enabled=false;
            if(available)try{echo=android.media.audiofx.AcousticEchoCanceler.create(input.getAudioSessionId());if(echo!=null)enabled=echo.setEnabled(true)==android.media.audiofx.AudioEffect.SUCCESS&&echo.getEnabled();}catch(RuntimeException ignored){}
            listener.echo(available,enabled);
            input.startRecording();short[] pcm=new short[1600];long total=0;int utterance=0;boolean wasSuspended=false;
            while(running){
                int n=input.read(pcm,0,pcm.length);if(!running)break;if(n<0)throw new IOException("Microphone read failed");if(n==0)continue;
                total+=n;utterance+=n;
                if(total%32000<n)listener.samples(total);
                boolean nextMode=interruptMode;
                if(nextMode!=mode){Recognizer previous=decoder;decoder=null;previous.close();mode=nextMode;decoder=mode?interruptRecognizer(model,16000):recognizer(model,16000,learned);interruptPolicy.reset();utterance=0;}
                boolean paused=suspended;
                if(paused!=wasSuspended){decoder.reset();utterance=0;wasSuspended=paused;}
                if(paused){utterance=0;continue;}
                boolean finalResult=decoder.acceptWaveForm(pcm,n);
                if(mode){
                    String phrase=finalResult?text(decoder.getResult()):partial(decoder.getPartialResult());
                    if(interruptPolicy.accept(phrase,finalResult,android.os.SystemClock.elapsedRealtime(),spoken,interruptMode)){interrupted=true;break;}
                }else if(finalResult&&matches(text(decoder.getResult()),learned)){detected=true;break;}
                if(finalResult)utterance=0;
                if(utterance>320000){decoder.reset();utterance=0;}
            }
        }catch(Throwable error){if(running)failure=error.getClass().getSimpleName();}
        finally{
            recorder=null;
            if(input!=null){try{input.stop();}catch(RuntimeException ignored){}input.release();}
            if(echo!=null)echo.release();
            if(decoder!=null)decoder.close();
        }
        if(running){if(interrupted)listener.interrupted();else if(detected)listener.wake();else if(failure!=null)listener.failed(failure);}
        running=false;
    }
    public void close(){running=false;AudioRecord input=recorder;if(input!=null)try{input.stop();}catch(RuntimeException ignored){}}
}
