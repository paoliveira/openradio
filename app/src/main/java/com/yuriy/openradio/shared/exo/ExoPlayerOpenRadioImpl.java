/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.exo;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.icy.IcyHeaders;
import com.google.android.exoplayer2.metadata.icy.IcyInfo;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.model.storage.EqualizerStorage;
import com.yuriy.openradio.shared.model.translation.EqualizerJsonStateSerializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateDeserializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateJsonDeserializer;
import com.yuriy.openradio.shared.model.translation.EqualizerStateSerializer;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.vo.EqualizerState;

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
    private SimpleExoPlayer mExoPlayer;
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
     * Current play URI.
     */
    private Uri mUri;

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

        final DefaultTrackSelector trackSelector = new DefaultTrackSelector(mContext);
        trackSelector.setParameters(new DefaultTrackSelector.ParametersBuilder(mContext).build());

        final SimpleExoPlayer.Builder builder = new SimpleExoPlayer.Builder(
                mContext, ExoPlayerUtils.buildRenderersFactory(mContext)
        );
        builder.setTrackSelector(trackSelector);
        builder.setMediaSourceFactory(new DefaultMediaSourceFactory(ExoPlayerUtils.getDataSourceFactory(mContext)));
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
        builder.setWakeMode(C.WAKE_MODE_NETWORK);
        builder.setHandleAudioBecomingNoisy(true);
        builder.setAudioAttributes(AudioAttributes.DEFAULT, true);

        mExoPlayer = builder.build();
        mExoPlayer.addListener(mComponentListener);
        mExoPlayer.addMetadataOutput(mComponentListener);
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
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(true);
            mExoPlayer.setMediaItem(new MediaItem.Builder().setUri(mUri).build());
            mExoPlayer.prepare();
        }
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    public void setVolume(final float value) {
        AppLogger.d(LOG_TAG + " volume to " + value);
        mExoPlayer.setVolume(value);
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
    }

    /**
     * Listener class for the players components events.
     */
    private final class ComponentListener implements MetadataOutput, Player.EventListener {

        /**
         * String tag to use in logs.
         */
        private final String LOG_TAG = ComponentListener.class.getSimpleName();

        /**
         * Main constructor.
         */
        private ComponentListener() {
            super();
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

        // Event listener

        @Override
        public void onTimelineChanged(@NonNull final Timeline timeline, int reason) {
            AppLogger.d(LOG_TAG + " onTimelineChanged " + timeline + ", reason " + reason);
            updateProgress();
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
                    initEqualizer(mExoPlayer.getAudioSessionId());

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
