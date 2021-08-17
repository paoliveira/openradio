/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.exo

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.yuriy.openradio.shared.exo.ExoPlayerUtils.buildRenderersFactory
import com.yuriy.openradio.shared.exo.ExoPlayerUtils.getDataSourceFactory
import com.yuriy.openradio.shared.exo.ExoPlayerUtils.playWhenReadyChangedToStr
import com.yuriy.openradio.shared.model.media.IEqualizerImpl
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.PlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Wrapper of the ExoPlayer.
 *
 * @param mContext          Application context.
 * @param mListener         Listener for the wrapper's events.
 * @param mMetadataListener Listener for the stream events.
 */
class ExoPlayerOpenRadioImpl(private val mContext: Context,
                             private val mListener: Listener,
                             private val mMetadataListener: MetadataListener) {
    /**
     * Listener for the main public events.
     */
    interface Listener {
        /**
         * Indicates an error while consume stream.
         *
         * @param error Exception associated with error.
         */
        fun onError(error: PlaybackException)

        fun onHandledError(error: PlaybackException)

        /**
         * Indicates that player is ready to play stream.
         */
        fun onPrepared()

        /**
         * @param playbackState
         */
        fun onPlaybackStateChanged(playbackState: Int)

        fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int)
    }

    /**
     * Instance of the ExoPlayer.
     */
    private var mExoPlayer: SimpleExoPlayer

    private val mIsReleased = AtomicBoolean(true)

    /**
     * Equalizer interface.
     */
    private var mEqualizer = IEqualizerImpl.makeInstance(mContext)

    /**
     * Handler for the ExoPlayer to handle events.
     */
    private val mUiScope = CoroutineScope(Dispatchers.Main)

    /**
     * Listener of the ExoPlayer components events.
     */
    private val mComponentListener = ComponentListener()

    /**
     * Current play URI.
     */
    private var mUri = Uri.EMPTY

    /**
     * Enumeration of the state of the last user's action.
     */
    private enum class UserState {
        NONE, PREPARE, PLAY, PAUSE, RESET
    }

    /**
     * State of the last user's action.
     */
    private var mUserState = UserState.NONE

    /**
     * Number of currently detected playback exceptions.
     */
    private val mNumOfExceptions = AtomicInteger(0)

    init {
        val trackSelector = DefaultTrackSelector(mContext)
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(mContext).build()
        val builder = SimpleExoPlayer.Builder(
            mContext, buildRenderersFactory(mContext)
        )
        builder.setTrackSelector(trackSelector)
        builder.setMediaSourceFactory(DefaultMediaSourceFactory(getDataSourceFactory(mContext)!!))
        builder.setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    AppPreferencesManager.getMinBuffer(mContext),
                    AppPreferencesManager.getMaxBuffer(mContext),
                    AppPreferencesManager.getPlayBuffer(mContext),
                    AppPreferencesManager.getPlayBufferRebuffer(mContext)
                )
                .build()
        )
        builder.setWakeMode(C.WAKE_MODE_NETWORK)
        builder.setHandleAudioBecomingNoisy(true)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setFlags(0)
            .setUsage(C.USAGE_MEDIA)
            .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
            .build()
        builder.setAudioAttributes(audioAttributes, true)
        mExoPlayer = builder.build()
        mIsReleased.set(false)
        mEqualizer.init(mExoPlayer.audioSessionId)
    }

    /**
     * Prepare player to play URI.
     *
     * @param uri URI to play.
     */
    fun prepare(uri: Uri?) {
        if (uri == null) {
            return
        }
        mUserState = UserState.PREPARE
        mUri = uri
        if (!mIsReleased.get()) {
            mExoPlayer.addListener(mComponentListener)
            mExoPlayer.playWhenReady = true
            mExoPlayer.setMediaItem(MediaItem.Builder().setUri(uri).build())
            mExoPlayer.prepare()
        }
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    fun setVolume(value: Float) {
        AppLogger.d("$LOG_TAG volume to $value")
        if (!mIsReleased.get()) {
            mExoPlayer.volume = value
        }
    }

    /**
     * Play current stream based on the URI passed to [.prepare] method.
     */
    fun play() {
        AppLogger.d("$LOG_TAG play")
        mUserState = UserState.PLAY
        prepare(mUri)
    }

    /**
     * Pause current stream based on the URI passed to [.prepare] )} method.
     */
    fun pause() {
        AppLogger.d("$LOG_TAG pause")
        mUserState = UserState.PAUSE
        if (!mIsReleased.get()) {
            mExoPlayer.stop()
            mExoPlayer.playWhenReady = false
        }
    }

    /**
     * Returns a value corresponded to whether or not current stream is playing.
     *
     * @return `true` in case of current stream is playing, `false` otherwise.
     */
    val isPlaying: Boolean
        get() {
            val isPlaying = !mIsReleased.get() && mExoPlayer.playWhenReady
            AppLogger.d("$LOG_TAG is playing:$isPlaying")
            return isPlaying
        }

    /**
     * Resets the player to its uninitialized state.
     */
    fun reset() {
        AppLogger.d("$LOG_TAG reset")
        mUserState = UserState.RESET
        if (!mIsReleased.get()) {
            mExoPlayer.stop()
        }
    }

    /**
     * Release the player and associated resources.
     */
    fun release() {
        if (mIsReleased.get()) {
            AppLogger.d("$LOG_TAG ExoPlayer impl already released")
            return
        }
        mUiScope.launch { releaseIntrnl() }
    }

    fun loadEqualizerState() {
        mEqualizer.loadState()
    }

    private fun releaseIntrnl() {
        if (mIsReleased.get()) {
            AppLogger.d("$LOG_TAG ExoPlayer impl already released")
            return
        }
        mEqualizer.deinit()
        mExoPlayer.removeListener(mComponentListener)
        reset()
        mExoPlayer.release()
        mIsReleased.set(true)
    }

    /**
     * Listener class for the players components events.
     */
    private inner class ComponentListener : Player.Listener {

        /**
         * String tag to use in logs.
         */
        private val mLogTag = "ComponentListener"

        private var mRawMetadata = AppUtils.EMPTY_STRING

        override fun onMetadata(metadata: Metadata) {

            // TODO: REFACTOR THIS QUICK CODE!!
            var entry: Metadata.Entry
            for (i in 0 until metadata.length()) {
                entry = metadata[i]
                AppLogger.d("$mLogTag Metadata entry:$entry")
                if (entry is IcyInfo) {
                    val info = metadata[i] as IcyInfo
                    AppLogger.d("$mLogTag IcyInfo title:$info")
                    var title = info.title
                    if (title.isNullOrEmpty()) {
                        return
                    }
                    title = title.trim { it <= ' ' }
                    if (title == mRawMetadata) {
                        return
                    }
                    mRawMetadata = title
                    mMetadataListener.onMetaData(title)
                }
                if (entry is IcyHeaders) {
                    val headers = metadata[i] as IcyHeaders
                    AppLogger.d("$mLogTag IcyHeaders name:$headers")
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // playbackState is one of the {@link Player}.STATE_ constants.
            AppLogger.d(
                "$mLogTag OnPlaybackStateChanged to ${PlayerUtils.playerStateToString(playbackState)}," +
                    " userState:$mUserState"
            )
            mListener.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (mUserState != UserState.PAUSE && mUserState != UserState.RESET) {
                        prepare(mUri)
                    }
                }
                Player.STATE_READY -> {
                    mListener.onPrepared()
                    mNumOfExceptions.set(0)
                }
                else -> {
                    // Other cases here
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            AppLogger.d("$mLogTag OnIsPlayingChanged to $isPlaying")
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            // playbackState is one of the {@link Player}.STATE_ constants.
            AppLogger.d(
                "$mLogTag OnPlayerStateChanged to $playWhenReady," +
                    " player state:${PlayerUtils.playerStateToString(playbackState)}"
            )
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            AppLogger.d(
                "$mLogTag OnPlayWhenReadyChanged to $playWhenReady, reason:${playWhenReadyChangedToStr(reason)}"
            )
            mListener.onPlayWhenReadyChanged(playWhenReady, reason)
        }

        override fun onPlayerError(exception: PlaybackException) {
            AppLogger.e("$mLogTag suspected url: $mUri")
            AppLogger.e("$mLogTag onPlayerError", exception)
            AppLogger.e("$mLogTag num of exceptions ${mNumOfExceptions.get()}")
            val cause = exception.cause
            AppLogger.e("$mLogTag cause: $cause")
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                mListener.onError(exception)
                return
            }
            if (mNumOfExceptions.getAndIncrement() <= MAX_EXCEPTIONS_COUNT) {
                if (cause is UnrecognizedInputFormatException) {
                    mListener.onHandledError(exception)
                } else {
                    prepare(mUri)
                }
                return
            }
            mListener.onError(exception)
        }
    }

    companion object {
        /**
         * String tag to use in logs.
         */
        private const val LOG_TAG = "ExoPlayerORImpl"

        /**
         *
         */
        private const val MAX_EXCEPTIONS_COUNT = 5
    }
}
