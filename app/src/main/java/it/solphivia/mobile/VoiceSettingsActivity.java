package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import java.util.*;

/** Same installed voice for both profiles; no paid endpoint, recording or model download. */
public final class VoiceSettingsActivity extends Activity {
    static final String SAMPLE="Ciao, sono Solphivia. Cosa posso fare per te? Ti ascolto e posso aiutarti. Scegli la voce che preferisci. Oggi sono le 14:30: ci sono 23 gradi e 2 messaggi. Apri WhatsApp e cerca una farmacia su Maps.";
    private TextToSpeech discovery;
    private DeviceSpeech preview;
    private TextView status;
    private final ArrayList<String> engines=new ArrayList<>();
    @Override public void onCreate(Bundle state){
        super.onCreate(state);VoicePreferences.initialize(this);preview=new DeviceSpeech(this);
        android.content.SharedPreferences prefs=getSharedPreferences("pet",0);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
        int pad=Math.round(18*getResources().getDisplayMetrics().density);page.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);title.setText("Voce di Solphivia");title.setTextSize(24);page.addView(title);
        TextView help=new TextView(this);help.setText("La scelta iniziale è una voce italiana naturale del motore Android installato. L’accento britannico resta facoltativo: la correzione Chiara prova a renderne più comprensibili le parole, ma non è un controllo dell’intensità dell’accento. Confronta le tre prove e scegli. Nessun modello aggiuntivo o servizio a pagamento; la disponibilità offline dipende dalle voci installate.");page.addView(help);
        Spinner engine=new Spinner(this);engine.setContentDescription("Motore vocale");page.addView(engine);
        Switch accent=new Switch(this);accent.setText("Usa una voce inglese · facoltativo");accent.setChecked(prefs.getBoolean("english_accent",false));page.addView(accent);
        Switch clear=new Switch(this);clear.setText("Correzione della voce inglese · sperimentale");clear.setChecked(prefs.getBoolean("clear_italian",true));page.addView(clear);
        Switch aloud=new Switch(this);aloud.setText("Leggi le risposte ad alta voce");aloud.setChecked(prefs.getBoolean("read_aloud",true));page.addView(aloud);
        Switch wake=new Switch(this);wake.setText("Richiamo automatico all’apertura");wake.setChecked(prefs.getBoolean("wake_on_open",true));page.addView(wake);
        Switch interrupt=new Switch(this);interrupt.setText("Interrompi con Stop, Alt o Basta Solphivia · sperimentale");interrupt.setChecked(prefs.getBoolean("voice_interrupt",true));page.addView(interrupt);
        TextView rateLabel=new TextView(this);page.addView(rateLabel);
        SeekBar rate=new SeekBar(this);rate.setMax(40);rate.setProgress(SpeechText.boundedRate(prefs.getInt("voice_rate_percent",100))-75);rate.setContentDescription("Ritmo della voce rispetto alle impostazioni Android");page.addView(rate);
        rateLabel.setText("Ritmo: "+(rate.getProgress()+75)+"% delle impostazioni Android");
        rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){rateLabel.setText("Ritmo: "+(progress+75)+"% delle impostazioni Android");}
            public void onStartTrackingTouch(SeekBar bar){} public void onStopTrackingTouch(SeekBar bar){}
        });
        EditText sample=new EditText(this);sample.setText(SAMPLE);sample.setMinLines(3);sample.setGravity(android.view.Gravity.TOP);sample.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);sample.setSaveEnabled(false);sample.setContentDescription("Frase per confrontare le pronunce");page.addView(sample);
        status=new TextView(this);page.addView(status);
        Button natural=new Button(this);natural.setText("Ascolta italiano naturale · consigliato");page.addView(natural);
        Button classic=new Button(this);classic.setText("Ascolta inglese classico");page.addView(classic);
        Button improved=new Button(this);improved.setText("Ascolta inglese con correzione Chiara");page.addView(improved);
        Button stop=new Button(this);stop.setText("Ferma la prova");page.addView(stop);stop.setOnClickListener(v->{preview.stop();status.setText("Prova fermata.");});
        Button save=new Button(this);save.setText("Salva e prova la scelta");page.addView(save);
        Button system=new Button(this);system.setText("Configura le voci Android");page.addView(system);
        system.setOnClickListener(v->{try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}catch(RuntimeException error){status.setText("Apri Impostazioni Android → Sintesi vocale.");}});
        ScrollView scroll=new ScrollView(this);scroll.addView(page);ScreenInsets.apply(this,scroll);setContentView(scroll);
        discovery=new TextToSpeech(this,code->{});
        ArrayList<String> labels=new ArrayList<>();labels.add("Motore predefinito Android");engines.add("");
        for(TextToSpeech.EngineInfo info:discovery.getEngines()){labels.add(info.label);engines.add(info.name);}
        engine.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        engine.setSelection(Math.max(0,engines.indexOf(prefs.getString("tts_engine",""))));
        natural.setOnClickListener(v->play(sample.getText().toString(),engines.get(engine.getSelectedItemPosition()),false,false,rate.getProgress()+75));
        classic.setOnClickListener(v->{play(sample.getText().toString(),engines.get(engine.getSelectedItemPosition()),true,false,rate.getProgress()+75);});
        improved.setOnClickListener(v->{play(sample.getText().toString(),engines.get(engine.getSelectedItemPosition()),true,true,rate.getProgress()+75);});
        save.setOnClickListener(v->{
            String selected=engines.get(engine.getSelectedItemPosition());
            prefs.edit().putString("tts_engine",selected).putBoolean("english_accent",accent.isChecked()).putBoolean("clear_italian",clear.isChecked()).putBoolean("read_aloud",aloud.isChecked()).putBoolean("wake_on_open",wake.isChecked()).putInt("voice_rate_percent",rate.getProgress()+75).putBoolean("voice_interrupt",interrupt.isChecked()).apply();
            if(!wake.isChecked())stopService(new Intent(this,VoiceSessionService.class));
            PetService.reloadSpeechPreferences();
            if(aloud.isChecked())play(sample.getText().toString(),selected,accent.isChecked(),clear.isChecked(),rate.getProgress()+75);
            else{preview.stop();status.setText("Impostazioni salvate. Lettura ad alta voce disattivata.");}
        });
    }
    private void play(String text,String engine,boolean english,boolean clear,int rate){
        if(text.trim().isEmpty()){status.setText("Inserisci una frase da ascoltare.");return;}
        if(!PetService.prepareSpeechPreview()){status.setText("Attendi la risposta in corso prima della prova.");return;}
        status.setText("Preparazione della prova…");
        preview.speak(text,engine,english,clear,rate,message->{if(!isFinishing()&&!isDestroyed())status.setText(message);});
    }
    @Override protected void onPause(){if(preview!=null)preview.stop();super.onPause();}
    @Override protected void onDestroy(){if(preview!=null)preview.stop();if(discovery!=null){discovery.shutdown();discovery=null;}super.onDestroy();}
}
