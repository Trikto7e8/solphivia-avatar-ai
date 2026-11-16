package it.solphivia.mobile;

import android.app.Activity;

/** Gives settings their own touch/keyboard focus, restoring the same overlay on exit. */
abstract class OverlaySettingsActivity extends Activity {
    @Override protected void onResume() {
        super.onResume();
        PetService.setInternalChatVisible(true);
    }

    @Override protected void onPause() {
        PetService.setInternalChatVisible(false);
        super.onPause();
    }
}
