package it.solphivia.mobile;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.provider.OpenableColumns;
import android.net.Uri;
import android.database.Cursor;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Base64;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Locale;
import java.text.Normalizer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Home-screen overlay containing Solphivia, her speech bubble, and the chat composer. */
public class PetService extends Service {
    public static final String ACTION_ANIMATE = "it.solphivia.mobile.action.ANIMATE";
    public static final String EXTRA_ANIMATION = "animation";
    public static final String EXTRA_SEND_MESSAGE = "send_message";
    private static final String ACTION_STOP = "it.solphivia.mobile.action.STOP";
    private static final String ACTION_PICK_VOICE = "it.solphivia.mobile.action.VOICE_INPUT";
    private static final String ACTION_PICK_FILES = "it.solphivia.mobile.action.PICK_FILES";
    private static final String ACTION_ATTACHMENTS = "it.solphivia.mobile.action.ATTACHMENTS";
    private static final String EXTRA_URIS = "attachment_uris";
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 180;
    private static final long REST_AFTER_MS = 5_000L;
    private static final long SLEEP_AFTER_MS = 5_000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private WindowManager wm;
    private FrameLayout panel;
    private PetView pet;
    private WindowManager.LayoutParams lp;
    private ScrollView speechScroll;
    private TextView speech;
    private EditText composer;
    private TextView sendButton;
    private String speechTarget = "";
    private String pendingMotion = "stay";
    private String animation = "idle";
    private Runnable typingTask;
    private ValueAnimator movementAnimator;
    private Runnable sitIdleTask;
    private Runnable sleepIdleTask;
    private boolean busy;
    private float touchX, touchY;
    private int oldX, oldY;
    private boolean dragged;
    private boolean stoppedRunOnTouchDown;
    private int restingY;
    private boolean imeVisible;
    private String speechEndMood = "idle";
    private TextToSpeech tts;
    private boolean ttsInitializationComplete;
    private boolean ttsReady;
    private boolean englishAccentActive;
    private boolean ttsPending;
    private boolean speechTypingDone = true;
    private boolean speechFinalized = true;
    private long utteranceSerial;
    private long currentUtteranceSerial;
    private String pendingUtterance;
    private float previousTouchX;
    private long touchRevision;
    private long speechTouchRevision;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, status -> main.post(() -> {
            if (tts == null) return;
            ttsInitializationComplete = true;
            if (status != TextToSpeech.SUCCESS) { ttsReady = false; ttsPending = false; pendingUtterance = null; finishSpokenLine(); return; }
            configureTtsVoice();
            if (!ttsReady) { ttsPending = false; pendingUtterance = null; finishSpokenLine(); return; }
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { finishTtsUtterance(id); }
                @Override public void onError(String id) { finishTtsUtterance(id); }
            });
            if (ttsReady && pendingUtterance != null) {
                String text = pendingUtterance; long serial = currentUtteranceSerial; pendingUtterance = null;
                startTtsUtterance(text, serial);
            }
        }));
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            getSharedPreferences("pet", 0).edit().putBoolean("overlay_off", true).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        startForegroundNotification();
        if (panel == null) createOverlay();
        if (intent != null && ACTION_ATTACHMENTS.equals(intent.getAction())) {
            ArrayList<String> uris = intent.getStringArrayListExtra(EXTRA_URIS);
            if (uris != null && !uris.isEmpty()) main.post(() -> sendMessageWithAttachments(uris));
        }
        if (intent != null && ACTION_ANIMATE.equals(intent.getAction()) && typingTask == null && !busy) {
            setAnimation(intent.getStringExtra(EXTRA_ANIMATION));
        }
        if (intent != null && intent.hasExtra(EXTRA_SEND_MESSAGE) && composer != null) {
            String message = intent.getStringExtra(EXTRA_SEND_MESSAGE);
            if (message != null && !message.trim().isEmpty()) main.post(() -> {
                if (composer != null) { composer.setText(message); sendMessage(); }
            });
        }
        return START_NOT_STICKY;
    }

    private void startForegroundNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("pet", "Avatar Solphivia", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, PetService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, "pet")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Solphivia è sullo schermo")
                .setContentText("Tocca per aprire le impostazioni")
                .setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Ferma", stop).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, note);
    }

    private void createOverlay() {
        int width = dp(PANEL_W), height = dp(PANEL_H);
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        lp = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        lp.x = getSharedPreferences("pet", 0).getInt("panel_x", Math.max(0, (getResources().getDisplayMetrics().widthPixels - width) / 2));
        lp.y = getSharedPreferences("pet", 0).getInt("panel_y", Math.round(getResources().getDisplayMetrics().heightPixels * 0.42f));
        clampPanel();
        restingY = lp.y;

        panel = new FrameLayout(this);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        panel.setBackgroundColor(Color.TRANSPARENT);

        // Bubble tail points from the callout toward Solphivia's mouth.
        BubbleTail tail = new BubbleTail(this);
        FrameLayout.LayoutParams tailLp = new FrameLayout.LayoutParams(dp(50), dp(22), Gravity.TOP | Gravity.LEFT);
        tailLp.leftMargin = dp(176); tailLp.topMargin = dp(25);
        panel.addView(tail, tailLp);

        speechScroll = new ScrollView(this);
        speechScroll.setFillViewport(false);
        speechScroll.setVerticalScrollBarEnabled(false);
        speechScroll.setBackground(shape(0xFFFAF7FC, dp(17), 0xFFE3D8E8));
        speechScroll.setElevation(dp(4));
        speech = new TextView(this);
        speech.setTextColor(0xFF302736);
        speech.setTextSize(13);
        speech.setLineSpacing(dp(1), 1f);
        speech.setPadding(dp(10), dp(7), dp(10), dp(7));
        speechScroll.addView(speech, new ScrollView.LayoutParams(-1, -2));
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(dp(184), dp(66), Gravity.TOP | Gravity.LEFT);
        bubbleLp.leftMargin = dp(5); bubbleLp.topMargin = dp(7);
        panel.addView(speechScroll, bubbleLp);

        pet = new PetView(this);
        pet.setContentDescription("Solphivia: trascina per spostare avatar, fumetto e chat");
        FrameLayout.LayoutParams petLp = new FrameLayout.LayoutParams(dp(90), dp(112), Gravity.TOP | Gravity.LEFT);
        petLp.leftMargin = dp(194); petLp.topMargin = 0;
        panel.addView(pet, petLp);
        pet.setCyborgSuit(getSharedPreferences("pet", 0).getBoolean("cyborg_suit", false));
        pet.setOnTouchListener(this::onPetTouch);

        FrameLayout composerRow = new FrameLayout(this);
        FrameLayout.LayoutParams rowLp = new FrameLayout.LayoutParams(dp(292), dp(46), Gravity.TOP | Gravity.LEFT);
        rowLp.leftMargin = dp(4); rowLp.topMargin = dp(129);
        panel.addView(composerRow, rowLp);

        composer = new EditText(this);
        composer.setSingleLine(true);
        composer.setMaxLines(1);
        composer.setTextSize(14);
        composer.setTextColor(0xFF29232D);
        composer.setHintTextColor(0xFF766D7B);
        composer.setHint("Scrivi a Solphivia…");
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.setPadding(dp(12), 0, dp(126), 0);
        composer.setBackground(shape(0xFFFCFEFF, dp(23), 0xFF9BE9F1));
        composer.setElevation(dp(5));
        composer.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        composer.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });
        composer.setOnFocusChangeListener((v, focused) -> {
            if (focused) { wakeFromRest(); resetIdleTimers(); }
        });
        composer.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) { wakeFromRest(); resetIdleTimers(); }
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
        composerRow.addView(composer, new FrameLayout.LayoutParams(-1, -1));

        TextView filesButton = createComposerAction("＋", "Aggiungi PDF, Word o immagini");
        FrameLayout.LayoutParams filesLp = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        filesLp.rightMargin = dp(82);
        composerRow.addView(filesButton, filesLp);
        filesButton.setOnClickListener(v -> openInputActivity(ACTION_PICK_FILES));

        TextView micButton = createComposerAction("🎙", "Detta un messaggio");
        FrameLayout.LayoutParams micLp = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        micLp.rightMargin = dp(43);
        composerRow.addView(micButton, micLp);
        micButton.setOnClickListener(v -> openInputActivity(ACTION_PICK_VOICE));

        sendButton = createComposerAction("➜", "Invia messaggio");
        sendButton.setTextSize(23);
        sendButton.setTypeface(null, Typeface.BOLD);
        FrameLayout.LayoutParams sendLp = new FrameLayout.LayoutParams(dp(36), dp(36), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        sendLp.rightMargin = dp(3);
        composerRow.addView(sendButton, sendLp);
        sendButton.setOnClickListener(v -> sendMessage());
        composer.setOnFocusChangeListener((v, focused) -> {
            if (focused) composer.setTextColor(0xFF29232D);
        });

        speech.setText("");
        speechScroll.setOnClickListener(v -> finishTyping());
        if (Build.VERSION.SDK_INT >= 30) {
            panel.setOnApplyWindowInsetsListener((v, insets) -> {
                int previousY = lp.y;
                int imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
                int navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                boolean keyboardNowVisible = imeBottom > navBottom;
                if (keyboardNowVisible && !imeVisible) { imeVisible = true; restingY = lp.y; }
                if (keyboardNowVisible) {
                    lp.y = Math.max(0, Math.min(restingY,
                            getResources().getDisplayMetrics().heightPixels - imeBottom - lp.height - dp(8)));
                } else if (imeVisible) {
                    imeVisible = false; lp.y = restingY; clampPanel();
                }
                if (previousY != lp.y) {
                    try { wm.updateViewLayout(panel, lp); } catch (RuntimeException ignored) { }
                }
                return insets;
            });
        }
        try { wm.addView(panel, lp); }
        catch (RuntimeException error) { panel = null; pet = null; stopSelf(); return; }
        panel.requestApplyInsets();
        composer.setOnClickListener(v -> composer.requestFocus());
        panel.setFocusableInTouchMode(true);
        panel.requestFocus();
        typeSpeech("Ciao, io sono Solphivia. Come posso aiutarti?");
    }

    private boolean onPetTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchRevision++;
                if (movementAnimator != null) movementAnimator.cancel();
                stoppedRunOnTouchDown = "run".equals(animation);
                wakeFromRest(); resetIdleTimers();
                touchX = event.getRawX(); previousTouchX = touchX; touchY = event.getRawY(); oldX = lp.x; oldY = lp.y; dragged = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - touchX, dy = event.getRawY() - touchY;
                // A tiny intentional finger movement should start the full run loop.
                if (Math.hypot(dx, dy) > 1f) dragged = true;
                if (dragged) {
                    cancelIdleTimers();
                    if (!"run".equals(animation)) setAnimation("run");
                    float stepX = event.getRawX() - previousTouchX;
                    if (Math.abs(stepX) > 0.5f) pet.setScaleX(stepX < 0 ? -1f : 1f);
                    previousTouchX = event.getRawX();
                    lp.x = oldX + Math.round(dx); lp.y = oldY + Math.round(dy);
                    clampPanel();
                    try { wm.updateViewLayout(panel, lp); } catch (RuntimeException ignored) { }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragged) {
                    if (!imeVisible) restingY = lp.y;
                    getSharedPreferences("pet", 0).edit().putInt("panel_x", lp.x).putInt("panel_y", restingY).apply();
                    // Keep the run cycle going after release; the next tap on Solphivia stops it.
                    resetIdleTimers();
                } else if (stoppedRunOnTouchDown) {
                    stoppedRunOnTouchDown = false;
                    setAnimation("idle");
                    if (pet != null) pet.setScaleX(1f);
                    resetIdleTimers();
                } else {
                    pet.performClick(); setAnimation("happy");
                    main.postDelayed(() -> { if (!busy && pet != null && "happy".equals(animation)) setAnimation("idle"); resetIdleTimers(); }, 1100);
                }
                stoppedRunOnTouchDown = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                // A canceled pointer sequence does not count as the tap that stops a run.
                if (stoppedRunOnTouchDown && pet != null) setAnimation("run");
                stoppedRunOnTouchDown = false;
                return true;
            default: return false;
        }
    }

    private TextView createComposerAction(String label, String description) {
        TextView button = new TextView(this);
        button.setText(label); button.setTextSize(19); button.setTextColor(0xFF155B66);
        button.setGravity(Gravity.CENTER); button.setContentDescription(description);
        button.setBackground(shape(0xFFDDFBFF, dp(20), 0xFF9BE9F1));
        button.setElevation(dp(4));
        return button;
    }

    private void openInputActivity(String action) {
        try { startActivity(new Intent(this, CompanionInputActivity.class).setAction(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException error) { setSpeech("Non riesco ad aprire questa funzione sul telefono."); }
    }

    private void sendMessage() {
        if (busy) return;
        String text = composer.getText().toString().trim();
        if (text.isEmpty()) return;
        submitMessage(text, new ArrayList<>());
    }

    private void sendMessageWithAttachments(ArrayList<String> uris) {
        if (busy || uris == null || uris.isEmpty()) return;
        String text = composer.getText().toString().trim();
        if (text.isEmpty()) text = "Puoi esaminare questi allegati e aiutarmi?";
        submitMessage(text, uris);
    }

    private void submitMessage(String text, ArrayList<String> attachmentUris) {
        if (handleVoiceModeCommand(text)) return;
        if (isShutdownCommand(text)) {
            composer.setText("");
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(), 0);
            getSharedPreferences("pet", 0).edit().putBoolean("overlay_off", true).apply();
            stopSelf();
            return;
        }
        applyCostumeCommand(text);
        if (!OpenAiChatClient.isConnected(this)) {
            setSpeech("Per parlare con me, apri l’app Solphivia e collega ChatGPT.");
            return;
        }
        cancelIdleTimers();
        composer.setText("");
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(), 0);
        composer.clearFocus();
        busy = true; composer.setEnabled(false); sendButton.setEnabled(false);
        setAnimation("thinking"); setSpeech(attachmentUris.isEmpty() ? "Ti ascolto…" : "Sto preparando gli allegati…");
        if (attachmentUris.isEmpty()) {
            dispatchMessage(text, new JSONArray());
            return;
        }
        worker.execute(() -> {
            try {
                JSONArray attachments = readAttachments(attachmentUris);
                main.post(() -> dispatchMessage(text, attachments));
            } catch (Exception error) {
                main.post(() -> {
                    busy = false; composer.setEnabled(true); sendButton.setEnabled(true);
                    setSpeech("Non riesco a leggere uno degli allegati. Prova con PDF, DOCX o immagini più piccole.");
                    resetIdleTimers();
                });
            }
        });
    }

    private JSONArray readAttachments(ArrayList<String> uris) throws Exception {
        JSONArray result = new JSONArray();
        long total = 0;
        for (String raw : uris) {
            Uri uri = Uri.parse(raw);
            try {
            String name = "allegato";
            long declaredSize = -1;
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (nameColumn >= 0) name = cursor.getString(nameColumn);
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) declaredSize = cursor.getLong(sizeColumn);
                }
            }
            if (declaredSize > 8L * 1024 * 1024) throw new IllegalArgumentException("Allegato oltre 8 MB");
            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";
            byte[] bytes;
            try (InputStream input = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) throw new IllegalStateException("File non accessibile");
                byte[] buffer = new byte[8192]; int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > 15L * 1024 * 1024) throw new IllegalArgumentException("Allegati oltre 15 MB complessivi");
                    output.write(buffer, 0, read);
                }
                bytes = output.toByteArray();
            }
            if (bytes.length == 0) throw new IllegalArgumentException("Allegato vuoto");
            JSONObject item = new JSONObject();
            item.put("filename", name);
            item.put("mime_type", mime);
            item.put("data_uri", "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP));
            result.put(item);
            } finally {
                // The picker grants temporary access to a user-selected document.
                // Release any persistable grant as soon as its bytes are read.
                try { getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
            }
        }
        return result;
    }

    private void dispatchMessage(String text, JSONArray attachments) {
        String storedText = text;
        if (attachments.length() > 0) {
            StringBuilder names = new StringBuilder("\n[Allegati: ");
            for (int i=0;i<attachments.length();i++) {
                JSONObject item = attachments.optJSONObject(i);
                if (item != null) { if (i > 0) names.append(", "); names.append(item.optString("filename", "file")); }
            }
            storedText += names.append(']').toString();
        }
        saveMessage("user", storedText);
        OpenAiChatClient.respond(this, worker, loadHistory(), attachments, new OpenAiChatClient.ChatCallback() {
            @Override public void onSuccess(String reply, String mood, String motion) {
                main.post(() -> {
                    busy = false; composer.setEnabled(true); sendButton.setEnabled(true);
                    pendingMotion = preserveRunIntent(motion);
                    saveMessage("assistant", reply);
                    speechEndMood = mood == null ? "idle" : mood;
                    typeSpeech(reply);
                });
            }
            @Override public void onError(String message) {
                main.post(() -> {
                    busy = false; composer.setEnabled(true); sendButton.setEnabled(true);
                    setAnimation("concerned"); setSpeech("C’è stato un problema. Quando vuoi, riprova.");
                    resetIdleTimers();
                });
            }
        });
    }

    private void typeSpeech(String text) {
        if (typingTask != null) { main.removeCallbacks(typingTask); typingTask = null; }
        if (tts != null) tts.stop();
        pendingUtterance = null;
        currentUtteranceSerial = ++utteranceSerial;
        speechTouchRevision = touchRevision;
        ttsPending = true;
        speechTypingDone = false;
        speechFinalized = false;
        cancelIdleTimers();
        speechTarget = text == null ? "" : text;
        speech.setText("");
        setAnimation("talk");
        if (ttsReady) startTtsUtterance(speechTarget, currentUtteranceSerial);
        else if (!ttsInitializationComplete) pendingUtterance = speechTarget;
        else { ttsPending = false; pendingUtterance = null; }
        int[] index = {0};
        typingTask = new Runnable() {
            @Override public void run() {
                if (index[0] >= speechTarget.length()) {
                    typingTask = null;
                    speechTypingDone = true;
                    finishSpokenLine();
                    return;
                }
                int end = index[0] + 1;
                if (Character.isHighSurrogate(speechTarget.charAt(index[0])) &&
                        end < speechTarget.length() && Character.isLowSurrogate(speechTarget.charAt(end))) end++;
                speech.append(speechTarget.substring(index[0], end)); index[0] = end;
                speechScroll.post(() -> speechScroll.fullScroll(View.FOCUS_DOWN));
                long delay = 62;
                char c = speechTarget.charAt(end - 1);
                if (c == '.' || c == '!' || c == '?' || c == '…') delay = 240;
                else if (c == ',' || c == ';' || c == ':') delay = 145;
                main.postDelayed(this, delay);
            }
        };
        typingTask.run();
    }

    private void finishTyping() {
        if (typingTask == null) return;
        main.removeCallbacks(typingTask); typingTask = null; speech.setText(speechTarget);
        speechTypingDone = true;
        finishSpokenLine();
    }

    private void finishSpokenLine() {
        if (!speechTypingDone || ttsPending || speechFinalized) return;
        speechFinalized = true;
        String endMood = speechEndMood;
        speechEndMood = "idle";
        final String motion = pendingMotion;
        pendingMotion = "stay";
        final long revision = touchRevision;
        if (speechTouchRevision != revision || "run".equals(animation)) { resetIdleTimers(); return; }
        if (endMood == null || endMood.equals("idle")) setAnimation("idle");
        else {
            setAnimation(endMood);
            main.postDelayed(() -> { if (!busy && pet != null && typingTask == null && touchRevision == revision && endMood.equals(animation)) setAnimation("idle"); }, 900);
        }
        main.postDelayed(() -> { if (pet != null && touchRevision == revision && typingTask == null && !ttsPending) moveOverlay(motion); }, 300);
        resetIdleTimers();
    }

    private void setSpeech(String text) {
        if (typingTask != null) { main.removeCallbacks(typingTask); typingTask = null; }
        if (tts != null) tts.stop();
        pendingUtterance = null; ttsPending = false; speechTypingDone = true; speechFinalized = true;
        speechTarget = text; speech.setText(text); setAnimation("idle"); resetIdleTimers();
    }

    private void startTtsUtterance(String text, long serial) {
        if (tts == null || !ttsReady || text == null) { ttsPending = false; finishSpokenLine(); return; }
        applySystemTtsSettings();
        String id = "solphivia-" + serial;
        String spokenText = englishAccentActive ? ItalianEnglishAccent.respell(text) : text;
        int result = tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, id);
        if (result == TextToSpeech.ERROR) { ttsPending = false; finishSpokenLine(); }
    }

    private void applySystemTtsSettings() {
        // Respect the user's Motorola/Android speech-rate and pitch sliders for every utterance.
        int rate = Settings.Secure.getInt(getContentResolver(), Settings.Secure.TTS_DEFAULT_RATE, 100);
        int pitch = Settings.Secure.getInt(getContentResolver(), Settings.Secure.TTS_DEFAULT_PITCH, 100);
        tts.setSpeechRate(Math.max(0.1f, rate / 100f));
        tts.setPitch(Math.max(0.1f, pitch / 100f));
    }

    private void configureTtsVoice() {
        boolean requestedEnglish = getSharedPreferences("pet", 0).getBoolean("english_accent", false);
        Locale locale = requestedEnglish ? Locale.UK : Locale.ITALIAN;
        int language = tts.setLanguage(locale);
        ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
        englishAccentActive = requestedEnglish && ttsReady;
        android.speech.tts.Voice best = null;
        int bestScore = Integer.MIN_VALUE;
        if (ttsReady && tts.getVoices() != null) {
            for (android.speech.tts.Voice voice : tts.getVoices()) {
                if (!locale.getLanguage().equalsIgnoreCase(voice.getLocale().getLanguage())) continue;
                // The English test voice must be installed locally; keep the existing Italian
                // device voice selection unchanged when the user chooses the normal mode.
                if (requestedEnglish && voice.isNetworkConnectionRequired()) continue;
                int score = voice.getQuality();
                if (requestedEnglish && "GB".equalsIgnoreCase(voice.getLocale().getCountry())) score += 1000;
                if (!requestedEnglish && "IT".equalsIgnoreCase(voice.getLocale().getCountry())) score += 1000;
                if (best == null || score > bestScore) {
                    best = voice;
                    bestScore = score;
                }
            }
        }
        boolean voiceSelected = best != null && tts.setVoice(best) == TextToSpeech.SUCCESS;
        if (requestedEnglish && !voiceSelected) {
            // Keep the app usable when the phone has no offline English voice installed.
            int fallback = tts.setLanguage(Locale.ITALIAN);
            ttsReady = fallback != TextToSpeech.LANG_MISSING_DATA && fallback != TextToSpeech.LANG_NOT_SUPPORTED;
            englishAccentActive = false;
        }
    }

    private boolean handleVoiceModeCommand(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").trim();
        boolean english;
        if (normalized.equals("voce inglese") || normalized.equals("usa voce inglese")
                || normalized.equals("attiva voce inglese") || normalized.equals("accento inglese")
                || normalized.equals("prova accento inglese")) {
            english = true;
        } else if (normalized.equals("voce italiana") || normalized.equals("usa voce italiana")
                || normalized.equals("attiva voce italiana") || normalized.equals("accento italiano")) {
            english = false;
        } else return false;

        getSharedPreferences("pet", 0).edit().putBoolean("english_accent", english).apply();
        if (tts != null && ttsInitializationComplete) {
            tts.stop();
            configureTtsVoice();
        }
        composer.setText("");
        if (english && ttsInitializationComplete && !englishAccentActive) {
            typeSpeech("Non trovo una voce inglese offline installata. Scaricala nelle impostazioni di sintesi vocale Android e riprova.");
        } else if (english) {
            typeSpeech("Accento inglese attivato. Dimmi una frase breve.");
        } else {
            typeSpeech("Voce italiana attivata.");
        }
        return true;
    }

    private void finishTtsUtterance(String id) {
        main.post(() -> {
            if (id == null || !id.equals("solphivia-" + currentUtteranceSerial)) return;
            ttsPending = false;
            finishSpokenLine();
        });
    }

    private void applyCostumeCommand(String text) {
        Boolean cyborg = requestedCyborgSuit(text);
        if (cyborg != null) {
            if (pet != null) pet.setCyborgSuit(cyborg);
            getSharedPreferences("pet", 0).edit().putBoolean("cyborg_suit", cyborg).apply();
        }
    }

    public static boolean isShutdownCommand(String text) {
        String value = normalizeCommand(text).replaceAll("[.!?]+$", "").trim();
        return value.equals("spegniti") || value.equals("spegni") || value.equals("spegni solphivia") ||
                value.equals("spegni app") || value.equals("spegni l'app") || value.equals("chiudi app") ||
                value.equals("chiudi l'app") || value.equals("chiudi applicazione") ||
                value.equals("chiudi l'applicazione") || value.equals("chiudi solphivia");
    }

    public static Boolean requestedCyborgSuit(String text) {
        String value = normalizeCommand(text);
        boolean restore = value.contains("originale") || value.contains("normale") || value.contains("look base") ||
                value.contains("togli") || value.contains("rimuovi") || value.contains("ripristina");
        boolean clothing = value.contains("vestit") || value.contains("abito") || value.contains("tuta") ||
                value.contains("look") || value.contains("armatura");
        boolean cyborg = value.contains("cyborg") || value.contains("automan") || value.contains("mutaforma") || value.contains("armatura");
        boolean action = value.contains("indossa") || value.contains("metti") || value.contains("trasform") ||
                value.contains("cambia") || value.contains("vestit") || value.contains("look") ||
                value.contains("abito") || value.contains("tuta") || value.contains("ripristina") || value.contains("togli");
        if (restore && (clothing || action)) return false;
        if (cyborg && (action || value.equals("cyborg") || value.equals("automan"))) return true;
        if (clothing && action) return true;
        return null;
    }

    private static String normalizeCommand(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text.toLowerCase(Locale.ITALIAN).replace('’', '\''), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").trim();
    }

    private void wakeFromRest() {
        if ("sit".equals(animation) || "sleep".equals(animation)) setAnimation("idle");
    }

    private void cancelIdleTimers() {
        if (sitIdleTask != null) main.removeCallbacks(sitIdleTask);
        if (sleepIdleTask != null) main.removeCallbacks(sleepIdleTask);
        sitIdleTask = null; sleepIdleTask = null;
    }

    private void resetIdleTimers() {
        cancelIdleTimers();
        if (pet == null || busy || typingTask != null || ttsPending || "run".equals(animation)) return;
        sitIdleTask = () -> {
            sitIdleTask = null;
            if (pet == null || busy || typingTask != null || ttsPending || "run".equals(animation)) return;
            setAnimation("sit");
            sleepIdleTask = () -> {
                sleepIdleTask = null;
                if (pet != null && !busy && typingTask == null && !ttsPending && "sit".equals(animation)) setAnimation("sleep");
            };
            main.postDelayed(sleepIdleTask, SLEEP_AFTER_MS);
        };
        main.postDelayed(sitIdleTask, REST_AFTER_MS);
    }

    private void setAnimation(String command) {
        if (pet != null && command != null) {
            String name = command.startsWith("run-") ? "run" : command;
            if (pet.setAnimation(name)) animation = name;
        }
    }

    private String preserveRunIntent(String motion) {
        JSONArray history = loadHistory();
        if (history.length() < 2) return motion;
        JSONObject latest = history.optJSONObject(history.length() - 1);
        if (latest == null || !"user".equals(latest.optString("role"))) return motion;
        String answer = latest.optString("content").trim().toLowerCase(java.util.Locale.ROOT);
        String direction = answer.contains("sinistra") ? "run-left" : answer.contains("destra") ? "run-right" : null;
        if (direction == null) return motion;
        for (int i = history.length() - 2; i >= 0; i--) {
            JSONObject item = history.optJSONObject(i);
            if (item == null || !"user".equals(item.optString("role"))) continue;
            String prior = item.optString("content").toLowerCase(java.util.Locale.ROOT);
            if (prior.contains("corri") || prior.contains("correre") || prior.contains("di corsa")) return direction;
            break;
        }
        return motion;
    }

    private void moveOverlay(String command) {
        if (panel == null || lp == null || command == null || command.equals("stay")) return;
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        int maxX = Math.max(0, m.widthPixels - lp.width), maxY = Math.max(0, m.heightPixels - lp.height);
        int toX = lp.x, toY = lp.y;
        boolean run = command.equals("run-left") || command.equals("run-right");
        switch (command) {
            case "left": case "run-left": toX = 0; break;
            case "right": case "run-right": toX = maxX; break;
            case "center": toX = maxX / 2; toY = maxY / 2; break;
            case "up": toY = 0; break;
            case "down": toY = maxY; break;
            case "near": toX = maxX / 2; toY = maxY; break;
            case "away": toX = maxX / 2; toY = 0; break;
            case "bounce": toY = Math.max(0, lp.y - dp(36)); break;
            default: return;
        }
        int fromX = lp.x, fromY = lp.y;
        final int targetX = toX, targetY = toY;
        if (run) { cancelIdleTimers(); setAnimation("run"); pet.setScaleX(command.equals("run-left") ? -1f : 1f); }
        else setAnimation("look");
        movementAnimator = ValueAnimator.ofFloat(0f, 1f);
        movementAnimator.setDuration(run ? 1050 : 750);
        movementAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        movementAnimator.addUpdateListener(a -> {
            if (panel == null) return;
            float t = (float) a.getAnimatedValue();
            lp.x = Math.round(fromX + (targetX - fromX) * t);
            lp.y = command.equals("bounce") ? fromY - Math.round((float) Math.sin(Math.PI * t) * dp(36))
                    : Math.round(fromY + (targetY - fromY) * t);
            clampPanel();
            try { wm.updateViewLayout(panel, lp); } catch (RuntimeException ignored) { }
        });
        movementAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator a) { cancelled = true; }
            @Override public void onAnimationEnd(android.animation.Animator a) {
                if (cancelled) { movementAnimator = null; return; }
                if (pet != null) {
                    if (run) { setAnimation("run"); pet.setScaleX(command.equals("run-left") ? -1f : 1f); }
                    else { setAnimation("idle"); pet.setScaleX(1f); }
                }
                if (lp != null) getSharedPreferences("pet", 0).edit().putInt("panel_x", lp.x).putInt("panel_y", lp.y).apply();
                resetIdleTimers();
                movementAnimator = null;
            }
        });
        movementAnimator.start();
    }

    private void saveMessage(String role, String body) {
        JSONArray history = loadHistory();
        try { history.put(new JSONObject().put("role", role).put("content", body)); } catch (Exception ignored) { }
        while (history.length() > 16) history.remove(0);
        getSharedPreferences("chat", 0).edit().putString("history", history.toString()).apply();
    }

    private JSONArray loadHistory() {
        try { return new JSONArray(getSharedPreferences("chat", 0).getString("history", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private void clampPanel() {
        if (lp == null) return;
        android.util.DisplayMetrics m = getResources().getDisplayMetrics();
        lp.x = Math.max(0, Math.min(lp.x, m.widthPixels - lp.width));
        lp.y = Math.max(0, Math.min(lp.y, m.heightPixels - lp.height));
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static class BubbleTail extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        BubbleTail(Context context) { super(context); paint.setColor(0xFFFAF7FC); }
        @Override protected void onDraw(Canvas canvas) {
            path.reset(); path.moveTo(0, 0); path.lineTo(getWidth(), getHeight() / 2f); path.lineTo(0, getHeight()); path.close();
            canvas.drawPath(path, paint);
        }
    }

    public static void animate(Context context, String command) {
        Intent intent = new Intent(context, PetService.class).setAction(ACTION_ANIMATE).putExtra(EXTRA_ANIMATION, command);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent); else context.startService(intent);
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        if (panel != null) { clampPanel(); restingY = lp.y; wm.updateViewLayout(panel, lp); }
    }

    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        cancelIdleTimers();
        if (typingTask != null) main.removeCallbacks(typingTask);
        if (movementAnimator != null) {
            movementAnimator.removeAllUpdateListeners(); movementAnimator.removeAllListeners();
            movementAnimator.cancel(); movementAnimator = null;
        }
        worker.shutdownNow();
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        if (pet != null) pet.pause();
        if (panel != null) { try { wm.removeView(panel); } catch (IllegalArgumentException ignored) { } panel = null; pet = null; }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
