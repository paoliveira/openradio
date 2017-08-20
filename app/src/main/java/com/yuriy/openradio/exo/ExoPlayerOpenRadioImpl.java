/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerImpl;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Wrapper of the ExoPlayer.
 */
public final class ExoPlayerOpenRadioImpl {

    /**
     * Listener for the main public events.
     */
    public interface Listener {

        /**
         * Indicates an error while consume stream.
         *
         * @param error Exception associated with error.
         */
        void onError(final ExoPlaybackException error);

        void onHandledError(final ExoPlaybackException error);

        /**
         * Indicates that player is ready to play stream.
         */
        void onPrepared();

        /**
         * Currently playing playback progress.
         *
         * @param position         playback position in the current window, in milliseconds.
         * @param bufferedPosition Estimate of the position in the current window up to which data is buffered,
         *                         in milliseconds.
         * @param duration         Duration of the current window in milliseconds,
         *                         or C.TIME_UNSET if the duration is not known.
         */
        void onProgress(long position, long bufferedPosition, long duration);
    }

    /**
     * String tag to use in logs.
     */
    private static final String LOG_TAG = ExoPlayerOpenRadioImpl.class.getSimpleName();

    /**
     *
     */
    private static final int MAX_EXCEPTIONS_COUNT = 5;

    /**
     * Instance of the ExoPlayer.
     */
    private final ExoPlayer mExoPlayer;

    /**
     * Handler for the ExoPlayer to handle events.
     */
    private final Handler mMainHandler;

    /**
     * Listener of the ExoPlayer components events.
     */
    private final ComponentListener mComponentListener;

    /**
     * Instance of the ExoPlayer wrapper events.
     */
    private final Listener mListener;

    /**
     * Array of the media renderers (Audio and Metadata).
     */
    private final Renderer[] mRenderers;

    /**
     * Number of the Audio renderers.
     */
    private final int mAudioRendererCount;

    /**
     * Power wake lock to keep ExoPlayer up and running when screen is off.
     */
    private PowerManager.WakeLock mWakeLock = null;

    /**
     * Instance of the Data Source factory.
     */
    private DataSource.Factory mMediaDataSourceFactory;

    /**
     * Event logger to use as debug logger.
     */
    private EventLogger mEventLogger;

    /**
     * Current play URI.
     */
    private Uri mUri;

    /**
     * Current Media Source.
     */
    private MediaSource mMediaSource;

    /**
     * Enumeration of the state of the last user's action.
     */
    private enum UserState {
        NONE,
        PREPARE,
        PLAY,
        PAUSE,
        RESET
    }

    /**
     * State of the last user's action.
     */
    private UserState mUserState = UserState.NONE;

    /**
     * Number of currently detected playback exceptions.
     */
    private final AtomicInteger mNumOfExceptions = new AtomicInteger(0);

    /**
     * Runnable implementation to handle playback progress.
     */
    private final Runnable mUpdateProgressAction = this::updateProgress;

    /**
     * Handler to handle playback progress runnable.
     */
    private final Handler mUpdateProgressHandler = new Handler();

    /**
     * Main constructor.
     *
     * @param context                Application context.
     * @param listener               Listener for the wrapper's events.
     * @param icyInputStreamListener ICY listener for the stream events.
     */
    public ExoPlayerOpenRadioImpl(@NonNull final Context context,
                                  @NonNull final Listener listener,
                                  @NonNull final IcyInputStreamListener icyInputStreamListener) {
        super();

        mMainHandler = new Handler();
        mComponentListener = new ComponentListener(this);
        mListener = listener;

        final MappingTrackSelector trackSelector = new DefaultTrackSelector();
        mEventLogger = new EventLogger(trackSelector);
        mMediaDataSourceFactory = buildDataSourceFactory(context, icyInputStreamListener);

        final List<Renderer> renderersList = new ArrayList<>();
        buildRenderers(context, mMainHandler, renderersList);
        mRenderers = renderersList.toArray(new Renderer[renderersList.size()]);

        int audioRendererCount = 0;
        for (final Renderer renderer : mRenderers) {
            switch (renderer.getTrackType()) {
                case C.TRACK_TYPE_AUDIO:
                    audioRendererCount++;
                    break;
            }
        }
        mAudioRendererCount = audioRendererCount;

        mExoPlayer = new ExoPlayerImpl(mRenderers, trackSelector, new DefaultLoadControl());
        mExoPlayer.addListener(mComponentListener);
    }

