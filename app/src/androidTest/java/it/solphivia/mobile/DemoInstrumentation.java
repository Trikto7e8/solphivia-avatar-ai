package it.solphivia.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.function.BooleanSupplier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/** Isolated HTTPS fixture test, without real keys, accounts or paid API calls. */
public final class DemoInstrumentation extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle args) { super.onCreate(args); this.args = args; start(); }

    private Object field(Object owner, String name) {
        try {
            Class<?> type = owner instanceof Class ? (Class<?>) owner : owner.getClass();
            Field field = type.getDeclaredField(name); field.setAccessible(true);
            return field.get(owner instanceof Class ? null : owner);
        } catch (Exception error) { throw new AssertionError(error); }
    }

    private void send(Object owner, String message) {
        runOnMainSync(() -> {
            ((EditText) field(owner, "composer")).setText(message);
            try {
                Method method = owner.getClass().getDeclaredMethod("sendMessage");
                method.setAccessible(true); method.invoke(owner);
            } catch (Exception error) { throw new AssertionError(error); }
        });
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            boolean[] matches = {false};
            runOnMainSync(() -> matches[0] = condition.getAsBoolean());
            if (matches[0]) return;
            Thread.sleep(100);
        }
        runOnMainSync(() -> {
            Object main = field(MainActivity.class, "visibleChat");
            Object pet = field(PetService.class, "activeInstance");
            StringBuilder flags = new StringBuilder("main=" + (main != null) + " pet=" + (pet != null));
            if (main != null) {
                for (String name : new String[]{"voiceWakePermissionPending", "backgroundNoticePending", "notificationRequestPending", "homeShortcutAttempted", "connectionNoticePending", "busy", "inputPending", "authBrowser", "activityPaused"}) flags.append(" ").append(name).append("=").append(field(main, name));
                flags.append(" focus=").append(((Activity) main).hasWindowFocus());
            }
            if (pet != null) flags.append(" attached=").append(field(pet, "overlayAttached"));
            Bundle diagnostic = new Bundle(); diagnostic.putString("stream", flags + "\n"); sendStatus(0, diagnostic);
        });
        throw new AssertionError("Timed out waiting for isolated demo state");
    }

    private void shell(String command) throws Exception {
        try (android.os.ParcelFileDescriptor output = getUiAutomation().executeShellCommand(command);
             java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(output)) {
            while (input.read() != -1) { }
        }
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        SSLSocketFactory original = HttpsURLConnection.getDefaultSSLSocketFactory();
        Activity[] activity = {null};
        try {
            String pkg = getTargetContext().getPackageName();
            if (!pkg.equals("it.solphivia.mobile.v11.verification") || !BuildConfig.DEMO_BASE_URL.equals("https://localhost:18443")) throw new AssertionError("Isolated local fixture only");
            byte[] certificate = android.util.Base64.decode(args.getString("ca_base64"), android.util.Base64.DEFAULT);
            KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType()); trust.load(null);
            trust.setCertificateEntry("synthetic-fixture", CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate)));
            TrustManagerFactory manager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); manager.init(trust);
            SSLContext tls = SSLContext.getInstance("TLS"); tls.init(null, manager.getTrustManagers(), null);
            // Trust exactly the test certificate within this isolated process. Hostname checks stay enabled.
            HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
            getTargetContext().getSharedPreferences("pet", 0).edit().putBoolean("wake_on_open", false).putBoolean("overlay_off", true).commit();
            getTargetContext().getSharedPreferences("chat", 0).edit().remove("history").commit();
            if (OpenAiChatClient.isConnected(getTargetContext())) throw new AssertionError("Fixture must have no ChatGPT account");
            shell("pm grant " + pkg + " android.permission.POST_NOTIFICATIONS");
            shell("appops set " + pkg + " SYSTEM_ALERT_WINDOW deny");
            runOnMainSync(() -> getTargetContext().startActivity(new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)));
            // The animated view can keep the queue active, so do not wait for global UI idle.
            await(() -> field(MainActivity.class, "visibleChat") != null);
            activity[0] = (Activity) field(MainActivity.class, "visibleChat");
            runOnMainSync(() -> activity[0].getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            await(() -> field(activity[0], "overlayPermissionButton") instanceof android.widget.Button
                    && ((android.view.View) field(activity[0], "overlayPermissionButton")).getVisibility() == android.view.View.VISIBLE);
            DemoAccess.activate(getTargetContext(), "x".repeat(43));
            send(activity[0], "Risposta sintetica di prova");
            await(() -> !(Boolean) field(activity[0], "busy") && ((android.widget.TextView) field(activity[0], "status")).getText().toString().contains("invito"));
            if (OpenAiChatClient.isAuthorizing()) throw new AssertionError("Demo opened OAuth");
            DemoAccess.activate(getTargetContext(), args.getString("invite"));
            send(activity[0], "Scrivi una frase per il test");
            await(() -> !(Boolean) field(activity[0], "busy") && "Risposta demo sintetica".equals(field(activity[0], "speechTarget")));
            shell("appops set " + pkg + " SYSTEM_ALERT_WINDOW allow");
            if (!Settings.canDrawOverlays(getTargetContext())) throw new AssertionError("Fixture overlay permission missing");
            runOnMainSync(() -> activity[0].recreate());
            await(() -> field(PetService.class, "activeInstance") != null
                    && (Boolean) field(field(PetService.class, "activeInstance"), "overlayAttached"));
            Object pet = field(PetService.class, "activeInstance");
            await(() -> !(Boolean) field(pet, "busy"));
            Activity configuration = startActivitySync(new Intent(getTargetContext(), DemoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await(() -> configuration.hasWindowFocus() && !(Boolean) field(pet, "overlayAttached"));
            EditText[] invitationField = {null};
            runOnMainSync(() -> {
                invitationField[0] = findEditor(configuration.getWindow().getDecorView());
                if (invitationField[0] == null) throw new AssertionError("Invitation editor missing");
                invitationField[0].requestFocus();
                ((android.view.inputmethod.InputMethodManager) configuration.getSystemService(Activity.INPUT_METHOD_SERVICE))
                        .showSoftInput(invitationField[0], android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
            await(() -> invitationField[0].hasFocus() && configuration.getWindow().getDecorView().getRootWindowInsets() != null
                    && configuration.getWindow().getDecorView().getRootWindowInsets().isVisible(android.view.WindowInsets.Type.ime()));
            runOnMainSync(configuration::finish);
            await(() -> field(PetService.class, "activeInstance") == pet && (Boolean) field(pet, "overlayAttached"));
            runOnMainSync(() -> {
                try {
                    Method voice=PetService.class.getDeclaredMethod("submitMessage",String.class,java.util.ArrayList.class,boolean.class);
                    voice.setAccessible(true);voice.invoke(pet,"Scrivi un secondo messaggio per il test",new java.util.ArrayList<String>(),true);
                }catch(Exception e){throw new AssertionError(e);}
            });
            await(() -> !(Boolean) field(pet, "busy") && "Risposta demo sintetica".equals(field(pet, "speechTarget")));
            send(pet, "Terzo messaggio oltre il limite");
            await(() -> !(Boolean) field(pet, "busy") && field(pet, "speechTarget").toString().contains("limite di richieste"));
            if (OpenAiChatClient.isAuthorizing() || OpenAiChatClient.isConnected(getTargetContext())) throw new AssertionError("Demo touched ChatGPT account");
            result.putString("stream", "PASS: no account/browser, invalid invite, typed main chat and transcribed-voice floating bubble over local HTTPS share the tester quota; third request refused; missing-overlay button and overlay return; invitation settings own keyboard focus and restore the same overlay service. Synthetic provider, no microphone/audio-quality test.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        } finally {
            HttpsURLConnection.setDefaultSSLSocketFactory(original);
            DemoAccess.disable(getTargetContext());
            runOnMainSync(() -> AppControls.close(getTargetContext()));
        }
    }

    private static EditText findEditor(android.view.View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i=0;i<group.getChildCount();i++) {
                EditText found=findEditor(group.getChildAt(i)); if(found!=null)return found;
            }
        }
        return null;
    }
}
