package it.solphivia.mobile;

import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Interchange with AI-memory-vault encrypted-envelope v1. Never uploads a backup. */
final class VaultBackup {
    static final int MAX_BACKUP_BYTES = 25 * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final OAEPParameterSpec OAEP = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    static byte[] random(int n) { byte[] b=new byte[n]; RANDOM.nextBytes(b); return b; }
    static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    static byte[] bytes(JSONObject o, String key, int min, int max) throws Exception {
        Object raw=o.get(key);
        if (!(raw instanceof String)) throw new IOException("Campo backup non valido");
        String s=(String)raw;
        if(s.length()>((long)max+2)/3*4 || s.length()%4!=0 || !s.matches("[A-Za-z0-9+/]*={0,2}")) throw new IOException("Base64 non valido");
        byte[] b=Base64.getDecoder().decode(s);
        if(b.length<min || b.length>max) throw new IOException("Dimensione backup non valida"); return b;
    }
    static void exact(JSONObject o, String... names) throws Exception {
        Set<String> allowed=new HashSet<>(Arrays.asList(names));
        if(o.length()!=allowed.size()) throw new IOException("Formato backup non supportato");
        Iterator<String> it=o.keys(); while(it.hasNext()) if(!allowed.contains(it.next())) throw new IOException("Campo backup non supportato");
    }
    static void require(boolean value) throws IOException { if(!value) throw new IOException("Formato Memory Vault v1 non valido"); }
    private static SecretKey wrapping(char[] pass, byte[] salt) throws Exception {
        require(pass.length>=12);
        PBEKeySpec spec=new PBEKeySpec(pass,salt,600000,256);
        byte[] key=null;
        try { key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); return new SecretKeySpec(key,"AES"); }
        finally { spec.clearPassword(); if(key!=null) Arrays.fill(key,(byte)0); }
    }
    private static byte[] aes(int mode, SecretKey key, byte[] iv, byte[] value) throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(mode,key,new GCMParameterSpec(128,iv)); return c.doFinal(value);
    }
    private static byte[] rsa(int mode, Key key, byte[] value) throws Exception {
        Cipher c=Cipher.getInstance("RSA/ECB/OAEPPadding"); c.init(mode,key,OAEP); return c.doFinal(value);
    }
    static JSONObject unlock(String text, char[] pass) throws Exception {
        byte[] privateBytes=null, dataBytes=null, plain=null;
        try {
            JSONObject root=new JSONObject(text);
            exact(root,"format","schemaVersion","kdf","keyWrapping","privateKeyEnvelope","payload");
            require("memoria-vault-encrypted".equals(root.getString("format")) && root.getInt("schemaVersion")==1);
            JSONObject kdf=root.getJSONObject("kdf"), wrap=root.getJSONObject("keyWrapping"), priv=root.getJSONObject("privateKeyEnvelope"), payload=root.getJSONObject("payload");
            exact(kdf,"name","hash","iterations","salt"); exact(wrap,"name","hash","publicKey","wrappedDataKey");
            exact(priv,"algorithm","iv","ciphertext"); exact(payload,"algorithm","iv","ciphertext");
            require("PBKDF2".equals(kdf.getString("name")) && "SHA-256".equals(kdf.getString("hash")) && kdf.getInt("iterations")==600000);
            require("RSA-OAEP".equals(wrap.getString("name")) && "SHA-256".equals(wrap.getString("hash")));
            require("AES-GCM".equals(priv.getString("algorithm")) && "AES-GCM".equals(payload.getString("algorithm")));
            byte[] salt=bytes(kdf,"salt",16,16), iv=bytes(priv,"iv",12,12), encryptedPrivate=bytes(priv,"ciphertext",1000,3000);
            byte[] publicBytes=bytes(wrap,"publicKey",300,1024), wrapped=bytes(wrap,"wrappedDataKey",384,384);
            byte[] payloadIv=bytes(payload,"iv",12,12), encryptedPayload=bytes(payload,"ciphertext",16,MAX_BACKUP_BYTES);
            privateBytes=aes(Cipher.DECRYPT_MODE,wrapping(pass,salt),iv,encryptedPrivate);
            KeyFactory factory=KeyFactory.getInstance("RSA");
            PrivateKey privateKey=factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            PublicKey publicKey=factory.generatePublic(new X509EncodedKeySpec(publicBytes));
            require(((java.security.interfaces.RSAKey)privateKey).getModulus().bitLength()==3072);
            require(((java.security.interfaces.RSAKey)privateKey).getModulus().equals(((java.security.interfaces.RSAKey)publicKey).getModulus()));
            dataBytes=rsa(Cipher.DECRYPT_MODE,privateKey,wrapped); require(dataBytes.length==32);
            plain=aes(Cipher.DECRYPT_MODE,new SecretKeySpec(dataBytes,"AES"),payloadIv,encryptedPayload);
            JSONObject snapshot=new JSONObject(new String(plain,StandardCharsets.UTF_8));
            require("memoria-vault".equals(snapshot.getString("format")) && snapshot.getInt("schemaVersion")==1);
            require(snapshot.getJSONObject("profile").getInt("schemaVersion")==1 && snapshot.getJSONArray("memories").length()<=10000);
            return snapshot;
        } finally {
            Arrays.fill(pass,'\0'); if(privateBytes!=null) Arrays.fill(privateBytes,(byte)0);
            if(dataBytes!=null) Arrays.fill(dataBytes,(byte)0); if(plain!=null) Arrays.fill(plain,(byte)0);
        }
    }
    static String seal(JSONObject snapshot, char[] pass) throws Exception {
        byte[] privateBytes=null, dataBytes=null, plain=null;
        try {
            byte[] salt=random(16), privateIv=random(12), payloadIv=random(12);
            SecretKey key=wrapping(pass,salt);
            KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA"); gen.initialize(3072); KeyPair pair=gen.generateKeyPair();
            privateBytes=pair.getPrivate().getEncoded(); dataBytes=random(32); plain=snapshot.toString().getBytes(StandardCharsets.UTF_8);
            return new JSONObject().put("format","memoria-vault-encrypted").put("schemaVersion",1)
                .put("kdf",new JSONObject().put("name","PBKDF2").put("hash","SHA-256").put("iterations",600000).put("salt",b64(salt)))
                .put("keyWrapping",new JSONObject().put("name","RSA-OAEP").put("hash","SHA-256").put("publicKey",b64(pair.getPublic().getEncoded())).put("wrappedDataKey",b64(rsa(Cipher.ENCRYPT_MODE,pair.getPublic(),dataBytes))))
                .put("privateKeyEnvelope",new JSONObject().put("algorithm","AES-GCM").put("iv",b64(privateIv)).put("ciphertext",b64(aes(Cipher.ENCRYPT_MODE,key,privateIv,privateBytes))))
                .put("payload",new JSONObject().put("algorithm","AES-GCM").put("iv",b64(payloadIv)).put("ciphertext",b64(aes(Cipher.ENCRYPT_MODE,new SecretKeySpec(dataBytes,"AES"),payloadIv,plain)))).toString();
        } finally { Arrays.fill(pass,'\0'); if(privateBytes!=null)Arrays.fill(privateBytes,(byte)0); if(dataBytes!=null)Arrays.fill(dataBytes,(byte)0); if(plain!=null)Arrays.fill(plain,(byte)0); }
    }
    /** Import only ordinary, approved, unexpired text; never voice/avatar assets or secrets. */
    static String selectedText(JSONObject snapshot, long now) throws Exception {
        JSONObject profile=snapshot.getJSONObject("profile");
        StringBuilder text=new StringBuilder("# Profilo dal Memory Vault\n");
        JSONObject identity=profile.optJSONObject("identity"), behavior=profile.getJSONObject("behavior");
        if(identity!=null) { append(text,"Nome",identity.optString("name")); append(text,"Identità",identity.optString("description")); }
        append(text,"Lingua",profile.getString("language")); append(text,"Tono",behavior.optString("tone"));
        append(text,"Lunghezza risposte",behavior.optString("responseLength"));
        for(String field: new String[]{"rules","boundaries"}) {
            JSONArray list=behavior.getJSONArray(field); require(list.length()<=100);
            for(int i=0;i<list.length();i++) append(text,field.equals("rules")?"Preferenza":"Limite",list.getString(i));
        }
        // Descriptive voice fields stay visible in the editor; changing a device engine remains explicit.
        JSONObject voice=profile.optJSONObject("voice");
        if(voice!=null) { append(text,"Timbro desiderato",voice.optString("timbreDescription")); append(text,"Accento desiderato",voice.optString("accentDescription")); }
        JSONArray memories=snapshot.getJSONArray("memories"); require(memories.length()<=10000);
        for(int i=0;i<memories.length();i++) {
            JSONObject memory=memories.getJSONObject(i);
            if(!"approved".equals(memory.optString("status")) || !"ordinary".equals(memory.optString("sensitivity"))) continue;
            String expires=memory.optString("expiresAt","");
            if(!expires.isEmpty()) { try { if(java.time.Instant.parse(expires).toEpochMilli()<=now)continue; } catch(Exception e){continue;} }
            require(memory.getInt("schemaVersion")==1);
            String title=memory.getString("title"), content=memory.getString("content"); require(title.length()<=120 && content.length()<=12000);
            text.append("\n## ").append(title).append('\n').append(content).append('\n');
            AssistantMemory.validate(text.toString());
        }
        AssistantMemory.validate(text.toString()); return text.toString();
    }
    private static void append(StringBuilder text,String name,String value) throws IOException { require(value.length()<=2000); if(!value.isEmpty())text.append(name).append(": ").append(value).append('\n'); }
    static JSONObject snapshot(String text, boolean readAloud, boolean english) throws Exception {
        AssistantMemory.validate(text); String now=java.time.Instant.now().toString(); JSONArray records=new JSONArray();
        for(int start=0;start<text.length();) {
            int end=Math.min(start+12000,text.length()); if(end<text.length() && Character.isLowSurrogate(text.charAt(end)))end--;
            records.put(new JSONObject().put("schemaVersion",1).put("id",UUID.randomUUID().toString()).put("kind","note")
                .put("category","Solphivia Mobile").put("title","Memoria Solphivia " + (records.length()+1)).put("content",text.substring(start,end))
                .put("tags",new JSONArray()).put("status","approved").put("sensitivity","ordinary")
                .put("source",new JSONObject().put("type","user-authored").put("label","Editor Solphivia Mobile").put("capturedAt",now))
                .put("createdAt",now).put("updatedAt",now)); start=end;
        }
        JSONObject profile=new JSONObject().put("schemaVersion",1).put("language","it-IT")
            .put("identity",new JSONObject().put("name","Solphivia"))
            .put("behavior",new JSONObject().put("rules",new JSONArray()).put("boundaries",new JSONArray()))
            .put("voice",new JSONObject().put("readAloud",readAloud).put("language","it-IT").put("pace","natural")
                .put("accentDescription",english?"Italiano con accento inglese":"Italiano"));
        return new JSONObject().put("format","memoria-vault").put("schemaVersion",1).put("vaultId",UUID.randomUUID().toString()).put("exportedAt",now).put("profile",profile).put("memories",records);
    }
    static String readFile(InputStream in) throws IOException {
        if(in==null)throw new IOException("File non accessibile");
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[8192]; int n;
        while((n=in.read(b))!=-1){if(out.size()+n>MAX_BACKUP_BYTES)throw new IOException("Backup oltre 25 MB");out.write(b,0,n);}
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString();
    }
}
