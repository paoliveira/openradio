package com.yuriy.openradio.exo;

import android.content.Context;
import android.os.Handler;

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
import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Wrapper of the ExoPlayer.
 */
public final class ExoPlayerOpenRadioImpl {

    private final ExoPlayer player;
    private final Renderer[] renderers;
    private final Handler mainHandler;
    private final ComponentListener componentListener;

    public ExoPlayerOpenRadioImpl(final Context context) {
        super();

        mainHandler = new Handler();
        componentListener = new ComponentListener();

        final List<Renderer> renderersList = new ArrayList<>();
        buildRenderers(context, mainHandler, renderersList);
        renderers = renderersList.toArray(new Renderer[renderersList.size()]);

        player = new ExoPlayerImpl(renderers, new DefaultTrackSelector(), new DefaultLoadControl());
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
