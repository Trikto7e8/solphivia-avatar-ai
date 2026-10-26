package it.solphivia.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small relay activity for Android's speech recognizer and document picker. */
public class CompanionInputActivity extends Activity {
    static final String ACTION_VOICE = "it.solphivia.mobile.action.VOICE_INPUT";
    static final String ACTION_HOME_ICON = "it.solphivia.mobile.action.HOME_ICON";
    static final String ACTION_SETTINGS = "it.solphivia.mobile.action.SETTINGS";
    static final String ACTION_FILES = "it.solphivia.mobile.action.PICK_FILES";
    static final String ACTION_ATTACHMENTS = "it.solphivia.mobile.action.ATTACHMENTS";
    static final String ACTION_WEATHER_SETUP = "it.solphivia.mobile.action.WEATHER_SETUP";
    static final String EXTRA_URIS = "attachment_uris";
    private static final int REQUEST_VOICE = 31;
    private static final int REQUEST_FILES = 32;
    private static final int REQUEST_LOCATION = 33;
    private final Handler main = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean locating;
    private volatile boolean destroyed;
    private boolean externalRequestPending;

    private boolean active() { return !destroyed && !isFinishing() && !isDestroyed(); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        externalRequestPending = state != null && state.getBoolean("external_request_pending", false);
        if (externalRequestPending) return;
        String action = getIntent() == null ? null : getIntent().getAction();
        if(ACTION_HOME_ICON.equals(action)){showHomeIconHelp();return;}
        if (ACTION_SETTINGS.equals(action)) {
            new AlertDialog.Builder(this).setTitle("Solphivia")
                    .setItems(new String[]{"Memoria e Memory Vault", "Voce italiana e pronuncia", "Prova animazioni", "Microfono e sessione", "Aggiungi icona sulla Home", "Ferma la voce", "Riprendi la voce"}, (d,w) -> {
                        if(w==6){PetService.resumeReading();finish();return;}
                        if(w==5){VoiceSessionService.interruptReading();finish();return;}
                        if(w==4){showHomeIconHelp();return;}
                        startActivity(new Intent(this,w==0?MemoryActivity.class:w==1?VoiceSettingsActivity.class:w==2?AnimationPreviewActivity.class:VoiceSessionActivity.class)); finish();
                    }).setOnCancelListener(d -> finish()).show();
        }
        else if (ACTION_WEATHER_SETUP.equals(action)) chooseWeatherLocation();
        else if (ACTION_VOICE.equals(action)) openVoiceInput();
        else openFilePicker();
    }

