package it.solphivia.mobile;

import android.content.*;
import android.net.Uri;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

final class DeviceCommandChecks {
    private static void check(boolean value,String message){RevisionChecks.check(value,message);}
    static void run(Context base){
        for(String text:new String[]{"non aprire Maps","come aprire Maps?","mi spieghi come chiamare?","dice: apri WhatsApp","apri Maps\nchiama 123"})check(DeviceCommand.parse(text)==null,"not an explicit single command");
        DeviceCommand app=DeviceCommand.parse("Puoi aprire l’app Google Foto per favore?");
        check(app.kind==DeviceCommand.Kind.APP&&app.value.equals("Google Foto"),"polite named app preserves label");
        List<LocalAppCommands.App> apps=Arrays.asList(new LocalAppCommands.App("Caffè","a"),new LocalAppCommands.App("Caffè","a"),new LocalAppCommands.App("Caffè","b"),new LocalAppCommands.App("Caffè Pro","c"));
        check(LocalAppCommands.matchingApps(apps,"caffe").size()==2,"distinct packages ambiguous, duplicate icons deduplicated");
        check(LocalAppCommands.matchingApps(apps,"caffe p").isEmpty(),"no fuzzy app launch");
        AtomicReference<Intent> opened=new AtomicReference<>();
        Context capture=new ContextWrapper(base){@Override public void startActivity(Intent intent){opened.set(intent);}};
        LocalAppCommands.handle(capture,"cerca Caffè & Bar, Roma su Maps");
        Uri search=opened.get().getData();
        check("https".equals(search.getScheme())&&"www.google.com".equals(search.getHost()),"fixed maps destination");
        check("Caffè & Bar, Roma".equals(search.getQueryParameter("query"))&&"1".equals(search.getQueryParameter("api")),"map query safely encoded");
        LocalAppCommands.handle(capture,"portami a Roma Termini");
        check("Roma Termini".equals(opened.get().getData().getQueryParameter("destination")),"directions destination");
        check(DeviceCommand.parse("cerca una farmacia vicino a me").kind==DeviceCommand.Kind.MAP_SEARCH,"nearby search");
        LocalAppCommands.handle(capture,"chiama il numero +1 (202) 555-0100");
        check(Intent.ACTION_DIAL.equals(opened.get().getAction())&&"+12025550100".equals(opened.get().getData().getSchemeSpecificPart()),"dial only, no direct call");
        check("0123456789".equals(DeviceCommand.phoneNumber("zero uno due tre quattro cinque sei sette otto nove")),"spoken digits preserve leading zero");
        for(String number:new String[]{"Anna","12#34","*123#","123;456","123,456","123?x=456","12","1234567890123456","123 e chiama 456"})check(DeviceCommand.phoneNumber(number)==null,"invalid number rejected");
        opened.set(null);
        check(LocalAppCommands.handle(capture,"chiama Anna").contains("rubrica")&&opened.get()==null,"contact names stay local without launch");
        check(LocalAppCommands.handle(capture,"apri whatsapp://send").contains("Non ho capito")&&opened.get()==null,"no arbitrary URI execution");
        Context missing=new ContextWrapper(base){@Override public void startActivity(Intent intent){throw new ActivityNotFoundException();}};
        check(LocalAppCommands.handle(missing,"portami a Roma").contains("Non trovo"),"missing handler feedback");
        Context blocked=new ContextWrapper(base){@Override public void startActivity(Intent intent){throw new SecurityException();}};
        check(LocalAppCommands.handle(blocked,"chiama 2025550100").contains("Android non ha consentito"),"blocked launch feedback");
        check(LocalAppCommands.handle(capture,"comandi").contains("Maps"),"discoverable command help");
        // Verify actual Android launcher visibility/resolution, without opening the selected app.
        List<LocalAppCommands.App> installed=new ArrayList<>();
        for(android.content.pm.ResolveInfo info:base.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
            if(info.activityInfo!=null&&!info.activityInfo.packageName.equals(base.getPackageName()))installed.add(new LocalAppCommands.App(info.loadLabel(base.getPackageManager()).toString(),info.activityInfo.packageName));
        }
        boolean genericChecked=false;
        for(LocalAppCommands.App candidate:installed){
            if(!candidate.label.matches("[A-Za-zÀ-ÿ ]+")||candidate.label.toLowerCase(Locale.ITALIAN).matches("maps|google maps|mappe|camera|fotocamera|telefono|whatsapp"))continue;
            if(LocalAppCommands.matchingApps(installed,candidate.label).size()!=1)continue;
            opened.set(null);LocalAppCommands.handle(capture,"apri "+candidate.label);
            check(opened.get()!=null&&opened.get().getComponent()!=null&&candidate.packageName.equals(opened.get().getComponent().getPackageName()),"generic launcher resolution uses exact installed app");
            genericChecked=true;break;
        }
        check(genericChecked,"at least one generic installed app tested through launcher query");

    }
}
