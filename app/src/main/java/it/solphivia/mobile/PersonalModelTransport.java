package it.solphivia.mobile;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import it.solphivia.mobile.provider.*;

/** Terminal-only bounded transport, reusing the immutable request/consent boundary. */
final class PersonalModelTransport {
    private static final java.util.Timer DEADLINES=new java.util.Timer("personal-model-deadlines",true);
    static String call(Context context,FrozenChatRequest request,RequestCancellation cancel)throws Exception {
        if(DemoAccess.enabled(context)||!ModelConnection.active(context))throw new IllegalStateException("Il collegamento è cambiato. Invia di nuovo.");
        ModelConnection profile=ModelConnection.read(context);
        if(!profile.matches(request.getConfiguration()))throw new IllegalStateException("Il collegamento è cambiato. Conferma di nuovo l’invio.");
        JSONObject payload=payload(profile,request);
        byte[] bytes=payload.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>ModelEndpoint.MAX_INPUT_BYTES)throw new IllegalArgumentException("Messaggio e memoria oltre il limite del collegamento.");
        if(cancel.isCancelled())throw new InterruptedIOException();
        String body;
        if(profile.endpoint.startsWith("http://"))body=localHttp(profile,bytes,cancel);
        else body=https(profile,bytes,cancel);
        return answer(profile.kind,new JSONObject(body));
    }
    static JSONObject payload(ModelConnection profile,FrozenChatRequest request)throws Exception {
        JSONArray messages=new JSONArray();
        for(ConversationMessage m:request.getMessages()) {
            if(!m.getAttachments().isEmpty())throw new IllegalArgumentException("Questo collegamento supporta testo e voce, non allegati.");
            messages.put(new JSONObject().put("role",m.getRole()).put("content",m.getContent()));
        }
        String persona=OpenAiChatClient.instructionsFor(request.getApprovedMemory());
        JSONObject data=new JSONObject().put("model",profile.model).put("stream",false);
        if(profile.kind==ModelEndpoint.Kind.CLAUDE)data.put("system",persona).put("messages",messages).put("max_tokens",512);
        else {
            JSONArray all=new JSONArray().put(new JSONObject().put("role","system").put("content",persona));
            for(int i=0;i<messages.length();i++)all.put(messages.get(i));
            data.put("messages",all);
            if(profile.kind==ModelEndpoint.Kind.OPENAI) {
                data.put("max_completion_tokens",1024);
                if(profile.model.startsWith("gpt-5"))data.put("reasoning_effort","minimal");
                data.put("store",false);
            } else data.put("max_tokens",512);
        }
        ModelEndpoint.checkInput(data.toString());return data;
    }
    static String answer(ModelEndpoint.Kind kind,JSONObject data)throws Exception {
        String text="";
        if(kind==ModelEndpoint.Kind.CLAUDE) {
            StringBuilder b=new StringBuilder();JSONArray content=data.optJSONArray("content");
            if(content!=null)for(int i=0;i<content.length();i++){JSONObject part=content.optJSONObject(i);if(part!=null&&"text".equals(part.optString("type")))b.append(part.optString("text"));}
            text=b.toString();
        }else {
            JSONArray choices=data.optJSONArray("choices");JSONObject choice=choices==null?null:choices.optJSONObject(0);
            JSONObject message=choice==null?null:choice.optJSONObject("message");
            if(message!=null)text=message.optString("content","");
        }
        if(text.trim().isEmpty()||text.length()>ModelEndpoint.MAX_REPLY_CHARS)throw new IllegalStateException("Risposta del modello vuota o oltre il limite.");
        return text;
    }
    private static String https(ModelConnection p,byte[] payload,RequestCancellation cancel)throws Exception {
        javax.net.ssl.HttpsURLConnection conn=(javax.net.ssl.HttpsURLConnection)new URL(p.endpoint).openConnection();
        cancel.onCancel(()->{conn.disconnect();return kotlin.Unit.INSTANCE;});
        java.util.TimerTask deadline=new java.util.TimerTask(){public void run(){conn.disconnect();}};
        DEADLINES.schedule(deadline,45000);
        try {
            conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(10000);conn.setReadTimeout(45000);
            conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setFixedLengthStreamingMode(payload.length);
            conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
            if(p.kind==ModelEndpoint.Kind.CLAUDE){conn.setRequestProperty("x-api-key",p.key);conn.setRequestProperty("anthropic-version","2023-06-01");}
            else if(!p.key.isEmpty())conn.setRequestProperty("Authorization","Bearer "+p.key);
            if(cancel.isCancelled())throw new InterruptedIOException();
            try(OutputStream out=conn.getOutputStream()){out.write(payload);}
            int status=conn.getResponseCode();
            if(status!=200)throw statusError(status);
            try(InputStream in=conn.getInputStream()){return readBounded(in,cancel);}
        }finally{deadline.cancel();conn.disconnect();}
    }
    /** Dedicated opt-in LAN socket. Does not enable cleartext traffic globally or send credentials. */
    private static String localHttp(ModelConnection p,byte[] payload,RequestCancellation cancel)throws Exception {
        URI uri=URI.create(p.endpoint);
        if(!ModelEndpoint.local(p.kind)||!ModelEndpoint.privateIpv4(uri.getHost())||!p.key.isEmpty())throw new IllegalArgumentException("Connessione locale HTTP non autorizzata.");
        byte[] ip=new byte[4];String[] parts=uri.getHost().split("\\.");for(int i=0;i<4;i++)ip[i]=(byte)Integer.parseInt(parts[i]);
        Socket socket=new Socket();
        cancel.onCancel(()->{try{socket.close();}catch(IOException ignored){}return kotlin.Unit.INSTANCE;});
        java.util.TimerTask deadline=new java.util.TimerTask(){public void run(){try{socket.close();}catch(IOException ignored){}}};
        DEADLINES.schedule(deadline,45000);
        try {
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(ip),uri.getPort()<0?80:uri.getPort()),10000);
            socket.setSoTimeout(45000);
            if(cancel.isCancelled())throw new InterruptedIOException();
            String header="POST "+uri.getRawPath()+" HTTP/1.1\r\nHost: "+uri.getRawAuthority()+"\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: "+payload.length+"\r\nConnection: close\r\n\r\n";
            OutputStream out=socket.getOutputStream();out.write(header.getBytes(StandardCharsets.US_ASCII));out.write(payload);out.flush();
            InputStream in=new BufferedInputStream(socket.getInputStream());
            String status=line(in,2048);if(!status.matches("HTTP/1\\.[01] 200(?: .*)?"))throw new IllegalStateException("Il server locale ha rifiutato la richiesta. Controlla indirizzo e modello.");
            int total=0,length=-1;boolean chunked=false;
            while(true) {
                String h=line(in,4096);total+=h.length();if(total>8192)throw new IOException("Intestazioni troppo grandi.");if(h.isEmpty())break;
                if(h.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:")){if(length!=-1)throw new IOException("Lunghezza duplicata.");length=Integer.parseInt(h.substring(15).trim());if(length<0||length>ModelEndpoint.MAX_RESPONSE_BYTES)throw new IOException("Risposta troppo grande.");}
                if(h.toLowerCase(java.util.Locale.ROOT).startsWith("transfer-encoding:")){if(!h.substring(18).trim().equalsIgnoreCase("chunked"))throw new IOException("Codifica non supportata.");chunked=true;}
            }
            ByteArrayOutputStream body=new ByteArrayOutputStream();
            if(chunked) {
                if(length!=-1)throw new IOException("Risposta HTTP ambigua.");
                for(int chunks=0;;chunks++) {
                    if(chunks>4096||cancel.isCancelled())throw new IOException("Risposta locale interrotta.");
                    String size=line(in,80).split(";",2)[0];int n=Integer.parseInt(size,16);
                    if(n<0||body.size()+n>ModelEndpoint.MAX_RESPONSE_BYTES)throw new IOException("Risposta troppo grande.");
                    if(n==0)break;copy(in,body,n);if(!line(in,2).isEmpty())throw new IOException("Frame HTTP non valido.");
                }
            } else if(length>=0)copy(in,body,length);
            else return readBounded(in,cancel);
            return body.toString("UTF-8");
        }finally{deadline.cancel();socket.close();}
    }
    private static String line(InputStream in,int max)throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1){if(c=='\n'){byte[] v=b.toByteArray();if(v.length==0||v[v.length-1]!='\r')throw new IOException("Intestazione non valida.");return new String(v,0,v.length-1,StandardCharsets.US_ASCII);}if(b.size()>=max)throw new IOException("Intestazione troppo grande.");b.write(c);}
        throw new EOFException();
    }
    private static void copy(InputStream in,OutputStream out,int length)throws IOException {
        byte[] b=new byte[2048];while(length>0){int n=in.read(b,0,Math.min(length,b.length));if(n<0)throw new EOFException();out.write(b,0,n);length-=n;}
    }
    private static String readBounded(InputStream in,RequestCancellation cancel)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;
        while((n=in.read(b))!=-1){if(cancel.isCancelled())throw new InterruptedIOException();if(out.size()+n>ModelEndpoint.MAX_RESPONSE_BYTES)throw new IOException("Risposta oltre il limite.");out.write(b,0,n);}
        return out.toString("UTF-8");
    }
    private static Exception statusError(int status){return new IllegalStateException(status==401||status==403?"Accesso API rifiutato. Controlla la chiave personale.":status==429?"Il tuo fornitore ha raggiunto il limite di utilizzo.":"Il fornitore non ha completato la richiesta (HTTP "+status+"). Nessun altro servizio è stato usato.");}
    private PersonalModelTransport() {}
}
