package it.solphivia.mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.*;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.app.SearchManager;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.RecognitionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Voice session owned by the user-started microphone foreground service. */
final class VoiceAccessController {
    private static VoiceAccessController activeController;
    private long ignoreAudioUntil;
    private String lastWakePartial = "";
    private final Runnable stableWake = () -> {
        if (this.phase != Phase.WAKE || !VoiceAccessRules.wake(lastWakePartial)) return;
        try {
            handleVoskResult(new JSONObject().put("text", lastWakePartial).toString(), true);
        } catch (JSONException ignored) { }
    };

    static void onAvatarSpeechChanged(boolean speaking) {
        VoiceAccessController controller = activeController;
        if (controller == null || controller.destroyed) return;
        controller.pauseRecognition();
        controller.main.removeCallbacks(controller.retryListening);
        if (!speaking) {
            controller.ignoreAudioUntil = android.os.SystemClock.elapsedRealtime() + 500;
            controller.main.postDelayed(controller.retryListening, 500);
        }
    }
    private enum Phase { IDLE, WAKE, GREETING, COMMAND, RESPONDING, SPEAKING }
    private final PetService host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService voiceWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService modelWorker = Executors.newSingleThreadExecutor();
    private Phase phase = Phase.IDLE;
    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private Future<?> modelTask;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback wifiCallback;
    private TextToSpeech tts;
    private TextView status;
    private boolean resumed, ttsReady;
    private boolean englishAccentActive;
    private boolean sessionSuppressed;
    private boolean userStopped;
    private boolean destroyed;
    private int generation;
    private boolean listeningRequested;
    private boolean modelLoading;
    private int streamGeneration;
    private int streamFailures;
    private String utterance;
    private String sessionName = "Marco";
    private android.app.AlertDialog previewDialog;
    private android.app.AlertDialog actionDialog;
    private final Runnable timeout = () -> {
        recoverResponse("La risposta ha impiegato troppo tempo.");
    };
    private final Runnable retryListening = () -> {
        if (!listeningRequested || !screenUnlocked() || destroyed
                || (phase != Phase.WAKE && phase != Phase.COMMAND)) return;
        if (speechService == null) prepareStreaming();
        else resumeRecognition();
    };

