package it.solphivia.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/** Inline screen reader using the same installed engine and accent preferences as the overlay. */
final class DeviceSpeech {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private int generation;
    DeviceSpeech(Context context){this.context=context.getApplicationContext();}
    void stop(){generation++;if(tts!=null){tts.stop();tts.shutdown();tts=null;}}
    void speak(String text){
        stop();
        android.content.SharedPreferences prefs=context.getSharedPreferences("pet",0);
        if(!prefs.getBoolean("read_aloud",true))return;
        int request=generation;
        String engine=prefs.getString("tts_engine","");
        tts=new TextToSpeech(context,status->main.post(()->{
            if(request!=generation || tts==null || status!=TextToSpeech.SUCCESS)return;
            boolean english=prefs.getBoolean("english_accent",false);
            Locale locale=english?Locale.UK:Locale.ITALIAN;
            int result=tts.setLanguage(locale);
            android.speech.tts.Voice best=null;int score=Integer.MIN_VALUE;
            if(tts.getVoices()!=null)for(android.speech.tts.Voice voice:tts.getVoices()){
                if(!locale.getLanguage().equals(voice.getLocale().getLanguage()) || (english&&voice.isNetworkConnectionRequired()))continue;
                int value=voice.getQuality()+(locale.getCountry().equals(voice.getLocale().getCountry())?1000:0);
                if(value>score){best=voice;score=value;}
            }
            if(best!=null && tts.setVoice(best)==TextToSpeech.SUCCESS)result=TextToSpeech.LANG_AVAILABLE;
            else if(english){english=false;result=tts.setLanguage(Locale.ITALIAN);}
            if(result<0)return;
            int rate=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_RATE,100);
            int pitch=android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.TTS_DEFAULT_PITCH,100);
            tts.setSpeechRate(Math.max(0.1f,rate/100f));tts.setPitch(Math.max(0.1f,pitch/100f));
            String spoken=english?ItalianEnglishAccent.respell(text):text;
            int limit=TextToSpeech.getMaxSpeechInputLength()-1;
            for(int start=0;start<spoken.length();){
                int end=Math.min(start+limit,spoken.length());if(end<spoken.length()&&Character.isLowSurrogate(spoken.charAt(end)))end--;
                if(tts.speak(spoken.substring(start,end),start==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,null,"inline-"+start)==TextToSpeech.ERROR)break;
                start=end;
            }
        }),engine.isEmpty()?null:engine);
    }
}
