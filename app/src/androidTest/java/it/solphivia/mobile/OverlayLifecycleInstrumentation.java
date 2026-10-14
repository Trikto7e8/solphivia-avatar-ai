package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import java.lang.reflect.Field;

/** Exercise actual activity/service transitions in the isolated test installation. */
public final class OverlayLifecycleInstrumentation extends Instrumentation {
    private volatile Activity resumedChat;
    private volatile java.util.concurrent.CountDownLatch resumed;
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        if (activity instanceof MainActivity && resumed != null) { resumedChat=activity;resumed.countDown(); }
    }
    private Activity openChat(Context context) throws Exception {
        resumed=new java.util.concurrent.CountDownLatch(1);
        runOnMainSync(()->context.startActivity(new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
        RevisionChecks.check(resumed.await(15,java.util.concurrent.TimeUnit.SECONDS),"chat resumed within timeout");
        return resumedChat;
    }
    @Override public void callActivityOnCreate(Activity activity, Bundle state) {
        super.callActivityOnCreate(activity, state);
        if (activity instanceof MainActivity) try {
            Field f=MainActivity.class.getDeclaredField("homeShortcutAttempted");
            f.setAccessible(true); f.setBoolean(activity,true);
            // Keep the test chat open instead of taking the connected launcher's Home shortcut.
            Field busy=MainActivity.class.getDeclaredField("busy");
            busy.setAccessible(true);busy.setBoolean(activity,true);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private Object field(Object instance,String name) throws Exception {
        Field f=PetService.class.getDeclaredField(name);f.setAccessible(true);return f.get(instance);
    }
    private void progress(String text) { Bundle b=new Bundle();b.putString("stream",text+"\n");sendStatus(0,b); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        Context context=getTargetContext();
        try {
            RevisionChecks.check(context.getPackageName().endsWith(".verification"),"isolated package required");
            RevisionChecks.check(Settings.canDrawOverlays(context),"test overlay permission required");
            context.getSharedPreferences("pet",0).edit().putBoolean("overlay_off",false).apply();
            Activity chat=openChat(context);
            progress("chat started");
            runOnMainSync(()->context.startForegroundService(new Intent(context,PetService.class)));
            android.os.SystemClock.sleep(1000);
            Object service=field(null,"activeInstance");
            RevisionChecks.check(service!=null,"overlay service running");
            RevisionChecks.check(Boolean.FALSE.equals(field(service,"overlayAttached")),"no floating window over internal chat; visible="+field(null,"internalChatVisible")+", finishing="+chat.isFinishing()+", focus="+chat.hasWindowFocus());
            progress("hidden window verified");
            runOnMainSync(chat::finish);android.os.SystemClock.sleep(1000);
            RevisionChecks.check(Boolean.TRUE.equals(field(service,"overlayAttached")),"floating window returns after chat closes");
            Activity reopened=openChat(context);
            progress("chat reopened");android.os.SystemClock.sleep(500);
            RevisionChecks.check(Boolean.FALSE.equals(field(service,"overlayAttached")),"reopening chat hides floating window again");
            runOnMainSync(reopened::finish);android.os.SystemClock.sleep(500);
            RevisionChecks.check(Boolean.TRUE.equals(field(service,"overlayAttached")),"second exit restores floating window");
            result.putString("stream","PASS: internal chat hides floating window; exiting restores it; repeated transition succeeds\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) {
            result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);
        } finally { context.stopService(new Intent(context,PetService.class)); }
    }
}
