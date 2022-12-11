/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.app.Notification
import android.content.Context
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.webkit.MimeTypeMap
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.icy.IcyInfo
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastContext
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import com.yuriy.openradio.shared.exo.extentions.toMediaItemMetadata
import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.notification.MediaNotificationManager
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.*
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.getStreamUrl
import com.yuriy.openradio.shared.vo.getStreamUrlFixed
import com.yuriy.openradio.shared.vo.toMediaItemPlayable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 11/04/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Wrapper over ExoPlayer.
 *
 * @param mContext  Application context.
 * @param mListener Listener for the wrapper's events.
 */
class OpenRadioPlayer(
    private val mContext: Context,
    private val mListener: Listener,
    private val mEqualizerLayer: EqualizerLayer
) {
    /**
     * Listener for the main public events.
     */
    interface Listener {

        fun onError(error: PlaybackException)

        fun onHandledError(error: PlaybackException)

        fun onReady()

        fun onIndexOnQueueChanges(value: Int)

        fun onPlaybackStateChanged(playbackState: Int)

        fun onStartForeground(notificationId: Int, notification: Notification)

        fun onStopForeground(removeNotification: Boolean)

        fun onCloseApp()
    }

    /**
     * The current player will either be an ExoPlayer (for local playback)
     * or a CastPlayer (for remote playback through a Cast device).
     */
    private lateinit var mCurrentPlayer: Player

    private var mMediaItems = Collections.synchronizedList<MediaItem>(ArrayList())

    /**
     * Handler for the ExoPlayer to handle events.
     */
    private val mUiScope = CoroutineScope(Dispatchers.Main)

    /**
     * Listener of the ExoPlayer components events.
     */
    private val mComponentListener = ComponentListener()

    /**
     * Current index of the item in play list.
     */
    private var mIndex = 0

    /**
     * Number of currently detected playback exceptions.
     */
    private val mNumOfExceptions = AtomicInteger(0)

    private lateinit var mNotificationManager: MediaNotificationManager

    private lateinit var mMediaSessionConnector: MediaSessionConnector

    private var mIsForegroundService = false

    private var mStreamMetadata = AppUtils.EMPTY_STRING

    private val mBufferingLabel = mContext.getString(R.string.buffering_infinite)

    private val mLiveStreamLabel = mContext.getString(R.string.media_description_default)

    @Volatile
    private var mStoppedByNetwork = false

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
        trackSelector.parameters = DefaultTrackSelector.Parameters.Builder(mContext).build()
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
        // Do not handle this via player - it handles OFF but doesnt ON.
        //builder.setHandleAudioBecomingNoisy(true)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
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
            newPlayer = if (DependencyRegistryCommon.isCastAvailable) mCastPlayer!! else mExoPlayer
        )
        mEqualizerLayer.init(mExoPlayer.audioSessionId)
    }

    fun onCreate(sessionToken: MediaSessionCompat.Token, mediaSessionConnector: MediaSessionConnector) {
        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        mNotificationManager = MediaNotificationManager(
            mContext,
            sessionToken,
            PlayerNotificationListener(),
            NotificationListener()
        )
        mNotificationManager.showNotificationForPlayer(mCurrentPlayer)
        mMediaSessionConnector = mediaSessionConnector
        mMediaSessionConnector.setPlayer(mCurrentPlayer)
        mMediaSessionConnector.setMediaMetadataProvider { player ->
            ExoPlayerUtils.getMediaMetadataCompat(player.currentMediaItem, mStreamMetadata)
        }
    }

    /**
     * Prepare player to play URI.
     */
    fun prepare(mediaId: String) {
        AppLogger.d("$LOG_TAG prepare $mediaId, cast[${mCastPlayer?.isCastSessionAvailable}]")
        if (this::mMediaSessionConnector.isInitialized) {
            mMediaSessionConnector.invalidateMediaSessionMetadata()
        }
        mNumOfExceptions.set(0)
        mIndex = 0
        synchronized(mMediaItems) {
            for ((index, mediaItem) in mMediaItems.withIndex()) {
                if (mediaItem.mediaId == mediaId) {
                    mIndex = index
                    break
                }
            }
        }
        prepareWithList(mIndex)
    }

    @Synchronized
    fun clearItems() {
        AppLogger.d("$LOG_TAG clear items")
        mMediaItems.clear()
        AppLogger.d("$LOG_TAG has ${mMediaItems.size} items")
    }

    @Synchronized
    fun add(item: RadioStation, position: Int) {
        AppLogger.d("$LOG_TAG add item")
        for (i in mMediaItems) {
            if (i.mediaId == item.id) {
                AppLogger.w("$LOG_TAG skip add duplicate item")
                return
            }
        }
        mMediaItems.add(position, rsToPlayerMediaItem(mContext, item))
        AppLogger.d("$LOG_TAG has ${mMediaItems.size} items")
    }

    @Synchronized
    fun addItems(list: List<RadioStation>) {
        AppLogger.d("$LOG_TAG add ${list.size} items")
        mMediaItems.addAll(rssToPlayerMediaItems(mContext, list))
        AppLogger.d("$LOG_TAG has ${mMediaItems.size} items")
    }

    @Synchronized
    fun updateItem(item: RadioStation) {
        AppLogger.d("$LOG_TAG update $item")
        for ((idx, i) in mMediaItems.withIndex()) {
            if (i.mediaId == item.id) {
                mMediaItems[idx] = rsToPlayerMediaItem(mContext, item)
                return
            }
        }
    }

    fun mediaItemCount(): Int {
        return mCurrentPlayer.mediaItemCount
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
        if (isPlaying) {
            return
        }
        AppLogger.d("$LOG_TAG play")
        prepareWithList(mIndex)
    }

    /**
     * Pause current stream based on the URI passed to [prepare] method.
     */
    fun pause() {
        if (isPlaying.not()) {
            return
        }
        AppLogger.d("$LOG_TAG pause")
        mCurrentPlayer.stop()
        mCurrentPlayer.playWhenReady = false
    }

    fun skipToPrevious(player: Player) {
        mIndex -= 1
    }

    fun skipToQueueItem(player: Player, id: Long) {

    }

    fun skipToNext(player: Player) {
        mIndex += 1
    }

    /**
     * Returns a value corresponded to whether or not current stream is playing.
     *
     * @return `true` in case of current stream is playing, `false` otherwise.
     */
    val isPlaying: Boolean
        get() {
            val isPlaying = mCurrentPlayer.isPlaying || mCurrentPlayer.isLoading ||
                    mCurrentPlayer.playbackState == Player.STATE_BUFFERING ||
                    mCurrentPlayer.playbackState == Player.STATE_READY
            AppLogger.d("$LOG_TAG is playing:$isPlaying")
            return isPlaying
        }

    /**
     * Resets the player to its uninitialized state.
     */
    fun reset() {
        AppLogger.d("$LOG_TAG reset")
        stopCurrentPlayer()
    }

    /**
     * Release the player and associated resources.
     */
    fun release() {
        mUiScope.launch { releaseIntrnl() }
    }

    fun isStoppedByNetwork(): Boolean {
        return mStoppedByNetwork
    }

    private fun prepareWithList(index: Int) {
        try {
            mCurrentPlayer.playWhenReady = true
            mCurrentPlayer.setMediaItems(mMediaItems, index, 0)
            mCurrentPlayer.prepare()
        } catch (e: IllegalSeekPositionException) {
            AnalyticsUtils.logIllegalSeekPosition(mMediaItems.size, e)
        }
    }

    private fun stopCurrentPlayer() {
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
            stopCurrentPlayer()
            prepareWithList(mIndex)
        }
        setVolume(
            AppPreferencesManager.getMasterVolume(
                mContext,
                OpenRadioService.MASTER_VOLUME_DEFAULT
            ).toFloat() / 100.0f
        )
        if (this::mMediaSessionConnector.isInitialized) {
            mMediaSessionConnector.setPlayer(mCurrentPlayer)
        }
        previousPlayer?.stop(/* reset= */true)
    }

    private fun releaseIntrnl() {
        AppLogger.d("$LOG_TAG release intrl")
        mEqualizerLayer.deinit()
        reset()
        mCurrentPlayer.release()
    }

    private fun updateStreamMetadata(msg: String) {
        mStreamMetadata = msg
        mMediaSessionConnector.invalidateMediaSessionMetadata()
    }

    /**
     * Listener class for the players components events.
     */
    private inner class ComponentListener : Player.Listener {

        @Deprecated("Deprecated in Java")
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            AppLogger.d(
                "$LOG_TAG player state changed $playbackState"
            )
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            AppLogger.d(
                "$LOG_TAG player error changed $error"
            )
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
                if (title == mStreamMetadata) {
                    continue
                }
                updateStreamMetadata(title)
            }
        }

        override fun onPlaybackStateChanged(playerState: Int) {
            AppLogger.d(
                "$LOG_TAG playback state changed ${PlayerUtils.playerStateToString(playerState)}"
            )
            mListener.onPlaybackStateChanged(playerState)
            when (playerState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    if (playerState == Player.STATE_BUFFERING) {
                        updateStreamMetadata(mBufferingLabel)
                    }
                    if (playerState == Player.STATE_READY) {
                        if (!mCurrentPlayer.playWhenReady) {
                            // If playback is paused we remove the foreground state which allows the
                            // notification to be dismissed. An alternative would be to provide a
                            // "close" button in the notification which stops playback and clears
                            // the notification.
                            mListener.onStopForeground(false)
                            mIsForegroundService = false
                        }
                    }
                    if (this@OpenRadioPlayer::mNotificationManager.isInitialized) {
                        mNotificationManager.showNotificationForPlayer(mCurrentPlayer)
                    }
                    mListener.onReady()
                }
                Player.STATE_IDLE -> {
                    if (this@OpenRadioPlayer::mNotificationManager.isInitialized) {
                        mNotificationManager.hideNotification()
                    }
                }
                else -> {
                    if (this@OpenRadioPlayer::mNotificationManager.isInitialized) {
                        mNotificationManager.hideNotification()
                    }
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (player.isPlaying && mStreamMetadata == mBufferingLabel) {
                updateStreamMetadata(mLiveStreamLabel)
            }
            if (events.contains(Player.EVENT_TIMELINE_CHANGED).not()
                && (events.contains(Player.EVENT_POSITION_DISCONTINUITY)
                || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED))
            ) {
                val index = if (mMediaItems.isNotEmpty()) {
                    Util.constrainValue(
                        player.currentMediaItemIndex,
                        /* min= */ 0,
                        /* max= */ mMediaItems.size - 1
                    )
                } else -1
                if (index != -1) {
                    mIndex = index
                    mListener.onIndexOnQueueChanges(mIndex)
                }
            }
        }

        override fun onPlayerError(exception: PlaybackException) {
            AppLogger.e("$LOG_TAG onPlayerError [${mNumOfExceptions.get()}]", exception)
            if (exception.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                updateStreamMetadata(toDisplayString(mContext, exception))
                mListener.onError(exception)
                mStoppedByNetwork = true
                return
            }
            val cause = exception.cause
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                updateStreamMetadata(toDisplayString(mContext, exception))
                mListener.onError(exception)
                return
            }
            if (mNumOfExceptions.getAndIncrement() <= MAX_EXCEPTIONS_COUNT) {
                if (cause is UnrecognizedInputFormatException) {
                    mListener.onHandledError(exception)
                } else {
                    prepareWithList(mIndex)
                }
                return
            }
            updateStreamMetadata(toDisplayString(mContext, exception))
            mListener.onError(exception)
        }

        private fun toDisplayString(context: Context, exception: PlaybackException): String {
            if (exception.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                return context.getString(R.string.media_stream_network_failed)
            }
            var msg = context.getString(R.string.media_stream_error)
            val cause = exception.cause
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                when (cause.responseCode) {
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        msg = context.getString(R.string.media_stream_http_403)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        msg = context.getString(R.string.media_stream_http_404)
                    }
                }
            }
            return msg
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

    /**
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener : PlayerNotificationManager.NotificationListener {

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !mIsForegroundService) {
                try {
                    mListener.onStartForeground(notificationId, notification)
                    mIsForegroundService = true
                } catch (throwable: Throwable) {
                    AppLogger.e("$LOG_TAG can't start foreground", throwable)
                }
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            mIsForegroundService = false
            mListener.onStopForeground(true)
        }
    }

    private inner class NotificationListener : MediaNotificationManager.Listener {

        override fun onCloseApp() {
            mListener.onCloseApp()
        }
    }

    companion object {
        /**
         * String tag to use in logs.
         */
        private const val LOG_TAG = "ORP"

        /**
         *
         */
        private const val MAX_EXCEPTIONS_COUNT = 5

        /**
         * Utility method to extract stream mime type from the stream extension (if exists).
         */
        private fun getMimeTypeFromUri(uri: Uri): String {
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
            return mime
        }

        private fun rssToPlayerMediaItems(context: Context, value: List<RadioStation>): List<MediaItem> {
            val list = ArrayList<MediaItem>()
            for (item in value) {
                list.add(rsToPlayerMediaItem(context, item))
            }
            return list
        }

        private fun rsToPlayerMediaItem(context: Context, value: RadioStation): MediaItem {
            val uri = Uri.parse(value.getStreamUrlFixed())
            return MediaItem.Builder()
                .setMediaId(value.id)
                .setUri(uri)
                .setMediaMetadata(MediaItemHelper.metadataFromRadioStation(context, value).toMediaItemMetadata())
                .setMimeType(getMimeTypeFromUri(uri))
                .build()
        }
    }
}