    /**
     * Prepare player to play URI.
     *
     * @param uri URI to play.
     */
    public void prepare(final Uri uri) {
        if (uri == null) {
            return;
        }
        AppLogger.d(LOG_TAG + " prepare:" + uri.toString());

        mUserState = UserState.PREPARE;

        mUri = uri;
        mMediaSource = new ExtractorMediaSource(
                mUri, mMediaDataSourceFactory, new DefaultExtractorsFactory(),
                mMainHandler, mEventLogger
        );

        mExoPlayer.prepare(mMediaSource);
        mExoPlayer.setPlayWhenReady(true);

        stayAwake(true);
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    public void setVolume(final float value) {
        AppLogger.d(LOG_TAG + " volume to " + value);
        final ExoPlayer.ExoPlayerMessage[] messages
                = new ExoPlayer.ExoPlayerMessage[mAudioRendererCount];
        int count = 0;
        for (final Renderer renderer : mRenderers) {
            if (renderer.getTrackType() == C.TRACK_TYPE_AUDIO) {
                messages[count++] = new ExoPlayer.ExoPlayerMessage(
                        renderer, C.MSG_SET_VOLUME, value
                );
            }
        }
        mExoPlayer.sendMessages(messages);
    }

    /**
     * Play current stream based on the URI passed to {@link #prepare(Uri)} method.
     */
    public void play() {
        AppLogger.d(LOG_TAG + " play");

        mUserState = UserState.PLAY;
        prepare(mUri);
    }

    /**
     * Pause current stream based on the URI passed to {@link #prepare(Uri)} method.
     */
    public void pause() {
        AppLogger.d(LOG_TAG + " pause");

        mUserState = UserState.PAUSE;

        mExoPlayer.stop();
        mExoPlayer.setPlayWhenReady(false);

        stayAwake(false);
    }

    /**
     * Returns a value corresponded to whether or not current stream is playing.
     *
     * @return {@code true} in case of current stream is playing, {@code false} otherwise.
     */
    public boolean isPlaying() {
        final boolean isPlaying = (mExoPlayer != null && mExoPlayer.getPlayWhenReady());
        AppLogger.d(LOG_TAG + " is playing:" + isPlaying);
        return isPlaying;
    }

    /**
     * Resets the player to its uninitialized state.
     */
    public void reset() {
        AppLogger.d(LOG_TAG + " reset");

        mUserState = UserState.RESET;

        stayAwake(false);
        mExoPlayer.stop();
        mMediaSource.releaseSource();
    }

    /**
     * Release the player and associated resources.
     */
    public void release() {
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressAction);
        reset();
        mExoPlayer.release();
    }

    /**
     * Build audio and metadata renderers.
     *
     * @param context     Application context.
     * @param mainHandler Handler to handle events.
     * @param out         List implementation to add renderers to.
     */
    private void buildRenderers(final Context context,
                                final Handler mainHandler,
                                final List<Renderer> out) {
        buildAudioRenderers(context, mainHandler, mComponentListener, buildAudioProcessors(), out);
        buildMetadataRenderers(mainHandler, mComponentListener, out);
    }

