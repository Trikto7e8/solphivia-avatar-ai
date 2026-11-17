package it.solphivia.mobile;

import android.content.Context;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;
import it.solphivia.mobile.provider.*;

/** One atomically sealed profile. No automatic credential reuse between origins. */
final class ModelConnection {
    final ModelEndpoint.Kind kind;
    final String endpoint, model, key, revision;
    private ModelConnection(ModelEndpoint.Kind k,String e,String m,String secret,String id){kind=k;endpoint=e;model=m;key=secret;revision=id;}
    static boolean active(Context c){return c.getSharedPreferences("model_connection",0).contains("profile");}
    static void clear(Context c){c.getSharedPreferences("model_connection",0).edit().remove("profile").commit();}
    static void save(Context c,ModelEndpoint.Kind kind,String base,String model,String secret,boolean allowLanHttp)throws Exception {
        ModelEndpoint.validateModel(model);
        if(secret.length()>512 || secret.contains("\r") || secret.contains("\n"))throw new IllegalArgumentException("Chiave API non valida.");
        if(!ModelEndpoint.local(kind)&&secret.isEmpty())throw new IllegalArgumentException("Inserisci una chiave API personale; non la password della chat.");
        String endpoint=ModelEndpoint.endpoint(kind,base,allowLanHttp,!secret.isEmpty());
        String id=UUID.randomUUID().toString();
        JSONObject profile=new JSONObject().put("kind",kind.name()).put("endpoint",endpoint).put("model",model).put("key",secret).put("revision",id);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,localKey());
        byte[] encrypted=cipher.doFinal(profile.toString().getBytes(StandardCharsets.UTF_8));
        byte[] packed=new byte[12+encrypted.length];System.arraycopy(cipher.getIV(),0,packed,0,12);System.arraycopy(encrypted,0,packed,12,encrypted.length);
        if(!c.getSharedPreferences("model_connection",0).edit().putString("profile",Base64.encodeToString(packed,Base64.NO_WRAP)).commit())throw new IllegalStateException("Collegamento non salvato.");
    }
    static ModelConnection read(Context c)throws Exception {
        try {
            byte[] packed=Base64.decode(c.getSharedPreferences("model_connection",0).getString("profile",""),Base64.NO_WRAP);
            if(packed.length<29||packed.length>8192)throw new IllegalStateException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,localKey(),new GCMParameterSpec(128,packed,0,12));
            JSONObject p=new JSONObject(new String(cipher.doFinal(packed,12,packed.length-12),StandardCharsets.UTF_8));
            return new ModelConnection(ModelEndpoint.Kind.valueOf(p.getString("kind")),p.getString("endpoint"),p.getString("model"),p.getString("key"),p.getString("revision"));
        }catch(Exception e){throw new IllegalStateException("Riconfigura il collegamento IA: le credenziali locali non sono disponibili.");}
    }
    ProviderConfiguration configuration() {
        ProviderId id=kind==ModelEndpoint.Kind.OLLAMA?ProviderId.OLLAMA:kind==ModelEndpoint.Kind.LM_STUDIO?ProviderId.LM_STUDIO:ProviderId.CUSTOM;
        return new ProviderConfiguration(new ProviderIdentity(id,ModelEndpoint.label(kind)),endpoint,model,revision,10000,45000);
    }
    boolean matches(ProviderConfiguration frozen){return configuration().stableFingerprint().equals(frozen.stableFingerprint());}
    @Override public String toString(){return "ModelConnection("+kind+", [credentials hidden])";}
    private static synchronized SecretKey localKey()throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        String alias="solphivia-personal-provider-v1";
        if(store.containsAlias(alias))return ((KeyStore.SecretKeyEntry)store.getEntry(alias,null)).getSecretKey();
        KeyGenerator gen=KeyGenerator.getInstance("AES","AndroidKeyStore");
        gen.init(new android.security.keystore.KeyGenParameterSpec.Builder(alias,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return gen.generateKey();
    }
}
