package it.solphivia.mobile;

import android.widget.EditText;
import java.util.WeakHashMap;

/** A submitted transcript is a receipt, not an unsent draft. UI thread only. */
final class SentVoiceTranscript {
    private static final WeakHashMap<EditText,String> receipts=new WeakHashMap<>();
    private static final WeakHashMap<EditText,Boolean> captures=new WeakHashMap<>();
    static void capture(EditText field){captures.put(field,true);}
    static void captureFinished(EditText field){captures.remove(field);}
    static boolean owns(EditText field){String value=receipts.get(field);return value!=null&&value.equals(field.getText().toString());}
    static void hold(EditText field,String text){field.setText(text);field.setSelection(0);receipts.put(field,text);}
    static void release(EditText field){receipts.remove(field);}
    static void clearFinished(){for(EditText field:new java.util.ArrayList<>(receipts.keySet())){if(field!=null&&captures.containsKey(field))continue;if(field!=null&&owns(field))field.setText("");receipts.remove(field);}}
}
