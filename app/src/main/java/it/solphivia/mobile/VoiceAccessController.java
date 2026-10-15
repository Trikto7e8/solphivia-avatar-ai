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
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Foreground voice session hosted by a transparent activity while the floating avatar stays visible. */
final class VoiceAccessController {
    private enum Phase { IDLE, WAKE, GREETING, COMMAND, RESPONDING, SPEAKING }
    private final Activity host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService voiceWorker = Executors.newSingleThreadExecutor();
    private Phase phase = Phase.IDLE;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status;
    private boolean resumed, ttsReady;
    private boolean requestingPermission;
    private boolean sessionSuppressed;
    private boolean userStopped;
    private boolean destroyed;
    private int generation;
    private int wakeFailures;
    private boolean listeningRequested;
    private String utterance;
    private String sessionName = "Marco";
    private android.app.AlertDialog previewDialog;
    private final Runnable timeout = () -> {
        sessionSuppressed = true;
        finishSession("La richiesta ha impiegato troppo tempo. Riapri Solphivia per riprovare.");
    };
    private final Runnable retryListening = () -> {
        if (listeningRequested && foregroundUnlocked() &&
                (phase == Phase.WAKE || phase == Phase.COMMAND)) listen(phase);
    };

    VoiceAccessController(Activity host) {
        this.host = host;
        status = new TextView(host);
        sessionName = host.getSharedPreferences("voice_access", 0).getString("name", "").trim();
        if (sessionName.isEmpty()) sessionName = "Marco";
        tts = new TextToSpeech(host, code -> main.post(() -> {
            if (destroyed || host.isDestroyed() || tts == null) return;
            if (code == TextToSpeech.SUCCESS) {
                android.speech.tts.Voice voice = SpeechVoiceSelection.select(tts, Locale.ITALIAN, true);
                ttsReady = voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS;
            }
            if (ttsReady) {
                if (resumed) begin();
            } else {
                sessionSuppressed = true;
                finishSession("Voce italiana offline non disponibile. Installala nelle impostazioni TTS e riapri Solphivia.");
            }
        }));
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) { }
            public void onDone(String id) { main.post(() -> {
                if (foregroundUnlocked() && (phase == Phase.GREETING || phase == Phase.SPEAKING) && id.equals(utterance)) listen(Phase.COMMAND);
            }); }
            public void onError(String id) { main.post(() -> {
                if (id.equals(utterance)) {
                    sessionSuppressed = true;
                    finishSession("Errore nella risposta vocale. Riprova.");
                }
            }); }
        });
    }

    private void begin() {
        if (!foregroundUnlocked() || phase != Phase.IDLE || !ttsReady || requestingPermission || sessionSuppressed || userStopped || destroyed) return;
        if (host.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestingPermission = true;
            host.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 71); return;
        }
        PetService.setVoiceAccessVisible(true);
        PetService.setVoiceAccessPresentation("Sono qui. Di' «Hola, Solphivia!» quando sei pronto.", "sit");
        listeningRequested = true; wakeFailures = 0; listen(Phase.WAKE);
    }

    private void presentAvatar(String text, String pose) {
        PetService.setVoiceAccessPresentation(text, pose);
    }

    private void listen(Phase next) {
        if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
        releaseRecognizer(); main.removeCallbacks(timeout);
        phase = next; int token = ++generation;
        try {
            boolean local = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(host);
            if (!local) {
                sessionSuppressed = true;
                finishSession("Riconoscimento vocale offline non disponibile. Installa il modello italiano e riprova.");
                return;
            }
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
                public void onSegmentResults(Bundle results) {
                    if (token == generation) handleResults(next, token, results, false);
                }
                public void onEndOfSegmentedSession() {
                    if (token != generation) return;
                    releaseRecognizer();
                    if (listeningRequested && foregroundUnlocked()) {
                        main.postDelayed(retryListening, 1500);
                    } else finishSession("Ascolto interrotto.");
                }
                public void onError(int error) {
                    if (token != generation) return;
                    if (listeningRequested && foregroundUnlocked() && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        if (++wakeFailures >= 5) {
                            sessionSuppressed = true;
                            finishSession("Il riconoscimento vocale si è interrotto. Riapri Solphivia per riprovare.");
                            return;
                        }
                        releaseRecognizer(); main.removeCallbacks(timeout);
                        status.setText(next == Phase.WAKE ? "Resto in attesa di «Hola, Solphivia!»…" : "Resto in ascolto dei tuoi comandi…");
                        main.postDelayed(retryListening, Math.min(5000, 750L * wakeFailures));
                    } else {
                        sessionSuppressed = true;
                        finishSession("Ascolto terminato. Riapri Solphivia per riprovare.");
                    }
                }
                public void onResults(Bundle results) {
                    if (token == generation) handleResults(next, token, results, true);
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            if (next == Phase.WAKE && Build.VERSION.SDK_INT >= 33) {
                intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
                        .putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION,
                                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS);
            }
            status.setText("Preparazione ascolto…");
            recognizer.startListening(intent);
        } catch (RuntimeException error) {
            sessionSuppressed = true;
            finishSession("Riconoscimento non disponibile. Controlla permesso microfono e servizio vocale.");
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.S)
    private SpeechRecognizer createOnDeviceRecognizer() {
        return SpeechRecognizer.createOnDeviceSpeechRecognizer(host);
    }

    private void handleResults(Phase next, int token, Bundle results, boolean sessionEnded) {
        if (token != generation) return;
        if (!foregroundUnlocked()) { finishSession("Ascolto interrotto."); return; }
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
        if (text.trim().isEmpty()) {
            if (sessionEnded && next == Phase.WAKE && listeningRequested) listen(Phase.WAKE);
            return;
        }
        wakeFailures = 0;
        if (next == Phase.WAKE && !VoiceAccessRules.stop(text) && !VoiceAccessRules.wake(text) && !sessionEnded) {
            status.setText("In attesa di «Hola, Solphivia!»…");
            return;
        }
        ++generation; releaseRecognizer(); main.removeCallbacks(timeout);
        if (VoiceAccessRules.stop(text)) {
            userStopped = true;
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
            if (tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) {
                sessionSuppressed = true;
                finishSession("Risposta vocale non disponibile.");
            }
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
        android.app.AlertDialog actionDialog = new android.app.AlertDialog.Builder(host).setTitle(action.title).setMessage(action.detail)
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
                    try { host.startActivity(intent); }
                    catch (RuntimeException error) { status.setText("Azione non disponibile sul telefono. Nessuna modifica è stata applicata."); }
                }).create();
        actionDialog.setCancelable(false);
        actionDialog.show();
    }

    private JSONArray loadHistory() {
        try { return new JSONArray(host.getSharedPreferences("chat", 0).getString("history", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private void speakAndResume(String message) {
        if (!listeningRequested || !foregroundUnlocked()) return;
        phase = Phase.SPEAKING; utterance = "voice-access-" + (++generation);
        presentAvatar(message, "talk"); status.setText(message);
        main.removeCallbacks(timeout); main.postDelayed(timeout, 45000);
        if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) {
            sessionSuppressed = true;
            finishSession("Risposta vocale non disponibile. Riprova.");
        }
    }
    private void pauseForConfirmation(String message) {
        ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); if (tts != null) tts.stop();
        PetService.setVoiceAccessVisible(false);
        status.setText(message); presentAvatar(message, "listen");
    }
    private void sendToSolphivia(String request) {
        main.removeCallbacks(timeout);
        if (!OpenAiChatClient.isConnected(host)) {
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
        OpenAiChatClient.respond(host, voiceWorker, history, new OpenAiChatClient.ChatCallback() {
            @Override public void onSuccess(String reply, String animationName, String motion, String mode) {
                main.post(() -> {
                    if (!listeningRequested || host.isFinishing() || host.isDestroyed()) return;
                    host.getSharedPreferences("chat", 0).edit().putString("history", history.toString()).apply();
                    speakAndResume(reply);
                });
            }
            @Override public void onError(String message) {
                main.post(() -> {
                    if (!listeningRequested || host.isFinishing() || host.isDestroyed()) return;
                    speakAndResume("Non riesco a completare la richiesta: " + message);
                });
            }
        });
    }

    private void openWhatsApp() {
        Intent launch = host.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) { speakAndResume("WhatsApp non è installato o non è disponibile."); return; }
        try { finishSession("WhatsApp aperto. Ascolto terminato."); host.startActivity(launch); }
        catch (RuntimeException error) { speakAndResume("Impossibile aprire WhatsApp."); }
    }

    private void showSharePreview(String draft) {
        pauseForConfirmation("Microfono spento. Controlla il testo prima di condividere.");
        EditText editor = new EditText(host);
        editor.setContentDescription("Testo da condividere su WhatsApp");
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
        editor.setText(draft);
        previewDialog = new android.app.AlertDialog.Builder(host)
                .setTitle("ShareBridge · Anteprima")
                .setMessage("Aprire la condivisione WhatsApp? Sceglierai il destinatario e confermerai l’invio dentro WhatsApp.")
                .setView(editor).setNegativeButton("Annulla", (dialog, which) -> speakAndResume("Condivisione annullata."))
                .setPositiveButton("Apri condivisione WhatsApp", null).create();
        previewDialog.setCancelable(false);
        previewDialog.setOnDismissListener(dialog -> { editor.setText(""); previewDialog = null; });
        previewDialog.show();
        previewDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = editor.getText().toString().trim();
            if (text.isEmpty()) { editor.setError("Inserisci un testo oppure annulla"); return; }
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, text);
            previewDialog.dismiss();
            try { finishSession("Condivisione aperta. Nessun invio automatico."); host.startActivity(share); status.setText("Condivisione aperta. Nessun invio automatico."); }
            catch (RuntimeException error) { status.setText("Condivisione WhatsApp non disponibile. Nessun messaggio inviato."); }
        });
    }

    private void releaseRecognizer() {
        if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
    }
    private boolean foregroundUnlocked() {
        android.os.PowerManager power = host.getSystemService(android.os.PowerManager.class);
        android.app.KeyguardManager keyguard = host.getSystemService(android.app.KeyguardManager.class);
        return resumed && power != null && power.isInteractive()
                && keyguard != null && !keyguard.isKeyguardLocked() && !host.isFinishing() && !host.isDestroyed();
    }
    private void finishSession(String message) {
        if (destroyed || host.isFinishing() || host.isDestroyed()) return;
        listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); if (tts != null) tts.stop();
        PetService.setVoiceAccessVisible(false);
        if (status != null) status.setText(message);
        PetService.setVoiceAccessPresentation(message, "sit");
    }
    void onPermissionResult(int request, int[] grants) {
        if (request == 71) {
            requestingPermission = false;
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) begin();
            else {
                sessionSuppressed = true;
                finishSession("Microfono negato. Puoi consentirlo nelle impostazioni di Solphivia.");
            }
        }
    }
    void onNewLaunch() { sessionSuppressed = false; userStopped = false; }
    void onResume() {
        resumed = true;
        if (ttsReady && !listeningRequested && phase == Phase.IDLE && !requestingPermission) begin();
    }
    void onWindowFocusChanged(boolean hasFocus) { if (hasFocus) onResume(); }
    void onPause() {
        resumed = false;
        if (!requestingPermission) {
            listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
            main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
            releaseRecognizer(); if (tts != null) tts.stop();
            PetService.setVoiceAccessVisible(false);
        }
        if (previewDialog != null) previewDialog.dismiss();
    }
    void onDestroy() {
        destroyed = true; listeningRequested = false; ++generation;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening); releaseRecognizer();
        voiceWorker.shutdownNow(); if (tts != null) { tts.shutdown(); tts = null; }
        PetService.setVoiceAccessVisible(false);
    }
    boolean isActive() { return listeningRequested && resumed && !sessionSuppressed && !userStopped; }
}
