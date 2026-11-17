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
public class DemoActivity extends OverlaySettingsActivity {
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
        notice.setText("Servizio Solphivia con codice d’invito, anche a voce. Le tue parole vengono trascritte dal riconoscitore già configurato e il testo viene inviato al server del progetto e a OpenAI; la risposta usa la voce Android attuale. Solo la conversazione di questo invito può essere inclusa, senza vault o allegati. Ogni tester ha una quota, con un tetto complessivo del progetto. Usa dati di prova. Il codice sarà verificato al primo invio.");
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
        Button activate = new Button(this); activate.setText("Attiva servizio Solphivia");
        activate.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        activate.setEnabled(!BuildConfig.DEMO_BASE_URL.isEmpty());
        activate.setOnClickListener(v -> {
            if(OpenAiChatClient.requestsInProgress()||PetService.voiceOutputBusy()){status.setText("Attendi la risposta prima di cambiare collegamento.");return;}
            try { DemoAccess.activate(this, invite.getText().toString().trim()); finish(); }
            catch (Exception error) { status.setText("Controlla il codice d’invito e la configurazione del servizio demo."); }
        });
        page.addView(activate);
        Button disable = new Button(this); disable.setText("Disattiva demo · usa ChatGPT");
        disable.setOnClickListener(v -> { if(OpenAiChatClient.requestsInProgress()||PetService.voiceOutputBusy()){status.setText("Attendi la risposta prima di cambiare collegamento.");return;} DemoAccess.disable(this); finish(); }); page.addView(disable);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
        ScreenInsets.apply(this, page);
    }
}
