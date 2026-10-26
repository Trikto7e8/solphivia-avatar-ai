package it.solphivia.mobile;

import android.content.*;
import android.app.KeyguardManager;
import android.content.pm.*;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.*;

/** Local user actions; never invoked on model responses, never calls or sends messages. */
final class LocalAppCommands {
    static boolean isWhatsApp(String text){
        DeviceCommand command=DeviceCommand.parse(text);
        return command!=null&&command.kind==DeviceCommand.Kind.APP&&isWhatsAppName(DeviceCommand.normalized(command.value));
    }
    private static boolean isWhatsAppName(String name){return name.matches("whatsapp|whats app|what s app|watsapp");}
    static final class App {
        final String label,packageName;
        App(String label,String packageName){this.label=label;this.packageName=packageName;}
    }
    static List<App> matchingApps(List<App> apps,String name){
        Map<String,App> found=new LinkedHashMap<>();
        for(App app:apps)if(DeviceCommand.normalized(app.label).equals(DeviceCommand.normalized(name)))found.put(app.packageName,app);
        return new ArrayList<>(found.values());
    }
    static Intent mapIntent(DeviceCommand command){
        Uri.Builder url=new Uri.Builder().scheme("https").authority("www.google.com");
        if(command.kind==DeviceCommand.Kind.DIRECTIONS)url.path("maps/dir/").appendQueryParameter("api","1").appendQueryParameter("destination",command.value);
        else url.path("maps/search/").appendQueryParameter("api","1").appendQueryParameter("query",command.value);
        return new Intent(Intent.ACTION_VIEW,url.build());
    }
    private static String start(Context context,Intent intent,String success){
        try{context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return success;}
        catch(ActivityNotFoundException error){return "Non trovo un’app disponibile per questo comando.";}
        catch(SecurityException error){return "Android non ha consentito questa apertura. Puoi usare l’icona dell’app.";}
        catch(RuntimeException error){return "Non riesco ad aprire l’app in questo momento.";}
    }
    static String handle(Context context,String text){
        if(SessionCommands.help(text))return SessionCommands.CLOSE_HELP;
        DeviceCommand command=DeviceCommand.parse(text);
        if(command==null)return null;
        KeyguardManager keyguard=context.getSystemService(KeyguardManager.class);
        if(command.kind!=DeviceCommand.Kind.HELP&&command.kind!=DeviceCommand.Kind.INVALID&&keyguard!=null&&keyguard.isKeyguardLocked())return "Sblocca il telefono e ripeti il comando.";
        switch(command.kind){
            case HELP:return "Puoi fermare la voce con Stop o Alt e riprendere con Riprendi, Prosegui o Ti ascolto. Per richiamarmi di’ Hola Solphivia. Puoi dire: apri Maps, apri il nome di un’app, cerca una farmacia su Maps, portami a Roma, oppure chiama seguito da un numero. Per telefonare confermi dal telefono. I nomi della rubrica non sono ancora supportati. Per chiudermi usa il pulsante ⏻ sotto l’avatar, oppure chiedimi come chiuderti.";
            case INVALID:return command.value.equals("numero")?"Per ora posso preparare una chiamata a un numero, senza cercare nella rubrica. Dettalo cifra per cifra oppure scrivilo; confermerai la chiamata dal telefono.":"Non ho capito il comando. Prova una sola azione, per esempio apri Maps.";
            case MAP_SEARCH:return start(context,mapIntent(command),"Apro la ricerca nelle mappe.");
            case DIRECTIONS:return start(context,mapIntent(command),"Apro il percorso nelle mappe. Controlla la destinazione prima di partire.");
            case DIAL:return start(context,new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",command.value,null)),"Ho preparato il numero sul telefono. Controllalo e premi Chiama per telefonare.");
            default:return openApp(context,command.value);
        }
    }
    private static String openApp(Context context,String requested){
        String name=DeviceCommand.normalized(requested);
        if(name.matches("fotocamera|camera"))return start(context,new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),"Apro la fotocamera.");
        if(name.matches("telefono|tastiera telefonica"))return start(context,new Intent(Intent.ACTION_DIAL),"Apro il telefono.");
        PackageManager pm=context.getPackageManager();
        Intent launch=null;String label=requested;
        if(isWhatsAppName(name)){
            launch=pm.getLaunchIntentForPackage("com.whatsapp");
            if(launch==null)launch=pm.getLaunchIntentForPackage("com.whatsapp.w4b");
            label="WhatsApp";
        }else if(name.matches("maps|google maps|mappe")){
            launch=pm.getLaunchIntentForPackage("com.google.android.apps.maps");label="Maps";
        }else{
            List<App> apps=new ArrayList<>();
            for(ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
                if(info.activityInfo!=null&&!info.activityInfo.packageName.equals(context.getPackageName()))apps.add(new App(info.loadLabel(pm).toString(),info.activityInfo.packageName));
            }
            List<App> matches=matchingApps(apps,name);
            if(matches.size()>1)return "Ci sono più app con quel nome. Usa un nome diverso oppure scegli l’app dalla sua icona.";
            if(matches.size()==1){App app=matches.get(0);launch=pm.getLaunchIntentForPackage(app.packageName);label=app.label;}
        }
        if(launch==null)return "Non trovo questa app: "+requested+". Usa il nome completo mostrato sotto la sua icona.";
        return start(context,launch,"Apro "+label+".");
    }
}
