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
import android.webkit.MimeTypeMap
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.gms.cast.framework.CastContext
import com.yuriy.openradio.shared.model.media.IEqualizerImpl
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.PlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
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
class OpenRadioPlayer(
    private val mContext: Context,
    private val mListener: Listener,
    private val mMetadataListener: MetadataListener
) {
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
     * The current player will either be an ExoPlayer (for local playback)
     * or a CastPlayer (for remote playback through a Cast device).
     */
    private lateinit var mCurrentPlayer: Player

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

    /**
     * If Cast is available, create a CastPlayer to handle communication with a Cast session.
     */
    private val mCastPlayer: CastPlayer? by lazy {
        AppLogger.i("$LOG_TAG init CastPlayer")
        try {
            val castContext = CastContext.getSharedInstance(mContext)
            CastPlayer(castContext).apply {
                setSessionAvailabilityListener(OpenRadioCastSessionAvailabilityListener())
                addListener(mComponentListener)
            }
        } catch (e: Exception) {
            // We wouldn't normally catch the generic `Exception` however
            // calling `CastContext.getSharedInstance` can throw various exceptions, all of which
            // indicate that Cast is unavailable.
            // Related internal bug b/68009560.
            AppLogger.e(
                "Cast is not available on this device. " +
                        "Exception thrown when attempting to obtain CastContext", e
            )
            null
        }
    }

    private val mExoPlayer: ExoPlayer by lazy {
        AppLogger.i("$LOG_TAG init ExoPlayer")
        val trackSelector = DefaultTrackSelector(mContext)
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(mContext).build()
        val builder = ExoPlayer.Builder(
            mContext, ExoPlayerUtils.buildRenderersFactory(mContext)
        )
        builder.setTrackSelector(trackSelector)
        builder.setMediaSourceFactory(DefaultMediaSourceFactory(ExoPlayerUtils.getDataSourceFactory(mContext)!!))
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
        val exoPlayer = builder.build()
        exoPlayer.addListener(mComponentListener)
        exoPlayer.playWhenReady = true
        exoPlayer
    }

    init {
        switchToPlayer(
            previousPlayer = null,
            newPlayer = if (mCastPlayer?.isCastSessionAvailable == true) mCastPlayer!! else mExoPlayer
        )
        mEqualizer.init(mExoPlayer.audioSessionId)
    }

    /**
     * Prepare player to play URI.
     *
     * @param uri URI to play.
     */
    fun prepare(uri: Uri?) {
        AppLogger.d("$LOG_TAG prepare $uri, cast[${mCastPlayer?.isCastSessionAvailable}]")
        if (uri == null) {
            return
        }
        mComponentListener.clearMetadata()
        mUserState = UserState.PREPARE
        mUri = uri
        mCurrentPlayer.playWhenReady = true
        val item = MediaItem.Builder()
            .setUri(mUri)
            .setMimeType(getMimeTypeFromUri(mUri))
            .build()
        mCurrentPlayer.setMediaItem(item)
        if (mCurrentPlayer == mExoPlayer) {
            mCurrentPlayer.prepare()
        }
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    fun setVolume(value: Float) {
        AppLogger.d("$LOG_TAG volume to $value")
        mCurrentPlayer.volume = value
    }

    /**
     * Play current stream based on the URI passed to [prepare] method.
     */
    fun play() {
        AppLogger.d("$LOG_TAG play")
        mUserState = UserState.PLAY
        prepare(mUri)
    }

    /**
     * Pause current stream based on the URI passed to [prepare] method.
     */
    fun pause() {
        AppLogger.d("$LOG_TAG pause")
        mUserState = UserState.PAUSE
        mCurrentPlayer.stop()
        mCurrentPlayer.playWhenReady = false
    }

    /**
     * Returns a value corresponded to whether or not current stream is playing.
     *
     * @return `true` in case of current stream is playing, `false` otherwise.
     */
    val isPlaying: Boolean
        get() {
            val isPlaying = mCurrentPlayer.playWhenReady
            AppLogger.d("$LOG_TAG is playing:$isPlaying")
            return isPlaying
        }

    /**
     * Resets the player to its uninitialized state.
     */
    fun reset() {
        mUserState = UserState.RESET
        stop()
    }

    /**
     * Release the player and associated resources.
     */
    fun release() {
        mUiScope.launch { releaseIntrnl() }
    }

    fun loadEqualizerState() {
        mEqualizer.loadState()
    }

    private fun stop() {
        mCurrentPlayer.clearMediaItems()
        mCurrentPlayer.stop()
    }

    private fun switchToPlayer(previousPlayer: Player?, newPlayer: Player) {
        AppLogger.i("$LOG_TAG prev player: $previousPlayer")
        AppLogger.i("$LOG_TAG new  player: $newPlayer")
        if (previousPlayer == newPlayer) {
            return
        }
        mCurrentPlayer = newPlayer
        AppLogger.i("$LOG_TAG curr player: $mCurrentPlayer")
        if (previousPlayer != null) {
            // We are joining a playback session. Loading the session from the new player is
            // not supported, so we stop playback.
            val volume = previousPlayer.volume
            val playWhenReady = previousPlayer.playWhenReady
            stop()
            mCurrentPlayer.playWhenReady = playWhenReady
            prepare(mUri)
            setVolume(volume)
        }
        previousPlayer?.stop()
    }

    private fun releaseIntrnl() {
        mEqualizer.deinit()
        reset()
        mCurrentPlayer.release()
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

        fun clearMetadata() {
            mRawMetadata = AppUtils.EMPTY_STRING
        }

        override fun onMetadata(metadata: Metadata) {

            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                var title = AppUtils.EMPTY_STRING
                // See https://en.wikipedia.org/wiki/ID3#ID3v2_frame_specification
                val msg = "Metadata entry:$entry"
                AppLogger.d(msg)
                when (entry) {
                    is IcyInfo -> {
                        title = entry.title ?: AppUtils.EMPTY_STRING
                    }
                    is TextInformationFrame -> {
                        when (entry.id) {
                            ExoPlayerUtils.METADATA_ID_TT2,
                            ExoPlayerUtils.METADATA_ID_TIT2 -> {
                                title = entry.value
                            }
                        }
                    }
                    else -> {
                        AnalyticsUtils.logMetadata(msg)
                    }
                }
                if (title.isEmpty()) {
                    continue
                }
                title = title.trim { it <= ' ' }
                if (title == mRawMetadata) {
                    continue
                }
                mRawMetadata = title
                AppLogger.d("Metadata final title:$title")
                mMetadataListener.onMetaData(title)
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
                "$mLogTag OnPlayWhenReadyChanged to $playWhenReady, reason:${
                    ExoPlayerUtils.playWhenReadyChangedToStr(reason)
                }"
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

    private inner class OpenRadioCastSessionAvailabilityListener : SessionAvailabilityListener {

        /**
         * Called when a Cast session has started and the user wishes to control playback on a
         * remote Cast receiver rather than play audio locally.
         */
        override fun onCastSessionAvailable() {
            switchToPlayer(mCurrentPlayer, mCastPlayer!!)
        }

        /**
         * Called when a Cast session has ended and the user wishes to control playback locally.
         */
        override fun onCastSessionUnavailable() {
            switchToPlayer(mCurrentPlayer, mExoPlayer)
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

        /**
         * Utility method to extract stream mime type from the stream extension (if exists).
         */
        fun getMimeTypeFromUri(uri: Uri): String {
            val mime: String =
                when (MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase(Locale.getDefault())) {
                    "aacp", "aac" -> MimeTypes.AUDIO_AAC
                    "ac3" -> MimeTypes.AUDIO_AC3
                    "ac4" -> MimeTypes.AUDIO_AC4
                    "flac" -> MimeTypes.AUDIO_FLAC
                    "mp3" -> MimeTypes.AUDIO_MPEG
                    "ogg", "oga" -> MimeTypes.AUDIO_OGG
                    "opus" -> MimeTypes.AUDIO_OPUS
                    "wav" -> MimeTypes.AUDIO_WAV
                    "weba" -> MimeTypes.AUDIO_WEBM
                    "m4a" -> "audio/m4a"
                    "m3u", "m3u8" -> MimeTypes.APPLICATION_M3U8
                    "ts" -> MimeTypes.VIDEO_MP2T
                    else -> MimeTypes.AUDIO_UNKNOWN
                }
            if (mime == MimeTypes.AUDIO_UNKNOWN) {
                AnalyticsUtils.logUnknownMime(uri.toString())
            }
            AppLogger.d("$LOG_TAG mime type:$mime")
            return mime
        }
    }
}
