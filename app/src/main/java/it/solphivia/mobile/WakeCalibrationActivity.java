package it.solphivia.mobile;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.os.*;
import android.widget.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.util.ArrayList;

/** Learn local phrase variants in memory from explicit foreground recordings; never save audio. */
public final class WakeCalibrationActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ArrayList<String> examples=new ArrayList<>();
    private TextView status;private Button record;private volatile boolean capturing;private volatile AudioRecord input;private boolean closed;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(30,30,30,30);
        TextView title=new TextView(this);title.setText("Impara il mio richiamo");title.setTextSize(24);page.addView(title);
        TextView help=new TextView(this);help.setText("Se il richiamo non viene riconosciuto, registra tre esempi di «Hola Solphivia», uno per pulsante, con la tua voce normale. Aspetta che Solphivia finisca di parlare.\n\nL’audio viene elaborato qui e scartato. Le varianti riconosciute restano sul telefono; non viene caricato online. Il richiamo si attiva già da solo alle aperture successive. Questa correzione è facoltativa. Un profilo va comunque provato in silenzio e con rumore per valutarne l’affidabilità.");page.addView(help);
        status=new TextView(this);page.addView(status);record=new Button(this);record.setText("Registra esempio 1 di 3");page.addView(record);
        record.setOnClickListener(v->{
            if(capturing)return;
            if(checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},91);return;}
            if(PetService.voiceOutputBusy()){status.setText("Aspetta che Solphivia finisca di parlare, poi riprova.");return;}
            stopService(new Intent(this,VoiceSessionService.class));
            capturing=true;record.setEnabled(false);status.setText("Preparazione del rilevatore…");new Thread(this::capture,"solphivia-wake-calibration").start();
        });ScreenInsets.apply(this,page);setContentView(page);
    }
    private void capture(){Model model=null;Recognizer recognizer=null;AudioRecord recorder=null;
        String acquired=null,failure=null;
        try{
            model=LocalWakeDetector.model(this);recognizer=new Recognizer(model,16000);
            StringBuilder phrases=new StringBuilder();
            int size=Math.max(6400,AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT));
            if(checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microfono non autorizzato");
            recorder=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);input=recorder;
            if(!capturing)return;
            if(recorder.getState()!=AudioRecord.STATE_INITIALIZED)throw new IllegalStateException("Microfono non disponibile");
            recorder.startRecording();main.post(()->{if(!closed&&capturing)status.setText("Adesso dì: Hola Solphivia. Ti ascolto per 4 secondi.");});
            short[] pcm=new short[1600];int total=0;
            while(capturing&&total<64000){
                int n=recorder.read(pcm,0,pcm.length);
                if(n<0)throw new IllegalStateException("Microfono non disponibile");
                if(n==0)continue;
                total+=n;
                if(recognizer.acceptWaveForm(pcm,n))phrases.append(' ').append(LocalWakeDetector.text(recognizer.getResult()));
            }
            if(!capturing)return;
            phrases.append(' ').append(LocalWakeDetector.text(recognizer.getFinalResult()));
            String phrase=VoicePhrases.normalized(phrases.toString());
            int words=phrase.isEmpty()?0:phrase.split(" ").length;
            if(words<3||words>6)throw new IllegalStateException("Ripeti il richiamo completo.");
            acquired=phrase;
        }catch(Throwable error){failure="Calibrazione non riuscita. Controlla il microfono e riprova.";}
        finally{capturing=false;input=null;if(recorder!=null){try{recorder.stop();}catch(RuntimeException ignored){}recorder.release();}if(recognizer!=null)recognizer.close();if(model!=null)model.close();main.post(()->{if(!closed)record.setEnabled(true);});}
        // Only enable another recording/start the service after releasing the microphone.
        String keyword=acquired,message=failure;
        main.post(()->{if(closed||isFinishing())return;record.setEnabled(true);
            if(keyword==null){status.setText(message==null?"Registrazione interrotta. Riprova.":message);return;}
            examples.add(keyword);
            if(examples.size()<3){record.setText("Registra esempio "+(examples.size()+1)+" di 3");status.setText("Esempio acquisito. Ripeti con la stessa voce naturale.");}
            else{getSharedPreferences("pet",0).edit().putString("wake_italian_phrases",String.join("\n",new java.util.LinkedHashSet<>(examples))).putBoolean("wake_on_open",true).apply();startForegroundService(new Intent(this,VoiceSessionService.class).setAction(VoiceSessionService.WAKE));finish();}
        });
    }
    @Override protected void onPause(){capturing=false;AudioRecord recorder=input;if(recorder!=null)try{recorder.stop();}catch(RuntimeException ignored){}super.onPause();}
    @Override protected void onResume(){super.onResume();if(record!=null&&!capturing)record.setEnabled(true);}
    @Override protected void onDestroy(){closed=true;main.removeCallbacksAndMessages(null);super.onDestroy();}
}
