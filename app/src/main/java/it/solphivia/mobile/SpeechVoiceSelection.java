package it.solphivia.mobile;

import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

/** Preserve the engine's user-selected voice before considering a fallback. */
final class SpeechVoiceSelection {
    static Voice select(TextToSpeech tts, Locale locale, boolean offlineOnly) {
        return select(tts,locale,offlineOnly,tts.getVoice());
    }

    static Voice select(TextToSpeech tts, Locale locale, boolean offlineOnly, Voice preferred) {
        if (matches(preferred, locale, offlineOnly)
                && sameRegion(locale,preferred)) return preferred;
        Voice engineDefault=tts.getVoice();
        if(matches(engineDefault,locale,offlineOnly)&&sameRegion(locale,engineDefault))return engineDefault;
        Voice best = null;
        int score = Integer.MIN_VALUE;
        Set<Voice> voices = tts.getVoices();
        if (voices != null) for (Voice voice : voices) {
            if (!matches(voice, locale, offlineOnly)) continue;
            int value = voice.getQuality()
                    + (sameRegion(locale,voice) ? 1000 : 0);
            if (best == null || value > score || (value==score&&voice.getName().compareTo(best.getName())<0)) { best = voice; score = value; }
        }
        return best;
    }

    private static boolean sameRegion(Locale locale,Voice voice){return locale.getCountry().isEmpty()||locale.getCountry().equalsIgnoreCase(voice.getLocale().getCountry());}

    private static boolean matches(Voice voice, Locale locale, boolean offlineOnly) {
        return voice != null && locale.getLanguage().equalsIgnoreCase(voice.getLocale().getLanguage())
                && (!offlineOnly || !voice.isNetworkConnectionRequired());
    }
}
