package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import java.util.*;

/** Uses installed Android engines, including independent open-source engines. */
public final class VoiceSettingsActivity extends Activity {
    private TextToSpeech speech;
    private TextView status;
    private final ArrayList<String> engines = new ArrayList<>();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        VoicePreferences.initialize(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18 * getResources().getDisplayMetrics().density); page.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this); title.setText("Voce di Solphivia"); title.setTextSize(24); page.addView(title);
        TextView help = new TextView(this);
        help.setText("Scegli un motore installato sul telefono. Sono compatibili anche motori liberi come eSpeak NG. L’accento inglese usa una voce inglese offline con adattamento della pronuncia italiana: ascolta la prova per valutarla."); page.addView(help);
        Spinner engine = new Spinner(this); page.addView(engine);
        android.content.SharedPreferences prefs = getSharedPreferences("pet", 0);
        Switch accent = new Switch(this); accent.setText("Italiano con accento inglese"); accent.setChecked(prefs.getBoolean("english_accent", false)); page.addView(accent);
        Switch aloud = new Switch(this); aloud.setText("Leggi le risposte ad alta voce"); aloud.setChecked(prefs.getBoolean("read_aloud", true)); page.addView(aloud);
        Switch wake = new Switch(this); wake.setText("Richiamo automatico all’apertura"); wake.setChecked(prefs.getBoolean("wake_on_open", true)); page.addView(wake);
        status = new TextView(this); page.addView(status);
        Button save = new Button(this); save.setText("Salva e prova la voce"); page.addView(save); save.setEnabled(false);
        Button system = new Button(this); system.setText("Installa o configura voci Android"); page.addView(system);
        system.setOnClickListener(v -> { try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); } catch (RuntimeException e) { status.setText("Apri Impostazioni Android → Sintesi vocale."); } });
        ScreenInsets.apply(this,page);setContentView(page);
        speech = new TextToSpeech(this, code -> { });
        // Engine discovery does not depend on a working/default engine's initialization callback.
        ArrayList<String> labels = new ArrayList<>(); labels.add("Motore predefinito Android"); engines.add("");
        for (TextToSpeech.EngineInfo info : speech.getEngines()) { labels.add(info.label); engines.add(info.name); }
        engine.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        int selectedIndex = engines.indexOf(prefs.getString("tts_engine", "")); engine.setSelection(Math.max(0, selectedIndex)); save.setEnabled(true);
        if(engines.size()==1)status.setText("Nessun motore vocale rilevato. Installa un motore TTS prima della prova.");
        save.setOnClickListener(v -> {
            String selected = engines.get(engine.getSelectedItemPosition());
            prefs.edit().putString("tts_engine", selected).putBoolean("english_accent", accent.isChecked()).putBoolean("read_aloud", aloud.isChecked()).apply();
            prefs.edit().putBoolean("wake_on_open",wake.isChecked()).apply();
            if(!wake.isChecked())stopService(new Intent(this,VoiceSessionService.class));
            if (android.provider.Settings.canDrawOverlays(this)) {
                Intent update = new Intent(this, PetService.class).setAction(PetService.ACTION_VOICE_SETTINGS);
                startForegroundService(update);
                status.setText("Impostazioni salvate. La prova viene letta dall’avatar.");
            } else status.setText("Impostazioni salvate. Attiva l’avatar flottante per ascoltare la prova.");
        });
    }
    @Override protected void onDestroy() { if (speech != null) { speech.shutdown(); speech = null; } super.onDestroy(); }
}
