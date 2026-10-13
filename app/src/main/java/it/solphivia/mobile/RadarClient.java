package it.solphivia.mobile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** DPC v2 SRI tiles: red channel encodes 0..100 mm/h, not display palette colors. */
final class RadarClient {
    static final double WORLD = 40075016.68557849;
    static final int ZOOM = 7, TILES = 1 << ZOOM;
    static final double SPAN = WORLD / TILES;
    static final double RADIUS_KM = 60;
    static final class Sample {
        final long time; final double nearestKm;
        Sample(long time, double km) { this.time=time; nearestKm=km; }
    }
    static HttpURLConnection connect(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(15000);
        c.setRequestProperty("Origin", "https://radar.protezionecivile.it");
        c.setRequestProperty("Referer", "https://radar.protezionecivile.it/");
        return c;
    }
    static long latestTime() throws Exception {
        HttpURLConnection c = connect("https://radar-api.protezionecivile.it/findLastProductByType?type=SRI");
        try {
            if (c.getResponseCode() != 200) throw new IOException("Radar API HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048]; int n;
                while ((n=in.read(buffer)) != -1) { if (out.size()+n > 65536) throw new IOException("Risposta radar troppo grande"); out.write(buffer,0,n); }
                org.json.JSONArray products = new JSONObject(out.toString("UTF-8")).getJSONArray("lastProducts");
                for (int i=0; i<products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);
                    if ("SRI".equals(p.optString("productType"))) return p.getLong("time");
                }
                throw new IOException("Nessuna scansione SRI disponibile");
            }
        } finally { c.disconnect(); }
    }
    static double pixelRain(int argb) { return ((argb >>> 24) & 255) < 2 ? Double.NaN : ((argb >>> 16) & 255) * 100.0 / 255; }
    static double worldX(double longitude) { return (longitude + 180) / 360 * WORLD; }
    static double worldY(double latitude) {
        double sin = Math.sin(Math.toRadians(Math.max(-85, Math.min(85, latitude))));
        return (0.5 - Math.log((1+sin)/(1-sin))/(4*Math.PI)) * WORLD;
    }
    static String tileUrl(long time, int x, int y) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd/HHmm", Locale.ROOT); f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return "https://s3-prod-dpc-radar-webp-cache.s3.eu-south-1.amazonaws.com/SRI/" + f.format(new Date(time))
                + "/" + ZOOM + "/" + x + "/" + y + "/sri.webp";
    }
    static Sample read(double lat, double lon, long time) throws Exception {
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < 34 || lat > 49 || lon < 4 || lon > 21)
            throw new IOException("Località fuori dall’area radar italiana");
        double cx = worldX(lon), cy = worldY(lat), scale = Math.cos(Math.toRadians(lat));
        double radius = RADIUS_KM * 1000 / scale;
        double nearest = Double.POSITIVE_INFINITY;
        int valid = 0;
        for (int ty=(int)Math.floor((cy-radius)/SPAN); ty<=(int)Math.floor((cy+radius)/SPAN); ty++) {
            for (int tx=(int)Math.floor((cx-radius)/SPAN); tx<=(int)Math.floor((cx+radius)/SPAN); tx++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                HttpURLConnection c = connect(tileUrl(time,tx,ty)); Bitmap tile = null;
                try {
                    if (c.getResponseCode()!=200) throw new IOException("Immagini radar non disponibili (HTTP " + c.getResponseCode() + ")");
                    try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096]; int n;
                        while ((n=in.read(buffer))!=-1) { if(out.size()+n>1024*1024) throw new IOException("Tile radar non valida"); out.write(buffer,0,n); }
                        byte[] bytes = out.toByteArray();
                        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
                        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                        if(bounds.outWidth!=256 || bounds.outHeight!=256) throw new IOException("Dimensioni tile radar inattese");
                        tile = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    }
                    if (tile==null) throw new IOException("Immagine radar non leggibile");
                    int[] pixels = new int[256*256]; tile.getPixels(pixels,0,256,0,0,256,256);
                    for(int y=0;y<256;y++) for(int x=0;x<256;x++) {
                        double km = Math.hypot((tx+(x+0.5)/256.0)*SPAN-cx,(ty+(y+0.5)/256.0)*SPAN-cy)*scale/1000;
                        if(km>RADIUS_KM) continue;
                        double rain = pixelRain(pixels[y*256+x]);
                        if(Double.isFinite(rain)) valid++;
                        if(rain>=1.0 && km<nearest) nearest=km;
                    }
                } finally { if(tile!=null) tile.recycle(); c.disconnect(); }
            }
        }
        if(valid==0) throw new IOException("Nessun dato radar nell’area");
        return new Sample(time,nearest);
    }
}
