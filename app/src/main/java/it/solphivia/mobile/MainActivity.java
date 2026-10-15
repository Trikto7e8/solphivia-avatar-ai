package it.solphivia.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Avatar-first Solphivia chat: speech bubble, touch interaction, and one-line composer. */
public class MainActivity extends Activity {
    private static MainActivity voiceUiInstance;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private FrameLayout stage;
    private ScrollView speechScroll;
    private TextView speech;
    private EditText composer;
    private Button sendButton;
    private Button connectButton;
    private TextView status;
    private PetView inlinePet;
    private BubbleTail speechTail;
    private boolean authBrowser;
    private boolean busy;
    private boolean activityPaused;
    private boolean sendAfterConnect;
    private boolean connectionNoticePending;
    private boolean voiceAccessStarted;
    private String queuedFirstMessage = "";
    private String currentAnimation = "idle";
    private float touchX, touchY, startTranslationX, startTranslationY;
    private boolean dragged;
    private boolean stoppedRunOnTouchDown;
    private Runnable typingTask;
    private Runnable sitIdleTask;
    private Runnable sleepIdleTask;
    private String speechTarget = "";
    private String pendingMotion = "stay";
    private final java.util.ArrayList<String> pendingFiles = new java.util.ArrayList<>();
    private Button filesButton;
    private boolean inputPending;
    private DeviceSpeech inlineVoice;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        VoicePreferences.initialize(this);
        inlineVoice=new DeviceSpeech(this);
        getWindow().setStatusBarColor(Color.rgb(250, 247, 252));
        getWindow().setNavigationBarColor(Color.rgb(250, 247, 252));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        } else {
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        authBrowser = OpenAiChatClient.isAuthorizing();
        buildScreen();
        restoreLatestSpeech();
    }

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(7), dp(14), dp(8));
        page.setBackgroundColor(Color.rgb(250, 247, 252));
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                top = bars.top; bottom = Math.max(bars.bottom, ime.bottom);
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
            }
            page.setPadding(dp(14), dp(7) + top, dp(14), dp(8) + bottom);
            return insets;
        });

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Solphivia"); title.setTextSize(23); title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(54, 37, 66));
        header.addView(title, new LinearLayout.LayoutParams(0, dp(42), 1));
        connectButton = new Button(this);
        connectButton.setTextSize(11);
        connectButton.setOnClickListener(v -> connectChatGPT());
        header.addView(connectButton, new LinearLayout.LayoutParams(-2, dp(40)));
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(42)));

        status = new TextView(this);
        status.setTextSize(12); status.setTextColor(Color.rgb(112, 96, 120));
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setPadding(dp(2), 0, 0, 0);
        page.addView(status, new LinearLayout.LayoutParams(-1, dp(25)));
        LinearLayout settingsRow = new LinearLayout(this);
        Button memoryButton = new Button(this); memoryButton.setText("Memoria");
        memoryButton.setOnClickListener(v -> startActivity(new Intent(this, MemoryActivity.class)));
        settingsRow.addView(memoryButton, new LinearLayout.LayoutParams(0, -2, 1));
        Button voiceButton = new Button(this); voiceButton.setText("Voce");
        voiceButton.setOnClickListener(v -> startActivity(new Intent(this, VoiceSettingsActivity.class)));
        settingsRow.addView(voiceButton, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(settingsRow);

        stage = new FrameLayout(this);
        stage.setBackground(roundRect(Color.WHITE, dp(24), 0xFFE9DDEC));
        stage.setClipToPadding(true);

        inlinePet = new PetView(this);
        String activeMode = getSharedPreferences("pet", 0).getString("active_mode", "prof");
        inlinePet.setCyborgSuit("vj".equals(activeMode));
        inlinePet.setSolphiviaMode(activeMode);
        inlinePet.setWeatherLook(getSharedPreferences("weather",0).getString("avatar_look","none"));
        if (getSharedPreferences("pet", 0).getBoolean("costume_override", false)) inlinePet.setClassicCyborgSuit(true);
        inlinePet.setContentDescription("Avatar di Solphivia: trascina per spostarla, fai uno swipe per farla correre");
        FrameLayout.LayoutParams petParams = new FrameLayout.LayoutParams(dp(220), dp(220), Gravity.CENTER);
        stage.addView(inlinePet, petParams);
        inlinePet.setTranslationX(dp(28));
        inlinePet.setOnTouchListener(this::onAvatarTouch);

        speechTail = new BubbleTail(this);
        stage.addView(speechTail, new FrameLayout.LayoutParams(dp(38), dp(20), Gravity.TOP | Gravity.LEFT));
        speechScroll = new ScrollView(this);
        speechScroll.setFillViewport(false);
        speechScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        speechScroll.setBackground(roundRect(0xFFF8F5F9, dp(20), 0xFFEDE4EF));
        speechScroll.setElevation(dp(4));
        speech = new TextView(this);
        speech.setTextSize(15); speech.setTextColor(0xFF302736);
        speech.setLineSpacing(dp(2), 1f);
        speech.setPadding(dp(11), dp(8), dp(11), dp(8));
        speechScroll.addView(speech, new ScrollView.LayoutParams(-1, -2));
        FrameLayout.LayoutParams speechParams = new FrameLayout.LayoutParams(dp(170), dp(82), Gravity.TOP | Gravity.LEFT);
        stage.addView(speechScroll, speechParams);
        speechScroll.setOnClickListener(v -> finishTyping());
        speech.setOnClickListener(v -> finishTyping());
        stage.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> positionSpeechBubble());
        inlinePet.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> positionSpeechBubble());

        LinearLayout.LayoutParams stageParams = new LinearLayout.LayoutParams(-1, 0, 1);
        stageParams.bottomMargin = dp(5);
        page.addView(stage, stageParams);

        LinearLayout composerRow = new LinearLayout(this);
        composerRow.setGravity(Gravity.CENTER_VERTICAL);
        filesButton = new Button(this); filesButton.setText("+"); filesButton.setContentDescription("Aggiungi file dal telefono");
        filesButton.setMinWidth(0); filesButton.setMinimumWidth(0); filesButton.setPadding(0,0,0,0);
        composerRow.addView(filesButton,new LinearLayout.LayoutParams(dp(44),dp(48)));
        filesButton.setOnClickListener(v -> openInlineInput(CompanionInputActivity.ACTION_FILES));
        filesButton.setOnLongClickListener(v -> {pendingFiles.clear(); filesButton.setText("+");status.setText("Allegati rimossi.");return true;});
        Button micButton=new Button(this); micButton.setText("🎙"); micButton.setContentDescription("Detta un messaggio");
        micButton.setMinWidth(0);micButton.setMinimumWidth(0);micButton.setPadding(0,0,0,0);
        composerRow.addView(micButton,new LinearLayout.LayoutParams(dp(44),dp(48)));
        micButton.setOnClickListener(v -> openInlineInput(CompanionInputActivity.ACTION_VOICE));
        composer = new EditText(this);
        composer.setTextSize(16); composer.setSingleLine(true); composer.setMaxLines(1);
        composer.setHint("Scrivi a Solphivia…");
        composer.setTextColor(0xFF29232D); composer.setHintTextColor(0xFF766D7B);
        composer.setPadding(dp(14), dp(9), dp(12), dp(9));
        composer.setBackground(roundRect(0xFFFCFEFF, dp(20), 0xFF9BE9F1));
        composer.setImeOptions(EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        composer.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0) { wakeFromRest(); resetIdleTimers(); }
            }
            @Override public void afterTextChanged(android.text.Editable editable) { }
        });
        composer.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });
        composerRow.addView(composer, new LinearLayout.LayoutParams(0, dp(52), 1));
        sendButton = new Button(this); sendButton.setText("➜"); sendButton.setTextSize(21);
        sendButton.setTextColor(0xFF155B66); sendButton.setContentDescription("Invia messaggio");
        sendButton.setBackground(roundRect(0xFFDDFBFF, dp(22), 0xFF9BE9F1));
        sendButton.setOnClickListener(v -> sendMessage());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(-2, dp(50));
        sendParams.leftMargin = dp(5);
        composerRow.addView(sendButton, sendParams);
        page.addView(composerRow, new LinearLayout.LayoutParams(-1, dp(52)));
        setContentView(page);
        page.requestApplyInsets();
        stage.post(this::positionSpeechBubble);
        refreshConnectionState();
        resetIdleTimers();
    }

    /** Keep the compact callout beside Solphivia's face, with its point aimed at her mouth. */
    private void positionSpeechBubble() {
        if (stage == null || inlinePet == null || speechScroll == null || speechTail == null ||
                stage.getWidth() == 0 || stage.getHeight() == 0 || inlinePet.getWidth() == 0) return;
        int width = Math.min(dp(178), Math.max(dp(118), Math.round(stage.getWidth() * 0.48f)));
        int height = dp(82);
        float mouthX = inlinePet.getX() + inlinePet.getWidth() * 0.52f;
        float mouthY = inlinePet.getY() + inlinePet.getHeight() * 0.40f;
        boolean pointsRight = mouthX >= stage.getWidth() * 0.5f;
        int left = pointsRight ? dp(8) : stage.getWidth() - width - dp(8);
        int top = Math.max(dp(5), Math.min(stage.getHeight() - height - dp(5), Math.round(mouthY - height * 0.5f)));

        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) speechScroll.getLayoutParams();
        bubbleParams.width = width; bubbleParams.height = height; bubbleParams.gravity = Gravity.TOP | Gravity.LEFT;
        bubbleParams.leftMargin = left; bubbleParams.topMargin = top;
        speechScroll.setLayoutParams(bubbleParams);

        int tailStart = pointsRight ? left + width - dp(2) : left - dp(2);
        int tailWidth = Math.max(dp(12), Math.abs(Math.round(mouthX) - tailStart));
        FrameLayout.LayoutParams tailParams = (FrameLayout.LayoutParams) speechTail.getLayoutParams();
        tailParams.width = tailWidth; tailParams.height = dp(20); tailParams.gravity = Gravity.TOP | Gravity.LEFT;
        tailParams.leftMargin = pointsRight ? tailStart : Math.round(mouthX);
        tailParams.topMargin = Math.max(0, Math.min(stage.getHeight() - dp(20), Math.round(mouthY - dp(10))));
        speechTail.setPointLeft(!pointsRight);
        speechTail.setLayoutParams(tailParams);
    }

    private boolean onAvatarTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                stoppedRunOnTouchDown = "run".equals(currentAnimation);
                wakeFromRest(); resetIdleTimers();
                touchX = event.getRawX(); touchY = event.getRawY();
                startTranslationX = view.getTranslationX(); startTranslationY = view.getTranslationY(); dragged = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - touchX, dy = event.getRawY() - touchY;
                if (Math.hypot(dx, dy) > 1f && !dragged) {
                    dragged = true;
                    if (!"run".equals(currentAnimation)) setAnimation("run");
                }
                if (dragged) {
                    cancelIdleTimers();
                    view.animate().cancel();
                    view.setTranslationX(clampX(startTranslationX + dx));
                    view.setTranslationY(clampY(startTranslationY + dy));
                    view.setScaleX(dx < 0 ? -1f : 1f);
                    positionSpeechBubble();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragged) {
                    resetIdleTimers();
                } else if (stoppedRunOnTouchDown) {
                    setAnimation("idle"); view.setScaleX(1f); resetIdleTimers();
                } else {
                    view.performClick(); setAnimation("happy");
                    main.postDelayed(() -> { if (!busy && "happy".equals(currentAnimation)) setAnimation("idle"); resetIdleTimers(); }, 1100);
                }
                stoppedRunOnTouchDown = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                if (stoppedRunOnTouchDown) setAnimation("run");
                stoppedRunOnTouchDown = false;
                return true;
        }
        return false;
    }

    private float clampX(float value) {
        float limit = Math.max(0, (stage.getWidth() - inlinePet.getWidth()) / 2f - dp(8));
        return Math.max(-limit, Math.min(limit, value));
    }

    private float clampY(float value) {
        float limit = Math.max(0, (stage.getHeight() - inlinePet.getHeight()) / 2f - dp(8));
        return Math.max(-limit, Math.min(limit, value));
    }

    private void runTo(String command) {
        if (stage == null || inlinePet == null || stage.getWidth() == 0) return;
        setAnimation("run");
        float edge = Math.max(0, (stage.getWidth() - inlinePet.getWidth()) / 2f - dp(12));
        float destination = command.equals("run-left") ? -edge : edge;
        inlinePet.animate().cancel();
        inlinePet.setRotation(command.equals("run-left") ? -5 : 5);
        inlinePet.animate().translationX(destination).setDuration(1050)
                .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .setUpdateListener(animation -> positionSpeechBubble())
                .withEndAction(() -> {
                    inlinePet.animate().setUpdateListener(null);
                    inlinePet.animate().rotation(0).setDuration(180).start();
                    resetIdleTimers();
                }).start();
    }

    private void moveAvatar(String command) {
        if (command == null || command.equals("stay") || stage.getWidth() == 0) { resetIdleTimers(); return; }
        if (command.equals("run-left") || command.equals("run-right")) { runTo(command); return; }
        float x = inlinePet.getTranslationX(), y = inlinePet.getTranslationY();
        float dx = Math.max(0, (stage.getWidth() - inlinePet.getWidth()) / 2f - dp(12));
        float dy = Math.max(0, (stage.getHeight() - inlinePet.getHeight()) / 2f - dp(12));
        switch (command) {
            case "left": x = -dx; break;
            case "right": x = dx; break;
            case "center": x = 0; y = 0; break;
            case "up": y = -dy; break;
            case "down": y = dy; break;
            case "near": x = 0; y = dy; break;
            case "away": x = 0; y = -dy; break;
            case "bounce":
                inlinePet.animate().scaleX(1.08f).scaleY(0.92f).setDuration(180).withEndAction(() ->
                        inlinePet.animate().scaleX(1f).scaleY(1f).setDuration(220).withEndAction(this::resetIdleTimers).start()).start();
                return;
        }
        setAnimation("look");
        inlinePet.animate().translationX(clampX(x)).translationY(clampY(y)).setDuration(750)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> { if (!busy) setAnimation("idle"); resetIdleTimers(); })
                .setUpdateListener(animation -> positionSpeechBubble()).start();
    }

    private void connectChatGPT() {
        if (authBrowser || OpenAiChatClient.isAuthorizing()) {
            authBrowser = true;
            status.setText("Completa l’autorizzazione ChatGPT già aperta e poi torna qui.");
            refreshConnectionState();
            return;
        }
        authBrowser = true;
        connectButton.setEnabled(false);
        composer.setEnabled(false); sendButton.setEnabled(false);
        status.setText("Completa il consenso nel browser; poi torna qui in Solphivia.");
        OpenAiChatClient.authorize(this, worker, new OpenAiChatClient.AuthCallback() {
            @Override public void onSuccess(String account) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    authBrowser = false; connectButton.setEnabled(true);
                    composer.setEnabled(true); sendButton.setEnabled(true);
                    Toast.makeText(MainActivity.this, "ChatGPT collegato", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("pet",0).edit().putBoolean("overlay_off",false).apply();
                    String firstMessage = sendAfterConnect ? composer.getText().toString().trim() : "";
                    if (Settings.canDrawOverlays(MainActivity.this)) queuedFirstMessage = firstMessage;
                    sendAfterConnect = false;
                    refreshConnectionState();
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        if (!firstMessage.isEmpty()) sendMessage();
                        Toast.makeText(MainActivity.this, "Chat pronta. Puoi attivare l’avatar flottante dal messaggio di stato.", Toast.LENGTH_LONG).show();
                    }
                    showFirstUseNotice();
                    if (!connectionNoticePending) {
                        submitQueuedFirstMessage();
                        startVoiceAccessOnLaunch();
                    }
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    authBrowser = false; sendAfterConnect = false; connectButton.setEnabled(true);
                    composer.setEnabled(true); sendButton.setEnabled(true); status.setText(message);
                });
            }
        });
    }

    private void sendMessage() {
        if (authBrowser || OpenAiChatClient.isAuthorizing() || busy) return;
        String text = composer.getText().toString().trim();
        if (text.isEmpty() && pendingFiles.isEmpty()) return;
        if (text.isEmpty()) text="Puoi esaminare questi allegati e aiutarmi?";
        if (!pendingFiles.isEmpty()) { sendInlineFiles(text); return; }
        String memoryReply=MemoryCommands.handle(this,text);
        if(memoryReply!=null){composer.setText("");speak(memoryReply,"idle","stay",false);return;}
        if(text.equalsIgnoreCase("memoria")){composer.setText("");startActivity(new Intent(this,MemoryActivity.class));return;}
        if(text.equalsIgnoreCase("impostazioni voce")){composer.setText("");startActivity(new Intent(this,VoiceSettingsActivity.class));return;}
        if (text.equalsIgnoreCase("nuova chat")) {
            getSharedPreferences("chat", 0).edit().remove("history").apply();
            composer.setText(""); setSpeech("Nuova chat pronta.", false); return;
        }
        if (PetService.isShutdownCommand(text)) {
            getSharedPreferences("pet", 0).edit().putBoolean("overlay_off", true).apply();
            stopService(new Intent(this, PetService.class));
            finishAndRemoveTask();
            return;
        }
        boolean modeCommand = PetService.applyModeCommand(this, text, inlinePet);
        Boolean cyborg = modeCommand ? null : PetService.requestedCyborgSuit(text);
        if (cyborg != null) {
            getSharedPreferences("pet", 0).edit().putBoolean("costume_override", cyborg).apply();
            inlinePet.setClassicCyborgSuit(cyborg);
            getSharedPreferences("pet", 0).edit().putBoolean("cyborg_suit", cyborg).apply();
        }
        if (!OpenAiChatClient.isConnected(this)) { sendAfterConnect=true; connectChatGPT(); return; }
        cancelIdleTimers();
        composer.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(), 0);
        busy = true; sendButton.setEnabled(false); composer.setEnabled(false);
        status.setText("Solphivia sta pensando…");
        setAnimation("thinking"); setSpeech("Ti ascolto…", false);
        JSONArray history = loadHistory();
        final String requestText=text;
        try {history.put(new JSONObject().put("role","user").put("content",requestText));}catch(Exception e){throw new IllegalStateException(e);}
        while(history.length()>16)history.remove(0);
        OpenAiChatClient.respond(this, worker, history, new OpenAiChatClient.ChatCallback() {
            @Override public void onSuccess(String reply, String animation, String motion, String mode) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    getSharedPreferences("chat",0).edit().putString("history",history.toString()).apply();
                    busy = false; sendButton.setEnabled(true); composer.setEnabled(true);
                    status.setText("Chat privata · collegata a ChatGPT");
                    PetService.applyContextualMode(MainActivity.this, mode, inlinePet);
                    speak(reply, animation, preserveRunIntent(motion), true);
                    composer.requestFocus();
                });
            }
            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    refreshConnectionState();
                    busy = false; sendButton.setEnabled(true); composer.setEnabled(true);
                    composer.setText(requestText);
                    status.setText(message); setAnimation("concerned"); setSpeech("C’è stato un problema. Quando vuoi, riprova.", false);
                    resetIdleTimers();
                    if(message.contains("subscription_sharing_usage_limit_exceeded") || message.contains("subscription_sharing_usage_unavailable")) {
                        status.setText("Limite del piano raggiunto · tocca qui per gestire l’utilizzo");
                        status.setOnClickListener(v -> openUsageSettings());
                    }
                });
            }
        });
    }

    private void openInlineInput(String action) {
        if(busy || inputPending)return;
        inputPending=true;
        try {startActivityForResult(new Intent(this,CompanionInputActivity.class).setAction(action).putExtra("inline",true),71);}
        catch(RuntimeException e){inputPending=false;status.setText("Funzione non disponibile sul telefono.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=71)return;inputPending=false;
        if(result!=RESULT_OK || data==null)return;
        if(data.hasExtra("dictation"))composer.setText(data.getStringExtra("dictation"));
        java.util.ArrayList<String> uris=data.getStringArrayListExtra(CompanionInputActivity.EXTRA_URIS);
        if(uris!=null)for(String uri:uris)if(!pendingFiles.contains(uri) && pendingFiles.size()<5)pendingFiles.add(uri);
        filesButton.setText(pendingFiles.isEmpty()?"+":"+"+pendingFiles.size());
        status.setText("Bozza pronta: controlla il messaggio e premi ➜. Tieni premuto + per rimuovere gli allegati.");
    }
    private void sendInlineFiles(String text) {
        if(!OpenAiChatClient.isConnected(this)){status.setText("Collega ChatGPT prima di inviare gli allegati.");return;}
        busy=true;sendButton.setEnabled(false);composer.setEnabled(false);
        java.util.ArrayList<String> selected=new java.util.ArrayList<>(pendingFiles);
        worker.execute(() -> {
            try {
                JSONArray files=AttachmentReader.read(this,selected,new long[]{0});
                JSONArray history=loadHistory();
                history.put(new JSONObject().put("role","user").put("content",text).put("attachments",AttachmentReader.references(files)));
                while(history.length()>16)history.remove(0);
                runOnUiThread(() -> {
                    if(isDestroyed())return;
                    OpenAiChatClient.respond(this,worker,history,files,new OpenAiChatClient.ChatCallback(){
                        public void onSuccess(String reply,String mood,String motion,String mode){
                            if(isDestroyed())return;
                            getSharedPreferences("chat",0).edit().putString("history",history.toString()).apply();
                            busy=false;sendButton.setEnabled(true);composer.setEnabled(true);composer.setText("");pendingFiles.clear();filesButton.setText("+");
                            PetService.applyContextualMode(MainActivity.this,mode,inlinePet);speak(reply,mood,motion,true);refreshConnectionState();
                        }
                        public void onError(String message){if(isDestroyed())return;busy=false;sendButton.setEnabled(true);composer.setEnabled(true);status.setText(message);}
                    });
                });
            }catch(Exception e){runOnUiThread(() -> {if(isDestroyed())return;busy=false;sendButton.setEnabled(true);composer.setEnabled(true);status.setText("Allegati: "+e.getMessage());});}
        });
    }

    private void speak(String text, String animation, String motion, boolean save) {
        if (save) saveMessage("assistant", text);
        if(inlineVoice!=null)inlineVoice.speak(text);
        setAnimation(animation);
        typeSpeech(text);
        pendingMotion = motion == null ? "stay" : motion;
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

    private void typeSpeech(String text) {
        finishTyping();
        speechTarget = text;
        speech.setText("");
        final int[] index = {0};
        typingTask = new Runnable() {
            @Override public void run() {
                if (index[0] >= text.length()) {
                    typingTask = null;
                    main.postDelayed(() -> moveAvatar(pendingMotion), 300);
                    return;
                }
                int start = index[0];
                int end = Math.min(text.length(), start + nextChunk(text, start));
                speech.append(text.substring(start, end)); index[0] = end;
                speechScroll.post(() -> speechScroll.fullScroll(View.FOCUS_DOWN));
                long delay = 55;
                char last = text.charAt(end - 1);
                if (last == '.' || last == '!' || last == '?' || last == '…') delay = 190;
                else if (last == ',' || last == ';' || last == ':') delay = 110;
                typingTask = this; main.postDelayed(this, delay);
            }
        };
        typingTask.run();
    }

    private int nextChunk(String text, int start) {
        int end = Math.min(text.length(), start + 2);
        if (end < text.length() && Character.isLowSurrogate(text.charAt(end))) end++;
        return end - start;
    }

    private void finishTyping() {
        if (typingTask != null) {
            main.removeCallbacks(typingTask);
            typingTask = null;
            speech.setText(speechTarget);
            main.postDelayed(() -> moveAvatar(pendingMotion), 300);
        }
    }

    private void setSpeech(String text, boolean type) { if (type) typeSpeech(text); else { finishTyping(); speech.setText(text); } }

    private void setAnimation(String animation) {
        currentAnimation = animation;
        if (inlinePet != null) inlinePet.setAnimation(animation);
        if (activityPaused && Settings.canDrawOverlays(this) && !getSharedPreferences("pet",0).getBoolean("overlay_off",false)) PetService.animate(this, animation);
    }

    private void wakeFromRest() {
        if ("sit".equals(currentAnimation) || "sleep".equals(currentAnimation)) setAnimation("idle");
    }

    private void cancelIdleTimers() {
        if (sitIdleTask != null) main.removeCallbacks(sitIdleTask);
        if (sleepIdleTask != null) main.removeCallbacks(sleepIdleTask);
        sitIdleTask = null;
        sleepIdleTask = null;
    }

    private void resetIdleTimers() {
        cancelIdleTimers();
        if (inlinePet == null || busy || typingTask != null || isFinishing() || "run".equals(currentAnimation)) return;
        sitIdleTask = () -> {
            sitIdleTask = null;
            if (inlinePet == null || busy || typingTask != null || isFinishing()) return;
            setAnimation("sit");
            sleepIdleTask = () -> {
                sleepIdleTask = null;
                if (inlinePet != null && !busy && typingTask == null && !isFinishing()) setAnimation("sleep");
            };
            main.postDelayed(sleepIdleTask, 5_000L);
        };
        main.postDelayed(sitIdleTask, 5_000L);
    }

    private void saveMessage(String role, String body) {
        if (!role.equals("user") && !role.equals("assistant")) return;
        JSONArray history = loadHistory();
        try { history.put(new JSONObject().put("role", role).put("content", body)); } catch (Exception ignored) {}
        while (history.length() > 16) history.remove(0);
        getSharedPreferences("chat", 0).edit().putString("history", history.toString()).apply();
    }

    @Override protected void onDestroy() {
        if (voiceUiInstance == this) voiceUiInstance = null;
        if(inlineVoice!=null)inlineVoice.stop();
        OpenAiChatClient.cancelAuthorization(this);
        main.removeCallbacksAndMessages(null);
        cancelIdleTimers();
        if (typingTask != null) main.removeCallbacks(typingTask);
        worker.shutdownNow();
        super.onDestroy();
    }

    private void restoreLatestSpeech() {
        JSONArray history = loadHistory();
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject item = history.optJSONObject(i);
            if (item != null && "assistant".equals(item.optString("role"))) {
                speech.setText(item.optString("content")); return;
            }
        }
        speech.setText("Ciao! Sono qui. Scrivimi quello che vuoi, oppure trascinami un po’ sullo schermo. ✨");
    }

    private JSONArray loadHistory() {
        try { return new JSONArray(getSharedPreferences("chat",0).getString("history","[]")); }
        catch(Exception ignored) { return new JSONArray(); }
    }

    private void refreshConnectionState() {
        boolean connected = OpenAiChatClient.isConnected(this);
        connectButton.setText(connected ? "ChatGPT ✓" : "Collega ChatGPT");
        boolean authPending = authBrowser || OpenAiChatClient.isAuthorizing();
        connectButton.setEnabled(!authPending);
        composer.setEnabled(!busy && !authPending); sendButton.setEnabled(!busy && !authPending);
        status.setText(connected ? (Settings.canDrawOverlays(this) ? "Chat privata · collegata a ChatGPT" : "Chat collegata · tocca per attivare l’avatar flottante") :
                (Settings.canDrawOverlays(this) ? "Scrivi per iniziare la chat" : "Chat pronta · avatar flottante disattivato"));
        status.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        });
    }

    private void showFirstUseNotice() {
        android.content.SharedPreferences p=getSharedPreferences("chatgpt_oauth",0);
        if (p.getBoolean("plan_welcome_seen",false)) return;
        connectionNoticePending = true;
        new AlertDialog.Builder(this).setTitle("Stai usando il tuo piano ChatGPT")
                .setMessage("Le richieste IA idonee in Solphivia useranno il tuo piano ChatGPT. Puoi controllare o gestire l’utilizzo nelle impostazioni di ChatGPT.")
                .setPositiveButton("Ho capito",(dialog,which)-> { })
                .setOnDismissListener(dialog -> {
                    connectionNoticePending = false;
                    p.edit().putBoolean("plan_welcome_seen",true).apply();
                    if (!isFinishing() && !isDestroyed()) {
                        submitQueuedFirstMessage();
                        startVoiceAccessOnLaunch();
                    }
                }).show();
    }

    private void openUsageSettings() {
        try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://chatgpt.com/settings/usage"))); }
        catch(Exception e) { Toast.makeText(this,"Apri ChatGPT → Impostazioni → Utilizzo",Toast.LENGTH_LONG).show(); }
    }

    @Override protected void onStart() {
        super.onStart();
        PetService.setInternalChatVisible(true);
    }

    @Override protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            if (!voiceAccessStarted) PetService.setInternalChatVisible(false);
            if (!voiceAccessStarted && !authBrowser && Settings.canDrawOverlays(this)
                    && !getSharedPreferences("pet",0).getBoolean("overlay_off",false)) {
                Intent service = new Intent(this, PetService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            }
        }
    }

    @Override protected void onResume() {
        super.onResume(); activityPaused = false;
        voiceUiInstance = this;
        authBrowser = OpenAiChatClient.isAuthorizing();
        if (!authBrowser && startVoiceAccessOnLaunch()) return;
        if (status != null) refreshConnectionState();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())) voiceAccessStarted = false;
        authBrowser = OpenAiChatClient.isAuthorizing();
        if (!authBrowser && startVoiceAccessOnLaunch()) return;
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || isFinishing() || OpenAiChatClient.isAuthorizing()) return;
        if (startVoiceAccessOnLaunch()) return;
    }

    private boolean startVoiceAccessOnLaunch() {
        if (voiceAccessStarted || !hasWindowFocus() || authBrowser || connectionNoticePending ||
                isFinishing() || isDestroyed()) return false;
        voiceAccessStarted = true;
        try {
            startActivity(new Intent(this, VoiceAccessActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            return true;
        } catch (RuntimeException error) {
            voiceAccessStarted = false;
            android.util.Log.w("Solphivia", "Voice Access non disponibile all’avvio", error);
            return false;
        }
    }

    private void submitQueuedFirstMessage() {
        if (queuedFirstMessage.isEmpty() || composer == null || busy) return;
        composer.setText(queuedFirstMessage);
        queuedFirstMessage = "";
        sendMessage();
    }

    static void setVoiceAccessPresentation(String text, String pose) {
        MainActivity activity = voiceUiInstance;
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.main.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (text != null) {
                    activity.speechTarget = text;
                    activity.setSpeech(text, false);
                }
                if (pose != null) activity.setAnimation(pose);
            });
        }
        PetService.setVoiceAccessPresentation(text, pose);
    }

    static void restartVoiceAccessWhenResumed() {
        MainActivity activity = voiceUiInstance;
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed())
            activity.voiceAccessStarted = false;
    }

    @Override protected void onPause() {
        super.onPause(); activityPaused = true;
        if(inlineVoice!=null)inlineVoice.stop();
        if (!voiceAccessStarted && !isFinishing() && !authBrowser && Settings.canDrawOverlays(this) && !getSharedPreferences("pet",0).getBoolean("overlay_off",false)) {
            Intent service = new Intent(this, PetService.class).setAction(PetService.ACTION_ANIMATE).putExtra(PetService.EXTRA_ANIMATION, currentAnimation);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        }
    }

    private GradientDrawable roundRect(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if ((stroke >>> 24) != 0) d.setStroke(dp(1), stroke); return d;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class BubbleTail extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path triangle = new Path();
        private boolean pointLeft;
        BubbleTail(Context context) { super(context); fill.setColor(0xFFF8F5F9); }
        void setPointLeft(boolean value) { pointLeft = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            triangle.reset();
            if (pointLeft) {
                triangle.moveTo(getWidth(), 0); triangle.lineTo(0, getHeight() / 2f); triangle.lineTo(getWidth(), getHeight());
            } else {
                triangle.moveTo(0, 0); triangle.lineTo(getWidth(), getHeight() / 2f); triangle.lineTo(0, getHeight());
            }
            triangle.close();
            canvas.drawPath(triangle, fill);
        }
    }
}
