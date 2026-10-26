package it.solphivia.mobile;

import android.widget.EditText;

/** Owns only the current capture preview, never text typed independently by the user. */
final class VoiceTranscriptComposer {
    private final EditText composer;
    private final String original;
    private final boolean originalReceipt;
    private String rendered;
    private boolean active;

    VoiceTranscriptComposer(EditText composer) {
        this.composer=composer;
        original=composer.getText().toString();originalReceipt=SentVoiceTranscript.owns(composer);rendered=original;active=original.isEmpty()||originalReceipt;if(active)SentVoiceTranscript.capture(composer);
    }
    boolean canSubmit(){return active && composer.getText().toString().equals(rendered);}
    void show(String text){
        if(!canSubmit()){active=false;SentVoiceTranscript.captureFinished(composer);return;}
        if(text==null || text.isEmpty())return;
        SentVoiceTranscript.release(composer);rendered=text;composer.setText(text);composer.setSelection(text.length());
    }
    void cancel(){
        if(canSubmit()){if(originalReceipt)SentVoiceTranscript.hold(composer,original);else{composer.setText(original);composer.setSelection(original.length());}}
        active=false;SentVoiceTranscript.captureFinished(composer);
    }
    void finish(){if(canSubmit())SentVoiceTranscript.hold(composer,rendered);active=false;SentVoiceTranscript.captureFinished(composer);}
}
