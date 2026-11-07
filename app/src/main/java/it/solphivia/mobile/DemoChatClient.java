package it.solphivia.mobile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.HttpsURLConnection;

/** Shared text transport for the activity and floating bubble. No OAuth fallback. */
final class DemoChatClient {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, Task> ACTIVE = new ConcurrentHashMap<>();

    static void cancel(ExecutorService owner) {
        for (Task task : ACTIVE.values()) {
            if (task.owner == owner) {
                task.cancelled = true;
                HttpsURLConnection connection = task.connection;
                if (connection != null) connection.disconnect();
                ACTIVE.remove(task.id, task);
            }
        }
    }

    static void respond(Context context, ExecutorService owner, JSONArray history, JSONArray attachments,
                        boolean voice, OpenAiChatClient.ChatCallback callback) {
        try {
            if (voice) throw new IllegalArgumentException("La demo è disponibile nella chat di testo.");
            if (attachments.length() != 0) throw new IllegalArgumentException("La demo accetta solo testo. Gli allegati restano sul telefono.");
            JSONObject last = history.optJSONObject(history.length() - 1);
            if (last == null || !"user".equals(last.optString("role"))) throw new IllegalArgumentException("Scrivi un messaggio per provare la demo.");
            JSONArray refs = last.optJSONArray("attachments");
            if (refs != null && refs.length() > 0) throw new IllegalArgumentException("La demo accetta solo testo.");
            String message = last.optString("content");
            DemoProtocol.validateMessage(message);
            String origin = DemoProtocol.origin(BuildConfig.DEMO_BASE_URL);
            Task task = new Task(context.getApplicationContext(), owner, callback, message, origin);
            ACTIVE.put(task.id, task);
            try { owner.execute(task); }
            catch (RuntimeException error) { ACTIVE.remove(task.id); throw error; }
        } catch (Exception error) {
            String message = error instanceof IllegalArgumentException ? error.getMessage() : "Il servizio demo è stato fermato.";
            MAIN.post(() -> { if (!owner.isShutdown()) callback.onError(message); });
        }
    }

    private static final class Task implements Runnable {
        final Context context;
        final ExecutorService owner;
        final OpenAiChatClient.ChatCallback callback;
        final String message, origin;
        final String id = UUID.randomUUID().toString();
        volatile boolean cancelled;
        volatile HttpsURLConnection connection;

        Task(Context context, ExecutorService owner, OpenAiChatClient.ChatCallback callback, String message, String origin) {
            this.context = context; this.owner = owner; this.callback = callback;
            this.message = message; this.origin = origin;
        }

        @Override public void run() {
            try {
                if (cancelled || owner.isShutdown()) return;
                String invite = DemoAccess.invite(context);
                connection = (HttpsURLConnection) new URL(origin + "/v1/chat").openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000); connection.setReadTimeout(45000);
                connection.setRequestMethod("POST"); connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("Authorization", "Bearer " + invite);
                byte[] payload = new JSONObject().put("message", message).put("request_id", id).toString().getBytes(StandardCharsets.UTF_8);
                if (payload.length > 8192) throw new IllegalStateException("Il messaggio codificato supera il limite della demo.");
                connection.setFixedLengthStreamingMode(payload.length);
                if (cancelled) return;
                try (java.io.OutputStream output = connection.getOutputStream()) { output.write(payload); }
                int status = connection.getResponseCode();
                InputStream stream = status == 200 ? connection.getInputStream() : connection.getErrorStream();
                if (stream == null) throw new IllegalStateException(DemoProtocol.error("unavailable"));
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                try (InputStream input = stream) {
                    byte[] buffer = new byte[2048]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (body.size() + count > DemoProtocol.MAX_RESPONSE_BYTES) throw new IllegalStateException("Risposta demo troppo grande.");
                        body.write(buffer, 0, count);
                    }
                }
                JSONObject result = new JSONObject(body.toString(StandardCharsets.UTF_8.name()));
                if (status != 200) throw new IllegalStateException(DemoProtocol.error(result.optString("error")));
                String reply = result.optString("reply");
                if (!id.equals(result.optString("request_id")) || reply.trim().isEmpty() || reply.length() > 4000) {
                    throw new IllegalStateException("Risposta demo non valida.");
                }
                MAIN.post(() -> { if (!cancelled && !owner.isShutdown()) callback.onSuccess(reply, "nod", "stay", ""); });
            } catch (Exception error) {
                String reason = error instanceof IllegalStateException ? error.getMessage() : "Non riesco a raggiungere la demo. Controlla la rete e il codice d’invito. Il tentativo può aver consumato una richiesta.";
                MAIN.post(() -> { if (!cancelled && !owner.isShutdown()) callback.onError(reason); });
            } finally {
                if (connection != null) connection.disconnect();
                ACTIVE.remove(id, this);
            }
        }
    }
}
