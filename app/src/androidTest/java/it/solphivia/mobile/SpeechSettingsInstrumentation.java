package it.solphivia.mobile;

import android.app.*;
import android.os.Bundle;
import android.content.SharedPreferences;

/** Future phone check of upgrade defaults; never opens a microphone or synthesizes audio. */
public final class SpeechSettingsInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            if(!getTargetContext().getPackageName().equals("it.solphivia.mobile.v11.verification"))throw new AssertionError("isolated package only");
            SharedPreferences prefs=getTargetContext().getSharedPreferences("pet",0);
            prefs.edit().clear().putBoolean("english_accent",true).putString("tts_engine","synthetic.engine").commit();
            VoicePreferences.initialize(getTargetContext());
            if(prefs.getBoolean("english_accent",true))throw new AssertionError("upgrade should select Italian");
            if(!prefs.getString("tts_engine","").equals("synthetic.engine"))throw new AssertionError("engine choice lost");
            prefs.edit().putBoolean("english_accent",true).commit();
            VoicePreferences.initialize(getTargetContext());
            if(!prefs.getBoolean("english_accent",false))throw new AssertionError("later explicit choice overwritten");
            prefs.edit().clear().commit();VoicePreferences.initialize(getTargetContext());
            if(prefs.getBoolean("english_accent",true))throw new AssertionError("fresh install should select Italian");
            result.putString("stream","PASS: initial upgrade/fresh install prefer Italian, later voice choice and selected engine preserved; no audio or provider request.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}
    }
}