    /**
     * Builds an array of {@link AudioProcessor}s that will process PCM audio before output.
     */
    private AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[0];
    }

    /**
     * Builds audio renderers for use by the player.
     *
     * @param context         Application context.
     * @param mainHandler     A handler associated with the main thread's looper.
     * @param eventListener   Listener for the events.
     * @param audioProcessors Array of audio processors.
     * @param out             An array to which the built renderers should be appended.
     */
    private void buildAudioRenderers(final Context context,
                                     final Handler mainHandler,
                                     final AudioRendererEventListener eventListener,
                                     final AudioProcessor[] audioProcessors,
                                     final List<Renderer> out) {
        out.add(new MediaCodecAudioRenderer(MediaCodecSelector.DEFAULT, null, true,
                mainHandler, eventListener, AudioCapabilities.getCapabilities(context),
                audioProcessors));
    }

    /**
     * Builds metadata renderers for use by the player.
     *
     * @param mainHandler A handler associated with the main thread's looper.
     * @param output      An output for the renderers.
     * @param out         An array to which the built renderers should be appended.
     */
    private void buildMetadataRenderers(final Handler mainHandler,
                                        final MetadataRenderer.Output output,
                                        final List<Renderer> out) {
        out.add(new MetadataRenderer(output, mainHandler.getLooper()));
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(@NonNull final Context context,
                                                      @NonNull final IcyInputStreamListener icyInputStreamListener) {
        final int timeOut = DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS;
        return new DefaultDataSourceFactory(
                context,
                null,
                new IcyHttpDataSourceFactory(
                        Util.getUserAgent(context, "OpenRadio"),
                        icyInputStreamListener,
                        timeOut
                )
        );
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

        /* Disable persistant wakelocks in media ExoPlayer based on property */
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

    /**
     * Decides whether or not to stay awake.
     *
     * @param awake {@code true} in case of it is necessary to stay awake, {@code false} otherwise.
     */
    private void stayAwake(final boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
    }

    /**
     * Listener class for the players components events.
     */
    private static final class ComponentListener implements
            AudioRendererEventListener, MetadataRenderer.Output, ExoPlayer.EventListener {

        /**
         * Reference to enclosing class.
         */
        @NonNull
        private final WeakReference<ExoPlayerOpenRadioImpl> mReference;

        /**
         * Main constructor.
         *
         * @param reference Reference to enclosing class.
         */
        private ComponentListener(@NonNull final ExoPlayerOpenRadioImpl reference) {
            super();

            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onAudioEnabled(final DecoderCounters counters) {
            AppLogger.d(LOG_TAG + " audioEnabled");
        }

        @Override
        public void onAudioSessionId(final int audioSessionId) {
            AppLogger.d(LOG_TAG + " audioSessionId:" + audioSessionId);
        }

        @Override
        public void onMetadata(final Metadata metadata) {
            AppLogger.d(LOG_TAG + " metadata:" + metadata);
        }

        @Override
        public void onAudioDecoderInitialized(final String decoderName,
                                              final long initializedTimestampMs,
                                              final long initializationDurationMs) {
            AppLogger.d(LOG_TAG + " audioDecoderInitialized " + decoderName);
        }

        @Override
        public void onAudioInputFormatChanged(final Format format) {
            AppLogger.d(LOG_TAG + " audioInputFormatChanged:" + format);
        }

        @Override
        public void onAudioTrackUnderrun(final int bufferSize,
                                         final long bufferSizeMs,
                                         final long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(final DecoderCounters counters) {
            AppLogger.d(LOG_TAG + " audioDisabled");
        }

        // Event listener

        @Override
        public void onTimelineChanged(final Timeline timeline, final Object manifest) {
            //AppLogger.d(LOG_TAG + " onTimelineChanged " + timeline + " " + manifest);
            final ExoPlayerOpenRadioImpl reference = mReference.get();
            if (reference == null) {
                return;
            }
            reference.updateProgress();
        }

        @Override
        public void onTracksChanged(final TrackGroupArray trackGroups,
                                    final TrackSelectionArray trackSelections) {
            //AppLogger.d(LOG_TAG + " onTracksChanged");
        }

        @Override
        public void onLoadingChanged(final boolean isLoading) {
            //AppLogger.d(LOG_TAG + " onLoadingChanged");
        }

        @Override
        public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
            final ExoPlayerOpenRadioImpl reference = mReference.get();
            if (reference == null) {
                return;
            }

            switch (playbackState) {
                case ExoPlayer.STATE_BUFFERING:
                    AppLogger.d(LOG_TAG + " STATE_BUFFERING");
                    break;
                case ExoPlayer.STATE_ENDED:
                    AppLogger.d(LOG_TAG + " STATE_ENDED, userState:" + reference.mUserState);
                    reference.mUpdateProgressHandler.removeCallbacks(reference.mUpdateProgressAction);

                    if (reference.mUserState != UserState.PAUSE && reference.mUserState != UserState.RESET) {
                        reference.prepare(reference.mUri);
                    }
                    break;
                case ExoPlayer.STATE_IDLE:
                    AppLogger.d(LOG_TAG + " STATE_IDLE");
                    break;
                case ExoPlayer.STATE_READY:
                    AppLogger.d(LOG_TAG + " STATE_READY");

                    reference.mListener.onPrepared();
                    reference.mNumOfExceptions.set(0);

                    break;
                default:
                    AppLogger.w(LOG_TAG + " onPlayerStateChanged to " + playbackState);
                    break;
            }

            reference.updateProgress();
        }

        @Override
        public void onPlayerError(final ExoPlaybackException exception) {
            AppLogger.e(LOG_TAG + " onPlayerError:\n" + Log.getStackTraceString(exception));

            final ExoPlayerOpenRadioImpl reference = mReference.get();
            if (reference == null) {
                return;
            }

            AppLogger.e(LOG_TAG + " num of exceptions " + reference.mNumOfExceptions.get());
            if (reference.mNumOfExceptions.getAndIncrement() <= MAX_EXCEPTIONS_COUNT) {
                if (exception.getCause() instanceof UnrecognizedInputFormatException) {
                    reference.mListener.onHandledError(exception);
                } else {
                    reference.prepare(reference.mUri);
                }
                return;
            }

            FabricUtils.logException(exception);

            reference.mListener.onError(exception);
        }

        @Override
        public void onPositionDiscontinuity() {
            //AppLogger.e(LOG_TAG + " onPositionDiscontinuity");
            final ExoPlayerOpenRadioImpl reference = mReference.get();
            if (reference == null) {
                return;
            }
            reference.updateProgress();
        }

        @Override
        public void onPlaybackParametersChanged(final PlaybackParameters playbackParameters) {
            //AppLogger.e(LOG_TAG + " onPlaybackParametersChanged");
        }

        @Override
        public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
            //AppLogger.e(LOG_TAG + " onRepeatModeChanged");
        }
    }

    /**
     * Handle playback update progress.
     */
    private void updateProgress() {
        long position = 0;
        long bufferedPosition = 0;
        long duration = 0;
        if (mExoPlayer != null) {
            position = mExoPlayer.getCurrentPosition();
            bufferedPosition = mExoPlayer.getBufferedPosition();
            duration = mExoPlayer.getDuration();
        }

        AppLogger.d(
                "Pos:" + position
                        + ", bufPos:" + bufferedPosition
                        + ", bufDur:" + (bufferedPosition - position)
        );

        mListener.onProgress(position, bufferedPosition, duration);

        // Cancel any pending updates and schedule a new one if necessary.
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressAction);
        final int playbackState = mExoPlayer == null ? ExoPlayer.STATE_IDLE : mExoPlayer.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (mExoPlayer.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            mUpdateProgressHandler.postDelayed(mUpdateProgressAction, delayMs);
        }
    }
}
