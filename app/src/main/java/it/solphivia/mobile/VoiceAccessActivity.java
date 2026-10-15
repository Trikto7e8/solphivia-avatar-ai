package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

/** Invisible, touch-through foreground host; the user only sees and touches the existing floating avatar. */
public final class VoiceAccessActivity extends Activity {
    private VoiceAccessController controller;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setContentView(new android.view.View(this));
        controller = new VoiceAccessController(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (controller != null) controller.onResume();
    }

    @Override protected void onPause() {
        if (controller != null) controller.onPause();
        super.onPause();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (controller != null) {
            controller.onNewLaunch();
            controller.onResume();
        }
    }

    @Override protected void onDestroy() {
        if (controller != null) controller.onDestroy();
        super.onDestroy();
    }
}
