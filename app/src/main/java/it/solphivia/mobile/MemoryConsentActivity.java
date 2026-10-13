package it.solphivia.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import java.util.*;
import java.util.function.Consumer;

/** In-memory, expiring, single-use grant for one frozen chat request. */
public final class MemoryConsentActivity extends Activity {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Pending> PENDING = new HashMap<>();
    private static final class Pending {
        final String preview, memory; final Consumer<String> approved; final Runnable cancelled;
        Pending(String p, String m, Consumer<String> a, Runnable c) { preview=p; memory=m; approved=a; cancelled=c; }
    }
    private String id;
    static void request(Context context, String preview, String memory, Consumer<String> approved, Runnable cancelled) {
        MAIN.post(() -> {
            if (memory.trim().isEmpty()) { approved.accept(""); return; }
            String key = UUID.randomUUID().toString();
            PENDING.put(key, new Pending(preview, memory, approved, cancelled));
            MAIN.postDelayed(() -> { Pending expired = PENDING.remove(key); if (expired != null) expired.cancelled.run(); }, 300000);
            try { context.startActivity(new Intent(context, MemoryConsentActivity.class).putExtra("grant", key).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) { PENDING.remove(key); cancelled.run(); }
        });
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        id = getIntent().getStringExtra("grant"); Pending pending = PENDING.get(id);
        if (pending == null) { finish(); return; }
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(24,24,24,24);
        TextView title = new TextView(this); title.setText("Memoria per questa risposta"); title.setTextSize(22); page.addView(title);
        TextView help = new TextView(this); help.setText("Destinazione: OpenAI / ChatGPT. Puoi modificare i ricordi da includere o inviare la richiesta senza memoria. Il consenso vale solo per questo invio e scade dopo 5 minuti."); page.addView(help);
        ScrollView scroll = new ScrollView(this); TextView request = new TextView(this); request.setText(pending.preview); scroll.addView(request);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        EditText memory = new EditText(this); memory.setGravity(android.view.Gravity.TOP); memory.setText(pending.memory);
        memory.setSaveEnabled(false);
        memory.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        page.addView(memory, new LinearLayout.LayoutParams(-1, 0, 1));
        Button yes = new Button(this); yes.setText("Invia con questi ricordi"); page.addView(yes);
        yes.setOnClickListener(v -> {
            try { AssistantMemory.validate(memory.getText().toString()); }
            catch (Exception e) { memory.setError(e.getMessage()); return; }
            consume(memory.getText().toString());
        });
        Button no = new Button(this); no.setText("Invia senza memoria"); page.addView(no); no.setOnClickListener(v -> consume(""));
        Button cancel = new Button(this); cancel.setText("Annulla invio"); page.addView(cancel); cancel.setOnClickListener(v -> onBackPressed());
        ScreenInsets.apply(this,page);setContentView(page);
    }
    private void consume(String text) {
        Pending pending = PENDING.remove(id);
        if (pending != null) pending.approved.accept(text);
        else Toast.makeText(this, "Anteprima scaduta. Invia di nuovo il messaggio.", Toast.LENGTH_LONG).show();
        finish();
    }
    @Override public void onBackPressed() { Pending pending = PENDING.remove(id); if (pending != null) pending.cancelled.run(); finish(); }
    @Override protected void onDestroy() {
        if (!isChangingConfigurations()) { Pending pending = PENDING.remove(id); if (pending != null) pending.cancelled.run(); }
        super.onDestroy();
    }
}
