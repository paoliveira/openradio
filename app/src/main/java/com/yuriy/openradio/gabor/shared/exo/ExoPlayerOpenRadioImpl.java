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

package com.yuriy.openradio.gabor.shared.exo;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.DefaultAudioSink;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.yuriy.openradio.gabor.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.gabor.shared.model.storage.EqualizerStorage;
import com.yuriy.openradio.gabor.shared.model.translation.EqualizerJsonStateSerializer;
import com.yuriy.openradio.gabor.shared.model.translation.EqualizerStateDeserializer;
import com.yuriy.openradio.gabor.shared.model.translation.EqualizerStateJsonDeserializer;
import com.yuriy.openradio.gabor.shared.model.translation.EqualizerStateSerializer;
import com.yuriy.openradio.gabor.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;
import com.yuriy.openradio.gabor.shared.vo.EqualizerState;

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

        /**
         * @param playbackState
         */
        void onPlaybackStateChanged(final int playbackState);
    }

    /**
     * String tag to use in logs.
     */
    private static final String LOG_TAG = "ExoPlayerORImpl";
    /**
     *
     */
    private static final int MAX_EXCEPTIONS_COUNT = 5;
    /**
     * Instance of the ExoPlayer.
     */
    private ExoPlayer mExoPlayer;
    /**
     * Instance of equalizer.
     */
    @Nullable
    private Equalizer mEqualizer;
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
     * Power wake lock to keep ExoPlayer up and running when screen is off.
     */
    private PowerManager.WakeLock mWakeLock = null;
    /**
     * Instance of the Data Source factory.
     */
    private final DataSource.Factory mMediaDataSourceFactory;
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
    private Runnable mUpdateProgressAction = this::updateProgress;
    /**
     * Handler to handle playback progress runnable.
     */
    private Handler mUpdateProgressHandler = new Handler();
    private final MetadataListener mMetadataListener;
    @NonNull
    private final Context mContext;

    /**
     * Main constructor.
     *
     * @param context          Application context.
     * @param listener         Listener for the wrapper's events.
     * @param metadataListener Listener for the stream events.
     */
    public ExoPlayerOpenRadioImpl(@NonNull final Context context,
                                  @NonNull final Listener listener,
                                  @NonNull final MetadataListener metadataListener) {
        super();

        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());
        mComponentListener = new ComponentListener();
        mListener = listener;
        mMetadataListener = metadataListener;

        mMediaDataSourceFactory = buildDataSourceFactory(mContext);

        final List<Renderer> renderersList = new ArrayList<>();
        buildRenderers(mContext, mMainHandler, renderersList);
        mRenderers = renderersList.toArray(new Renderer[0]);

        final TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
        final ExoPlayer.Builder builder = new ExoPlayer.Builder(mContext, mRenderers);
        builder.setTrackSelector(new DefaultTrackSelector(mContext, trackSelectionFactory));
        builder.setLoadControl(
                new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                AppPreferencesManager.getMinBuffer(mContext),
                                AppPreferencesManager.getMaxBuffer(mContext),
                                AppPreferencesManager.getPlayBuffer(mContext),
                                AppPreferencesManager.getPlayBufferRebuffer(mContext)
                        )
                        .build()
        );

        mExoPlayer = builder.build();
        mExoPlayer.addListener(mComponentListener);
        mExoPlayer.setForegroundMode(true);
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

        @C.ContentType int type = Util.inferContentType(uri);
        mUri = uri;
        switch (type) {
            case C.TYPE_HLS:
                mMediaSource = new HlsMediaSource.Factory(mMediaDataSourceFactory)
                        .createMediaSource(
                                new MediaItem.Builder().setUri(mUri).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                        );
                break;
            case C.TYPE_OTHER:
                mMediaSource = new ProgressiveMediaSource.Factory(mMediaDataSourceFactory)
                        .createMediaSource(new MediaItem.Builder().setUri(mUri).build());
                break;
            case C.TYPE_SS:
            case C.TYPE_DASH:
            default:
                AppLogger.e("Unsupported extension:" + type);
                break;
        }

        if (mExoPlayer != null) {
            mExoPlayer.setMediaSource(mMediaSource);
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayer.prepare();
        }

        stayAwake(true);
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    public void setVolume(final float value) {
        AppLogger.d(LOG_TAG + " volume to " + value);
        for (final Renderer renderer : mRenderers) {
            if (renderer.getTrackType() != C.TRACK_TYPE_AUDIO) {
                continue;
            }
            mExoPlayer.createMessage(renderer).setType(Renderer.MSG_SET_VOLUME).setPayload(value).send();
        }
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
     * Pause current stream based on the URI passed to {@link #prepare(Uri)} )} method.
     */
    public void pause() {
        AppLogger.d(LOG_TAG + " pause");

        mUserState = UserState.PAUSE;

        if (mExoPlayer != null) {
            mExoPlayer.stop();
            mExoPlayer.setPlayWhenReady(false);
        }

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
        if (mExoPlayer != null) {
            mExoPlayer.stop();
        }
    }

    /**
     * Release the player and associated resources.
     */
    public void release() {
        if (mExoPlayer == null) {
            AppLogger.d(LOG_TAG + " ExoPlayer impl already released");
            return;
        }
        if (AppUtils.isUiThread()) {
            releaseIntrnl();
        } else {
            mMainHandler.post(this::releaseIntrnl);
        }
    }

    public void updateEqualizer() {
        if (mEqualizer == null) {
            AppLogger.e("Can not update equalizer");
            return;
        }
        final EqualizerStateDeserializer deserializer = new EqualizerStateJsonDeserializer();
        final EqualizerState state = deserializer.deserialize(
                mContext, EqualizerStorage.loadEqualizerState(mContext)
        );
        EqualizerState.applyState(mEqualizer, state);

        state.printState();
    }

    public void saveState() {
        if (mEqualizer == null) {
            AppLogger.e("Can not save equalizer's state");
            return;
        }
        final EqualizerStateSerializer serializer = new EqualizerJsonStateSerializer();
        EqualizerState state = null;
        try {
            state = EqualizerState.createState(mEqualizer);
        } catch (final IllegalArgumentException e) {
            AppLogger.e("Can not create state from " + mEqualizer + ", " + e);
        } catch (final IllegalStateException e) {
            AppLogger.e("Can not create state from " + mEqualizer + ", " + e);
        } catch (final UnsupportedOperationException e) {
            AppLogger.e("Can not create state from " + mEqualizer + ", " + e);
        } catch (final RuntimeException e) {
            // Some times this happen with "AudioEffect: set/get parameter error"
            AppLogger.e("Can not create state from " + mEqualizer + ", " + e);
        }
        if (state != null) {
            EqualizerStorage.saveEqualizerState(mContext, serializer.serialize(state));
        }
    }

    private void initEqualizer(final int audioSessionId) {
        if (mEqualizer != null) {
            return;
        }
        try {
            AnalyticsUtils.logMessage("Eq pre-inited:" + mEqualizer);
            mEqualizer = new Equalizer(0, audioSessionId);
            AnalyticsUtils.logMessage("Eq inited:" + mEqualizer);
        } catch (final Exception e) {
            mEqualizer = null;
            EqualizerStorage.saveEqualizerState(mContext, "");
            AnalyticsUtils.logException(new RuntimeException("Can not init eq:" + e));
            return;
        }
        //TODO: Do state operations in separate thread.
        if (EqualizerStorage.isEmpty(mContext)) {
            mEqualizer.setEnabled(false);
            mEqualizer.setEnabled(true);
            saveState();
        } else {
            updateEqualizer();
        }
    }

    private void releaseIntrnl() {
        if (mEqualizer != null) {
            mEqualizer.setEnabled(false);
            mEqualizer.release();
            AnalyticsUtils.logMessage("Eq de-inited:" + mEqualizer);
            mEqualizer = null;
        }

        if (mExoPlayer == null) {
            AppLogger.d(LOG_TAG + " ExoPlayer impl already released");
            return;
        }

        mExoPlayer.removeListener(mComponentListener);
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressAction);
        reset();
        mExoPlayer.release();

        mExoPlayer = null;
        mUpdateProgressHandler = null;
        mUpdateProgressAction = null;
        mMediaSource = null;
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
        out.add(
                new MediaCodecAudioRenderer(
                        context,
                        MediaCodecSelector.DEFAULT,
                        false,
                        mainHandler,
                        eventListener,
                        new DefaultAudioSink(AudioCapabilities.getCapabilities(context), audioProcessors)
                )
        );
    }

    /**
     * Builds metadata renderers for use by the player.
     *
     * @param mainHandler A handler associated with the main thread's looper.
     * @param output      An output for the renderers.
     * @param out         An array to which the built renderers should be appended.
     */
    private void buildMetadataRenderers(final Handler mainHandler,
                                        final MetadataOutput output,
                                        final List<Renderer> out) {
        out.add(new MetadataRenderer(output, mainHandler.getLooper()));
    }

    /**
     * Returns a new DataSource factory.
     *
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(@NonNull final Context context) {
        final String userAgent = AppPreferencesManager.isCustomUserAgent(context)
                ? AppPreferencesManager.getCustomUserAgent(context)
                : AppUtils.getDefaultUserAgent(context);
        AppLogger.d("UserAgent:" + userAgent);
        return new DefaultDataSourceFactory(
                context, new DefaultHttpDataSourceFactory(userAgent)
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

        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                washeld = true;
                mWakeLock.release();
            }
            mWakeLock = null;
        }

        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, ExoPlayer.class.getName());
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
    private final class ComponentListener implements
            AudioRendererEventListener, MetadataOutput, Player.EventListener {

        /**
         * Main constructor.
         */
        private ComponentListener() {
            super();
        }

        @Override
        public void onAudioEnabled(@NonNull final DecoderCounters counters) {
            AppLogger.d(LOG_TAG + " audioEnabled");
        }

        @Override
        public void onAudioSessionId(final int audioSessionId) {
            AppLogger.d(LOG_TAG + " audioSessionId:" + audioSessionId);
            initEqualizer(audioSessionId);
        }

        @Override
        public void onMetadata(@NonNull final Metadata metadata) {

            // TODO: REFACTOR THIS QUICK CODE!!

            if (metadata == null) {
                return;
            }
            Metadata.Entry entry;
            for (int i = 0; i < metadata.length(); ++i) {
                entry = metadata.get(i);
                if (entry == null) {
                    return;
                }
                if (entry instanceof IcyInfo) {
                    final IcyInfo info = (IcyInfo) metadata.get(i);
                    if (info != null) {
                        AppLogger.d(LOG_TAG + " IcyInfo title:" + info);
                        String title = info.title;
                        if (TextUtils.isEmpty(title)) {
                            return;
                        }
                        title = title.trim();
                        mMetadataListener.onMetaData(title);
                    }
                }
                if (entry instanceof IcyHeaders) {
                    final IcyHeaders headers = (IcyHeaders) metadata.get(i);
                    if (headers != null) {
                        AppLogger.d(LOG_TAG + " IcyHeaders name:" + headers);
                    }
                }
            }
        }

        @Override
        public void onAudioDecoderInitialized(@NonNull final String decoderName,
                                              final long initializedTimestampMs,
                                              final long initializationDurationMs) {
            AppLogger.d(LOG_TAG + " audioDecoderInitialized " + decoderName);
        }

        @Override
        public void onAudioInputFormatChanged(@NonNull final Format format) {
            AppLogger.d(LOG_TAG + " audioInputFormatChanged:" + format);
        }

        @Override
        public void onAudioDisabled(@NonNull final DecoderCounters counters) {
            AppLogger.d(LOG_TAG + " audioDisabled");
        }

        // Event listener

        @Override
        public void onTimelineChanged(@NonNull final Timeline timeline, int reason) {
            AppLogger.d(LOG_TAG + " onTimelineChanged " + timeline + ", reason " + reason);
            updateProgress();
        }

        @Override
        public void onTracksChanged(@NonNull final TrackGroupArray trackGroups,
                                    @NonNull final TrackSelectionArray trackSelections) {
            //AppLogger.d(LOG_TAG + " onTracksChanged");
        }

        @Override
        public void onPlaybackStateChanged(final int playbackState) {
            AppLogger.d(LOG_TAG + " onPlayerStateChanged to " + playbackState);
            mListener.onPlaybackStateChanged(playbackState);
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    AppLogger.d(LOG_TAG + " STATE_BUFFERING");
                    break;
                case Player.STATE_ENDED:
                    AppLogger.d(LOG_TAG + " STATE_ENDED, userState:" + mUserState);
                    mUpdateProgressHandler.removeCallbacks(mUpdateProgressAction);

                    if (mUserState != UserState.PAUSE && mUserState != UserState.RESET) {
                        prepare(mUri);
                    }
                    break;
                case Player.STATE_IDLE:
                    AppLogger.d(LOG_TAG + " STATE_IDLE");
                    break;
                case Player.STATE_READY:
                    AppLogger.d(LOG_TAG + " STATE_READY");

                    mListener.onPrepared();
                    mNumOfExceptions.set(0);

                    break;
                default:
                    break;
            }

            updateProgress();
        }

        @Override
        public void onPlayerError(@NonNull final ExoPlaybackException exception) {
            AppLogger.e(LOG_TAG + " suspected url: " + mUri);
            AppLogger.e(LOG_TAG + " onPlayerError:\n" + Log.getStackTraceString(exception));
            AppLogger.e(LOG_TAG + " num of exceptions " + mNumOfExceptions.get());
            if (mNumOfExceptions.getAndIncrement() <= MAX_EXCEPTIONS_COUNT) {
                if (exception.getCause() instanceof UnrecognizedInputFormatException) {
                    mListener.onHandledError(exception);
                } else {
                    prepare(mUri);
                }
                return;
            }

            AnalyticsUtils.logException(exception);

            mListener.onError(exception);
        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            AppLogger.e(LOG_TAG + " onPositionDiscontinuity:" + reason);
            updateProgress();
        }

        @Override
        public void onPlaybackParametersChanged(@NonNull final PlaybackParameters playbackParameters) {
            //AppLogger.e(LOG_TAG + " onPlaybackParametersChanged");
        }

        @Override
        public void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
            //AppLogger.e(LOG_TAG + " onRepeatModeChanged");
        }

        @Override
        public void onShuffleModeEnabledChanged(final boolean shuffleModeEnabled) {

        }
    }

    /**
     * Handle playback update progress.
     */
    private void updateProgress() {
        final ExoPlayer exoPlayer = mExoPlayer;
        if (exoPlayer == null) {
            // TODO: Investigate why this callback's loop still exists even after destroy()
            AppLogger.w(LOG_TAG + " update progress with null player");
            return;
        }

        if (exoPlayer.getCurrentTimeline() == Timeline.EMPTY) {
            // TODO: Investigate why an empty timeline is here, probably because it is obsolete reference to player
            AppLogger.w(LOG_TAG + " update progress with empty timeline");
            return;
        }

        final long position = exoPlayer.getCurrentPosition();
        final long bufferedPosition = exoPlayer.getBufferedPosition();
        final long duration = exoPlayer.getDuration();

        AppLogger.d(
                "Pos:" + position
                        + ", bufPos:" + bufferedPosition
                        + ", bufDur:" + (bufferedPosition - position)
        );

        mListener.onProgress(position, bufferedPosition, duration);

        // Cancel any pending updates and schedule a new one if necessary.
        if (mUpdateProgressHandler == null) {
            // TODO: Investigate why this callback's loop still exists even after destroy()
            AppLogger.w(LOG_TAG + " update progress with null handler");
            return;
        }
        mUpdateProgressHandler.removeCallbacks(mUpdateProgressAction);
        final int playbackState = exoPlayer.getPlaybackState();
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            long delayMs;
            if (exoPlayer.getPlayWhenReady() && playbackState == Player.STATE_READY) {
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