    VoiceAccessController(PetService host) {
        this.host = host;
        activeController = this;
        status = new TextView(host);
        sessionName = host.getSharedPreferences("voice_access", 0).getString("name", "").trim();
        if (sessionName.isEmpty()) sessionName = "Marco";
        String engine = host.getSharedPreferences("pet", 0).getString("tts_engine", "");
        tts = new TextToSpeech(host, code -> main.post(() -> {
            if (destroyed || tts == null) return;
            if (code == TextToSpeech.SUCCESS) configureTtsVoice();
            if (ttsReady) {
                if (resumed) begin();
            } else {
                sessionSuppressed = true;
                finishSession("La voce scelta da Android non è disponibile. Controlla Sintesi vocale nelle impostazioni e riapri Solphivia.");
            }
        }), engine.isEmpty() ? null : engine);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) { android.util.Log.d("SolphiviaVoice", "Voice Access TTS started"); }
            public void onDone(String id) { main.post(() -> {
                android.util.Log.d("SolphiviaVoice", "Voice Access TTS completed");
                if (screenUnlocked() && (phase == Phase.GREETING || phase == Phase.SPEAKING) && id.equals(utterance)) {
                    main.removeCallbacks(timeout);
                    ignoreAudioUntil = android.os.SystemClock.elapsedRealtime() + 500;
                    resumeListening(Phase.COMMAND);
                }
            }); }
            public void onError(String id) { main.post(() -> {
                android.util.Log.w("SolphiviaVoice", "Voice Access TTS failed");
                if (id != null && id.equals(utterance)) {
                    recoverResponse("Android non è riuscito a pronunciare la risposta.");
                }
            }); }
        });
    }

    private void configureTtsVoice() {
        boolean requestedEnglish = host.getSharedPreferences("pet", 0).getBoolean("english_accent", false);
        Locale locale = requestedEnglish ? Locale.UK : Locale.ITALIAN;
        int language = tts.setLanguage(locale);
        ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
        englishAccentActive = requestedEnglish && ttsReady;
        android.speech.tts.Voice best = ttsReady ? SpeechVoiceSelection.select(tts, locale, requestedEnglish) : null;
        boolean selected = best != null && tts.setVoice(best) == TextToSpeech.SUCCESS;
        if (requestedEnglish && !selected) {
            int fallback = tts.setLanguage(Locale.ITALIAN);
            ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED;
            englishAccentActive = false;
        }
        if (ttsReady) {
            int rate = android.provider.Settings.Secure.getInt(host.getContentResolver(), android.provider.Settings.Secure.TTS_DEFAULT_RATE, 100);
            int pitch = android.provider.Settings.Secure.getInt(host.getContentResolver(), android.provider.Settings.Secure.TTS_DEFAULT_PITCH, 100);
            tts.setSpeechRate(Math.max(0.1f, rate / 100f));
            tts.setPitch(Math.max(0.1f, pitch / 100f));
        }
    }

    private int speak(String text, String id) {
        String spoken = englishAccentActive ? ItalianEnglishAccent.respell(text) : text;
        return tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    private void begin() {
        if (!screenUnlocked() || !ttsReady || sessionSuppressed || userStopped || destroyed) return;
        if (listeningRequested) {
            if (model != null && speechService == null && !modelLoading) prepareStreaming();
            return;
        }
        if (host.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            finishSession("Consenti il microfono riaprendo Solphivia."); return;
        }
        PetService.prepareForVoiceAccess();
        PetService.setVoiceAccessVisible(true);
        listeningRequested = true; phase = Phase.WAKE;
        ensureModelAndStart();
    }

    private void presentAvatar(String text, String pose) {
        PetService.setVoiceAccessPresentation(text, pose);
    }

    private void ensureModelAndStart() {
        if (!listeningRequested || !screenUnlocked() || modelLoading) return;
        if (model != null) { prepareStreaming(); return; }
        File installed = OfflineVoiceModelInstaller.modelDirectory(host);
        if (OfflineVoiceModelInstaller.isInstalled(installed)) {
            modelLoading = true;
            modelTask = modelWorker.submit(() -> loadModel(installed));
            return;
        }
        if (!OfflineVoiceModelInstaller.isWifiConnected(host)) {
            presentAvatar("Per attivare l’ascolto senza interruzioni, connetti il Wi-Fi per scaricare una volta il modello vocale (48 MB).", "sit");
            status.setText("In attesa del Wi-Fi per il modello vocale offline.");
            registerWifiCallback();
            return;
        }
        modelLoading = true;
        presentAvatar("Scarico il modello vocale offline via Wi-Fi…", "thinking");
        modelTask = modelWorker.submit(() -> {
            try {
                File dir = OfflineVoiceModelInstaller.downloadAndInstall(host, (downloaded, total) -> {
                    if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Download interrotto.");
                    if (total > 0 && downloaded % (1024 * 1024) < 32 * 1024) {
                        int percent = (int) Math.min(100, downloaded * 100 / total);
                        main.post(() -> {
                            if (listeningRequested && !destroyed) presentAvatar("Preparo il riconoscimento offline… " + percent + "%", "thinking");
                        });
                    }
                });
                loadModel(dir);
            } catch (Exception error) {
                main.post(() -> {
                    modelLoading = false;
                    if (listeningRequested && !destroyed) {
                        presentAvatar("Non riesco a scaricare il modello vocale. Resta sul Wi-Fi e riapri Solphivia.", "sit");
                        status.setText("Download modello offline non riuscito: " + error.getMessage());
                    }
                });
            }
        });
    }

    private void loadModel(File dir) {
        try {
            Model loaded = new Model(dir.getAbsolutePath());
            main.post(() -> {
                modelLoading = false;
                if (destroyed || !listeningRequested || !screenUnlocked()) { loaded.close(); return; }
                model = loaded;
                unregisterWifiCallback();
                prepareStreaming();
            });
        } catch (IOException | RuntimeException error) {
            main.post(() -> {
                modelLoading = false;
                if (listeningRequested && !destroyed) {
                    presentAvatar("Il modello vocale non si è caricato. Riprova con il Wi-Fi attivo.", "sit");
                    status.setText("Caricamento modello offline non riuscito: " + error.getMessage());
                }
            });
        }
    }

    private void prepareStreaming() {
        if (!listeningRequested || !screenUnlocked() || model == null
                || (phase != Phase.WAKE && phase != Phase.COMMAND)) return;
        if (speechService != null) { resumeRecognition(); return; }
        try {
            if (recognizer == null) recognizer = new Recognizer(model, 16000.0f);
            if (speechService == null) speechService = new SpeechService(recognizer, 16000.0f);
            final int stream = ++streamGeneration;
            if (!speechService.startListening(new RecognitionListener() {
                @Override public void onResult(String hypothesis) { main.post(() -> {
                    if (stream != streamGeneration) return;
                    streamFailures = 0;
                    handleVoskResult(hypothesis, true);
                }); }
                @Override public void onPartialResult(String hypothesis) {
                    main.post(() -> {
                        if (stream != streamGeneration || destroyed || !listeningRequested || !screenUnlocked() || phase != Phase.WAKE
                                || PetService.isAvatarSpeaking() || android.os.SystemClock.elapsedRealtime() < ignoreAudioUntil) return;
                        try {
                            String partial = new JSONObject(hypothesis).optString("partial", "").trim();
                            if (partial.equals(lastWakePartial)) return;
                            main.removeCallbacks(stableWake);
                            lastWakePartial = partial;
                            if (partial.isEmpty()) return;
                            // Common-word aliases require a stable full phrase, never a prefix.
                            if (VoiceAccessRules.wake(partial) && VoiceAccessRules.wakeCandidate(partial)) {
                                handleVoskResult(new JSONObject().put("text", partial).toString(), true);
                            } else {
                                if (VoiceAccessRules.wake(partial)) main.postDelayed(stableWake, 900);
                                presentAvatar("Sto sentendo: «" + partial.substring(0, Math.min(100, partial.length())) + "»", "sit");
                            }
                        } catch (JSONException ignored) { }
                    });
                }
                @Override public void onFinalResult(String hypothesis) { main.post(() -> {
                    if (stream != streamGeneration) return;
                    handleVoskResult(hypothesis, true);
                    recoverStream(stream);
                }); }
                @Override public void onError(Exception error) {
                    main.post(() -> recoverStream(stream));
                }
                @Override public void onTimeout() {
                    main.post(() -> recoverStream(stream));
                }
            })) {
                recoverStream(stream);
            } else {
                resumeRecognition();
                android.util.Log.d("SolphiviaVoice", "Streaming started, overlaySpeaking=" + PetService.isAvatarSpeaking());
                showPhaseStatus();
            }
        } catch (IOException | RuntimeException error) {
            recoverStream(streamGeneration);
        }
    }

    /** Recreate a terminated audio stream; unpausing a dead thread cannot restart it. */
    private void recoverStream(int stream) {
        if (stream != streamGeneration || destroyed || !listeningRequested || !screenUnlocked()) return;
        releaseRecognizer();
        main.removeCallbacks(retryListening);
        if (host.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            finishSession("Microfono non consentito. Abilitalo nelle impostazioni e riapri Solphivia.");
            return;
        }
        // Speech/confirmation completion owns the next restart in those phases.
        if (phase != Phase.WAKE && phase != Phase.COMMAND) return;
        long delay = Math.min(10000L, 500L << streamFailures);
        streamFailures = Math.min(streamFailures + 1, 5);
        status.setText("Ripristino del microfono…");
        presentAvatar("Il microfono si è interrotto. Provo a riprendere l’ascolto…", "sit");
        main.postDelayed(retryListening, delay);
    }

    private void handleVoskResult(String result, boolean finalResult) {
        if (!finalResult || destroyed || !listeningRequested || !screenUnlocked()
                || PetService.isAvatarSpeaking() || android.os.SystemClock.elapsedRealtime() < ignoreAudioUntil
                || (phase != Phase.WAKE && phase != Phase.COMMAND)) return;
        String text;
        try { text = new JSONObject(result).optString("text", "").trim(); }
        catch (JSONException ignored) { text = ""; }
        main.removeCallbacks(stableWake);
        lastWakePartial = "";
        if (text.isEmpty()) { if (speechService != null) speechService.reset(); return; }
        if (phase == Phase.WAKE && !VoiceAccessRules.stop(text) && !VoiceAccessRules.wake(text)) {
            resumeRecognition();
            status.setText("In ascolto, in attesa di «Hola, Solphivia!»…");
            presentAvatar("Ho sentito «" + text.substring(0, Math.min(100, text.length())) + "». Sono ancora in ascolto: riprova «Hola, Solphivia!».", "sit");
            return;
        }
        pauseRecognition();
        ++generation; main.removeCallbacks(timeout);
        if (VoiceAccessRules.stop(text)) {
            userStopped = true;
            finishSession("Va bene, mi fermo.");
            return;
        }
        if (VoiceAccessRules.wake(text)) {
            phase = Phase.GREETING; utterance = "voice-access-" + generation;
            String greeting = VoiceAccessRules.greeting(sessionName); status.setText(greeting);
            presentAvatar(greeting, "wave");
            main.postDelayed(timeout, 12000);
            if (speak(greeting, utterance) == TextToSpeech.ERROR) {
                recoverResponse("Risposta vocale non disponibile.");
            }
        } else {
            phase = Phase.RESPONDING;
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

    private void resumeListening(Phase next) {
        if (!listeningRequested || !screenUnlocked()) return;
        phase = next;
        if (recognizer == null || speechService == null) prepareStreaming();
        else resumeRecognition();
        showPhaseStatus();
    }

    private void showPhaseStatus() {
        if (phase == Phase.WAKE) {
            status.setText("In attesa di «Hola, Solphivia!»… Di' «Fermati» per spegnere il microfono.");
            presentAvatar("Sono qui. Di' «Hola, Solphivia!» quando sei pronto.", "sit");
        } else if (phase == Phase.COMMAND) {
            status.setText("In ascolto: dimmi cosa vuoi fare.");
            presentAvatar("Ti ascolto…", "listen");
        }
    }

    private void pauseRecognition() {
        main.removeCallbacks(stableWake);
        lastWakePartial = "";
        if (speechService != null) speechService.setPause(true);
    }

    private void resumeRecognition() {
        if (phase != Phase.WAKE && phase != Phase.COMMAND) return;
        if (PetService.isAvatarSpeaking()) { pauseRecognition(); return; }
        long remaining = ignoreAudioUntil - android.os.SystemClock.elapsedRealtime();
        if (remaining > 0) {
            pauseRecognition();
            main.removeCallbacks(retryListening);
            main.postDelayed(retryListening, remaining);
            return;
        }
        if (speechService != null) {
            speechService.reset();
            speechService.setPause(false);
            android.util.Log.d("SolphiviaVoice", "Recognition resumed in " + phase);
        }
    }

    private void registerWifiCallback() {
        if (wifiCallback != null) return;
        connectivity = host.getSystemService(ConnectivityManager.class);
        if (connectivity == null) return;
        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) {
                main.post(() -> {
                    if (listeningRequested && !destroyed && OfflineVoiceModelInstaller.isWifiConnected(host)) {
                        unregisterWifiCallback();
                        ensureModelAndStart();
                    }
                });
            }
        };
        try {
            connectivity.registerNetworkCallback(new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), wifiCallback);
        } catch (RuntimeException error) { wifiCallback = null; }
    }

    private void unregisterWifiCallback() {
        if (connectivity != null && wifiCallback != null) {
            try { connectivity.unregisterNetworkCallback(wifiCallback); } catch (RuntimeException ignored) { }
        }
        wifiCallback = null;
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
        android.view.ContextThemeWrapper theme = new android.view.ContextThemeWrapper(host, android.R.style.Theme_Material_Light_Dialog_Alert);
        actionDialog = new android.app.AlertDialog.Builder(theme).setTitle(action.title).setMessage(action.detail)
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
                    try { host.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); resumeListening(Phase.COMMAND); }
                    catch (RuntimeException error) { speakAndResume("Azione non disponibile sul telefono. Nessuna modifica applicata."); }
                }).create();
        actionDialog.setCancelable(false);
        actionDialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        actionDialog.setOnDismissListener(dialog -> actionDialog = null);
        actionDialog.show();
    }

    private JSONArray loadHistory() {
        try { return new JSONArray(host.getSharedPreferences("chat", 0).getString("history", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private void speakAndResume(String message) {
        if (!listeningRequested || !screenUnlocked()) return;
        phase = Phase.SPEAKING; utterance = "voice-access-" + (++generation);
        pauseRecognition();
        presentAvatar(message, "talk"); status.setText(message);
        main.removeCallbacks(timeout); main.postDelayed(timeout, 45000);
        if (speak(message, utterance) == TextToSpeech.ERROR) {
            recoverResponse("Risposta vocale non disponibile.");
        }
    }
    private void recoverResponse(String message) {
        if (destroyed || !listeningRequested || !screenUnlocked()) return;
        ++generation;
        utterance = null;
        main.removeCallbacks(timeout);
        if (tts != null) tts.stop();
        ignoreAudioUntil = android.os.SystemClock.elapsedRealtime() + 500;
        resumeListening(Phase.COMMAND);
        presentAvatar(message + " Ti ascolto: puoi riprovare.", "listen");
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
        pauseRecognition();
        if (!OpenAiChatClient.isConnected(host)) {
            speakAndResume("Per rispondere alle richieste vocali, collega prima ChatGPT dalla schermata principale.");
            return;
        }
        phase = Phase.RESPONDING; presentAvatar("Ci penso…", "thinking");
        final int requestGeneration = generation;
        status.setText("Solphivia sta pensando…");
        JSONArray history = loadHistory();
        try { history.put(new JSONObject().put("role", "user").put("content", request)); }
        catch (Exception error) { speakAndResume("Non sono riuscita a preparare la richiesta. Riprova."); return; }
        while (history.length() > 16) history.remove(0);
        main.postDelayed(timeout, 45000);
        OpenAiChatClient.respond(host, voiceWorker, history, new OpenAiChatClient.ChatCallback() {
            @Override public void onSuccess(String reply, String animationName, String motion, String mode) {
                main.post(() -> {
                    if (!listeningRequested || destroyed || generation != requestGeneration) return;
                    host.getSharedPreferences("chat", 0).edit().putString("history", history.toString()).apply();
                    speakAndResume(reply);
                });
            }
            @Override public void onError(String message) {
                main.post(() -> {
                    if (!listeningRequested || destroyed || generation != requestGeneration) return;
                    speakAndResume("Non riesco a completare la richiesta: " + message);
                });
            }
        });
    }

    private void openWhatsApp() {
        Intent launch = host.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) { speakAndResume("WhatsApp non è installato o non è disponibile."); return; }
        try { host.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); resumeListening(Phase.COMMAND); }
        catch (RuntimeException error) { speakAndResume("Impossibile aprire WhatsApp."); }
    }

    private void showSharePreview(String draft) {
        pauseForConfirmation("Microfono spento. Controlla il testo prima di condividere.");
        android.view.ContextThemeWrapper theme = new android.view.ContextThemeWrapper(host, android.R.style.Theme_Material_Light_Dialog_Alert);
        EditText editor = new EditText(theme);
        editor.setContentDescription("Testo da condividere su WhatsApp");
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editor.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
        editor.setText(draft);
        previewDialog = new android.app.AlertDialog.Builder(theme)
                .setTitle("ShareBridge · Anteprima")
                .setMessage("Aprire la condivisione WhatsApp? Sceglierai il destinatario e confermerai l’invio dentro WhatsApp.")
                .setView(editor).setNegativeButton("Annulla", (dialog, which) -> speakAndResume("Condivisione annullata."))
                .setPositiveButton("Apri condivisione WhatsApp", null).create();
        previewDialog.setCancelable(false);
        previewDialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        previewDialog.setOnDismissListener(dialog -> { editor.setText(""); previewDialog = null; });
        previewDialog.show();
        previewDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = editor.getText().toString().trim();
            if (text.isEmpty()) { editor.setError("Inserisci un testo oppure annulla"); return; }
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                    .setPackage("com.whatsapp").putExtra(Intent.EXTRA_TEXT, text);
            previewDialog.dismiss();
            try { host.startActivity(share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); resumeListening(Phase.COMMAND); }
            catch (RuntimeException error) { speakAndResume("Condivisione WhatsApp non disponibile. Nessun messaggio inviato."); }
        });
    }

    private void releaseRecognizer() {
        ++streamGeneration; // Ignore queued callbacks from a released service.
        main.removeCallbacks(stableWake);
        lastWakePartial = "";
        if (speechService != null) {
            try { speechService.cancel(); } catch (RuntimeException ignored) { }
            try { speechService.shutdown(); } catch (RuntimeException ignored) { }
            speechService = null;
        }
        if (recognizer != null) {
            try { recognizer.close(); } catch (RuntimeException ignored) { }
            recognizer = null;
        }
    }
    private boolean screenUnlocked() {
        android.os.PowerManager power = host.getSystemService(android.os.PowerManager.class);
        android.app.KeyguardManager keyguard = host.getSystemService(android.app.KeyguardManager.class);
        return resumed && power != null && power.isInteractive()
                && keyguard != null && !keyguard.isKeyguardLocked() && !destroyed;
    }
    private void finishSession(String message) {
        if (destroyed) return;
        android.util.Log.i("SolphiviaVoice", "Voice session finished, phase=" + phase);
        listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); unregisterWifiCallback();
        if (modelTask != null && !modelTask.isDone()) modelTask.cancel(true);
        modelLoading = false;
        if (tts != null) tts.stop();
        PetService.setVoiceAccessVisible(false);
        if (status != null) status.setText(message);
        PetService.setVoiceAccessPresentation(message, "sit");
        host.onVoiceSessionFinished(this);
    }
    void onNewLaunch() { sessionSuppressed = false; userStopped = false; }
    void onResume() {
        resumed = true;
        if (ttsReady && !listeningRequested && phase == Phase.IDLE) begin();
    }
    void onPause() {
        resumed = false;
        listeningRequested = false; ++generation; phase = Phase.IDLE; utterance = null;
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening);
        releaseRecognizer(); unregisterWifiCallback();
        if (modelTask != null && !modelTask.isDone()) modelTask.cancel(true);
        modelLoading = false;
        if (tts != null) tts.stop();
        PetService.setVoiceAccessVisible(false);
        if (previewDialog != null) previewDialog.dismiss();
        if (actionDialog != null) actionDialog.dismiss();
    }
    void onDestroy() {
        destroyed = true; listeningRequested = false; ++generation;
        if (activeController == this) activeController = null;
        if (previewDialog != null) previewDialog.dismiss();
        if (actionDialog != null) actionDialog.dismiss();
        main.removeCallbacks(timeout); main.removeCallbacks(retryListening); releaseRecognizer(); unregisterWifiCallback();
        if (model != null) { model.close(); model = null; }
        voiceWorker.shutdownNow(); modelWorker.shutdownNow(); if (tts != null) { tts.shutdown(); tts = null; }
        PetService.setVoiceAccessVisible(false);
    }
    boolean isActive() { return listeningRequested && resumed && !sessionSuppressed && !userStopped; }
}
