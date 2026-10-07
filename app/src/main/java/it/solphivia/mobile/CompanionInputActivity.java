package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/** Small relay activity for Android's speech recognizer and document picker. */
public class CompanionInputActivity extends Activity {
    static final String ACTION_VOICE = "it.solphivia.mobile.action.VOICE_INPUT";
    static final String ACTION_FILES = "it.solphivia.mobile.action.PICK_FILES";
    static final String ACTION_ATTACHMENTS = "it.solphivia.mobile.action.ATTACHMENTS";
    static final String EXTRA_URIS = "attachment_uris";
    private static final int REQUEST_VOICE = 31;
    private static final int REQUEST_FILES = 32;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String action = getIntent() == null ? null : getIntent().getAction();
        if (ACTION_VOICE.equals(action)) openVoiceInput();
        else openFilePicker();
    }

    private void openVoiceInput() {
        Intent voice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.toLanguageTag());
        voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla con Solphivia");
        voice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        try { startActivityForResult(voice, REQUEST_VOICE); }
        catch (Exception e) {
            Toast.makeText(this, "Riconoscimento vocale non disponibile sul telefono.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openFilePicker() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*"
        });
        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(pick, REQUEST_FILES); }
        catch (Exception e) {
            Toast.makeText(this, "Non trovo un selettore di file sul telefono.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) { finish(); return; }
        if (requestCode == REQUEST_VOICE) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty() && !matches.get(0).trim().isEmpty()) {
                Intent send = new Intent(this, PetService.class).putExtra(PetService.EXTRA_SEND_MESSAGE, matches.get(0));
                startService(send);
            }
        } else if (requestCode == REQUEST_FILES) {
            ArrayList<Uri> selected = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount() && i < 5; i++)
                    selected.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) selected.add(data.getData());
            ArrayList<String> values = new ArrayList<>();
            for (Uri uri : selected) {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                values.add(uri.toString());
            }
            if (!values.isEmpty()) {
                Intent send = new Intent(this, PetService.class).setAction(ACTION_ATTACHMENTS)
                        .putStringArrayListExtra(EXTRA_URIS, values)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (data.getClipData() != null) send.setClipData(data.getClipData());
                else if (!selected.isEmpty()) send.setClipData(ClipData.newRawUri("allegato", selected.get(0)));
                startService(send);
            }
        }
        finish();
    }
}
