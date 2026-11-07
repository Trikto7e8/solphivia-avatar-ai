package it.solphivia.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Explicit opt-in to the invitation-backed text demo. */
public class DemoActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText("Prova senza account"); title.setTextSize(24);
        page.addView(title);
        TextView notice = new TextView(this);
        notice.setText("Demo di testo con codice d’invito. Il singolo messaggio viene inviato al server del progetto e a OpenAI. La cronologia precedente, la memoria e gli allegati restano esclusi. Usa testi di prova. Le richieste sono limitate dal server, anche in caso di errori. Il codice sarà verificato all’invio. Puoi tornare a ChatGPT in ogni momento.");
        page.addView(notice);
        TextView label = new TextView(this); label.setText("Codice d’invito"); page.addView(label);
        EditText invite = new EditText(this);
        invite.setId(android.view.View.generateViewId()); label.setLabelFor(invite.getId());
        invite.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        invite.setSaveEnabled(false); invite.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        page.addView(invite);
        TextView status = new TextView(this);
        status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if (BuildConfig.DEMO_BASE_URL.isEmpty()) status.setText("Il gestore deve ancora configurare l’indirizzo HTTPS della demo in questa build.");
        page.addView(status);
        Button activate = new Button(this); activate.setText("Attiva demo di testo");
        activate.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        activate.setEnabled(!BuildConfig.DEMO_BASE_URL.isEmpty());
        activate.setOnClickListener(v -> {
            try { DemoAccess.activate(this, invite.getText().toString().trim()); finish(); }
            catch (Exception error) { status.setText("Controlla il codice d’invito e la configurazione del servizio demo."); }
        });
        page.addView(activate);
        Button disable = new Button(this); disable.setText("Disattiva demo · usa ChatGPT");
        disable.setOnClickListener(v -> { DemoAccess.disable(this); finish(); }); page.addView(disable);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
        ScreenInsets.apply(this, page);
    }
}
