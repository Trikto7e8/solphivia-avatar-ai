package it.solphivia.mobile;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.widget.*;

/** Optional configuration only: does not replace the launcher or request microphone permissions. */
public final class ModelConnectionsActivity extends OverlaySettingsActivity {
    private LinearLayout page;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);showChoices();}
    private void startPage(String title){
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);int p=Math.round(20*getResources().getDisplayMetrics().density);page.setPadding(p,p,p,p);
        TextView heading=new TextView(this);heading.setText(title);heading.setTextSize(24);page.addView(heading);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);ScreenInsets.apply(this,page);
    }
    private void text(String value){TextView t=new TextView(this);t.setText(value);t.setTextSize(16);page.addView(t);}
    private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setMinHeight(Math.round(48*getResources().getDisplayMetrics().density));b.setOnClickListener(v->action.run());page.addView(b);}
    private void showChoices(){
        startPage("Collegamenti IA");
        text("Scegli chi produce le risposte. Voce, microfono e avatar restano quelli di Solphivia. La quota del progetto vale solo per il servizio con invito. Le API personali possono avere costi separati dal tuo abbonamento alla chat.");
        button("Servizio Solphivia · codice d’invito",()->{startActivity(new Intent(this,DemoActivity.class));finish();});
        button("Collega ChatGPT · modalità esistente",()->{if(!canChange())return;ModelConnection.clear(this);DemoAccess.disable(this);startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.ACTION_CONNECT_CHATGPT));finish();});
        for(ModelEndpoint.Kind kind:ModelEndpoint.Kind.values())button(ModelEndpoint.label(kind),()->configure(kind));
        button("Personalità e memoria",()->new AlertDialog.Builder(this).setTitle("Solphivia rimane Solphivia")
            .setMessage("Il modello riceve la stessa identità e i tag dell’avatar. La voce Android resta indipendente dal modello. La memoria personale viene inviata solo dopo il consenso già previsto; le richieste vocali escludono vault e allegati. Il collegamento portatile a Memory Vault e le personalizzazioni aggiuntive restano separati da questa configurazione.")
            .setPositiveButton("Ho capito",null).show());
    }
    private boolean canChange(){if(OpenAiChatClient.requestsInProgress()||PetService.voiceOutputBusy()){Toast.makeText(this,"Attendi la risposta o ferma la lettura prima di cambiare collegamento.",Toast.LENGTH_LONG).show();return false;}return true;}
    private EditText input(String label,boolean secret){
        TextView l=new TextView(this);l.setText(label);page.addView(l);
        EditText field=new EditText(this);field.setId(android.view.View.generateViewId());l.setLabelFor(field.getId());field.setSingleLine(true);
        field.setSaveEnabled(false);field.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        field.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_URI));page.addView(field);return field;
    }
    private void configure(ModelEndpoint.Kind kind){
        startPage(ModelEndpoint.label(kind));
        boolean local=ModelEndpoint.local(kind);
        text(local?"Sul PC avvia il server di LM Studio o Ollama e carica il modello, per esempio Qwen. Il PC deve restare acceso e raggiungibile dal telefono. Qui configuri una connessione al PC, non installi il modello sul telefono.":"Usa una chiave API del tuo fornitore. Non inserire la password di Gemini, Claude o ChatGPT. Il normale abbonamento alla chat non garantisce l’uso API. La chiave viene cifrata sul telefono e inviata solo al servizio confermato.");
        EditText base=input("Indirizzo del servizio (base URL)",false);base.setText(ModelEndpoint.defaultBase(kind));base.setEnabled(ModelEndpoint.defaultBase(kind).isEmpty());
        if(local)base.setHint("http://192.168.1.50:1234 oppure :11434");
        EditText model=input("Identificativo del modello",false);
        EditText key=input(local?"Chiave del server HTTPS, se richiesta":"Chiave API personale",true);
        // Deliberately blank on every visit/provider change. No prefill, no model discovery requests.
        CheckBox lan=new CheckBox(this);lan.setText("Consento HTTP soltanto verso l’IP privato del mio PC, senza chiavi. Il testo non è cifrato sulla rete locale.");lan.setVisibility(local?android.view.View.VISIBLE:android.view.View.GONE);page.addView(lan);
        TextView status=new TextView(this);status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);page.addView(status);
        button("Rivedi e attiva questo collegamento",()->{
            if(!canChange())return;
            try {
                String selectedModel=model.getText().toString().trim(),secret=key.getText().toString().trim();
                ModelEndpoint.validateModel(selectedModel);
                String endpoint=ModelEndpoint.endpoint(kind,base.getText().toString(),lan.isChecked(),!secret.isEmpty());
                if(!local&&secret.isEmpty())throw new IllegalArgumentException("Inserisci una chiave API personale.");
                new AlertDialog.Builder(this).setTitle("Conferma destinazione")
                    .setMessage(ModelEndpoint.label(kind)+"\n"+endpoint+"\nModello: "+selectedModel+"\n\nLe prossime richieste useranno questo servizio. Nessuna cronologia di altri collegamenti sarà trasferita. Non viene inviata una richiesta di prova automatica.")
                    .setNegativeButton("Annulla",null).setPositiveButton("Attiva",(d,w)->{
                        if(!canChange())return;
                        try {ModelConnection.save(this,kind,base.getText().toString(),selectedModel,secret,lan.isChecked());DemoAccess.disable(this);key.setText("");finish();}
                        catch(Exception e){status.setText(e.getMessage()==null?"Collegamento non salvato.":e.getMessage());}
                    }).show();
            }catch(Exception e){status.setText(e.getMessage());}
        });
        button("Torna ai collegamenti",this::showChoices);
    }
}
