package it.solphivia.mobile;

/** Small, explicit voice-phase model used to keep labels tied to the operation in progress. */
final class VoiceStatusState {
    static final String CAPTURE_STARTING_LABEL = "Avvio del microfono…";
    static final String LISTENING_LABEL = "Ti ascolto…";
    static final String TRANSCRIBING_LABEL = "Sto trascrivendo…";
    static final String THINKING_LABEL = "Sto pensando…";
    static final String PLAYING_LABEL = "Sto parlando…";
    static final String ERROR_LABEL = "Errore del microfono.";
    static final String IDLE_LABEL = "Microfono spento.";

    enum Phase { IDLE, CAPTURING, TRANSCRIBING, GENERATING, PLAYING, ERROR }

    private Phase phase = Phase.IDLE;

    Phase phase() { return phase; }
    void captureStarted() { phase = Phase.CAPTURING; }
    void transcriptionStarted() { phase = Phase.TRANSCRIBING; }
    void generationStarted() { phase = Phase.GENERATING; }
    void playbackStarted() { phase = Phase.PLAYING; }
    void error() { phase = Phase.ERROR; }
    void idle() { phase = Phase.IDLE; }

    String label() {
        switch (phase) {
            case CAPTURING: return LISTENING_LABEL;
            case TRANSCRIBING: return TRANSCRIBING_LABEL;
            case GENERATING: return THINKING_LABEL;
            case PLAYING: return PLAYING_LABEL;
            case ERROR: return ERROR_LABEL;
            case IDLE:
            default: return IDLE_LABEL;
        }
    }
}
