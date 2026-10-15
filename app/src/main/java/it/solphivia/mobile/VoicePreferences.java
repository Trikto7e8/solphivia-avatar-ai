package it.solphivia.mobile;

import android.content.Context;

final class VoicePreferences {
    static void initialize(Context context) {
        android.content.SharedPreferences p=context.getSharedPreferences("pet",0);
        android.content.SharedPreferences.Editor edit=p.edit();
        if(!p.contains("english_accent"))edit.putBoolean("english_accent",true);
        if(!p.contains("tts_engine")) {
            try {context.getPackageManager().getPackageInfo("com.reecedunn.espeak",0);edit.putString("tts_engine","com.reecedunn.espeak");}
            catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        }
        edit.apply();
    }
}
