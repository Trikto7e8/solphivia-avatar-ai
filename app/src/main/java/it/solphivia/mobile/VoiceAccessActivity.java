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
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.app.SearchManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Foreground, opt-in, bounded two-turn session. No vault, overlay or chat access. */
public final class VoiceAccessActivity extends Activity {
    private enum Phase { IDLE, WAKE, GREETING, COMMAND, RESPONDING, SPEAKING }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService voiceWorker = Executors.newSingleThreadExecutor();
    private PetView avatar;
    private TextView avatarSpeech;
    private Phase phase = Phase.IDLE;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status;
    private EditText name;
    private Button start, stop;
    private CheckBox network;
    private boolean resumed, ttsReady;
    private int generation;
    private int wakeFailures;
    private boolean listeningRequested;
    private String utterance;
    private android.app.AlertDialog previewDialog;
    private final Runnable timeout = () -> finishSession("Tempo scaduto. Premi Avvia per riprovare.");
    private final Runnable retryWake = () -> {
        if (listeningRequested && foregroundUnlocked() && phase == Phase.WAKE) listen(Phase.WAKE);
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(20 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding); scroll.addView(page);
        TextView title = new TextView(this); title.setText("Solphivia Voice Access · v0.1"); title.setTextSize(24); page.addView(title);
        FrameLayout avatarStage = new FrameLayout(this);
        avatarStage.setBackground(roundRect(Color.WHITE, padding, 0xFFE9DDEC));
        avatar = new PetView(this);
        String activeMode = getSharedPreferences("pet", 0).getString("active_mode", "prof");
        avatar.setCyborgSuit("vj".equals(activeMode)); avatar.setSolphiviaMode(activeMode);
        avatar.setWeatherLook(getSharedPreferences("weather", 0).getString("avatar_look", "none"));
        if (getSharedPreferences("pet", 0).getBoolean("costume_override", false)) avatar.setClassicCyborgSuit(true);
        avatar.setContentDescription("Avatar animato di Solphivia");
        FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(dp(220), dp(220), android.view.Gravity.CENTER);
        avatarStage.addView(avatar, avatarParams);
        avatarSpeech = new TextView(this);
        avatarSpeech.setText("Sono qui. Di' «Hola, Solphivia!» quando sei pronto.");
        avatarSpeech.setTextSize(16); avatarSpeech.setTypeface(null, Typeface.BOLD);
        avatarSpeech.setTextColor(0xFF302736); avatarSpeech.setGravity(android.view.Gravity.CENTER);
        avatarSpeech.setPadding(dp(12), dp(8), dp(12), dp(8));
        avatarSpeech.setBackground(roundRect(0xFFF8F5F9, dp(18), 0xFFEDE4EF));
        FrameLayout.LayoutParams bubbleParams = new FrameLayout.LayoutParams(-1, -2, android.view.Gravity.TOP);
        avatarStage.addView(avatarSpeech, bubbleParams);
        page.addView(avatarStage, new LinearLayout.LayoutParams(-1, dp(250)));
        TextView info = new TextView(this);
        info.setText("Inserisci il tuo nome e premi Avvia e attendi il saluto. Il microfono resta in attesa di Hola, Solphivia! finché non lo riconosce. Solo allora Solphivia risponde e ascolta il comando. Dopo il saluto puoi dire Apri WhatsApp oppure Condividi Ciao Marco su WhatsApp. ShareBridge mostra un’anteprima modificabile: confermi l’apertura di WhatsApp, scegli il destinatario e decidi lì se inviare. Ogni ascolto dura al massimo 15 secondi. Uscire dalla schermata interrompe la sessione.\nIl nome resta sul telefono, separato da AI Memory Vault. Audio e trascrizioni non vengono salvati dall’app.");
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
        start = new Button(this); start.setText("Avvia e attendi il saluto"); page.addView(start);
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
            status.setText(ttsReady ? (listeningRequested ? "In attesa di «Hola, Solphivia!»…" : "Pronto. Premi Avvia e attendi il saluto.") : "Voce italiana offline non disponibile. Installala nelle impostazioni TTS e riapri Voice Access.");
        }));
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) { }
            public void onDone(String id) { main.post(() -> {
                if (foregroundUnlocked() && (phase == Phase.GREETING || phase == Phase.SPEAKING) && id.equals(utterance)) listen(Phase.COMMAND);
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
        listeningRequested = true; wakeFailures = 0; listen(Phase.WAKE);
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
                public void onReadyForSpeech(Bundle b) {
                    if (token != generation) return;
                    if (next == Phase.WAKE) {
                        status.setText("In attesa di «Hola, Solphivia!»… Premi Interrompi per spegnere il microfono.");
                        avatarSpeech.setText("Sono qui. Di' «Hola, Solphivia!» quando sei pronto."); avatar.setAnimation("listen");
                    } else { status.setText("In ascolto: dimmi cosa vuoi fare."); avatarSpeech.setText("Ti ascolto…"); avatar.setAnimation("listen"); }
                }
                public void onBeginningOfSpeech() { }
                public void onRmsChanged(float rms) { }
                public void onBufferReceived(byte[] buffer) { }
                public void onEndOfSpeech() { }
                public void onPartialResults(Bundle b) { }
                public void onEvent(int type, Bundle b) { }
                public void onError(int error) {
                    if (token != generation) return;
                    if (next == Phase.WAKE && listeningRequested && foregroundUnlocked()
                            && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                            && error != SpeechRecognizer.ERROR_CLIENT && ++wakeFailures <= 8) {
                        releaseRecognizer(); main.removeCallbacks(timeout);
                        status.setText("Il riconoscitore è in pausa. Riprovo l’attesa del saluto…");
                        main.postDelayed(retryWake, Math.min(2500, 500L * wakeFailures));
                    } else finishSession("Ascolto terminato. Premi Avvia per riprovare (codice " + error + ").");
                }
                public void onResults(Bundle results) {
                    if (token != generation) return;
                    if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                    wakeFailures = 0;
                    ++generation; releaseRecognizer(); main.removeCallbacks(timeout);
                    if (next == Phase.WAKE) {
                        if (!VoiceAccessRules.wake(text)) {
                            status.setText("Saluto non riconosciuto. Resto in attesa di «Hola, Solphivia!»…");
                            listen(Phase.WAKE); return;
                        }
                        phase = Phase.GREETING; utterance = "voice-access-" + generation;
                        String greeting = VoiceAccessRules.greeting(name.getText().toString()); status.setText(greeting);
                        avatarSpeech.setText(greeting); avatar.setAnimation("talk");
                        main.postDelayed(timeout, 15000);
                        if (tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) finishSession("Risposta vocale non disponibile.");
                    } else {
                        avatarSpeech.setText("Hai detto: " + text);
                        VoiceAction action = parseVoiceAction(text);
                        if (action != null) { confirmVoiceAction(action); }
                        else if (VoiceAccessRules.openWhatsApp(text)) {
                            finishSession("WhatsApp aperto. Ascolto terminato."); openWhatsApp();
                        } else {
                            String draft = ShareBridge.preview(text);
                            if (draft != null) {
                                finishSession("Microfono spento. Controlla il testo prima di condividere.");
                                showSharePreview(draft);
                            } else {
                                sendToSolphivia(text);
                            }
                        }
                    }
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            status.setText("Preparazione ascolto…");
            if (next != Phase.WAKE) main.postDelayed(timeout, 15000);
            recognizer.startListening(intent);
        } catch (RuntimeException error) { finishSession("Riconoscimento non disponibile. Controlla permesso microfono e servizio vocale."); }
    }

    private static final class VoiceAction {
        final String title, detail, kind, value;
        final int first, second;
        VoiceAction(String kind, String title, String detail, String value, int first, int second) {
            this.kind=kind; this.title=title; this.detail=detail; this.value=value; this.first=first; this.second=second;
        }
    }
    private VoiceAction parseVoiceAction(String text) {
        if (text == null) return null;
        Matcher search = Pattern.compile("^\\s*cerca\\s+(.+?)\\s+(?:sul web|su internet)[.!?]*\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (search.matches()) return new VoiceAction("search", "Cerca sul web", "Aprire una ricerca per: " + search.group(1).trim() + "?", search.group(1).trim(), 0, 0);
        Matcher timer = Pattern.compile("(?:metti|imposta|avvia)?\\s*(?:un\\s+)?timer(?:\\s+di)?\\s+(\\d+|un[oa]?|due|tre|quattro|cinque|dieci|quindici|venti|trenta|sessanta)\\s+(secondi|minuti|ore)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (timer.find()) {
            int amount = voiceNumber(timer.group(1));
            int seconds = amount * ("ore".equalsIgnoreCase(timer.group(2)) ? 3600 : "minuti".equalsIgnoreCase(timer.group(2)) ? 60 : 1);
            if (amount > 0 && seconds <= 86400) return new VoiceAction("timer", "Imposta un timer", "Impostare un timer di " + amount + " " + timer.group(2) + "?", "", seconds, 0);
        }
        Matcher alarm = Pattern.compile("(?:metti|imposta)?\\s*(?:una\\s+)?sveglia\\s+alle\\s+(\\d{1,2})(?:[:.](\\d{2}))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        if (alarm.find()) {
            int hour = Integer.parseInt(alarm.group(1)), minute = alarm.group(2) == null ? 0 : Integer.parseInt(alarm.group(2));
            if (hour <= 23 && minute <= 59) return new VoiceAction("alarm", "Imposta una sveglia", String.format(Locale.ITALIAN, "Impostare una sveglia alle %02d:%02d?", hour, minute), "", hour, minute);
        }
        Matcher event = Pattern.compile("(?:crea|aggiungi|metti|fissa)?\\s*(?:un\\s+)?appuntamento(?:\\s+(.+))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text.trim());
        if (event.matches()) {
            String title = event.group(1) == null ? "" : event.group(1).trim();
            return new VoiceAction("calendar", "Crea un appuntamento", title.isEmpty() ? "Aprire il calendario per inserire un appuntamento?" : "Aprire il calendario con questa bozza: " + title + "?", title, 0, 0);
        }
        return null;
    }
    private int voiceNumber(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "un": case "uno": case "una": return 1;
            case "due": return 2; case "tre": return 3; case "quattro": return 4; case "cinque": return 5;
            case "dieci": return 10; case "quindici": return 15; case "venti": return 20; case "trenta": return 30; case "sessanta": return 60;
            default: return 0;
        }
    }
    private void confirmVoiceAction(VoiceAction action) {
        finishSession("In attesa della tua conferma.");
        new android.app.AlertDialog.Builder(this).setTitle(action.title).setMessage(action.detail)
                .setNegativeButton("Annulla", (dialog, which) -> status.setText("Azione annullata. Premi Avvia per riprendere."))
                .setPositiveButton("Continua", (dialog, which) -> {
                    Intent intent;
                    if ("search".equals(action.kind)) {
                        intent = new Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, action.value);
                    } else if ("timer".equals(action.kind)) {
                        intent = new Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, action.first)
                                .putExtra(AlarmClock.EXTRA_MESSAGE, "Timer Solphivia").putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    } else if ("alarm".equals(action.kind)) {
                        intent = new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, action.first)
                                .putExtra(AlarmClock.EXTRA_MINUTES, action.second).putExtra(AlarmClock.EXTRA_MESSAGE, "Sveglia Solphivia")
                                .putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                    } else {
                        intent = new Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                                .putExtra(CalendarContract.Events.TITLE, action.value.isEmpty() ? "Appuntamento" : action.value);
                    }
                    try { startActivity(intent); }
                    catch (RuntimeException error) { status.setText("Azione non disponibile sul telefono. Nessuna modifica è stata applicata."); }
                }).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable roundRect(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(radius);
        shape.setStroke(dp(1), stroke); return shape;
    }
    private JSONArray loadHistory() {
        try { return new JSONArray(getSharedPreferences("chat", 0).getString("history", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private void speakAndResume(String message) {
        if (!listeningRequested || !foregroundUnlocked()) return;
        phase = Phase.SPEAKING; utterance = "voice-access-" + (++generation);
        avatarSpeech.setText(message); status.setText(message); avatar.setAnimation("talk");
        main.removeCallbacks(timeout); main.postDelayed(timeout, 45000);
        if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR)
            finishSession("Risposta vocale non disponibile. Riprova.");
    }
    private void sendToSolphivia(String request) {
        main.removeCallbacks(timeout);
        if (!OpenAiChatClient.isConnected(this)) {
            speakAndResume("Per rispondere alle richieste vocali, collega prima ChatGPT dalla schermata principale.");
            return;
        }
        phase = Phase.RESPONDING; avatar.setAnimation("thinking");
        avatarSpeech.setText("Ci penso…"); status.setText("Solphivia sta pensando…");
        JSONArray history = loadHistory();
        try { history.put(new JSONObject().put("role", "user").put("content", request)); }
        catch (Exception error) { speakAndResume("Non sono riuscita a preparare la richiesta. Riprova."); return; }
        while (history.length() > 16) history.remove(0);
        main.postDelayed(timeout, 45000);
        OpenAiChatClient.respond(this, voiceWorker, history, new OpenAiChatClient.ChatCallback() {
            @Override public void onSuccess(String reply, String animationName, String motion, String mode) {
                main.post(() -> {
                    if (!listeningRequested || isFinishing() || isDestroyed()) return;
                    getSharedPreferences("chat", 0).edit().putString("history", history.toString()).apply();
                    speakAndResume(reply);
                });
            }
            @Override public void onError(String message) {
                main.post(() -> {
                    if (!listeningRequested || isFinishing() || isDestroyed()) return;
                    speakAndResume("Non riesco a completare la richiesta: " + message);
                });
            }
        });
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
        listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryWake);
        releaseRecognizer(); if (tts != null) tts.stop();
        start.setEnabled(true); stop.setEnabled(false); name.setEnabled(true); network.setEnabled(true); status.setText(message); avatarSpeech.setText(message); if (avatar != null) avatar.setAnimation("idle");
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 71) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) begin();
            else status.setText("Microfono negato. Puoi consentirlo nelle impostazioni dell’app.");
        }
    }
    @Override protected void onResume() { super.onResume(); resumed = true; }
    @Override protected void onStart() { super.onStart(); PetService.setVoiceAccessVisible(true); }
    @Override protected void onStop() { PetService.setVoiceAccessVisible(false); super.onStop(); }
    @Override protected void onPause() {
        resumed = false; finishSession("Ascolto interrotto. Premi Avvia e attendi il saluto per ripartire.");
        if (previewDialog != null) previewDialog.dismiss();
        super.onPause();
    }
    @Override protected void onDestroy() { finishSession("Ascolto terminato."); voiceWorker.shutdownNow(); if (tts != null) { tts.shutdown(); tts = null; } super.onDestroy(); }
}
