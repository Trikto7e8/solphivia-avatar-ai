package it.solphivia.mobile;

import android.content.Context;
import java.util.Locale;

/** Explicit user commands are the only chat actions allowed to write persistent memory. */
final class MemoryCommands {
    static String handle(Context context,String message) {
        String text=message.trim(),lower=text.toLowerCase(Locale.ITALIAN),note=null;
        for(String prefix:new String[]{"ricorda:","ricorda che ","memorizza:"})
            if(lower.startsWith(prefix)){note=text.substring(prefix.length()).trim();break;}
        if(note==null)return null;
        if(note.isEmpty())return "Scrivi il ricordo dopo «ricorda:» oppure apri Memoria dal menu ☰.";
        try {
            AssistantMemory.write(context,AssistantMemory.read(context)+"\n\n## Ricordo aggiunto\n"+note+"\n");
            return "Ho salvato sul telefono: "+note+". Puoi modificarlo o rimuoverlo in Memoria.";
        } catch(Exception e){return "Non ho salvato il ricordo: "+e.getMessage();}
    }
}
