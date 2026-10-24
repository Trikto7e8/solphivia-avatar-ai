package it.solphivia.mobile;

import android.widget.EditText;

/** Owns only the current capture preview, never text typed independently by the user. */
final class VoiceTranscriptComposer {
    private final EditText composer;
    private final String original;
    private String rendered;
    private boolean active;

    VoiceTranscriptComposer(EditText composer) {
        this.composer=composer;
        original=composer.getText().toString();rendered=original;active=original.isEmpty();
    }
    boolean canSubmit(){return active && composer.getText().toString().equals(rendered);}
    void show(String text){
        if(!canSubmit()){active=false;return;}
        if(text==null || text.isEmpty())return;
        rendered=text;composer.setText(text);composer.setSelection(text.length());
    }
    void cancel(){
        if(canSubmit()){composer.setText(original);composer.setSelection(original.length());}
        active=false;
    }
    void finish(){active=false;}
}
