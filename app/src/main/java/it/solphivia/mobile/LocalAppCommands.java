package it.solphivia.mobile;

import android.content.Context;
import android.content.Intent;
import java.text.Normalizer;
import java.util.Locale;

/** Explicit user commands only; never executes model output or sends app messages. */
final class LocalAppCommands {
    static boolean isWhatsApp(String text) {
        if (text == null) return false;
        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ITALIAN), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[.!?,;:]", " ").replaceAll("\\s+", " ").trim();
        return normalized.matches("(?:(?:puoi|potresti) )?(?:aprire|apri|avvia|avviare)(?: l app| l'app)? (?:whatsapp|whats app|what s app|watsapp)(?: per favore)?");
    }

    static String handle(Context context, String text) {
        if (!isWhatsApp(text)) return null;
        Intent launch = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
        if (launch == null) launch = context.getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b");
        if (launch == null) return "Non trovo WhatsApp installato sul telefono.";
        try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return "Apro WhatsApp.";
        } catch (RuntimeException error) {
            return "Android non mi ha consentito di aprire WhatsApp. Puoi aprirlo dalla sua icona.";
        }
    }
}
