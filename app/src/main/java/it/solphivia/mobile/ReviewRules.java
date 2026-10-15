package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Pure policies shared by Android code and the offline regression checks. */
final class ReviewRules {
    static boolean weatherSetup(String text, boolean hasAttachments) {
        if (hasAttachments) return false;
        String value = Normalizer.normalize(text.toLowerCase(Locale.ITALIAN), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[.!?]+$", "").trim().replaceAll("\\s+", " ");
        return value.equals("meteo") || value.equals("meteorina") || value.equals("previsioni") ||
                value.equals("che tempo fa") || value.equals("che tempo fara") ||
                value.equals("attiva meteo") || value.equals("attiva meteorina");
    }

    static boolean freshRadar(long scan, long now) {
        return scan > 0 && scan <= now + 60_000L && now - scan <= 15 * 60_000L;
    }

    static boolean nearbyEcho(double distanceKm) {
        return Double.isFinite(distanceKm) && distanceKm >= 0 && distanceKm <= 30;
    }
}
