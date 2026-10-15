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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** App-foreground wake phrase and bounded command session. No background or locked-screen listening. */
public final class VoiceAccessActivity extends Activity {
    private enum Phase { IDLE, WAKE, GREETING, COMMAND, RESPONDING, SPEAKING }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService voiceWorker = Executors.newSingleThreadExecutor();
    private Phase phase = Phase.IDLE;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status;
    private boolean resumed, ttsReady;
    private boolean requestingPermission;
    private boolean closedInternally;
    private int generation;
    private int wakeFailures;
    private boolean listeningRequested;
    private String utterance;
    private String sessionName = "Marco";
    private android.app.AlertDialog previewDialog;
    private final Runnable timeout = () -> finishSession("La richiesta ha impiegato troppo tempo. Riapri Solphivia per riprovare.");
    private final Runnable retryListening = () -> {
        if (listeningRequested && foregroundUnlocked() &&
                (phase == Phase.WAKE || phase == Phase.COMMAND)) listen(phase);
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new android.view.View(this));
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        status = new TextView(this);
        sessionName = getSharedPreferences("voice_access", 0).getString("name", "").trim();
        if (sessionName.isEmpty()) sessionName = "Marco";
        tts = new TextToSpeech(this, code -> main.post(() -> {
            if (isDestroyed() || tts == null) return;
            if (code == TextToSpeech.SUCCESS) {
                android.speech.tts.Voice voice = SpeechVoiceSelection.select(tts, Locale.ITALIAN, true);
                ttsReady = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS;
            }
            if (ttsReady) {
                if (resumed) begin();
            } else {
                finishSession("Voce italiana offline non disponibile. Installala nelle impostazioni TTS e riapri Solphivia.");
            }
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
        if (!foregroundUnlocked() || phase != Phase.IDLE || !ttsReady || requestingPermission) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestingPermission = true;
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 71); return;
        }
        enterOverlaySession();
        listeningRequested = true; wakeFailures = 0; listen(Phase.WAKE);
    }

    /** Keep the activity alive for foreground microphone access while the existing home avatar remains visible. */
    private void enterOverlaySession() {
        setContentView(new android.view.View(this));
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
        MainActivity.setVoiceAccessPresentation("Sono qui. Di' «Hola, Solphivia!» quando sei pronto.", "sit");
    }

    private void presentAvatar(String text, String pose) {
        MainActivity.setVoiceAccessPresentation(text, pose);
    }

    private void listen(Phase next) {
        if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
        releaseRecognizer(); main.removeCallbacks(timeout);
        phase = next; int token = ++generation;
        try {
            boolean local = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
            if (!local) { finishSession("Riconoscimento vocale offline non disponibile. Installa il modello italiano e riprova."); return; }
            recognizer = createOnDeviceRecognizer();
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {
                    if (token != generation) return;
                    if (next == Phase.WAKE) {
                        status.setText("In attesa di «Hola, Solphivia!»… Di' «Fermati» per spegnere il microfono.");
                        presentAvatar("Sono qui. Di' «Hola, Solphivia!» quando sei pronto.", "sit");
                    } else { status.setText("In ascolto: dimmi cosa vuoi fare."); presentAvatar("Ti ascolto…", "listen"); }
                }
                public void onBeginningOfSpeech() { }
                public void onRmsChanged(float rms) { }
                public void onBufferReceived(byte[] buffer) { }
                public void onEndOfSpeech() { }
                public void onPartialResults(Bundle b) { }
                public void onEvent(int type, Bundle b) { }
                public void onError(int error) {
                    if (token != generation) return;
                    if (listeningRequested && foregroundUnlocked() && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        wakeFailures++;
                        releaseRecognizer(); main.removeCallbacks(timeout);
                        status.setText(next == Phase.WAKE ? "Resto in attesa di «Hola, Solphivia!»…" : "Resto in ascolto dei tuoi comandi…");
                        main.postDelayed(retryListening, Math.min(5000, 750L * wakeFailures));
                    } else finishSession("Ascolto terminato. Riapri Solphivia per riprovare.");
                }
                public void onResults(Bundle results) {
                    if (token != generation) return;
                    if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                    wakeFailures = 0;
                    ++generation; releaseRecognizer(); main.removeCallbacks(timeout);
                    if (VoiceAccessRules.stop(text)) {
                        finishSession("Va bene, mi fermo.");
                        return;
                    }
                    if (next == Phase.WAKE) {
                        if (!VoiceAccessRules.wake(text)) {
                            status.setText("Saluto non riconosciuto. Resto in attesa di «Hola, Solphivia!»…");
                            listen(Phase.WAKE); return;
                        }
                        phase = Phase.GREETING; utterance = "voice-access-" + generation;
                        String greeting = VoiceAccessRules.greeting(sessionName); status.setText(greeting);
                        presentAvatar(greeting, "wave");
                        if (tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) finishSession("Risposta vocale non disponibile.");
                    } else {
                        presentAvatar("Hai detto: " + text, "listen");
                        VoiceAction action = parseVoiceAction(text);
                        if (action != null) { confirmVoiceAction(action); }
                        else if (VoiceAccessRules.openWhatsApp(text)) {
                            openWhatsApp();
                        } else {
                            String draft = ShareBridge.preview(text);
                            if (draft != null) {
                                pauseForConfirmation("Microfono spento. Controlla il testo prima di condividere.");
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
            recognizer.startListening(intent);
        } catch (RuntimeException error) { finishSession("Riconoscimento non disponibile. Controlla permesso microfono e servizio vocale."); }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.S)
    private SpeechRecognizer createOnDeviceRecognizer() {
        return SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
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
        pauseForConfirmation("In attesa della tua conferma.");
        setPassThrough(false);
        android.app.AlertDialog actionDialog = new android.app.AlertDialog.Builder(this).setTitle(action.title).setMessage(action.detail)
                .setNegativeButton("Annulla", (dialog, which) -> speakAndResume("Azione annullata."))
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
                }).create();
        actionDialog.setOnDismissListener(ignored -> setPassThrough(true));
        actionDialog.show();
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
        presentAvatar(message, "talk"); status.setText(message);
        main.removeCallbacks(timeout); main.postDelayed(timeout, 45000);
        if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR)
            finishSession("Risposta vocale non disponibile. Riprova.");
    }
    private void pauseForConfirmation(String message) {
        ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); if (tts != null) tts.stop();
        status.setText(message); presentAvatar(message, "listen");
    }
    private void sendToSolphivia(String request) {
        main.removeCallbacks(timeout);
        if (!OpenAiChatClient.isConnected(this)) {
            speakAndResume("Per rispondere alle richieste vocali, collega prima ChatGPT dalla schermata principale.");
            return;
        }
        phase = Phase.RESPONDING; presentAvatar("Ci penso…", "thinking");
        status.setText("Solphivia sta pensando…");
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
        if (launch == null) { speakAndResume("WhatsApp non è installato o non è disponibile."); return; }
        try { MainActivity.setVoiceAccessPresentation("WhatsApp aperto. Ascolto terminato.", "sit"); startActivity(launch); }
        catch (RuntimeException error) { speakAndResume("Impossibile aprire WhatsApp."); }
    }

