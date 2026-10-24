package it.solphivia.mobile;

/** Monotonic-time policy: five seconds to start speaking after each finished reply. */
final class VoiceFollowUpWindow {
    static final long WINDOW_MILLIS = 5000L;
    private boolean awaitingReply, speechStarted;
    private long deadline;

    void replyStarted() { awaitingReply=true; deadline=0; speechStarted=false; }
    boolean awaitingReply() { return awaitingReply; }
    void replyFinished() { awaitingReply=false; }
    void captureReady(long now) { if(!awaitingReply && deadline==0)deadline=now+WINDOW_MILLIS; }
    boolean speechStarted(long now) {
        if(awaitingReply || deadline==0 || now>=deadline)return false;
        speechStarted=true;
        return true;
    }
    boolean expired(long now) { return deadline!=0 && !speechStarted && now>=deadline; }
    boolean canRetry(long now) { return !awaitingReply && deadline!=0 && !speechStarted && now<deadline; }
    void rest() { awaitingReply=false; deadline=0; speechStarted=false; }
}
