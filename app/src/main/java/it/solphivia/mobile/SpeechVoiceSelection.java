package it.solphivia.mobile;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

/** Preserve the engine's user-selected voice before considering a fallback. */
final class SpeechVoiceSelection {
    static Voice select(TextToSpeech tts, Locale locale, boolean offlineOnly) {
        Voice preferred = tts.getVoice();
        if (matches(preferred, locale, offlineOnly)
                && locale.getCountry().equalsIgnoreCase(preferred.getLocale().getCountry())) return preferred;
        Voice best = null;
        int score = Integer.MIN_VALUE;
        Set<Voice> voices = tts.getVoices();
        if (voices != null) for (Voice voice : voices) {
            if (!matches(voice, locale, offlineOnly)) continue;
            int value = voice.getQuality()
                    + (locale.getCountry().equalsIgnoreCase(voice.getLocale().getCountry()) ? 1000 : 0);
            if (best == null || value > score) { best = voice; score = value; }
        }
        return best;
    }

    private static boolean matches(Voice voice, Locale locale, boolean offlineOnly) {
        return voice != null && locale.getLanguage().equalsIgnoreCase(voice.getLocale().getLanguage())
                && (!offlineOnly || !voice.isNetworkConnectionRequired());
    }
}
