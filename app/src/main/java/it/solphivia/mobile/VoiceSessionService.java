package it.solphivia.mobile;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.speech.*;
import java.util.ArrayList;

/** Local wake listening; Android speech recognition runs only for a requested command. */
public final class VoiceSessionService extends Service implements RecognitionListener {
    static final String START="it.solphivia.voice.START", WAKE="it.solphivia.voice.WAKE", PAUSE="it.solphivia.voice.PAUSE", OFF="it.solphivia.voice.OFF";
    private static VoiceSessionService instance;
    private static String statusText="Microfono spento.";
    static String statusText(){return statusText;}
    private final Handler main=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private LocalWakeDetector localWake;
    private final VoiceStatusState voiceStatus = new VoiceStatusState();
    private long wakeGeneration;
    private int commandStarts;
    private boolean listening,stopped,foregroundReady;
    private final VoiceSessionState session=new VoiceSessionState();
    private long nextListen;
    private long recognitionStarted;
    static boolean running(){return instance!=null;}
    static boolean ready(){return instance!=null&&instance.foregroundReady&&!instance.stopped;}
    static boolean backgroundRestricted(android.content.Context context){
        return Build.VERSION.SDK_INT>=28&&context.getSystemService(ActivityManager.class).isBackgroundRestricted();
    }
    @Override public void onCreate(){super.onCreate();instance=this;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        boolean recovery=intent==null;
        if((recovery&&!getSharedPreferences("pet",0).getBoolean("voice_requested",false)) || (intent!=null&&OFF.equals(intent.getAction()))){stopSelf();return START_NOT_STICKY;}
        if(backgroundRestricted(this)){statusText="Ascolto interrotto: Android limita Solphivia in background. Consenti l’uso in background nelle impostazioni batteria dell’app.";stopSelf();return START_NOT_STICKY;}
        if(checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
        if(!SpeechRecognizer.isRecognitionAvailable(this)){notifyState("Riconoscimento vocale non disponibile");stopSelf();return START_NOT_STICKY;}
        session.paused=recovery||PAUSE.equals(intent.getAction()) || WAKE.equals(intent.getAction());
        getSharedPreferences("pet",0).edit().putBoolean("voice_requested",true).apply();
        if(listening&&session.paused){listening=false;recognizer.cancel();PetService.voiceListening(false);}
        try{notifyState(session.paused?"Richiamo attivo: Hola Solphivia":"Sessione vocale attiva");}
        catch(RuntimeException error){statusText="Ascolto interrotto: riapri Solphivia per autorizzare il microfono.";getSharedPreferences("pet",0).edit().putBoolean("voice_requested",false).commit();stopSelf();return START_NOT_STICKY;}
        if(recognizer==null){recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(this);}
        main.removeCallbacks(tick);main.post(tick);
        MainActivity.voiceServiceReady();
        if(recovery){
            android.content.SharedPreferences health=getSharedPreferences("voice_health",0);
            health.edit().putInt("system_recoveries",health.getInt("system_recoveries",0)+1).apply();
            if(!getSharedPreferences("pet",0).getBoolean("overlay_off",false))startForegroundService(new Intent(this,PetService.class));
        }
        return START_STICKY;
    }
    private void closeWake(){wakeGeneration++;if(localWake!=null){localWake.close();localWake=null;}}
    private void deliver(String text){if(!MainActivity.acceptVoiceMessage(text))startForegroundService(new Intent(this,PetService.class).putExtra(PetService.EXTRA_SEND_MESSAGE,text).putExtra("voice_message",true));}
    private void startWake(){
        long generation=++wakeGeneration;
        localWake=new LocalWakeDetector(this,new LocalWakeDetector.Listener(){
            public void wake(){main.post(()->{if(stopped || !session.paused || wakeGeneration!=generation)return;closeWake();session.paused=false;nextListen=SystemClock.elapsedRealtime()+1000;notifyState("Saluto riconosciuto · ascolto il comando dopo la risposta");deliver("Hola Solphivia");});}
            public void failed(String reason){main.post(()->{if(!stopped&&wakeGeneration==generation){statusText="Richiamo locale non disponibile: "+reason;stopSelf();}});}
            public void samples(long count){getSharedPreferences("voice_health",0).edit().putLong("local_samples",count).putInt("command_starts",commandStarts).apply();}
        });localWake.start();
    }
    private final Runnable tick=new Runnable(){public void run(){
        if(stopped)return;
        long now=SystemClock.elapsedRealtime();
        if(listening && now-recognitionStarted>60000L){listening=false;recognizer.cancel();session.paused=true;nextListen=now+1000;}
        if(PetService.voiceOutputBusy()){
            if(localWake!=null)localWake.suspend(true);
            if(listening){listening=false;recognizer.cancel();session.paused=true;}
            nextListen=now+650;
        }else if(now>=nextListen){
            if(session.paused){if(!listening && localWake==null){notifyState("In ascolto locale · Hola Solphivia");startWake();}else if(localWake!=null)localWake.suspend(false);}
            else if(!listening){
                closeWake();listening=true;voiceStatus.captureStarted();notifyState(voiceStatus.label());recognitionStarted=now;commandStarts++;
                getSharedPreferences("voice_health",0).edit().putInt("command_starts",commandStarts).apply();
                try{recognizer.startListening(VoicePhrases.recognitionIntent());}
                catch(RuntimeException error){listening=false;voiceStatus.idle();session.paused=true;nextListen=now+1500;notifyState("Comando non disponibile · torno al richiamo locale");}
            }
        }
        main.postDelayed(this,250);
    }};
    private void notifyState(String state){
        statusText=state;
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("voice_session","Microfono Solphivia",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,20,new Intent(this,VoiceSessionActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pause=PendingIntent.getService(this,21,new Intent(this,VoiceSessionService.class).setAction(PAUSE),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent off=PendingIntent.getService(this,22,new Intent(this,VoiceSessionService.class).setAction(OFF),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note=new Notification.Builder(this,"voice_session").setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Microfono Solphivia attivo").setContentText(state).setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null,"Stop dialogo",pause).build())
                .addAction(new Notification.Action.Builder(null,"Spegni microfono",off).build()).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(8,note,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(8,note);
        foregroundReady=true;
    }
    @Override public void onResults(Bundle bundle){
        if(stopped || !listening)return;
        listening=false;voiceStatus.transcriptionStarted();PetService.voiceListening(false);nextListen=SystemClock.elapsedRealtime()+600;
        ArrayList<String> matches=bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        VoiceSessionState.Decision decision=session.accept(VoicePhrases.best(matches));
        if(decision.action==VoiceSessionState.OFF){stopSelf();return;}
        if(decision.action==VoiceSessionState.PAUSE){voiceStatus.idle();notifyState("Richiamo attivo: Hola Solphivia");return;}
        if(decision.action==VoiceSessionState.IGNORE){voiceStatus.idle();session.paused=true;return;}
        session.paused=!VoicePhrases.greeting(decision.text);
        voiceStatus.generationStarted();notifyState(voiceStatus.label());
        deliver(decision.text);
        nextListen=SystemClock.elapsedRealtime()+1000;
    }
    @Override public void onError(int error){
        if(stopped || !listening)return;
        listening=false;voiceStatus.error();
        PetService.voiceListening(false);
        if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){stopSelf();return;}
        session.paused=true;nextListen=SystemClock.elapsedRealtime()+1000;
        getSharedPreferences("voice_health",0).edit().putInt("command_error",error).apply();
        notifyState("Comando terminato · torno al richiamo locale");
    }

    @Override public void onReadyForSpeech(Bundle params){voiceStatus.captureStarted();PetService.voiceListening(true);} @Override public void onBeginningOfSpeech(){} @Override public void onRmsChanged(float rms){}
    @Override public void onBufferReceived(byte[] bytes){} @Override public void onEndOfSpeech(){} @Override public void onPartialResults(Bundle partial){} @Override public void onEvent(int type,Bundle params){}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){getSharedPreferences("pet",0).edit().putBoolean("voice_requested",false).commit();stopped=true;listening=false;closeWake();main.removeCallbacksAndMessages(null);if(recognizer!=null){recognizer.cancel();recognizer.destroy();recognizer=null;}if(instance==this)instance=null;if(!statusText.startsWith("Ascolto interrotto") && !statusText.startsWith("Richiamo locale non disponibile"))statusText="Microfono spento.";PetService.voiceListening(false);stopForeground(true);super.onDestroy();}
}
