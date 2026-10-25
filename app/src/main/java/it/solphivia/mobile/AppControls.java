package it.solphivia.mobile;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Icon;

/** Explicit close and launcher actions. No process killing, data deletion or silent pinning. */
final class AppControls {
    static void close(Context context){
        context.getSharedPreferences("pet",0).edit().putBoolean("overlay_off",true).putBoolean("voice_requested",false).commit();
        context.stopService(new Intent(context,VoiceSessionService.class));
        context.stopService(new Intent(context,PetService.class));
        context.stopService(new Intent(context,OAuthKeepAliveService.class));
        ActivityManager manager=context.getSystemService(ActivityManager.class);
        if(manager!=null)for(ActivityManager.AppTask task:manager.getAppTasks())task.finishAndRemoveTask();
    }
    static String addHomeIcon(Context context){
        ShortcutManager manager=context.getSystemService(ShortcutManager.class);
        if(manager==null||!manager.isRequestPinShortcutSupported())return "Apri l’elenco delle app, tieni premuta Solphivia Avatar AI e trascinala sulla Home.";
        try{
            for(ShortcutInfo pinned:manager.getPinnedShortcuts()){
                Intent target=pinned.getIntent();
                if(pinned.isEnabled()&&target!=null&&target.getComponent()!=null&&target.getComponent().getClassName().equals(MainActivity.class.getName()))return "Il collegamento è già aggiunto: controlla le pagine e le cartelle della Home. L’app è anche nell’elenco delle applicazioni.";
            }
            Intent open=new Intent(context,MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            ShortcutInfo shortcut=new ShortcutInfo.Builder(context,"solphivia-home").setShortLabel("Solphivia").setLongLabel("Apri Solphivia").setIcon(Icon.createWithResource(context,R.mipmap.ic_launcher)).setIntent(open).build();
            return manager.requestPinShortcut(shortcut,null)?"Conferma Aggiungi nella richiesta Android per mettere Solphivia sulla Home.":"Il launcher non ha accettato la richiesta. Puoi trascinare l’icona dall’elenco delle app alla Home.";
        }catch(RuntimeException error){return "Non riesco ad aggiungere l’icona da qui. Tieni premuta Solphivia Avatar AI nell’elenco delle app e trascinala sulla Home.";}
    }
}
