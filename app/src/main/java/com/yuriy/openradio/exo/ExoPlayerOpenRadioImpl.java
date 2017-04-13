package com.yuriy.openradio.exo;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerImpl;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Wrapper of the ExoPlayer.
 */
public final class ExoPlayerOpenRadioImpl {

    public interface Listener {

        void onCompletion();

        void onError(final String message);

        void onPrepared();
    }

    private final ExoPlayer player;
    private final Renderer[] renderers;
    private final Handler mainHandler;
    private final ComponentListener componentListener;
    private final Listener mListener;
    private PowerManager.WakeLock mWakeLock = null;

    public ExoPlayerOpenRadioImpl(@NonNull final Context context,
                                  @NonNull final Listener listener) {
        super();

        mainHandler = new Handler();
        componentListener = new ComponentListener();
        mListener = listener;

        final List<Renderer> renderersList = new ArrayList<>();
        buildRenderers(context, mainHandler, renderersList);
        renderers = renderersList.toArray(new Renderer[renderersList.size()]);

        player = new ExoPlayerImpl(renderers, new DefaultTrackSelector(), new DefaultLoadControl());
    }

    public void prepare() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Connection", "keep-alive");
        headers.put("Keep-Alive", "timeout=2000");
        stayAwake(true);
    }

    public void setVolume(final float value) {

    }

    public void play() {
        stayAwake(true);
    }

    public void pause() {
        stayAwake(false);
    }

    public boolean isPlaying() {

        return false;
    }

    public void reset() {
        // Resets the MediaPlayer to its uninitialized state.
        // After calling this method, you will have to initialize it again
        // by setting the data source and calling prepare().
        stayAwake(false);
    }

    private void buildRenderers(Context context, Handler mainHandler, List<Renderer> out) {
        buildAudioRenderers(context, mainHandler, componentListener, buildAudioProcessors(), out);
        buildMetadataRenderers(context, mainHandler, componentListener, out);
    }

    /**
     * Builds an array of {@link AudioProcessor}s that will process PCM audio before output.
     */
    private AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[0];
    }

    private void buildAudioRenderers(Context context,
                                     Handler mainHandler,
                                     AudioRendererEventListener eventListener,
                                     AudioProcessor[] audioProcessors,
                                     List<Renderer> out) {
        out.add(new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT, null, true,
                mainHandler, eventListener, AudioCapabilities.getCapabilities(context),
                audioProcessors));
    }

    /**
     * Builds metadata renderers for use by the player.
     *
     * @param context     The {@link Context} associated with the player.
     * @param mainHandler A handler associated with the main thread's looper.
     * @param output      An output for the renderers.
     * @param out         An array to which the built renderers should be appended.
     */
    private void buildMetadataRenderers(Context context,
                                        Handler mainHandler,
                                        MetadataRenderer.Output output,
                                        List<Renderer> out) {
        out.add(new MetadataRenderer(output, mainHandler.getLooper()));
    }

    /**
     * Set the low-level power management behavior for this MediaPlayer. This
     * can be used when the MediaPlayer is not playing.
     *
     * <p>This function has the Player access the low-level power manager
     * service to control the device's power usage while playing is occurring.
     * The parameter is a combination of {@link PowerManager} wake flags.
     * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
     * permission.
     * By default, no attempt is made to keep the device awake during playback.
     *
     * @param context The Context to use.
     * @param mode    The power/wake mode to set.
     * @see PowerManager
     */
    public void setWakeMode(final Context context, int mode) {
        boolean washeld = false;

        /* Disable persistant wakelocks in media player based on property */
//        if (SystemProperties.getBoolean("audio.offload.ignore_setawake", false) == true) {
//            Log.w(TAG, "IGNORING setWakeMode " + mode);
//            return;
//        }

        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        final PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode|PowerManager.ON_AFTER_RELEASE, ExoPlayer.class.getName());
        mWakeLock.setReferenceCounted(false);
        if (washeld) {
            mWakeLock.acquire();
        }
    }

    private void stayAwake(final boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    private static final class ComponentListener implements
            AudioRendererEventListener, MetadataRenderer.Output {

        private ComponentListener() {
            super();
        }

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onMetadata(Metadata metadata) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs,
                                              long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs,
                                         long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }
    }
}
