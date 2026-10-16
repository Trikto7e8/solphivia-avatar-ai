package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;

/** Permission/start relay; its transparent task stays behind other apps. */
public final class VoiceAccessActivity extends Activity {
    private static VoiceAccessActivity startingActivity;
    private boolean permissionRequested;
    private boolean starting;
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());

    static void serviceStarted() {
        VoiceAccessActivity activity = startingActivity;
        if (activity != null) activity.main.post(() -> {
            if (!activity.isDestroyed()) {
                activity.main.removeCallbacksAndMessages(null);
                activity.moveTaskToBack(true);
                activity.overridePendingTransition(0, 0);
            }
        });
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startingActivity = this;
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(new android.view.View(this));
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        starting = false;
    }

    @Override protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionRequested) {
                permissionRequested = true;
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 71);
            }
            return;
        }
        if (getWindow().getDecorView().hasWindowFocus()) startVoice();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            startVoice();
    }

    private void startVoice() {
        if (starting) return;
        if (!android.provider.Settings.canDrawOverlays(this)) { finish(); return; }
        // Battery restriction state can remain cached after the user allows background use.
        // Start from this focused activity and let Android enforce actual FGS permissions.
        starting = true;
        try {
            startForegroundService(new Intent(this, PetService.class).setAction(PetService.ACTION_VOICE_START));
            main.postDelayed(() -> {
                if (!isFinishing()) {
                    android.widget.Toast.makeText(this, "Android non ha avviato l’ascolto. Riapri Solphivia per riprovare.", android.widget.Toast.LENGTH_LONG).show();
                    finish();
                }
            }, 10000);
        } catch (RuntimeException error) {
            android.widget.Toast.makeText(this, "Non riesco ad avviare il microfono. Riapri Solphivia.", android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onDestroy() {
        if (startingActivity == this) startingActivity = null;
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != 71) return;
        if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            if (getWindow().getDecorView().hasWindowFocus()) startVoice();
        }
        else {
            PetService.setVoiceAccessPresentation("Microfono negato. Puoi consentirlo nelle impostazioni di Solphivia.", "sit");
            finish();
        }
    }
}
