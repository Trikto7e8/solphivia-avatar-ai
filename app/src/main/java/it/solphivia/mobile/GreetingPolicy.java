package it.solphivia.mobile;

final class GreetingPolicy {
    private static long last=-10000L;
    static boolean claim() {
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-last<3000L)return false;
        last=now;return true;
    }
}
