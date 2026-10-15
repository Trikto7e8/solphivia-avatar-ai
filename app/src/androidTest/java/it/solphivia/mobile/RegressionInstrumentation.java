package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Offline integration checks. Run only on a dedicated emulator/test installation. */
public final class RegressionInstrumentation extends Instrumentation {
    private volatile Activity resumedMain;
    private final java.util.concurrent.CountDownLatch mainResumed=new java.util.concurrent.CountDownLatch(1);
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        if(activity instanceof MainActivity){resumedMain=activity;mainResumed.countDown();}
    }
    @Override public void callActivityOnCreate(Activity activity, Bundle state) {
        super.callActivityOnCreate(activity, state);
        if (activity instanceof MainActivity) try {
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("homeShortcutAttempted");
            field.setAccessible(true); field.setBoolean(activity, true);
        } catch (Exception error) { throw new AssertionError(error); }
    }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        File small = new File(getTargetContext().getCacheDir(), "regression-document.txt");
        File large = new File(getTargetContext().getCacheDir(), "regression-large.bin");
        try {
            Context context = getTargetContext();
            check(context.getPackageName().endsWith(".verification"), "Tests require isolated verificationBuild; never run on the personal installation");
            String revisionResult=RevisionChecks.run(this);
            ConsentChecks.run(this);
            try (FileOutputStream out = new FileOutputStream(small)) { out.write("Documento di prova: pagina 2 contiene 42.".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            JSONArray files = AttachmentReader.read(context, new ArrayList<>(Collections.singletonList(Uri.fromFile(small).toString())), new long[]{0});
            JSONArray refs = AttachmentReader.references(files);
            check(!refs.toString().contains("data_uri"), "history must not contain file payloads");
            JSONArray history = new JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", "Leggi il file").put("attachments", refs))
                    .put(new JSONObject().put("role", "assistant").put("content", "Letto."))
                    .put(new JSONObject().put("role", "user").put("content", "Cosa dice pagina 2?"));
            JSONArray input = OpenAiChatClient.buildInput(context, history, new JSONArray());
            check(input.getJSONObject(0).getJSONArray("content").getJSONObject(1).getString("file_data").contains("base64,"), "follow-up retains original file");
            try (FileOutputStream out = new FileOutputStream(large)) {
                byte[] block = new byte[8192];
                for (int i = 0; i < 1024; i++) out.write(block);
                out.write(1);
            }
            boolean rejected = false;
            try { AttachmentReader.read(context, new ArrayList<>(Collections.singletonList(Uri.fromFile(large).toString())), new long[]{0}); }
            catch (IllegalArgumentException expected) { rejected = expected.getMessage().contains("8 MB"); }
            check(rejected, "real byte count enforces per-file limit");
            ExecutorService stopped = Executors.newSingleThreadExecutor(); stopped.shutdownNow();
            OpenAiChatClient.respond(context, stopped, new JSONArray(), new OpenAiChatClient.ChatCallback() {
                public void onSuccess(String a, String b, String c, String d) { throw new AssertionError("unexpected callback"); }
                public void onError(String error) { throw new AssertionError("unexpected callback"); }
            });
            runOnMainSync(() -> {
                PetView pet = new PetView(context);
                context.getSharedPreferences("pet", 0).edit().putBoolean("costume_override", true).putString("mode_preference", "auto").apply();
                pet.setClassicCyborgSuit(true);
                PetService.applyContextualMode(context, "prof", pet);
                check(pet.isCyborgSuit(), "automatic mode must preserve explicit costume");
                PetService.applyModeCommand(context, "prof", pet);
                check(!pet.isCyborgSuit(), "explicit mode must release costume override");
                pet.pause();
            });
            runOnMainSync(()->context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            check(mainResumed.await(15,java.util.concurrent.TimeUnit.SECONDS),"connection screen resumed within timeout");
            Activity activity=resumedMain;
            check(!activity.isFinishing(), "disconnected account keeps connection screen accessible");
            check(android.provider.Settings.canDrawOverlays(context), "test setup requires overlay app-op");
            for (int i = 0; i < 3; i++) {
                context.startForegroundService(new Intent(context, PetService.class).putExtra(PetService.EXTRA_SEND_MESSAGE, "saluta"));
                android.os.SystemClock.sleep(1500);
                boolean running = false;
                android.app.ActivityManager manager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(50))
                    if (service.service.getClassName().equals(PetService.class.getName())) running = true;
                check(running, "overlay greeting starts foreground service");
                context.stopService(new Intent(context, PetService.class));
                android.os.SystemClock.sleep(300);
            }
            runOnMainSync(activity::finish);
            result.putString("stream", revisionResult+"\nPASS: single-use memory consent, cancel and send without memory; attachment follow-up, metadata-only history, byte limits, stopped executor, costume persistence/reset, connection screen, three overlay greeting/start/stop cycles\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            small.delete(); large.delete();
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
