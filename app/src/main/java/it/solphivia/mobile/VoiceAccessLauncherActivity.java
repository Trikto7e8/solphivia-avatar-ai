package it.solphivia.mobile;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

/** Launches the existing overlay and its transparent voice host without showing a second avatar screen. */
public final class VoiceAccessLauncherActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!Settings.canDrawOverlays(this)
                || getSharedPreferences("chatgpt_oauth", 0).getBoolean("authorizing", false)
                || OpenAiChatClient.isAuthorizing()) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }

        getSharedPreferences("pet", 0).edit().putBoolean("overlay_off", false).apply();
        removeOldChatTasks();
        PetService.setInternalChatVisible(false);
        Intent service = new Intent(this, PetService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        startActivity(new Intent(this, VoiceAccessActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        overridePendingTransition(0, 0);
        finish();
    }

    private void removeOldChatTasks() {
        ActivityManager manager = getSystemService(ActivityManager.class);
        if (manager == null) return;
        for (ActivityManager.AppTask task : manager.getAppTasks()) {
            ActivityManager.RecentTaskInfo info = task.getTaskInfo();
            if (info.baseActivity != null && MainActivity.class.getName().equals(info.baseActivity.getClassName()))
                task.finishAndRemoveTask();
        }
    }
}
