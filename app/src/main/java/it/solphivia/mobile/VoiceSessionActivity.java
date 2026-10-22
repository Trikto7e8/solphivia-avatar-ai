package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;

/** Explicit microphone start, separate from the optional single-utterance dictation. */
public final class VoiceSessionActivity extends Activity {
    private TextView status;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){status.setText(VoiceSessionService.statusText());main.postDelayed(this,500);}};
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(28,28,28,28);
        TextView title=new TextView(this);title.setText("Parla con Solphivia");title.setTextSize(24);page.addView(title);
        TextView help=new TextView(this);help.setText("Il richiamo si attiva da solo quando apri Solphivia, con il permesso microfono. Di’ «Hola Solphivia» e attendi «Cosa posso fare per te?», poi pronuncia il comando. L’attesa usa il riconoscitore italiano locale: l’audio non viene inviato online né salvato.\n\nPer trascrivere il comando si usa il servizio Android, che può usare Internet. Il comando vocale viene inviato automaticamente, senza premere Invia. I comandi locali, come «apri WhatsApp», restano sul telefono; le altre richieste vanno al fornitore collegato con la cronologia testuale, senza aggiungere ricordi personali o allegati. Per includere la memoria personale usa la chat scritta e la sua conferma. Dopo la risposta torna al richiamo; mentre Solphivia parla l’ascolto è sospeso.\n\nStop dialogo torna al richiamo. Spegni microfono ferma tutto fino alla prossima apertura o a Parla subito. Non parte al riavvio del telefono. Se il tuo richiamo non viene capito puoi correggerlo con tre esempi locali. La funzione resta sperimentale fino alla prova con la tua voce e nell’ambiente reale.");page.addView(help);
        status=new TextView(this);page.addView(status);
        Button start=new Button(this);start.setText("Parla subito · senza richiamo");page.addView(start);
        Button learn=new Button(this);learn.setText("Impara o correggi Hola Solphivia");page.addView(learn);learn.setOnClickListener(v->startActivity(new Intent(this,WakeCalibrationActivity.class)));
        start.setOnClickListener(v->{
            if(!android.provider.Settings.canDrawOverlays(this)){status.setText("Attiva prima l’avatar flottante nelle impostazioni di Solphivia.");return;}
            if(checkSelfPermission("android.permission.RECORD_AUDIO")!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},81);return;}
            begin();
        });
        Button pause=new Button(this);pause.setText("Stop dialogo · richiamo attivo");page.addView(pause);
        pause.setOnClickListener(v->{if(VoiceSessionService.running()){startService(new Intent(this,VoiceSessionService.class).setAction(VoiceSessionService.PAUSE));status.setText("Dialogo fermo. Microfono ancora attivo per il richiamo.");}else status.setText("Microfono spento.");});
        Button stop=new Button(this);stop.setText("Spegni microfono");page.addView(stop);
        stop.setOnClickListener(v->{stopService(new Intent(this,VoiceSessionService.class));status.setText("Microfono spento. Per ripartire usa Parla subito o riapri l’app.");});
        Button dictation=new Button(this);dictation.setText("Detta una sola frase da modificare");page.addView(dictation);
        dictation.setOnClickListener(v->{stopService(new Intent(this,VoiceSessionService.class));startActivity(new Intent(this,CompanionInputActivity.class).setAction(CompanionInputActivity.ACTION_VOICE));});
        ScreenInsets.apply(this,page);setContentView(page);
    }
    private void begin(){getSharedPreferences("pet",0).edit().putBoolean("overlay_off",false).apply();startForegroundService(new Intent(this,PetService.class).setAction(PetService.ACTION_GREET));startForegroundService(new Intent(this,VoiceSessionService.class).setAction(VoiceSessionService.START));status.setText("Sessione richiesta. Stato e arresto disponibili nella notifica Microfono Solphivia.");}
    @Override protected void onResume(){super.onResume();main.post(refresh);}
    @Override protected void onPause(){main.removeCallbacks(refresh);super.onPause();}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==81){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)begin();else status.setText("Microfono non autorizzato: puoi continuare a scrivere.");}}
}
