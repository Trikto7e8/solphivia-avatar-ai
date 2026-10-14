package it.solphivia.mobile;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

final class RevisionChecks {
    static String run(Instrumentation test) throws Exception {
        Context context=test.getTargetContext();
        check(context.getPackageName().endsWith(".verification"),"Use isolated verification application only");
        AssistantMemory.write(context,"interop-mobile: caffè ☕");
        check(AssistantMemory.read(context).contains("caffè ☕"),"local encrypted UTF8 round trip");
        byte[] local=read(new FileInputStream(new File(context.getFilesDir(),"solphivia-memory.enc")));
        check(!new String(local,StandardCharsets.UTF_8).contains("interop-mobile"),"no plaintext local memory");
        char[] oversized=new char[AssistantMemory.MAX_BYTES+1];Arrays.fill(oversized,'x');
        try {AssistantMemory.write(context,new String(oversized));throw new AssertionError("oversized memory accepted");}catch(IOException expected){}
        check(AssistantMemory.read(context).contains("interop-mobile"),"failed write preserves memory");
        check(MemoryCommands.handle(context,"ricorda: preferisco esempi brevi").startsWith("Ho salvato"),"explicit local memory command");
        check(AssistantMemory.read(context).contains("preferisco esempi brevi"),"local memory command persisted");
        check(MemoryCommands.handle(context,"come stai?")==null,"ordinary chat never writes memory");
        try {AssistantMemory.readText(new ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}));throw new AssertionError("bad UTF8 accepted");}catch(IOException expected){}
        String fixture=new String(read(test.getContext().getAssets().open("vault-fixture.json")),StandardCharsets.UTF_8);
        JSONObject imported=VaultBackup.unlock(fixture,"synthetic-test-passphrase-42".toCharArray());
        String selected=VaultBackup.selectedText(imported,System.currentTimeMillis());
        check(selected.contains("allowed-interop")&&!selected.contains("excluded"),"vault filters sensitive, archived, suggested and expired records");
        try{VaultBackup.unlock(fixture,"wrong-passphrase-42".toCharArray());throw new AssertionError("wrong passphrase accepted");}catch(javax.crypto.AEADBadTagException expected){}
        JSONObject corrupt=new JSONObject(fixture); JSONObject payload=corrupt.getJSONObject("payload");
        String encoded=payload.getString("ciphertext");payload.put("ciphertext",(encoded.charAt(0)=='A'?"B":"A")+encoded.substring(1));
        try{VaultBackup.unlock(corrupt.toString(),"synthetic-test-passphrase-42".toCharArray());throw new AssertionError("tamper accepted");}catch(javax.crypto.AEADBadTagException expected){}
        JSONObject snapshot=VaultBackup.snapshot(AssistantMemory.read(context),false,true);
        String envelope=VaultBackup.seal(snapshot,"synthetic-test-passphrase-42".toCharArray());
        check(VaultBackup.unlock(envelope,"synthetic-test-passphrase-42".toCharArray()).getJSONArray("memories").length()==1,"native portable round trip");
        try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"vault-android.json"))){out.write(envelope.getBytes(StandardCharsets.UTF_8));}
        check(Double.isNaN(RadarClient.pixelRain(0x00ff0000)),"transparent radar is no data");
        check(RadarClient.pixelRain(0xff000000)==0,"black radar pixel is zero");
        check(RadarClient.pixelRain(0xff030000)>1,"SRI intensity reads red channel");
        check(Math.abs(RadarClient.worldX(0)-RadarClient.WORLD/2)<0.01,"radar longitude origin");
        check(Math.abs(RadarClient.worldY(0)-RadarClient.WORLD/2)<0.01,"radar latitude origin");
        final PetView[] holder=new PetView[1];test.runOnMainSync(()->holder[0]=new PetView(context));
        long deadline=android.os.SystemClock.uptimeMillis()+15000;
        while(android.os.SystemClock.uptimeMillis()<deadline){
            final boolean[] loaded={true};
            test.runOnMainSync(()->{try{for(Field f:PetView.class.getDeclaredFields())if(f.getType()==Bitmap[].class){f.setAccessible(true);for(Bitmap b:(Bitmap[])f.get(holder[0]))if(b==null)loaded[0]=false;}}catch(Exception e){throw new AssertionError(e);}});
            if(loaded[0])break;android.os.SystemClock.sleep(100);
        }
        test.runOnMainSync(()->{
            try{
                PetView pet=holder[0];pet.layout(0,0,180,224);
                int frames=0;
                for(Field f:PetView.class.getDeclaredFields())if(f.getType()==Bitmap[].class){f.setAccessible(true);for(Bitmap b:(Bitmap[])f.get(pet)){check(b!=null,"decoded sprite "+f.getName());frames++;}}
                check(frames==108,"all 108 sprites loaded");
                Bitmap bitmap=Bitmap.createBitmap(180,224,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);
                Field started=PetView.class.getDeclaredField("animationStartedAt");started.setAccessible(true);
                int states=0;
                for(String mode:new String[]{"prof","vj","codex","meteorina"})for(boolean cyborg:new boolean[]{false,true})for(String weather:new String[]{"none","rain","sun","cold"}){
                    pet.setSolphiviaMode(mode);pet.setCyborgSuit(cyborg);pet.setWeatherLook(weather);
                    for(String state:new String[]{"idle","mode","look","thinking","listen","talk","sit","sleep","happy","concerned","sad","run","wave","nod","explain","celebrate"}){
                        check(pet.setAnimation(state),"valid state "+state);
                        for(int elapsed:new int[]{0,200,800,1600}){started.setLong(pet,android.os.SystemClock.uptimeMillis()-elapsed);bitmap.eraseColor(0);pet.draw(canvas);check(nonempty(bitmap),"render "+mode+"/"+weather+"/"+state);states++;}
                    }
                }
                check(!pet.setAnimation("invalid"),"invalid animation rejected");
                context.getSharedPreferences("pet",0).edit().putString("mode_preference","auto")
                        .putBoolean("costume_override",false).putLong("context_mode_changed_at",0).putString("active_mode","prof").apply();
                PetService.applyContextualMode(context,"meteorina",pet);
                check("meteorina".equals(pet.getSolphiviaMode()),"automatic Meteorina mode");
                PetService.applyContextualMode(context,"codex",pet);
                check("meteorina".equals(pet.getSolphiviaMode()),"automatic outfit change cooldown");
                PetService.applyModeCommand(context,"prof",pet);
                PetService.applyContextualMode(context,"vj",pet);
                check("prof".equals(pet.getSolphiviaMode()),"manual outfit beats context");
                PetService.applyModeCommand(context,"modalita auto",pet);
                context.getSharedPreferences("pet",0).edit().putLong("context_mode_changed_at",0).apply();
                PetService.applyContextualMode(context,"invented-mode",pet);
                check("prof".equals(pet.getSolphiviaMode()),"invalid mode ignored");
                PetService.applyContextualMode(context,"codex",pet);
                check("codex".equals(pet.getSolphiviaMode()),"automatic choice resumes");
                for(String gesture:new String[]{"nod","explain","celebrate"}){
                    pet.setAnimation(gesture);started.setLong(pet,android.os.SystemClock.uptimeMillis()-2000);
                    pet.draw(canvas);check("idle".equals(pet.getSolphiviaAnimation()),"gesture finishes: "+gesture);
                }
                pet.setAnimation("thinking");started.setLong(pet,android.os.SystemClock.uptimeMillis()-5000);pet.draw(canvas);
                check("thinking".equals(pet.getSolphiviaAnimation()),"thinking persists until response");
                pet.setCyborgSuit(false);pet.setAnimation("sleep");started.setLong(pet,android.os.SystemClock.uptimeMillis());pet.draw(canvas);int[] first=new int[180*224];bitmap.getPixels(first,0,180,0,0,180,224);
                started.setLong(pet,android.os.SystemClock.uptimeMillis()-800);bitmap.eraseColor(0);pet.draw(canvas);int[] second=new int[first.length];bitmap.getPixels(second,0,180,0,0,180,224);
                check(!Arrays.equals(first,second),"sleep advances frames");
                pet.pause();bitmap.recycle();
                android.util.Log.i("SolphiviaRevision", "PASS memory/vault/radar math; rendered "+states+" animation combinations");
            }catch(Exception e){throw new AssertionError(e);}
        });
        long time=RadarClient.latestTime();check(ReviewRules.freshRadar(time,System.currentTimeMillis()),"live radar is recent");
        RadarClient.Sample sample=RadarClient.read(41.9028,12.4964,time);
        android.util.Log.i("SolphiviaRevision","PASS live DPC SRI for Rome, time="+time+", nearestKm="+sample.nearestKm);
        return "PASS: encrypted local memory, JS-to-Android vault import, bad-password/tamper rejection, consent filters, portable export; 108 sprites, 2048 rendered animation combinations, sleep progression; live DPC SRI Rome scan="+time+", nearestKm="+sample.nearestKm;
    }
    static byte[] read(InputStream in)throws IOException{try(in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static boolean nonempty(Bitmap b){int[] row=new int[b.getWidth()];for(int y=0;y<b.getHeight();y++){b.getPixels(row,0,row.length,0,y,row.length,1);for(int p:row)if((p>>>24)!=0)return true;}return false;}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
