package it.solphivia.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;

/** Sprite-state controller for Solphivia's expressions, movement, rest, and outfits. */
public class PetView extends View {
    private static final String TAG = "SolphiviaPet";
    private static final int IDLE_FRAME_COUNT = 4;
    private static final int ACTION_FRAME_COUNT = 8;
    private static final long IDLE_FRAME_MS = 760;
    private static final long ACTION_FRAME_MS = 145;
    private static final int[] CLOSED_MOUTH_IDLE = {0, 2, 5, 0};

    private final Bitmap[] idleFrames = new Bitmap[IDLE_FRAME_COUNT];
    private final Bitmap[] talkFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] cyborgFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] lookFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] listenFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] emotionFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] runFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] cyborgRunFrames = new Bitmap[ACTION_FRAME_COUNT];
    private final Bitmap[] sitFrames = new Bitmap[IDLE_FRAME_COUNT];
    private final Bitmap[] sleepFrames = new Bitmap[IDLE_FRAME_COUNT];
    private final Bitmap[] cyborgSitFrames = new Bitmap[IDLE_FRAME_COUNT];
    private final Bitmap[] cyborgSleepFrames = new Bitmap[IDLE_FRAME_COUNT];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private boolean runLoopScheduled;
    private long runCycleStartedAt;
    private int runFrameIndex;
    private boolean running = true;
    private String animation = "idle";
    private long animationStartedAt = android.os.SystemClock.uptimeMillis();
    private final Choreographer.FrameCallback runFrameCallback = this::onRunFrame;

    private void onRunFrame(long frameTimeNanos) {
        if (!running || !isAttachedToWindow() || !"run".equals(animation)) {
            runLoopScheduled = false;
            return;
        }
        long elapsedMs = Math.max(0L, android.os.SystemClock.uptimeMillis() - runCycleStartedAt);
        int nextFrame = (int) ((elapsedMs / 85L) % ACTION_FRAME_COUNT);
        if (nextFrame != runFrameIndex) {
            runFrameIndex = nextFrame;
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(runFrameCallback);
    }
    private boolean cyborgSuit;

    public PetView(Context context) {
        super(context);
        setContentDescription("Solphivia animata: trascina per spostarla, tocca per interagire");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        loadFrames(context, "talk", talkFrames, options);
        loadFrames(context, "cyborg", cyborgFrames, options);
        loadFrames(context, "look", lookFrames, options);
        loadFrames(context, "listen", listenFrames, options);
        loadFrames(context, "emotion", emotionFrames, options);
        loadFrames(context, "run", runFrames, options);
        loadFrames(context, "cyborg_run", cyborgRunFrames, options);
        loadFrames(context, "sit", sitFrames, options);
        loadFrames(context, "sleep", sleepFrames, options);
        loadFrames(context, "cyborg_sit", cyborgSitFrames, options);
        loadFrames(context, "cyborg_sleep", cyborgSleepFrames, options);
        loadFrames(context, "idle", idleFrames, options);
    }

    private void loadFrames(Context context, String name, Bitmap[] target,
                            BitmapFactory.Options options) {
        for (int i = 0; i < target.length; i++) {
            String path = String.format(java.util.Locale.ROOT,
                    "solphivia/%s/frame_%02d.png", name, i);
            try (InputStream input = context.getAssets().open(path)) {
                target[i] = BitmapFactory.decodeStream(input, null, options);
            } catch (IOException | RuntimeException error) {
                Log.e(TAG, "Impossibile caricare " + path, error);
            }
        }
    }

    public void pause() { running = false; stopRunLoop(); }

    public void setCyborgSuit(boolean enabled) {
        if (cyborgSuit == enabled) return;
        cyborgSuit = enabled;
        invalidate();
    }

    public boolean isCyborgSuit() { return cyborgSuit; }

    public String getSolphiviaAnimation() { return animation; }

    /** Apply a whitelisted animation command from the assistant layer. */
    public boolean setAnimation(String command) {
        if (command == null) return false;
        String next = command.trim().toLowerCase(java.util.Locale.ROOT);
        switch (next) {
            case "idle": case "look": case "thinking": case "listen": case "talk":
            case "sit": case "sleep": case "happy": case "concerned": case "sad": case "run":
                if ("run".equals(next) && "run".equals(animation) && runLoopScheduled) {
                    running = true;
                    return true;
                }
                if (!"run".equals(next)) stopRunLoop();
                animation = next;
                animationStartedAt = android.os.SystemClock.uptimeMillis();
                runFrameIndex = 0;
                running = true;
                if ("run".equals(next) && isAttachedToWindow()) startRunLoop();
                invalidate();
                return true;
            default:
                return false;
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        if ("run".equals(animation)) startRunLoop();
        invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        stopRunLoop();
        super.onDetachedFromWindow();
    }

    private void startRunLoop() {
        if (runLoopScheduled) return;
        runLoopScheduled = true;
        runCycleStartedAt = android.os.SystemClock.uptimeMillis();
        runFrameIndex = 0;
        Choreographer.getInstance().postFrameCallback(runFrameCallback);
    }

    private void stopRunLoop() {
        if (runLoopScheduled) Choreographer.getInstance().removeFrameCallback(runFrameCallback);
        runLoopScheduled = false;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();
        long elapsed = now - animationStartedAt;
        Bitmap[] voiceFrames = cyborgSuit ? cyborgFrames : talkFrames;
        Bitmap frame;
        boolean animate = true;
        switch (animation) {
            case "look": case "thinking":
                if (elapsed >= ACTION_FRAME_COUNT * ACTION_FRAME_MS) {
                    animation = "idle";
                    animationStartedAt = now;
                    frame = voiceFrames[CLOSED_MOUTH_IDLE[0]];
                } else if (cyborgSuit) {
                    int calm = CLOSED_MOUTH_IDLE[(int) (elapsed / IDLE_FRAME_MS) % IDLE_FRAME_COUNT];
                    frame = voiceFrames[calm];
                } else frame = lookFrames[(int) (elapsed / ACTION_FRAME_MS) % ACTION_FRAME_COUNT];
                break;
            case "listen":
                if (cyborgSuit) {
                    frame = voiceFrames[CLOSED_MOUTH_IDLE[(int) (elapsed / IDLE_FRAME_MS) % IDLE_FRAME_COUNT]];
                } else frame = listenFrames[(int) (elapsed / ACTION_FRAME_MS) % ACTION_FRAME_COUNT];
                break;
            case "talk":
                frame = voiceFrames[(int) (elapsed / 115) % ACTION_FRAME_COUNT];
                break;
            case "run":
                Bitmap[] cycle = cyborgSuit ? cyborgRunFrames : runFrames;
                if (runLoopScheduled) {
                    frame = cycle[runFrameIndex];
                    animate = false; // The run cycle resynchronizes to Android display frames and elapsed time.
                } else {
                    frame = cycle[(int) (elapsed / 95) % ACTION_FRAME_COUNT];
                }
                break;
            case "sit":
                Bitmap[] sitting = cyborgSuit ? cyborgSitFrames : sitFrames;
                frame = sitting[(int) (elapsed / IDLE_FRAME_MS) % IDLE_FRAME_COUNT];
                break;
            case "sleep":
                Bitmap[] sleeping = cyborgSuit ? cyborgSleepFrames : sleepFrames;
                frame = sleeping[0];
                animate = false;
                break;
            case "happy":
                frame = cyborgSuit ? cyborgFrames[1] : emotionFrames[7]; animate = false; break;
            case "concerned":
                frame = cyborgSuit ? cyborgFrames[0] : emotionFrames[0]; animate = false; break;
            case "sad":
                frame = cyborgSuit ? cyborgFrames[0] : emotionFrames[4]; animate = false; break;
            default:
                frame = voiceFrames[CLOSED_MOUTH_IDLE[(int) (elapsed / IDLE_FRAME_MS) % IDLE_FRAME_COUNT]];
                break;
        }
        if (frame == null) frame = idleFrames[0];
        if (frame != null) {
            destination.set(0, 0, getWidth(), getHeight());
            if (animation.equals("idle")) {
                float phase = (elapsed % (IDLE_FRAME_MS * IDLE_FRAME_COUNT)) /
                        (float) (IDLE_FRAME_MS * IDLE_FRAME_COUNT) * (float) (Math.PI * 2);
                float density = getResources().getDisplayMetrics().density;
                canvas.save();
                canvas.translate((float) Math.sin(phase) * density * 0.8f,
                        (float) Math.sin(phase * 2f) * density * 1.3f);
                canvas.drawBitmap(frame, null, destination, paint);
                canvas.restore();
            } else canvas.drawBitmap(frame, null, destination, paint);
        }
        // Sleep draws one still frame and schedules nothing, reducing idle battery use.
        if (running && isShown() && animate) postInvalidateDelayed(55);
    }
}
