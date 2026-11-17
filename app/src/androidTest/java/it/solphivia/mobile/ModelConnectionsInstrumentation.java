package it.solphivia.mobile;

import android.app.*;
import android.os.Bundle;
import android.content.Context;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Synthetic adapter checks only. No microphone, cloud keys or real provider calls. */
public final class ModelConnectionsInstrumentation extends Instrumentation {
    static final String MEMORY="SYNTHETIC_VAULT_DO_NOT_SEND_WITH_VOICE";
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    static class Reply implements OpenAiChatClient.ChatCallback {
        final CountDownLatch done=new CountDownLatch(1);volatile String text,error;
        public void onSuccess(String t,String a,String m,String mode){text=t;done.countDown();}
        public void onError(String e){error=e;done.countDown();}
        void await()throws Exception{check(done.await(12,TimeUnit.SECONDS),"adapter callback timeout");}
    }
    static class Capture implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
        final CompletableFuture<String> captured=new CompletableFuture<>();
        final CountDownLatch release=new CountDownLatch(1);
        Capture(boolean delayed)throws Exception{
            server.setSoTimeout(10000);
            new Thread(()->{
                try(Socket socket=server.accept()) {
                    socket.setSoTimeout(5000);InputStream in=socket.getInputStream();ByteArrayOutputStream h=new ByteArrayOutputStream();String header="";int n;
                    while((n=in.read())!=-1){h.write(n);header=h.toString("US-ASCII");if(header.endsWith("\r\n\r\n"))break;}
                    int length=0;for(String line:header.split("\r\n"))if(line.toLowerCase().startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                    byte[] body=new byte[length];int at=0;while(at<length&&(n=in.read(body,at,length-at))>0)at+=n;
                    captured.complete(new String(body,0,at,StandardCharsets.UTF_8));
                    if(delayed)release.await(8,TimeUnit.SECONDS);
                    byte[] response="{\"choices\":[{\"message\":{\"content\":\"Risposta locale [[SOLPHIVIA:nod]] [[SOLPHIVIA:stay]] [[SOLPHIVIA:mode-prof]]\"}}]}".getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: "+response.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));socket.getOutputStream().write(response);
                }catch(Exception e){captured.completeExceptionally(e);}
            },"synthetic-model-server").start();
        }
        String base(){return "http://127.0.0.1:"+server.getLocalPort();}
        public void close()throws IOException{release.countDown();server.close();}
    }
    static boolean click(View v,String label){
        if(v instanceof Button&&label.equals(((Button)v).getText().toString()))return v.performClick();
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(click(((ViewGroup)v).getChildAt(i),label))return true;
        return false;
    }
    @Override public void onStart(){
        Bundle result=new Bundle();StringBuilder report=new StringBuilder();Context c=getTargetContext();ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            check(c.getPackageName().endsWith(".verification"),"isolated installation required");
            DemoAccess.disable(c);ModelConnection.clear(c);
            check(!OpenAiChatClient.isConnected(c),"test must not have a personal ChatGPT account");
            c.getSharedPreferences("chat",0).edit().putString("history","[{\"role\":\"user\",\"content\":\"OLD_PROVIDER_PRIVATE\"}]").commit();
            ModelConnection.save(c,ModelEndpoint.Kind.GEMINI,"","synthetic-model","SYNTHETIC-API-KEY",false);
            ModelConnection profile=ModelConnection.read(c);
            check(profile.key.equals("SYNTHETIC-API-KEY"),"encrypted profile round trip");
            check(!c.getSharedPreferences("model_connection",0).getAll().toString().contains("SYNTHETIC-API-KEY"),"no key at rest in plaintext");
            check(!ConversationRoute.historyKey(c).equals("history"),"provider history isolation");
            check(c.getSharedPreferences("chat",0).getString(ConversationRoute.historyKey(c),"[]").equals("[]"),"old conversation not inherited");
            report.append("PASS encrypted profile, no plaintext key, provider history isolated\n");
            AssistantMemory.write(c,MEMORY);
            c.getSharedPreferences("memory",0).edit().putBoolean("enabled",true).commit();
            JSONArray history=new JSONArray().put(new JSONObject().put("role","user").put("content","Messaggio vocale gia trascritto"));
            for(ModelEndpoint.Kind kind:new ModelEndpoint.Kind[]{ModelEndpoint.Kind.GEMINI,ModelEndpoint.Kind.CLAUDE,ModelEndpoint.Kind.OPENAI}) {
                ModelConnection.save(c,kind,"","synthetic-model","SYNTHETIC-API-KEY",false);
                ModelConnection selected=ModelConnection.read(c);
                it.solphivia.mobile.provider.FrozenChatRequest frozen=it.solphivia.mobile.provider.ProviderRequestFactory.freezeLegacy(
                    history,new JSONArray(),it.solphivia.mobile.provider.RequestMode.VOICE,selected.configuration(),"",System.currentTimeMillis(),null);
                JSONObject payload=PersonalModelTransport.payload(selected,frozen);
                check(!payload.toString().contains("SYNTHETIC-API-KEY")&&!payload.toString().contains(MEMORY),"cloud payload cannot embed key or voice vault");
                if(kind==ModelEndpoint.Kind.CLAUDE)check(payload.has("system")&&payload.getInt("max_tokens")==512,"native Claude protocol");
                else check("system".equals(payload.getJSONArray("messages").getJSONObject(0).getString("role")),"shared identity for compatible cloud protocol");
            }
            check("Claude sintetico".equals(PersonalModelTransport.answer(ModelEndpoint.Kind.CLAUDE,new JSONObject("{\"content\":[{\"type\":\"text\",\"text\":\"Claude sintetico\"}]}"))),"Claude text parsing");
            report.append("PASS OpenAI/Gemini/Claude payload formats and common identity; synthetic only, no cloud request\n");
            try(Capture server=new Capture(false)) {
                ModelConnection.save(c,ModelEndpoint.Kind.LM_STUDIO,server.base(),"qwen3:4b","",true);
                Reply reply=new Reply();OpenAiChatClient.respond(c,worker,history,new JSONArray(),true,reply);reply.await();
                check(reply.error==null&&"Risposta locale".equals(reply.text),"voice route through immutable adapter");
                String payload=server.captured.get(2,TimeUnit.SECONDS);
                check(!payload.contains(MEMORY)&&!payload.contains("OLD_PROVIDER_PRIVATE"),"voice excludes vault and other-provider history");
                check(payload.contains("qwen3:4b"),"selected local model");
                report.append("PASS transcribed voice -> local model -> parsed avatar reply, no vault, no browser\n");
            }
            try(Capture server=new Capture(false)) {
                ModelConnection.save(c,ModelEndpoint.Kind.OLLAMA,server.base(),"qwen3:4b","",true);
                Instrumentation.ActivityMonitor consent=addMonitor(MemoryConsentActivity.class.getName(),null,false);
                Reply reply=new Reply();OpenAiChatClient.respond(c,worker,history,new JSONArray(),false,reply);
                Activity dialog=waitForMonitorWithTimeout(consent,7000);removeMonitor(consent);check(dialog!=null,"personal provider memory consent shown");
                check(!server.captured.isDone(),"no request before consent");
                runOnMainSync(()->check(click(dialog.getWindow().getDecorView(),"Invia senza memoria"),"consent button"));
                reply.await();check(reply.error==null&&!server.captured.get().contains(MEMORY),"typed send without vault");
                report.append("PASS typed memory consent gates actual local dispatch\n");
            }
            try(Capture first=new Capture(false);Capture second=new Capture(false)) {
                ModelConnection.save(c,ModelEndpoint.Kind.OLLAMA,first.base(),"qwen3:4b","",true);
                Instrumentation.ActivityMonitor consent=addMonitor(MemoryConsentActivity.class.getName(),null,false);
                Reply reply=new Reply();OpenAiChatClient.respond(c,worker,history,new JSONArray(),false,reply);
                Activity dialog=waitForMonitorWithTimeout(consent,7000);removeMonitor(consent);check(dialog!=null,"second consent shown");
                ModelConnection.save(c,ModelEndpoint.Kind.OLLAMA,second.base(),"qwen3:4b","",true);
                runOnMainSync(()->check(click(dialog.getWindow().getDecorView(),"Invia con questi ricordi"),"consent approval"));
                reply.await();check(reply.error!=null&&!first.captured.isDone()&&!second.captured.isDone(),"changed destination rejected without sending");
                report.append("PASS destination changed after consent -> neither endpoint receives data\n");
            }
            try(Capture server=new Capture(true)) {
                ModelConnection.save(c,ModelEndpoint.Kind.LM_STUDIO,server.base(),"qwen3:4b","",true);
                Reply reply=new Reply();OpenAiChatClient.respond(c,worker,history,new JSONArray(),true,reply);
                server.captured.get(7,TimeUnit.SECONDS);OpenAiChatClient.cancelRequests(worker);
                server.release.countDown();reply.await();check(reply.text==null&&reply.error!=null&&reply.error.contains("annullata"),"cancellation must report cancellation, never a stale success");
                report.append("PASS active network cancellation, no stale callback or provider fallback\n");
            }
            check(!OpenAiChatClient.isAuthorizing()&&!OpenAiChatClient.isConnected(c),"no OAuth or account use");
            result.putString("stream",report.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream",report+"FAIL "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally{OpenAiChatClient.cancelRequests(worker);worker.shutdownNow();ModelConnection.clear(c);DemoAccess.disable(c);}
    }
}
