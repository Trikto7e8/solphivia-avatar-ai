package it.solphivia.mobile;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

/** Keeps file references in history and materializes bounded content only for a request. */
final class AttachmentReader {
    static final long MAX_FILE = 8L * 1024 * 1024;
    static final long MAX_TOTAL = 15L * 1024 * 1024;

    static JSONArray read(Context context, ArrayList<String> uris, long[] total) throws Exception {
        JSONArray result = new JSONArray();
        for (String raw : uris) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Uri uri = Uri.parse(raw);
            String name = "allegato";
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (column >= 0) name = cursor.getString(column);
                }
            }
            String mime = context.getContentResolver().getType(uri);
            if (mime == null) mime = "application/octet-stream";
            byte[] bytes;
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) throw new IllegalStateException("Allegato non più accessibile: " + name);
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    total[0] += count;
                    if ((long) output.size() + count > MAX_FILE) throw new IllegalArgumentException("Allegato oltre 8 MB: " + name);
                    if (total[0] > MAX_TOTAL) throw new IllegalArgumentException("Allegati della conversazione oltre 15 MB. Inizia una nuova chat.");
                    output.write(buffer, 0, count);
                }
                bytes = output.toByteArray();
            }
            if (bytes.length == 0) throw new IllegalArgumentException("Allegato vuoto: " + name);
            result.put(new JSONObject().put("uri", raw).put("filename", name).put("mime_type", mime)
                    .put("size", bytes.length).put("data_uri", "data:" + mime + ";base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)));
        }
        return result;
    }

    static JSONArray references(JSONArray attachments) throws Exception {
        JSONArray refs = new JSONArray();
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject item = attachments.getJSONObject(i);
            refs.put(new JSONObject().put("uri", item.getString("uri")).put("filename", item.getString("filename")));
        }
        return refs;
    }
}
