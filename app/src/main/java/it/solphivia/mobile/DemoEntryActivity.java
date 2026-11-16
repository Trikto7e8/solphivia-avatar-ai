package it.solphivia.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

/** Explicit account choice shared by the activity and floating bubble. */
public class DemoEntryActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        new AlertDialog.Builder(this).setTitle("Come vuoi usare Solphivia?")
                .setItems(new String[]{"Servizio Solphivia · codice d’invito", "Collega ChatGPT", "Altre IA · API personali o locali"}, (dialog, choice) -> {
                    Intent target = choice == 2 ? new Intent(this, ModelConnectionsActivity.class) : choice == 0 ? new Intent(this, DemoActivity.class)
                            : new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_CONNECT_CHATGPT);
                    startActivity(target);
                    finish();
                }).setNegativeButton("Annulla", (dialog, choice) -> finish())
                .setOnCancelListener(dialog -> finish()).show();
    }
}
