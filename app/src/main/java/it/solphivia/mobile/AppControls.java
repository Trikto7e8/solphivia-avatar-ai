package it.solphivia.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Icon;

/** Explicit close and launcher actions. No process killing, data deletion or silent pinning. */
final class AppControls {
    static void close(Context context){
        ReadingContinuation.clear();
        context.getSharedPreferences("pet",0).edit().putBoolean("overlay_off",true).putBoolean("voice_requested",false).commit();
        context.stopService(new Intent(context,VoiceSessionService.class));
        context.stopService(new Intent(context,PetService.class));
        context.stopService(new Intent(context,OAuthKeepAliveService.class));
        ActivityManager manager=context.getSystemService(ActivityManager.class);
        if(manager!=null)for(ActivityManager.AppTask task:manager.getAppTasks())task.finishAndRemoveTask();
    }
    static void refreshHomeIcons(Context context){
        ShortcutManager manager=context.getSystemService(ShortcutManager.class);if(manager==null)return;
        try{
            java.util.List<ShortcutInfo> updates=new java.util.ArrayList<>();
            for(ShortcutInfo pinned:manager.getPinnedShortcuts()){
                Intent target=pinned.getIntent();
                if(pinned.isEnabled()&&target!=null&&target.getComponent()!=null&&target.getComponent().getClassName().equals(MainActivity.class.getName()))
                    updates.add(new ShortcutInfo.Builder(context,pinned.getId()).setShortLabel("Solphivia IA").setLongLabel("Apri Solphivia IA").setIcon(Icon.createWithResource(context,R.mipmap.ic_launcher)).setIntent(target).build());
            }
            if(!updates.isEmpty())manager.updateShortcuts(updates);
        }catch(RuntimeException ignored){}
    }
    static String addHomeIcon(Context context){
        return "Per avere una sola Solphivia nell’icona, usa l’icona normale dell’app:\n\n1. Dalla Home scorri verso l’alto per aprire l’elenco delle app.\n2. Tieni premuta Solphivia IA e trascinala sulla Home.\n3. Tieni premuto il vecchio collegamento con il piccolo badge e scegli Rimuovi.\n\nLe scorciatoie Android possono mostrare una seconda piccola icona: non ne creiamo altre.";
    }
}
