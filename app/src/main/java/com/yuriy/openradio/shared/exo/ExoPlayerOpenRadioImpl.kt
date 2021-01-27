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
package com.yuriy.openradio.shared.exo

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.DiscontinuityReason
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.icy.IcyHeaders
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.yuriy.openradio.shared.exo.ExoPlayerUtils.buildRenderersFactory
import com.yuriy.openradio.shared.exo.ExoPlayerUtils.getDataSourceFactory
import com.yuriy.openradio.shared.model.media.IEqualizerImpl
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getMaxBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getMinBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getPlayBuffer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager.getPlayBufferRebuffer
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.w
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * Wrapper of the ExoPlayer.
 *
 * @param mContext         Application context.
 * @param listener         Listener for the wrapper's events.
 * @param metadataListener Listener for the stream events.
 */
class ExoPlayerOpenRadioImpl(private val mContext: Context,
                             listener: Listener,
                             metadataListener: MetadataListener) {
    /**
     * Listener for the main public events.
     */
    interface Listener {
        /**
         * Indicates an error while consume stream.
         *
         * @param error Exception associated with error.
         */
        fun onError(error: ExoPlaybackException)

        fun onHandledError(error: ExoPlaybackException)

        /**
         * Indicates that player is ready to play stream.
         */
        fun onPrepared()

        /**
         * Currently playing playback progress.
         *
         * @param position         playback position in the current window, in milliseconds.
         * @param bufferedPosition Estimate of the position in the current window up to which data is buffered,
         * in milliseconds.
         * @param duration         Duration of the current window in milliseconds,
         * or C.TIME_UNSET if the duration is not known.
         */
        fun onProgress(position: Long, bufferedPosition: Long, duration: Long)

        /**
         * @param playbackState
         */
        fun onPlaybackStateChanged(playbackState: Int)
    }

    /**
     * Instance of the ExoPlayer.
     */
    private var mExoPlayer: SimpleExoPlayer?

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
    private val mComponentListener: ComponentListener

    private val mAudioListener: AudioListenerImpl

    /**
     * Instance of the ExoPlayer wrapper events.
     */
    private val mListener: Listener

    /**
     * Current play URI.
     */
    private var mUri: Uri? = null

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
     * Runnable implementation to handle playback progress.
     */
    private var mUpdateProgressAction: Runnable? = Runnable { updateProgress() }

    /**
     * Handler to handle playback progress runnable.
     */
    private var mUpdateProgressHandler: Handler? = Handler()
    private val mMetadataListener: MetadataListener

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
        if (mExoPlayer != null) {
            mExoPlayer!!.addListener(mComponentListener)
            mExoPlayer!!.addMetadataOutput(mComponentListener)
            mExoPlayer!!.addAudioListener(mAudioListener)
            mExoPlayer!!.playWhenReady = true
            mExoPlayer!!.setMediaItem(MediaItem.Builder().setUri(uri).build())
            mExoPlayer!!.prepare()
        }
    }

    /**
     * Sets volume.
     *
     * @param value Value of the volume.
     */
    fun setVolume(value: Float) {
        d("$LOG_TAG volume to $value")
        mExoPlayer!!.volume = value
    }

    /**
     * Play current stream based on the URI passed to [.prepare] method.
     */
    fun play() {
        d("$LOG_TAG play")
        mUserState = UserState.PLAY
        prepare(mUri)
    }

    /**
     * Pause current stream based on the URI passed to [.prepare] )} method.
     */
    fun pause() {
        d("$LOG_TAG pause")
        mUserState = UserState.PAUSE
        if (mExoPlayer != null) {
            mExoPlayer!!.stop()
            mExoPlayer!!.playWhenReady = false
        }
    }

    /**
     * Returns a value corresponded to whether or not current stream is playing.
     *
     * @return `true` in case of current stream is playing, `false` otherwise.
     */
    val isPlaying: Boolean
        get() {
            val isPlaying = mExoPlayer != null && mExoPlayer!!.playWhenReady
            d("$LOG_TAG is playing:$isPlaying")
            return isPlaying
        }

    /**
     * Resets the player to its uninitialized state.
     */
    fun reset() {
        d("$LOG_TAG reset")
        mUserState = UserState.RESET
        mEqualizer.deinit()
        if (mExoPlayer != null) {
            mExoPlayer!!.stop()
        }
    }

    /**
     * Release the player and associated resources.
     */
    fun release() {
        if (mExoPlayer == null) {
            d("$LOG_TAG ExoPlayer impl already released")
            return
        }
        mUiScope.launch { releaseIntrnl() }
    }

    fun loadEqualizerState() {
        mEqualizer.loadState()
    }

    private fun releaseIntrnl() {
        if (mExoPlayer == null) {
            d("$LOG_TAG ExoPlayer impl already released")
            return
        }
        mEqualizer.deinit()
        mExoPlayer!!.removeListener(mComponentListener)
        mExoPlayer!!.removeMetadataOutput(mComponentListener)
        mExoPlayer!!.removeAudioListener(mAudioListener)
        mUpdateProgressHandler!!.removeCallbacks(mUpdateProgressAction!!)
        reset()
        mExoPlayer!!.release()
        mExoPlayer = null
        mUpdateProgressHandler = null
        mUpdateProgressAction = null
    }

    /**
     * Listener class for the players components events.
     */
    private inner class ComponentListener : MetadataOutput, Player.EventListener {

        /**
         * String tag to use in logs.
         */
        private val mLogTag = ComponentListener::class.java.simpleName
        override fun onMetadata(metadata: Metadata) {

            // TODO: REFACTOR THIS QUICK CODE!!
            var entry: Metadata.Entry
            for (i in 0 until metadata.length()) {
                entry = metadata[i]
                d("$mLogTag Metadata entry:$entry")
                if (entry is IcyInfo) {
                    val info = metadata[i] as IcyInfo
                    d("$mLogTag IcyInfo title:$info")
                    var title = info.title
                    if (title.isNullOrEmpty()) {
                        return
                    }
                    title = title.trim { it <= ' ' }
                    mMetadataListener.onMetaData(title)
                }
                if (entry is IcyHeaders) {
                    val headers = metadata[i] as IcyHeaders
                    d("$mLogTag IcyHeaders name:$headers")
                }
            }
        }

        // Event listener
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            d("$mLogTag onTimelineChanged $timeline, reason $reason")
            updateProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            d("$mLogTag onPlayerStateChanged to $playbackState")
            mListener.onPlaybackStateChanged(playbackState)
            when (playbackState) {
                Player.STATE_BUFFERING -> d("$mLogTag STATE_BUFFERING")
                Player.STATE_ENDED -> {
                    d("$mLogTag STATE_ENDED, userState:$mUserState")
                    mUpdateProgressHandler!!.removeCallbacks(mUpdateProgressAction!!)
                    if (mUserState != UserState.PAUSE && mUserState != UserState.RESET) {
                        prepare(mUri)
                    }
                }
                Player.STATE_IDLE -> d("$mLogTag STATE_IDLE")
                Player.STATE_READY -> {
                    d("$mLogTag STATE_READY")
                    mListener.onPrepared()
                    mNumOfExceptions.set(0)
                }
                else -> {
                }
            }
            updateProgress()
        }

        override fun onPlayerError(exception: ExoPlaybackException) {
            e("$mLogTag suspected url: $mUri")
            e("$mLogTag onPlayerError: ${Log.getStackTraceString(exception)}")
            e(mLogTag + " num of exceptions " + mNumOfExceptions.get())
            if (mNumOfExceptions.getAndIncrement() <= MAX_EXCEPTIONS_COUNT) {
                if (exception.cause is UnrecognizedInputFormatException) {
                    mListener.onHandledError(exception)
                } else {
                    prepare(mUri)
                }
                return
            }
            logException(exception)
            mListener.onError(exception)
        }

        override fun onPositionDiscontinuity(@DiscontinuityReason reason: Int) {
            e("$mLogTag onPositionDiscontinuity:$reason")
            updateProgress()
        }
    }

    /**
     * Listener class for the players audio events.
     */
    private inner class AudioListenerImpl : AudioListener {

        override fun onAudioSessionId(audioSessionId: Int) {
            d("onAudioSessionId:$audioSessionId")
            mEqualizer.init(audioSessionId)
        }
    }

    /**
     * Handle playback update progress.
     */
    private fun updateProgress() {
        val exoPlayer: ExoPlayer? = mExoPlayer
        if (exoPlayer == null) {
            // TODO: Investigate why this callback's loop still exists even after destroy()
            w("$LOG_TAG update progress with null player")
            return
        }
        if (exoPlayer.currentTimeline === Timeline.EMPTY) {
            // TODO: Investigate why an empty timeline is here, probably because it is obsolete reference to player
            w("$LOG_TAG update progress with empty timeline")
            return
        }
        val position = exoPlayer.currentPosition
        val bufferedPosition = exoPlayer.bufferedPosition
        val duration = exoPlayer.duration
        d(
                "Pos:" + position
                        + ", bufPos:" + bufferedPosition
                        + ", bufDur:" + (bufferedPosition - position)
        )
        mListener.onProgress(position, bufferedPosition, duration)

        // Cancel any pending updates and schedule a new one if necessary.
        if (mUpdateProgressHandler == null) {
            // TODO: Investigate why this callback's loop still exists even after destroy()
            w("$LOG_TAG update progress with null handler")
            return
        }
        mUpdateProgressHandler!!.removeCallbacks(mUpdateProgressAction!!)
        val playbackState = exoPlayer.playbackState
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs: Long
            if (exoPlayer.playWhenReady && playbackState == Player.STATE_READY) {
                delayMs = 1000 - position % 1000
                if (delayMs < 200) {
                    delayMs += 1000
                }
            } else {
                delayMs = 1000
            }
            mUpdateProgressHandler!!.postDelayed(mUpdateProgressAction!!, delayMs)
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

    init {
        mComponentListener = ComponentListener()
        mAudioListener = AudioListenerImpl()
        mListener = listener
        mMetadataListener = metadataListener
        val trackSelector = DefaultTrackSelector(mContext)
        trackSelector.parameters = ParametersBuilder(mContext).build()
        val builder = SimpleExoPlayer.Builder(
                mContext, buildRenderersFactory(mContext)
        )
        builder.setTrackSelector(trackSelector)
        builder.setMediaSourceFactory(DefaultMediaSourceFactory(getDataSourceFactory(mContext)!!))
        builder.setLoadControl(
                DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                getMinBuffer(mContext),
                                getMaxBuffer(mContext),
                                getPlayBuffer(mContext),
                                getPlayBufferRebuffer(mContext)
                        )
                        .build()
        )
        builder.setWakeMode(C.WAKE_MODE_NETWORK)
        builder.setHandleAudioBecomingNoisy(true)
        builder.setAudioAttributes(AudioAttributes.DEFAULT, true)
        mExoPlayer = builder.build()
    }
}
