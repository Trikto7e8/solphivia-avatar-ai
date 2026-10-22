package it.solphivia.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/** Inline screen reader using the same installed engine and accent preferences as the overlay. */
final class DeviceSpeech {
    private static volatile boolean outputBusy;
    static boolean outputBusy(){return outputBusy;}
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private int generation;
    DeviceSpeech(Context context){this.context=context.getApplicationContext();}
    void stop(){generation++;outputBusy=false;if(tts!=null){tts.stop();tts.shutdown();tts=null;}}
    void speak(String text){
        stop();
        android.content.SharedPreferences prefs=context.getSharedPreferences("pet",0);
        if(!prefs.getBoolean("read_aloud",true))return;
        outputBusy=true;
        int request=generation;
        String engine=prefs.getString("tts_engine","");
        tts=new TextToSpeech(context,status->main.post(()->{
            if(request!=generation || tts==null)return;
            if(status!=TextToSpeech.SUCCESS){outputBusy=false;return;}
            boolean english=prefs.getBoolean("english_accent",false);
            Locale locale=english?Locale.UK:Locale.ITALIAN;
            int result=tts.setLanguage(locale);
            android.speech.tts.Voice best=SpeechVoiceSelection.select(tts,locale,english);
            if(best!=null && tts.setVoice(best)==TextToSpeech.SUCCESS)result=TextToSpeech.LANG_AVAILABLE;
            else if(english){english=false;result=tts.setLanguage(Locale.ITALIAN);}
            if(result<0){outputBusy=false;return;}
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){
                public void onStart(String id){}
                public void onDone(String id){if("inline-final".equals(id) && request==generation)outputBusy=false;}
                public void onError(String id){if(request==generation)outputBusy=false;}
            });
            int rate=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_RATE,100);
            int pitch=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_PITCH,100);
            tts.setSpeechRate(Math.max(0.1f,rate/100f));tts.setPitch(Math.max(0.1f,pitch/100f));
            String spoken=english?ItalianEnglishAccent.respell(text):text;
            if(spoken.isEmpty()){outputBusy=false;return;}
            int limit=TextToSpeech.getMaxSpeechInputLength()-1;
            for(int start=0;start<spoken.length();){
                int end=Math.min(start+limit,spoken.length());if(end<spoken.length()&&Character.isLowSurrogate(spoken.charAt(end)))end--;
                if(tts.speak(spoken.substring(start,end),start==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,null,end==spoken.length()?"inline-final":"inline-"+start)==TextToSpeech.ERROR){outputBusy=false;break;}
                start=end;
            }
        }),engine.isEmpty()?null:engine);
    }
}
