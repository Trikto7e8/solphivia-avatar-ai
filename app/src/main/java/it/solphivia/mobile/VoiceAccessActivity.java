package it.solphivia.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

/** Foreground, opt-in, bounded two-turn session. No vault, overlay or chat access. */
public final class VoiceAccessActivity extends Activity {
    private enum Phase { IDLE, WAKE, GREETING, COMMAND }
    private final Handler main = new Handler(Looper.getMainLooper());
    private Phase phase = Phase.IDLE;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status;
    private EditText name;
    private Button start, stop;
    private CheckBox network;
    private boolean resumed, ttsReady;
    private int generation;
    private String utterance;
    private android.app.AlertDialog previewDialog;
    private final Runnable timeout = () -> finishSession("Tempo scaduto. Premi Avvia per riprovare.");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(20 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding); scroll.addView(page);
        TextView title = new TextView(this); title.setText("Solphivia Voice Access · v0.1"); title.setTextSize(24); page.addView(title);
        TextView info = new TextView(this);
        info.setText("Premi Avvia, poi di’ Hola, Solphivia! Dopo il saluto puoi dire Apri WhatsApp oppure Condividi Ciao Marco su WhatsApp. ShareBridge mostra un’anteprima modificabile: confermi l’apertura di WhatsApp, scegli il destinatario e decidi lì se inviare. Ogni ascolto dura al massimo 15 secondi. Uscire dalla schermata interrompe la sessione.\nIl nome resta sul telefono, separato da AI Memory Vault. Audio e trascrizioni non vengono salvati dall’app.");
        info.setTextSize(18); page.addView(info);
        TextView label = new TextView(this); label.setText("Come ti chiami?"); page.addView(label);
        name = new EditText(this); name.setId(android.view.View.generateViewId()); label.setLabelFor(name.getId());
        name.setSingleLine(true); name.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});
        name.setText(getSharedPreferences("voice_access", 0).getString("name", "")); page.addView(name);
        network = new CheckBox(this);
        network.setText("Consento il riconoscitore di sistema se quello locale non è disponibile: il fornitore potrebbe elaborare audio online.");
        page.addView(network);
        status = new TextView(this); status.setTextSize(20);
        status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE); page.addView(status);
        start = new Button(this); start.setText("Avvia ascolto"); page.addView(start);
        stop = new Button(this); stop.setText("Interrompi"); page.addView(stop);
        start.setMinHeight((int)(48 * getResources().getDisplayMetrics().density));
        stop.setMinHeight((int)(48 * getResources().getDisplayMetrics().density));
        start.setOnClickListener(v -> begin()); stop.setOnClickListener(v -> finishSession("Ascolto interrotto."));
        setContentView(scroll); ScreenInsets.apply(this, scroll);
        finishSession("Preparazione voce locale…");
        tts = new TextToSpeech(this, code -> main.post(() -> {
            if (isDestroyed() || tts == null) return;
            if (code == TextToSpeech.SUCCESS) {
                android.speech.tts.Voice voice = SpeechVoiceSelection.select(tts, Locale.ITALIAN, true);
                ttsReady = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS;
            }
            status.setText(ttsReady ? "Pronto. Premi Avvia per ascoltare." : "Voce italiana offline non disponibile. Installala nelle impostazioni TTS e riapri Voice Access.");
        }));
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) { }
            public void onDone(String id) { main.post(() -> {
                if (foregroundUnlocked() && phase == Phase.GREETING && id.equals(utterance)) listen(Phase.COMMAND);
            }); }
            public void onError(String id) { main.post(() -> {
                if (id.equals(utterance)) finishSession("Errore nella risposta vocale. Riprova.");
            }); }
        });
    }

    private void begin() {
        if (!foregroundUnlocked() || phase != Phase.IDLE) return;
        if (name.getText().toString().trim().isEmpty()) { name.setError("Inserisci il tuo nome"); return; }
        if (!ttsReady) { status.setText("È necessaria una voce italiana offline. Riapri dopo averla installata."); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 71); return;
        }
        getSharedPreferences("voice_access", 0).edit().putString("name", name.getText().toString().trim()).apply();
        listen(Phase.WAKE);
    }

    private void listen(Phase next) {
        if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
        releaseRecognizer(); main.removeCallbacks(timeout);
        phase = next; int token = ++generation;
        start.setEnabled(false); stop.setEnabled(true); name.setEnabled(false); network.setEnabled(false);
        try {
            boolean local = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
            if (!local && !network.isChecked()) { finishSession("Riconoscimento locale non disponibile. Puoi consentire quello di sistema oppure interrompere."); return; }
            if (!local && !SpeechRecognizer.isRecognitionAvailable(this)) { finishSession("Nessun riconoscitore vocale disponibile."); return; }
            recognizer = local ? SpeechRecognizer.createOnDeviceSpeechRecognizer(this) : SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) { if (token == generation) status.setText(next == Phase.WAKE ? "In ascolto: di’ Hola, Solphivia!" : "In ascolto: Apri WhatsApp oppure Condividi [testo] su WhatsApp"); }
                public void onBeginningOfSpeech() { }
                public void onRmsChanged(float rms) { }
                public void onBufferReceived(byte[] buffer) { }
                public void onEndOfSpeech() { }
                public void onPartialResults(Bundle b) { }
                public void onEvent(int type, Bundle b) { }
                public void onError(int error) { if (token == generation) finishSession("Ascolto terminato (codice " + error + "). Premi Avvia per riprovare."); }
                public void onResults(Bundle results) {
                    if (token != generation) return;
                    if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                    ++generation; releaseRecognizer(); main.removeCallbacks(timeout);
                    if (next == Phase.WAKE) {
                        if (!VoiceAccessRules.wake(text)) { finishSession("Saluto non riconosciuto. Premi Avvia e di’ Hola, Solphivia!"); return; }
                        phase = Phase.GREETING; utterance = "voice-access-" + generation;
                        String greeting = VoiceAccessRules.greeting(name.getText().toString()); status.setText(greeting);
                        main.postDelayed(timeout, 15000);
                        if (tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) finishSession("Risposta vocale non disponibile.");
                    } else {
                        finishSession("Comando non supportato. Puoi dire Apri WhatsApp oppure Condividi [testo] su WhatsApp.");
                        if (VoiceAccessRules.openWhatsApp(text)) openWhatsApp();
                        else {
                            String draft = ShareBridge.preview(text);
                            if (draft != null) showSharePreview(draft);
                        }
                    }
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            status.setText("Preparazione ascolto…"); main.postDelayed(timeout, 15000); recognizer.startListening(intent);
        } catch (RuntimeException error) { finishSession("Riconoscimento non disponibile. Controlla permesso microfono e servizio vocale."); }
    }

    private void openWhatsApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) { status.setText("WhatsApp non è installato o non è disponibile."); return; }
        try { startActivity(launch); status.setText("WhatsApp aperto. Ascolto terminato."); }
        catch (RuntimeException error) { status.setText("Impossibile aprire WhatsApp."); }
    }

    private void showSharePreview(String draft) {
        status.setText("Microfono spento. Controlla il testo prima di condividere.");
        EditText editor = new EditText(this);
        editor.setContentDescription("Testo da condividere su WhatsApp");
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
        editor.setText(draft);
        previewDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("ShareBridge · Anteprima")
                .setMessage("Aprire la condivisione WhatsApp? Sceglierai il destinatario e confermerai l’invio dentro WhatsApp.")
                .setView(editor).setNegativeButton("Annulla", (dialog, which) -> status.setText("Condivisione annullata."))
                .setPositiveButton("Apri condivisione WhatsApp", null).create();
        previewDialog.setOnDismissListener(dialog -> { editor.setText(""); previewDialog = null; });
        previewDialog.show();
        previewDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = editor.getText().toString().trim();
            if (text.isEmpty()) { editor.setError("Inserisci un testo oppure annulla"); return; }
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, text);
            previewDialog.dismiss();
            try { startActivity(share); status.setText("Condivisione aperta. Nessun invio automatico."); }
            catch (RuntimeException error) { status.setText("Condivisione WhatsApp non disponibile. Nessun messaggio inviato."); }
        });
    }

    private void releaseRecognizer() {
        if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
    }
    private boolean foregroundUnlocked() {
        return resumed && getSystemService(android.os.PowerManager.class).isInteractive()
                && !getSystemService(android.app.KeyguardManager.class).isKeyguardLocked();
    }
    private void finishSession(String message) {
        ++generation; phase = Phase.IDLE; utterance = null; main.removeCallbacks(timeout);
        releaseRecognizer(); if (tts != null) tts.stop();
        start.setEnabled(true); stop.setEnabled(false); name.setEnabled(true); network.setEnabled(true); status.setText(message);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 71) status.setText(grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED
                ? "Microfono consentito. Premi Avvia." : "Microfono negato. Puoi consentirlo nelle impostazioni dell’app.");
    }
    @Override protected void onResume() { super.onResume(); resumed = true; }
    @Override protected void onStart() { super.onStart(); PetService.setInternalChatVisible(true); }
    @Override protected void onStop() { PetService.setInternalChatVisible(false); super.onStop(); }
    @Override protected void onPause() {
        resumed = false; finishSession("Ascolto interrotto. Premi Avvia per ripartire.");
        if (previewDialog != null) previewDialog.dismiss();
        super.onPause();
    }
    @Override protected void onDestroy() { finishSession("Ascolto terminato."); if (tts != null) { tts.shutdown(); tts = null; } super.onDestroy(); }
}
