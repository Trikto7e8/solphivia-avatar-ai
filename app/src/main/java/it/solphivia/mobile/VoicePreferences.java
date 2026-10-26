package it.solphivia.mobile;

import android.content.Context;

final class VoicePreferences {
    static void initialize(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("pet",0);
        android.content.SharedPreferences.Editor edit=p.edit();
        // Explicit alpha12 choice: prefer natural Italian once, then respect later user choices.
        if(p.getInt("italian_voice_revision",0)<1){
            edit.putBoolean("english_accent",false).putInt("italian_voice_revision",1);
        }
        // Do not select a different installed engine or overwrite the user's voice choice.
        if(!p.contains("tts_engine"))edit.putString("tts_engine","");
        edit.apply();
    }
}
