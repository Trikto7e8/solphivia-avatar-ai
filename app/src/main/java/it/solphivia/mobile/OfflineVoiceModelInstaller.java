package it.solphivia.mobile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.StatFs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads the Italian Vosk model once, only over Wi-Fi, and installs it atomically. */
final class OfflineVoiceModelInstaller {
    static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-it-0.22.zip";
    private static final String MODEL_ROOT = "vosk-model-small-it-0.22";
    private static final long MAX_ARCHIVE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_EXPANDED_BYTES = 512L * 1024 * 1024;

    interface Progress { void onProgress(long downloaded, long total); }

    private OfflineVoiceModelInstaller() { }

    static File modelDirectory(Context context) {
        return new File(context.getFilesDir(), "voice-model-it-0.22");
    }

    static boolean isInstalled(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        File acoustic = new File(dir, "am/final.mdl");
        File config = new File(dir, "conf/model.conf");
        File graph = new File(dir, "graph");
        return acoustic.isFile() && config.isFile() && graph.isDirectory()
                && (new File(graph, "HCLG.fst").isFile() || new File(graph, "HCLr.fst").isFile());
    }

    static boolean isWifiConnected(Context context) {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network active = manager.getActiveNetwork();
        NetworkCapabilities caps = active == null ? null : manager.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    static File downloadAndInstall(Context context, Progress progress) throws IOException {
        if (!isWifiConnected(context)) throw new IOException("Connetti il Wi-Fi per scaricare il modello vocale.");
        File base = context.getFilesDir();
        File archive = new File(base, "voice-model-it-0.22.zip.part");
        File unpacked = new File(base, "voice-model-it-0.22.tmp");
        File installed = modelDirectory(context);
        deleteRecursively(archive);
        deleteRecursively(unpacked);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(MODEL_URL).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/zip");
            connection.connect();
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new IOException("Download del modello non riuscito (HTTP " + code + ").");
            long total = connection.getContentLengthLong();
            if (total > MAX_ARCHIVE_BYTES) throw new IOException("Archivio del modello più grande del previsto.");
            long needed = Math.max(160L * 1024 * 1024, total > 0 ? total * 3 : 160L * 1024 * 1024);
            if (new StatFs(base.getAbsolutePath()).getAvailableBytes() < needed)
                throw new IOException("Spazio libero insufficiente per installare il modello vocale.");
            copyArchive(connection.getInputStream(), archive, total, progress);
        } finally {
            if (connection != null) connection.disconnect();
        }

        if (archive.length() == 0 || archive.length() > MAX_ARCHIVE_BYTES)
            throw new IOException("Download del modello incompleto.");
        unpack(archive, unpacked);
        if (!isInstalled(unpacked)) throw new IOException("Il modello scaricato non contiene i file vocali previsti.");
        deleteRecursively(installed);
        if (!unpacked.renameTo(installed)) throw new IOException("Impossibile installare il modello vocale.");
        deleteRecursively(archive);
        return installed;
    }

    private static void copyArchive(InputStream source, File target, long total, Progress progress) throws IOException {
        long written = 0;
        try (InputStream in = new BufferedInputStream(source);
             FileOutputStream file = new FileOutputStream(target);
             BufferedOutputStream out = new BufferedOutputStream(file)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                written += read;
                if (written > MAX_ARCHIVE_BYTES || (total > 0 && written > total))
                    throw new IOException("Dimensione del modello inattesa.");
                out.write(buffer, 0, read);
                if (progress != null) progress.onProgress(written, total);
            }
            out.flush();
            file.getFD().sync();
        }
        if (total > 0 && written != total) throw new IOException("Download del modello incompleto.");
    }

    private static void unpack(File archive, File target) throws IOException {
        if (!target.mkdirs() && !target.isDirectory()) throw new IOException("Impossibile preparare il modello vocale.");
        String canonicalRoot = target.getCanonicalPath() + File.separator;
        long expanded = 0;
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[32 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains(":") || hasParentTraversal(name))
                    throw new IOException("Percorso non valido nell’archivio del modello.");
                if (name.equals(MODEL_ROOT) || name.equals(MODEL_ROOT + "/")) { zip.closeEntry(); continue; }
                String prefix = MODEL_ROOT + "/";
                if (!name.startsWith(prefix)) throw new IOException("Archivio del modello inatteso.");
                String relative = name.substring(prefix.length());
                if (relative.isEmpty()) { zip.closeEntry(); continue; }
                File output = new File(target, relative);
                if (!output.getCanonicalPath().startsWith(canonicalRoot))
                    throw new IOException("Percorso non sicuro nell’archivio del modello.");
                if (entry.isDirectory()) {
                    if (!output.mkdirs() && !output.isDirectory()) throw new IOException("Impossibile creare il modello vocale.");
                } else {
                    File parent = output.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) throw new IOException("Impossibile estrarre il modello vocale.");
                    try (FileOutputStream file = new FileOutputStream(output); BufferedOutputStream out = new BufferedOutputStream(file)) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            expanded += read;
                            if (expanded > MAX_EXPANDED_BYTES) throw new IOException("Modello espanso più grande del previsto.");
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        file.getFD().sync();
                    }
                }
                zip.closeEntry();
            }
        } catch (IOException error) {
            deleteRecursively(target);
            throw error;
        }
    }

    private static boolean hasParentTraversal(String path) {
        for (String part : path.split("/")) if ("..".equals(part)) return true;
        return false;
    }

    static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        // Best effort cleanup for temporary download/install files.
        file.delete();
    }
}
