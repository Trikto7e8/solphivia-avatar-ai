package it.solphivia.mobile;

import android.content.Context;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

final class DemoAccess {
    private static final String ALIAS = "solphivia-demo-invite-v1";

    static boolean enabled(Context context) {
        return context.getSharedPreferences("demo", 0).getBoolean("enabled", false);
    }

    static void disable(Context context) {
        context.getSharedPreferences("demo", 0).edit().remove("invite").putBoolean("enabled", false).commit();
    }

    static void activate(Context context, String code) throws Exception {
        DemoProtocol.origin(BuildConfig.DEMO_BASE_URL);
        if (!code.matches("[A-Za-z0-9_-]{1,128}")) throw new IllegalArgumentException("Controlla il codice d’invito.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
        byte[] packed = new byte[12 + encrypted.length];
        System.arraycopy(cipher.getIV(), 0, packed, 0, 12);
        System.arraycopy(encrypted, 0, packed, 12, encrypted.length);
        if (!context.getSharedPreferences("demo", 0).edit().putString("invite", Base64.encodeToString(packed, Base64.NO_WRAP)).putBoolean("enabled", true).putString("conversation", java.util.UUID.randomUUID().toString()).commit()) {
            throw new IllegalStateException("Non riesco a salvare il codice sul telefono.");
        }
        ModelConnection.clear(context);
    }

    static String invite(Context context) throws Exception {
        byte[] packed = Base64.decode(context.getSharedPreferences("demo", 0).getString("invite", ""), Base64.NO_WRAP);
        if (packed.length < 29) throw new IllegalStateException("Configura il codice in Prova senza account.");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, packed, 0, 12));
        return new String(cipher.doFinal(packed, 12, packed.length - 12), StandardCharsets.UTF_8);
    }

    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
