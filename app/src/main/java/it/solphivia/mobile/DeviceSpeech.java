package it.solphivia.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/** Inline screen reader using the same installed engine and accent preferences as the overlay. */
final class DeviceSpeech {
    private static volatile boolean outputBusy;
    private static volatile DeviceSpeech active;
    private volatile String speakingText="";
    static String spokenText(){return outputBusy&&active!=null?active.speakingText:"";}
    static void interruptAll(){DeviceSpeech current=active;if(current!=null)current.stop();}
    static boolean outputBusy(){return outputBusy;}
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private int generation;
    DeviceSpeech(Context context){this.context=context.getApplicationContext();}
    void stop(){generation++;if(active==this){active=null;outputBusy=false;}speakingText="";if(tts!=null){tts.stop();tts.shutdown();tts=null;}}
    void speak(String text){
        android.content.SharedPreferences prefs=context.getSharedPreferences("pet",0);
        if(!prefs.getBoolean("read_aloud",true)){stop();return;}
        speak(text,prefs.getString("tts_engine",""),prefs.getBoolean("english_accent",false),
                prefs.getBoolean("clear_italian",true),prefs.getInt("voice_rate_percent",100),null);
    }
    void speak(String text,String engine,boolean requestedEnglish,boolean clear,int ratePercent,java.util.function.Consumer<String> feedback){
        interruptAll();stop();active=this;speakingText=text==null?"":text;
        outputBusy=true;
        int request=generation;
        tts=new TextToSpeech(context,status->main.post(()->{
            if(request!=generation || tts==null)return;
            if(status!=TextToSpeech.SUCCESS){outputBusy=false;if(feedback!=null)feedback.accept("Il motore vocale non è disponibile.");return;}
            boolean english=requestedEnglish;
            Locale locale=english?Locale.UK:Locale.ITALIAN;
            android.speech.tts.Voice preferred=tts.getVoice();
            int result=tts.setLanguage(locale);
            android.speech.tts.Voice best=SpeechVoiceSelection.select(tts,locale,english,preferred);
            if(best!=null && tts.setVoice(best)==TextToSpeech.SUCCESS)result=TextToSpeech.LANG_AVAILABLE;
            else if(english){english=false;result=tts.setLanguage(Locale.ITALIAN);}
            if(result<0){outputBusy=false;if(feedback!=null)feedback.accept("Voce non disponibile: controlla i dati vocali Android.");return;}
            if(feedback!=null)feedback.accept(english?"Prova con la voce inglese del motore selezionato.":(requestedEnglish?"Voce inglese offline non disponibile: questa prova usa la voce italiana.":"Prova con la voce italiana selezionata."));
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
                public void onStart(String id){}
                public void onDone(String id){if("inline-final".equals(id) && request==generation)outputBusy=false;}
                public void onError(String id){if(request==generation)outputBusy=false;}
            });
            int rate=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_RATE,100);
            int pitch=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_PITCH,100);
            tts.setSpeechRate(SpeechText.rate(rate,ratePercent));tts.setPitch(Math.max(0.1f,pitch/100f));
            String spoken=SpeechText.prepare(text,english,clear);
            java.util.List<String> chunks=SpeechText.chunks(spoken,Math.max(100,TextToSpeech.getMaxSpeechInputLength()-1));
            if(chunks.isEmpty()){outputBusy=false;return;}
            for(int i=0;i<chunks.size();i++){
                if(tts.speak(chunks.get(i),i==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,null,i==chunks.size()-1?"inline-final":"inline-"+i)==TextToSpeech.ERROR){outputBusy=false;break;}
            }
        }),engine.isEmpty()?null:engine);
    }
}
