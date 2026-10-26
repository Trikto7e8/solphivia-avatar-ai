package it.solphivia.mobile;

import java.text.Normalizer;
import java.util.Locale;

/** Exact user intents only: asking how to close must never close the app. */
final class SessionCommands {
    static final String CLOSE_HELP="Per chiudermi, premi il pulsante ⏻ sotto l’avatar, oppure dopo Hola Solphivia di’ Chiudi Solphivia. Si fermano avatar, voce e microfono; i dati restano salvati. Spegni microfono ferma solo l’ascolto. Puoi riaprirmi dall’icona.";
    private static String normal(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ITALIAN).replace('’','\'').trim().replaceAll("[.!?]+$","").replaceAll("\\s+"," ").trim();}
    static boolean close(String text){String value=normal(text);return value.matches("spegniti|spegni|spegni solphivia|spegni app|spegni l'app|chiudi app|chiudi l'app|chiudi applicazione|chiudi l'applicazione|chiudi solphivia|chiuditi");}
    static boolean help(String text){String value=normal(text);return value.matches("come faccio a chiuderti|come posso chiuderti|come ti chiudo|come faccio a spegnerti|come posso spegnerti|come ti spengo|come si chiude solphivia|come si spegne solphivia|come chiudo solphivia");}
}
