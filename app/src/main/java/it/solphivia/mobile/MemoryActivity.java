package it.solphivia.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Local memory editor and explicit file exchange with a vault on any device. */
public final class MemoryActivity extends Activity {
    private EditText editor;
    private TextView status;
    private String savedText = "";
    private final java.util.concurrent.ExecutorService io = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        page.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this); title.setText("Memoria di Solphivia"); title.setTextSize(23); page.addView(title);
        TextView help = new TextView(this);
        help.setText("Memoria cifrata sul telefono e indipendente dal modello. Puoi aggiungere ricordi anche scrivendo «ricorda: testo» nella chat. Prima dell’invio al modello rivedi i ricordi. Importa testo o un backup AI Memory Vault v1: solo ricordi approvati, ordinari e non scaduti. Esporta crea un nuovo vault dal testo dell’editor; non sincronizza il vault originale.");
        page.addView(help);
        Switch enabled = new Switch(this); enabled.setText("Usa la memoria nelle risposte");
        enabled.setChecked(getSharedPreferences("memory", 0).getBoolean("enabled", true));
        enabled.setOnCheckedChangeListener((v, checked) -> getSharedPreferences("memory", 0).edit().putBoolean("enabled", checked).apply());
        page.addView(enabled);
        editor = new EditText(this); editor.setGravity(android.view.Gravity.TOP); editor.setTextSize(15);
        editor.setSaveEnabled(false);
        editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        page.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); page.addView(status);
        LinearLayout buttons = new LinearLayout(this);
        button(buttons, "Salva", () -> save());
        button(buttons, "Importa", () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
            try { startActivityForResult(intent, 1); } catch (RuntimeException e) { status.setText("Selettore file non disponibile."); }
        });
        button(buttons, "Esporta", () -> {
            if (!save()) return;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json").putExtra(Intent.EXTRA_TITLE, "solphivia-memory-vault.json");
            try { startActivityForResult(intent, 2); } catch (RuntimeException e) { status.setText("Selettore file non disponibile."); }
        });
        page.addView(buttons);
        ScreenInsets.apply(this,page);setContentView(page);
        try { savedText = AssistantMemory.read(this); Object draft=getLastNonConfigurationInstance();editor.setText(draft instanceof String?(String)draft:savedText); }
        catch (IOException e) { status.setText("Memoria non leggibile: " + e.getMessage()); }
    }

    private void button(LinearLayout row, String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private boolean save() {
        try { String text = editor.getText().toString(); AssistantMemory.write(this, text); savedText = text; status.setText("Memoria salvata sul telefono."); return true; }
        catch (IOException e) { status.setText(e.getMessage()); return false; }
    }
    @Override public Object onRetainNonConfigurationInstance() { return editor.getText().toString(); }
    @Override public void onBackPressed() {
        if (savedText.equals(editor.getText().toString())) { super.onBackPressed(); return; }
        new AlertDialog.Builder(this).setTitle("Salvare la memoria?")
                .setPositiveButton("Salva", (d, w) -> { if (save()) finish(); })
                .setNegativeButton("Esci senza salvare", (d, w) -> finish()).setNeutralButton("Resta", null).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        String exportText = editor.getText().toString();
        if (request==2) { askPassword("Proteggi la copia portabile", password -> io.execute(() -> {
            try {
                android.content.SharedPreferences prefs=getSharedPreferences("pet",0);
                String encrypted=VaultBackup.seal(VaultBackup.snapshot(exportText,prefs.getBoolean("read_aloud",true),prefs.getBoolean("english_accent",false)),password);
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
                    if(out==null)throw new IOException("Destinazione non accessibile"); out.write(encrypted.getBytes(StandardCharsets.UTF_8));
                }
                runOnUiThread(() -> { if(!isDestroyed())status.setText("Copia cifrata esportata, compatibile con AI Memory Vault v1."); });
            } catch(Exception e) { runOnUiThread(() -> { if(!isDestroyed())status.setText("Esportazione non riuscita. Il file di destinazione può essere vuoto: riprova."); }); }
            finally { java.util.Arrays.fill(password,'\0'); }
        })); return; }
        status.setText(request == 1 ? "Leggo il file…" : "Esporto la memoria…");
        io.execute(() -> {
            try {
                if (request == 1) {
                    String text;
                    try (InputStream input = getContentResolver().openInputStream(data.getData())) { text = VaultBackup.readFile(input); }
                    if (text.trim().startsWith("{")) {
                        String backup=text;
                        runOnUiThread(() -> {
                            if(isDestroyed())return;
                            askPassword("Apri il backup Memory Vault", password -> io.execute(() -> {
                                try {
                                    String extracted=VaultBackup.selectedText(VaultBackup.unlock(backup,password),System.currentTimeMillis());
                                    runOnUiThread(() -> offerImport(extracted));
                                } catch(Exception e) { runOnUiThread(() -> { if(!isDestroyed())status.setText("Backup non importato: password errata, formato non supportato o memoria oltre 32 KB."); }); }
                                finally { java.util.Arrays.fill(password,'\0'); }
                            }));
                        });
                        return;
                    }
                    AssistantMemory.validate(text);
                    String imported = text;
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        offerImport(imported);
                    });
                }
            } catch (Exception e) { runOnUiThread(() -> { if (!isDestroyed()) status.setText("Operazione non riuscita: " + e.getMessage()); }); }
        });
    }
    private void offerImport(String text) {
        if(isFinishing() || isDestroyed())return;
        new AlertDialog.Builder(this).setTitle("Importa nella memoria")
                .setMessage("Aggiungi il testo oppure sostituisci l’editor. Controlla i ricordi prima di salvare; la memoria corrente resta intatta fino al salvataggio.")
                .setPositiveButton("Aggiungi",(d,w)-> { String combined=editor.getText()+"\n\n"+text; try { AssistantMemory.validate(combined); editor.setText(combined); status.setText("Controlla il testo e premi Salva."); } catch(IOException e){status.setText(e.getMessage());} })
                .setNeutralButton("Sostituisci",(d,w)-> {editor.setText(text);status.setText("Controlla il testo e premi Salva.");})
                .setNegativeButton("Annulla",null).show();
    }
    private void askPassword(String title, java.util.function.Consumer<char[]> next) {
        if(isFinishing() || isDestroyed())return;
        EditText password=new EditText(this); password.setSingleLine(true);
        password.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false); password.setHint("Passphrase di almeno 12 caratteri");
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(password).setPositiveButton("Continua",null).setNegativeButton("Annulla",null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(password.length()<12) {password.setError("Servono almeno 12 caratteri.");return;}
            char[] chars=new char[password.length()]; android.text.TextUtils.getChars(password.getText(),0,chars.length,chars,0);
            password.setText(""); dialog.dismiss(); status.setText("Elaborazione locale del backup…"); next.accept(chars);
        })); dialog.show();
    }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }
}