    private void showHomeIconHelp(){
        new AlertDialog.Builder(this).setTitle("Icona unica Solphivia IA").setMessage(AppControls.addHomeIcon(this))
            .setPositiveButton("Vai alla Home",(d,w)->{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));finish();})
            .setNegativeButton("Chiudi",(d,w)->finish()).setOnCancelListener(d->finish()).show();
    }

    private void chooseWeatherLocation() {
        new AlertDialog.Builder(this).setTitle("Solphivia Meteorina")
                .setMessage("Come vuoi indicare la posizione per il meteo? Le coordinate saranno inviate a Open-Meteo per ottenere le previsioni e conservate sul telefono per gli aggiornamenti.")
                .setPositiveButton("Usa GPS", (dialog, which) -> useGps())
                .setNeutralButton("Scrivo una località", (dialog, which) -> askCity())
                .setNegativeButton("Annulla", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish()).show();
    }

    private void useGps() {
        if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"}, REQUEST_LOCATION);
            return;
        }
        findLocation();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != REQUEST_LOCATION) return;
        boolean granted = false;
        for (int result : grants) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        if (granted) findLocation();
        else new AlertDialog.Builder(this).setTitle("Posizione non autorizzata")
                .setMessage("Puoi comunque scegliere una città senza attivare il GPS.")
                .setPositiveButton("Scegli città", (d, w) -> askCity())
                .setNegativeButton("Annulla", (d, w) -> finish()).show();
    }

    private void findLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location best = null;
            for (String provider : locationManager.getProviders(true)) {
                try {
                    Location candidate = locationManager.getLastKnownLocation(provider);
                    if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
                } catch (SecurityException ignored) { }
            }
            if (best != null && System.currentTimeMillis() - best.getTime() < 2 * 60 * 60 * 1000L) {
                sendWeatherLocation(best);
                return;
            }
            locating = true;
            Toast.makeText(this, "Cerco la posizione GPS…", Toast.LENGTH_SHORT).show();
            locationListener = new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    if (!locating) return;
                    locating = false; main.removeCallbacks(locationTimeout); sendWeatherLocation(location);
                }
                @Override public void onProviderEnabled(String provider) { }
                @Override public void onProviderDisabled(String provider) { }
            };
            boolean requested = false;
            for (String provider : locationManager.getProviders(true)) {
                try { locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper()); requested = true; }
                catch (SecurityException ignored) { }
            }
            if (!requested) { locating = false; askCity(); return; }
            main.postDelayed(locationTimeout, 20000L);
        } catch (SecurityException | IllegalArgumentException error) {
            askCity();
        }
    }

    private final Runnable locationTimeout = () -> {
        if (!locating) return;
        locating = false;
        removeLocationUpdates();
        new AlertDialog.Builder(this).setTitle("Posizione non disponibile")
                .setMessage("Non ho ricevuto una posizione GPS. Puoi inserire la città a mano.")
                .setPositiveButton("Scegli città", (d, w) -> askCity())
                .setNegativeButton("Annulla", (d, w) -> finish()).show();
    };

    private void sendWeatherLocation(Location location) {
        removeLocationUpdates();
        Intent update = new Intent(this, PetService.class).setAction(PetService.ACTION_WEATHER_LOCATION)
                .putExtra(PetService.EXTRA_LATITUDE, location.getLatitude())
                .putExtra(PetService.EXTRA_LONGITUDE, location.getLongitude())
                .putExtra(PetService.EXTRA_PLACE, "Posizione GPS");
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(update); else startService(update);
        Toast.makeText(this, "Meteorina aggiornata dalla posizione GPS", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void removeLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
        }
        locationListener = null;
    }

    private void askCity() {
        if (!active()) return;
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("es. Torre a Mare, Bari");
        String saved = getSharedPreferences("weather", 0).getString("place", "Torre a Mare, Bari");
        input.setText(saved); input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Scegli la località")
                .setMessage("Inserisci una città o una località. Non userò il GPS; le coordinate della località saranno inviate a Open-Meteo per le previsioni.")
                .setView(input)
                .setPositiveButton("Usa questa località", (d, w) -> geocode(input.getText().toString().trim()))
                .setNegativeButton("Annulla", (d, w) -> finish()).show();
    }

    private void geocode(String query) {
        if (query.isEmpty()) { finish(); return; }
        Toast.makeText(this, "Cerco la località…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String address = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(query, "UTF-8") + "&count=1&language=it&format=json";
                HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(12000); connection.setReadTimeout(15000);
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) body.append(line);
                } finally { connection.disconnect(); }
                JSONArray results = new JSONObject(body.toString()).optJSONArray("results");
                if (results == null || results.length() == 0) throw new IllegalArgumentException("Località non trovata.");
                JSONObject item = results.getJSONObject(0);
                String label = item.optString("name", query);
                String admin = item.optString("admin1", "");
                String country = item.optString("country", "");
                if (!admin.isEmpty() && !admin.equals(label)) label += ", " + admin;
                if (!country.isEmpty()) label += ", " + country;
                final String locationLabel = label;
                Intent update = new Intent(this, PetService.class).setAction(PetService.ACTION_WEATHER_LOCATION)
                        .putExtra(PetService.EXTRA_LATITUDE, item.getDouble("latitude"))
                        .putExtra(PetService.EXTRA_LONGITUDE, item.getDouble("longitude"))
                        .putExtra(PetService.EXTRA_PLACE, locationLabel);
                main.post(() -> {
                    if (!active()) return;
                    if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(update); else startService(update);
                    Toast.makeText(this, "Località meteo impostata: " + locationLabel, Toast.LENGTH_SHORT).show(); finish();
                });
            } catch (Exception error) {
                main.post(() -> {
                    if (!active()) return;
                    Toast.makeText(this, error.getMessage() == null ? "Ricerca della località non riuscita." : error.getMessage(), Toast.LENGTH_LONG).show();
                    askCity();
                });
            }
        }, "solphivia-geocode").start();
    }

    private void openVoiceInput() {
        Intent voice = VoicePhrases.recognitionIntent();
        voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla con Solphivia");
        try { externalRequestPending = true; startActivityForResult(voice, REQUEST_VOICE); }
        catch (Exception e) {
            Toast.makeText(this, "Riconoscimento vocale non disponibile sul telefono.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void openFilePicker() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*", "text/plain", "text/markdown", "text/csv"
        });
        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { externalRequestPending = true; startActivityForResult(pick, REQUEST_FILES); }
        catch (Exception e) {
            Toast.makeText(this, "Non trovo un selettore di file sul telefono.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        externalRequestPending = false;
        if (!active()) return;
        if(requestCode==REQUEST_VOICE && !getIntent().getBooleanExtra("inline",false))
            startForegroundService(new Intent(this,PetService.class).setAction(PetService.ACTION_INPUT_DONE));
        if (resultCode != RESULT_OK || data == null) { finish(); return; }
        if (requestCode == REQUEST_VOICE) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty() && !matches.get(0).trim().isEmpty()) {
                String recognized=VoicePhrases.best(matches);
                if(getIntent().getBooleanExtra("inline",false)) { setResult(RESULT_OK,new Intent().putExtra("dictation",recognized)); }
                else { Intent send = new Intent(this, PetService.class).putExtra(PetService.EXTRA_DRAFT_MESSAGE, recognized); startForegroundService(send); }
            }
        } else if (requestCode == REQUEST_FILES) {
            ArrayList<Uri> selected = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount() && i < 5; i++)
                    selected.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) selected.add(data.getData());
            ArrayList<String> values = new ArrayList<>();
            for (Uri uri : selected) {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { }
                values.add(uri.toString());
            }
            if (!values.isEmpty()) {
                if(getIntent().getBooleanExtra("inline",false)) {
                    Intent result=new Intent().putStringArrayListExtra(EXTRA_URIS,values).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if(data.getClipData()!=null)result.setClipData(data.getClipData());
                    else result.setClipData(ClipData.newRawUri("allegato",selected.get(0)));
                    setResult(RESULT_OK,result); finish();return;
                }
                Intent send = new Intent(this, PetService.class).setAction(ACTION_ATTACHMENTS)
                        .putStringArrayListExtra(EXTRA_URIS, values)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (data.getClipData() != null) send.setClipData(data.getClipData());
                else if (!selected.isEmpty()) send.setClipData(ClipData.newRawUri("allegato", selected.get(0)));
                startForegroundService(send);
            }
        }
        finish();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        main.removeCallbacksAndMessages(null);
        locating = false;
        main.removeCallbacks(locationTimeout);
        removeLocationUpdates();
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("external_request_pending", externalRequestPending);
        super.onSaveInstanceState(state);
    }
}