    private void showSharePreview(String draft) {
        pauseForConfirmation("Microfono spento. Controlla il testo prima di condividere.");
        setPassThrough(false);
        EditText editor = new EditText(this);
        editor.setContentDescription("Testo da condividere su WhatsApp");
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
        editor.setText(draft);
        previewDialog = new android.app.AlertDialog.Builder(this)
                .setTitle("ShareBridge · Anteprima")
                .setMessage("Aprire la condivisione WhatsApp? Sceglierai il destinatario e confermerai l’invio dentro WhatsApp.")
                .setView(editor).setNegativeButton("Annulla", (dialog, which) -> speakAndResume("Condivisione annullata."))
                .setPositiveButton("Apri condivisione WhatsApp", null).create();
        previewDialog.setOnDismissListener(dialog -> { editor.setText(""); previewDialog = null; setPassThrough(true); });
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
    private void setPassThrough(boolean enabled) {
        int flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        if (enabled) getWindow().addFlags(flags); else getWindow().clearFlags(flags);
    }
    private boolean foregroundUnlocked() {
        return resumed && getSystemService(android.os.PowerManager.class).isInteractive()
                && !getSystemService(android.app.KeyguardManager.class).isKeyguardLocked();
    }
    private void finishSession(String message) {
        if (isFinishing() || isDestroyed()) return;
        closedInternally = true;
        listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); if (tts != null) tts.stop();
        if (status != null) status.setText(message);
        MainActivity.setVoiceAccessPresentation(message, "sit");
        finish();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request == 71) {
            requestingPermission = false;
            setPassThrough(true);
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) begin();
            else {
                finishSession("Microfono negato. Puoi consentirlo nelle impostazioni di Solphivia.");
            }
        }
    }
    @Override protected void onResume() { super.onResume(); resumed = true; if (ttsReady && !listeningRequested && phase == Phase.IDLE && !requestingPermission) begin(); }
    @Override protected void onStart() { super.onStart(); PetService.setVoiceAccessVisible(true); }
    @Override protected void onStop() { PetService.setVoiceAccessVisible(false); super.onStop(); }
    @Override protected void onPause() {
        resumed = false;
        if (!requestingPermission) {
            if (!closedInternally) MainActivity.restartVoiceAccessWhenResumed();
            finishSession("Ascolto interrotto.");
        }
        if (previewDialog != null) previewDialog.dismiss();
        super.onPause();
    }
    @Override protected void onDestroy() { finishSession("Ascolto terminato."); voiceWorker.shutdownNow(); if (tts != null) { tts.shutdown(); tts = null; } super.onDestroy(); }
}
