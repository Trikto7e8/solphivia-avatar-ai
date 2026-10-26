package it.solphivia.mobile;

final class GreetingPolicy {
    static final String OPENING="Hola! Sono Solphivia. Puoi chiedermi qualcosa, dire apri Maps o apri WhatsApp. Per fermare la voce di’ Stop; per continuare di’ Riprendi. Di’ Comandi per l’elenco completo. Il pulsante tondo a sinistra mi spegne. Per iniziare, di’ Hola Solphivia.";
    private static long last=-10000L;
    static boolean claim() {
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-last<3000L)return false;
        last=now;return true;
    }
}
