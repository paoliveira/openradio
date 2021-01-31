/*
 * Copyright 2014 - 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.service

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.Process
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.broadcast.AbstractReceiver
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast
import com.yuriy.openradio.shared.broadcast.BTConnectionReceiver
import com.yuriy.openradio.shared.broadcast.BecomingNoisyReceiver
import com.yuriy.openradio.shared.broadcast.ClearCacheReceiver
import com.yuriy.openradio.shared.broadcast.ClearCacheReceiverListener
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver
import com.yuriy.openradio.shared.broadcast.MasterVolumeReceiver
import com.yuriy.openradio.shared.broadcast.MasterVolumeReceiverListener
import com.yuriy.openradio.shared.broadcast.RemoteControlReceiver
import com.yuriy.openradio.shared.exo.ExoPlayerOpenRadioImpl
import com.yuriy.openradio.shared.exo.MetadataListener
import com.yuriy.openradio.shared.model.api.ApiServiceProvider
import com.yuriy.openradio.shared.model.api.ApiServiceProviderImpl
import com.yuriy.openradio.shared.model.media.item.MediaItemAllCategories
import com.yuriy.openradio.shared.model.media.item.MediaItemChildCategories
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand
import com.yuriy.openradio.shared.model.media.item.MediaItemCommandDependencies
import com.yuriy.openradio.shared.model.media.item.MediaItemCountriesList
import com.yuriy.openradio.shared.model.media.item.MediaItemCountryStations
import com.yuriy.openradio.shared.model.media.item.MediaItemFavoritesList
import com.yuriy.openradio.shared.model.media.item.MediaItemLocalsList
import com.yuriy.openradio.shared.model.media.item.MediaItemPopularStations
import com.yuriy.openradio.shared.model.media.item.MediaItemRecentlyAddedStations
import com.yuriy.openradio.shared.model.media.item.MediaItemRoot
import com.yuriy.openradio.shared.model.media.item.MediaItemSearchFromApp
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.model.parser.JsonDataParserImpl
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.model.storage.LocationStorage
import com.yuriy.openradio.shared.model.storage.RadioStationsStorage
import com.yuriy.openradio.shared.model.storage.ServiceLifecyclePreferencesManager
import com.yuriy.openradio.shared.model.storage.SleepTimerStorage
import com.yuriy.openradio.shared.model.storage.cache.CacheType
import com.yuriy.openradio.shared.model.timer.SleepTimerImpl
import com.yuriy.openradio.shared.model.timer.SleepTimerListener
import com.yuriy.openradio.shared.notification.MediaNotification
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.FileUtils
import com.yuriy.openradio.shared.utils.IntentUtils
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.NetUtils
import com.yuriy.openradio.shared.utils.PackageValidator
import com.yuriy.openradio.shared.utils.RadioStationsComparator
import com.yuriy.openradio.shared.utils.SortUtils
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.vo.PlaybackStateError
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/13/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class OpenRadioService : MediaBrowserServiceCompat() {

    /**
     * ExoPlayer's implementation to play Radio stream.
     */
    private var mExoPlayerORImpl: ExoPlayerOpenRadioImpl? = null

    /**
     * Listener of the ExoPlayer's event.
     */
    private val mListener: ExoPlayerOpenRadioImpl.Listener

    /**
     * Media Session.
     */
    private var mSession: MediaSessionCompat? = null

    /**
     * Callback listener to listen media session events.
     */
    private var mMediaSessionCb: MediaSessionCompat.Callback? = null

    /**
     * Index of the current playing song.
     */
    private var mCurrentIndexOnQueue = MediaSessionCompat.QueueItem.UNKNOWN_ID
    private var mCurrentStreamTitle: String? = null
    private var mPauseReason = PauseReason.DEFAULT

    /**
     * Collection of the Radio Stations.
     */
    private val mRadioStationsStorage: RadioStationsStorage
    private var mCurrentMediaId: String? = null

    /**
     * Notification object.
     */
    private var mMediaNotification: MediaNotification? = null

    /**
     * Flag that indicates whether application runs over normal Android or Auto version.
     */
    private var mIsAndroidAuto = false

    /**
     * Flag that indicates whether application runs over normal Android or Android TV.
     */
    var isTv = false
        private set

    private var mPackageValidator: PackageValidator? = null

    private enum class PauseReason { DEFAULT, NOISY }

    private val mDelayedStopHandler: Handler

    /**
     * Map of the Media Item commands that responsible for the Media Items List creation.
     */
    private val mMediaItemCommands: MutableMap<String?, MediaItemCommand> = HashMap()
    private var mPosition: Long = 0
    private var mBufferedPosition: Long = 0
    private var mLastPlayedUrl: String? = null
    private val mMasterVolumeBroadcastReceiver: MasterVolumeReceiver
    private val mClearCacheReceiver: ClearCacheReceiver

    /**
     * The BroadcastReceiver that tracks network connectivity changes.
     */
    private val mConnectivityReceiver: AbstractReceiver
    private val mNoisyAudioStreamReceiver: AbstractReceiver
    private val mBTConnectionReceiver: BTConnectionReceiver

    /**
     * Track last selected Radio Station. This filed used when AA uses buffering/duration and the "Last Played"
     * Radio Station is not actually in any lists, it is single entity.
     */
    private var mLastKnownRS: RadioStation? = null
    private var mRestoredRS: RadioStation? = null
    private var mApiServiceProvider: ApiServiceProvider? = null
    private val mUiScope = CoroutineScope(Dispatchers.Main)

    /**
     * Processes Messages sent to it from onStartCommand() that indicate which command to process.
     */
    @Volatile
    private var mServiceHandler: ServiceHandler? = null

    private val mDownloader: Downloader
    private val mRadioStationsComparator: Comparator<RadioStation>
    private val mStartIds: ConcurrentLinkedQueue<Int>
    private val mTimerListener = SleepTimerListenerImpl()
    private val mTimer = SleepTimerImpl.makeInstance(mTimerListener)

    interface ResultListener {
        fun onResult()
    }

    @SuppressLint("HandlerLeak")
    private inner class DelayedStopHandler : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            if (mExoPlayerORImpl != null && mExoPlayerORImpl!!.isPlaying) {
                d("$CLASS_NAME ignoring delayed stop since ExoPlayerORImpl in use.")
                return
            }
            d("$CLASS_NAME stopping service with delay handler.")
            stopSelfResultInt()
        }
    }

    override fun onCreate() {
        val start = System.currentTimeMillis()
        super.onCreate()
        i("$CLASS_NAME On Create")
        val context = applicationContext
        mPackageValidator = PackageValidator(context, R.xml.allowed_media_browser_callers)
        val orientationStr: String
        val orientation = resources.configuration.orientation
        orientationStr = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            "Landscape"
        } else {
            "Portrait"
        }
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            d("$CLASS_NAME running on a TV Device in $orientationStr")
            isTv = true
        } else {
            d("$CLASS_NAME running on a non-TV Device")
        }

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread, which we don't
        // want to block.
        val thread = HandlerThread("ORS-Thread")
        thread.start()
        // Looper associated with the HandlerThread.
        val looper = thread.looper
        // Get the HandlerThread's Looper and use it for our Handler.
        mServiceHandler = ServiceHandler(looper)
        mApiServiceProvider = ApiServiceProviderImpl(context, JsonDataParserImpl())
        mBTConnectionReceiver.register(context)
        mBTConnectionReceiver.locateDevice(context, null)

        // Add Media Items implementations to the map
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_ROOT] = MediaItemRoot()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_ALL_CATEGORIES] = MediaItemAllCategories()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_COUNTRIES_LIST] = MediaItemCountriesList()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_COUNTRY_STATIONS] = MediaItemCountryStations()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES] = MediaItemChildCategories()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_FAVORITES_LIST] = MediaItemFavoritesList()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST] = MediaItemLocalsList()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP] = MediaItemSearchFromApp()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_POPULAR_STATIONS] = MediaItemPopularStations()
        mMediaItemCommands[MediaIdHelper.MEDIA_ID_RECENT_ADDED_STATIONS] = MediaItemRecentlyAddedStations()
        mCurrentIndexOnQueue = MediaSessionCompat.QueueItem.UNKNOWN_ID

        // Need this component for API 20 and earlier.
        // I wish to get rid of this because it keeps listen to broadcast even after application is destroyed :-(
        val mediaButtonReceiver = ComponentName(context, RemoteControlReceiver::class.java)

        // Start a new MediaSession
        mSession = MediaSessionCompat(context, "OpenRadioService", mediaButtonReceiver, null)

        sessionToken = mSession!!.sessionToken
        mMediaSessionCb = MediaSessionCallback()
        mSession!!.setCallback(mMediaSessionCb)
        mSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mMediaNotification = MediaNotification(this)
        AnalyticsUtils.logMessage("OpenRadioService[" + this.hashCode() + "]->onCreate")
        mMediaNotification!!.notifyService("Application just started")
        mMasterVolumeBroadcastReceiver.register(context)
        mClearCacheReceiver.register(context)
        ServiceLifecyclePreferencesManager.isServiceActive(context, true)

        val sleepTimerEnabled = SleepTimerStorage.loadEnabled(context)
        val sleepTimerDate = SleepTimerStorage.loadDate(context)
        mTimer.handle(sleepTimerEnabled, sleepTimerDate.time)

        i("$CLASS_NAME created in ${(System.currentTimeMillis() - start)} ms")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        i("$CLASS_NAME on start command:$intent, id:$startId")
        mStartIds.add(startId)
        if (intent != null) {
            sendMessage(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        d("$CLASS_NAME On Destroy:${hashCode()}")
        super.onDestroy()
        val context = applicationContext
        ServiceLifecyclePreferencesManager.isServiceActive(context, false)
        if (mServiceHandler != null) {
            mServiceHandler!!.looper.quit()
        }
        mBTConnectionReceiver.unregister(context)
        mConnectivityReceiver.unregister(context)
        mNoisyAudioStreamReceiver.unregister(context)
        mMasterVolumeBroadcastReceiver.unregister(context)
        mClearCacheReceiver.unregister(context)
        mApiServiceProvider!!.close()
        stopService()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        val str = "clientPkgName=$clientPackageName, clientUid=$clientUid, systemUid=${Process.SYSTEM_UID}, " +
                "myUid=${Process.myUid()}, rootHints=${IntentUtils.bundleToString(rootHints)}"
        d(CLASS_NAME + str)
        AnalyticsUtils.logMessage(str)
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator!!.isKnownCaller(clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            w("$CLASS_NAME IGNORING request from untrusted package $clientPackageName")
            return null
        }
        mIsAndroidAuto = if (AppUtils.isAutomotive(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
            i("$CLASS_NAME package name is Android Auto")
            true
        } else {
            i("$CLASS_NAME package name is not Android Auto")
            false
        }
        mCurrentParentId = getCurrentParentId(rootHints)
        mIsRestoreState = getRestoreState(rootHints)
        setPlaybackState(getCurrentPlaybackState(rootHints))
        initInternals()
        return BrowserRoot(MediaIdHelper.MEDIA_ID_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        i("$CLASS_NAME OnLoadChildren $parentId")
        var isSameCatalogue = false
        // Check whether category had changed.
        if (parentId == mCurrentParentId) {
            isSameCatalogue = true
        }
        mCurrentParentId = parentId

        // If Parent Id contains Country Code - use it in the API.
        var countryCode = MediaIdHelper.getCountryCode(mCurrentParentId)
        if (countryCode.isNullOrEmpty()) {
            // Otherwise, use whatever is stored in preferences.
            countryCode = LocationStorage.getLastCountryCode(applicationContext)
        }
        val context = applicationContext
        val command = mMediaItemCommands[MediaIdHelper.getId(mCurrentParentId)]
        val dependencies = MediaItemCommandDependencies(
                context, mDownloader, result, mRadioStationsStorage, mApiServiceProvider!!,
                countryCode, mCurrentParentId!!, mIsAndroidAuto, isSameCatalogue, mIsRestoreState,
                object : ResultListener {
                    override fun onResult() {
                        this@OpenRadioService.onResult()
                    }
                },
                mRadioStationsComparator
        )
        mIsRestoreState = false
        if (command != null) {
            command.execute(
                    object : MediaItemCommand.IUpdatePlaybackState {
                        override fun updatePlaybackState(error: String?) {
                            updatePlaybackState()
                        }
                    },
                    dependencies
            )
        } else {
            w("$CLASS_NAME skipping unmatched parentId: $mCurrentParentId")
            result.sendResult(dependencies.mediaItems)
        }
        // Registers BroadcastReceiver to track network connection changes.
        mConnectivityReceiver.register(context)
    }

    /**
     * @param intent
     */
    private fun sendMessage(intent: Intent) {
        // Create a Message that will be sent to ServiceHandler.
        val message = mServiceHandler!!.makeMessage(intent)
        // Send the Message to ServiceHandler.
        mServiceHandler!!.sendMessage(message)
    }

    /**
     * @param exception
     */
    private fun onHandledError(exception: ExoPlaybackException) {
        e("$CLASS_NAME ExoPlayer handled exception:$exception")
        val throwable = exception.cause
        if (throwable is UnrecognizedInputFormatException) {
            handleUnrecognizedInputFormatException()
        }
    }

    /**
     * Handles exception related to unrecognized url. Try to parse url deeply to extract actual stream one from
     * playlist.
     */
    private fun handleUnrecognizedInputFormatException() {
        val playlistUrl = mLastPlayedUrl
        AnalyticsUtils.logMessage("UnrecognizedInputFormat:$playlistUrl")
        handleStopRequest(
                PlaybackStateError("Can not get play url.", PlaybackStateError.Code.UNRECOGNIZED_URL)
        )
        GlobalScope.launch(Dispatchers.IO) {
            withTimeout(API_CALL_TIMEOUT_MS) {
                if (playlistUrl.isNullOrEmpty()) {
                    e("HandleUnrecognizedInputFormatException with empty URL")
                    return@withTimeout
                }
                val urls = NetUtils.extractUrlsFromPlaylist(applicationContext, playlistUrl)
                mUiScope.launch {
                    // Silently clear last references and try to restart:
                    initInternals()
                    handlePlayListUrlsExtracted(urls)
                }
            }
        }
    }

    private fun handlePlayListUrlsExtracted(urls: Array<String?>) {
        if (urls.isEmpty()) {
            handleStopRequest(PlaybackStateError(getString(R.string.media_player_error), PlaybackStateError.Code.GENERAL))
            return
        }
        val radioStation = getRadioStationByMediaId(mCurrentMediaId)
        if (radioStation == null) {
            handleStopRequest(PlaybackStateError(getString(R.string.media_player_error), PlaybackStateError.Code.GENERAL))
            return
        }
        // TODO: Refactor
        radioStation.mediaStream.clear()
        radioStation.mediaStream.setVariant(0, urls[0]!!)
        handlePlayRequest()
    }

    private fun onPrepared() {
        i("$CLASS_NAME ExoPlayer prepared")
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        val radioStation = getRadioStationByMediaId(mCurrentMediaId)
        // Save latest selected Radio Station.
        // Use it in Android Auto mode to display in the side menu as Latest Radio Station.
        if (radioStation != null) {
            LatestRadioStationStorage.add(radioStation, applicationContext)
        }
        updateMetadata(mCurrentStreamTitle)
    }

    private fun stopService() {
        d("$CLASS_NAME stop Service")
        // Service is being killed, so make sure we release our resources
        handleStopRequest()
        releaseExoPlayer()
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        if (mSession != null) {
            d("$CLASS_NAME clear media session")
            mSession!!.isActive = false
            mSession!!.setMediaButtonReceiver(null)
            mSession!!.setCallback(null)
            mSession!!.release()
            mSession = null
            mMediaSessionCb = null
        }
    }

    /**
     * Clear Exo Player and associated resources.
     */
    private fun releaseExoPlayer() {
        mCurrentStreamTitle = null
        if (mExoPlayerORImpl == null) {
            return
        }
        mExoPlayerORImpl!!.release()
        mExoPlayerORImpl = null
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private fun createMediaPlayerIfNeeded() {
        if (mExoPlayerORImpl == null) {
            mExoPlayerORImpl = ExoPlayerOpenRadioImpl(
                    applicationContext, mListener,
                    object : MetadataListener {
                        override fun onMetaData(title: String) {
                            d("$CLASS_NAME Metadata title:$title")
                            updateMetadata(title)
                        }
                    }
            )
        } else {
            mExoPlayerORImpl!!.reset()
        }
    }

    /**
     * Retrieve currently selected Radio Station asynchronously.<br></br>
     * If the URl is not yet obtained via API the it will be retrieved as well,
     * appropriate event will be dispatched via listener.
     *
     * @param listener [RadioStationUpdateListener]
     */
    private fun getCurrentPlayingRSAsync(listener: RadioStationUpdateListener) {
        val radioStation = getRadioStationByMediaId(mCurrentMediaId)
        if (radioStation == null) {
            listener.onComplete(null)
            return
        }

        // This indicates that Radio Station's url was not downloaded.
        // Currently, when list of the stations received they comes without stream url
        // and bitrate, upon selecting one - it is necessary to load additional data.
        if (!radioStation.isMediaStreamEmpty()) {
            listener.onComplete(radioStation)
            return
        }
        val id = radioStation.id
        GlobalScope.launch(Dispatchers.IO) {
            withTimeout(API_CALL_TIMEOUT_MS) {
                if (mApiServiceProvider == null) {
                    mUiScope.launch { listener.onComplete(null) }
                    return@withTimeout
                }
                // Start download information about Radio Station
                val radioStationUpdated = mApiServiceProvider!!
                        .getStation(mDownloader, UrlBuilder.getStation(id), CacheType.NONE)
                if (radioStationUpdated == null) {
                    mUiScope.launch { listener.onComplete(null) }
                    return@withTimeout
                }
                mUiScope.launch { listener.onComplete(radioStationUpdated) }
            }
        }
    }

    private fun buildMetadata(radioStation: RadioStation): MediaMetadataCompat? {
        if (radioStation.isMediaStreamEmpty()) {
            updatePlaybackState(
                    PlaybackStateError(getString(R.string.no_data_message), PlaybackStateError.Code.GENERAL)
            )
        }
        return MediaItemHelper.metadataFromRadioStation(applicationContext, radioStation)
    }

    /**
     * Updates Metadata for the currently playing Radio Station. This method terminates without
     * throwing exception if one of the stream parameters is invalid.
     */
    private fun updateMetadata(streamTitle: String?) {
        if (mSession == null) {
            e("$CLASS_NAME update metadata with null media session")
            return
        }
        if (getString(R.string.buffering_infinite) != streamTitle) {
            mCurrentStreamTitle = streamTitle
        }
        val radioStation = getRadioStationByMediaId(mCurrentMediaId)
        if (radioStation == null) {
            w("$CLASS_NAME can not update Metadata - Radio Station is null")
            setPlaybackState(PlaybackStateCompat.STATE_ERROR)
            updatePlaybackState(
                    PlaybackStateError(getString(R.string.no_metadata), PlaybackStateError.Code.GENERAL)
            )
            return
        }
        val metadata = MediaItemHelper.metadataFromRadioStation(
                applicationContext, radioStation, streamTitle
        )
        if (metadata == null) {
            w("$CLASS_NAME can not update Metadata - MediaMetadata is null")
            return
        }
        val trackId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        // TODO: Check whether we can use media id from Radio Station
        if (radioStation.id != trackId) {
            w("$CLASS_NAME track ID '$trackId' should match mediaId '${radioStation.id}'")
            return
        }
        d("$CLASS_NAME updating metadata for MusicId:${radioStation.id}, title:$streamTitle")
        try {
            mSession!!.setMetadata(metadata)
        } catch (e: IllegalStateException) {
            e("$CLASS_NAME can not set metadata:$e")
        }
    }

    /**
     * Return Radio Station object by provided Media Id.
     *
     * @param mediaId Media Id of the station.
     *
     * @return [RadioStation] or `null`.
     */
    private fun getRadioStationByMediaId(mediaId: String?): RadioStation? {
        if (mediaId.isNullOrEmpty()) {
            return null
        }
        var radioStation = mRadioStationsStorage.getById(mediaId)
        if (radioStation == null) {
            radioStation = mLastKnownRS
        }
        if (radioStation == null) {
            radioStation = mRestoredRS
        }
        return radioStation
    }

    /**
     * Returns current queue item.
     *
     * @return
     */
    private val currentQueueItem: RadioStation?
        get() = mRadioStationsStorage.getAt(mCurrentIndexOnQueue)

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the ExoPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     * be released or not
     */
    private fun relaxResources(releaseMediaPlayer: Boolean) {
        d("$CLASS_NAME relax resources, release:$releaseMediaPlayer")

        // stop being a foreground service
        stopForeground(true)
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY.toLong())

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            releaseExoPlayer()
        }
    }

    /**
     * Handles event when Bluetooth connected to same device within application lifetime.
     */
    private fun handleBTSameDeviceConnected() {
        val autoPlay = AppPreferencesManager.isBtAutoPlay(applicationContext)
        d(
                "$CLASS_NAME BTSameDeviceConnected, do auto play:$autoPlay, " +
                        "state:${MediaItemHelper.playbackStateToString(mState)}, pause reason:$mPauseReason"
        )
        if (!autoPlay) {
            return
        }
        // Restore playback if it was paused by noisy receiver.
        if (mState == PlaybackStateCompat.STATE_PAUSED && mPauseReason == PauseReason.NOISY) {
            handlePlayRequest()
        }
    }

    /**
     * Handle a request to play Radio Station.
     */
    private fun handlePlayRequest() {
        mUiScope.launch { handlePlayRequestUiThread() }
    }

    private fun handlePlayRequestUiThread() {
        d("$CLASS_NAME handle play request, state:${MediaItemHelper.playbackStateToString(mState)}")
        if (mSession == null) {
            e("$CLASS_NAME handle play with null media session")
            return
        }
        mCurrentStreamTitle = null
        val context = applicationContext
        if (!ConnectivityReceiver.checkConnectivityAndNotify(context)) {
            return
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        if (!mSession!!.isActive) {
            mSession!!.isActive = true
        }

        // actually play the song
        if (mState == PlaybackStateCompat.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            if (mExoPlayerORImpl != null && !mExoPlayerORImpl!!.isPlaying) {
                mExoPlayerORImpl!!.play()
            } else {
                e("$CLASS_NAME handle play on UI thread with null/invalid player")
            }
            d("$CLASS_NAME ConfigAndStartMediaPlayer set state playing")
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        } else {
            // If we're stopped or playing a song,
            // just go ahead to the new song and (re)start playing.
            getCurrentPlayingRSAsync(
                    object : RadioStationUpdateListener {
                        override fun onComplete(radioStation: RadioStation?) {
                            getCurrentPlayingRSAsyncCb(radioStation)
                        }
                    }
            )
        }
        mNoisyAudioStreamReceiver.register(context)
    }

    private fun getCurrentPlayingRSAsyncCb(radioStation: RadioStation?) {
        if (radioStation == null) {
            e("$CLASS_NAME ignore play next song, cannot find it, idx $mCurrentIndexOnQueue")
            return
        }
        if (mLastKnownRS != null && mLastKnownRS!! == radioStation) {
            e("$CLASS_NAME ignore play next song, last known is the same as requested.")
            updatePlaybackState()
            return
        }
        mLastKnownRS = radioStation
        val metadata = buildMetadata(radioStation)
        if (metadata == null) {
            e("$CLASS_NAME ignore play next song, cannot find metadata, idx $mCurrentIndexOnQueue")
            return
        }
        val source = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toLowerCase(Locale.ROOT)
        d(
                "$CLASS_NAME play, idx:$mCurrentIndexOnQueue," +
                        " id:${metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}, source:$source"
        )
        if (source.isEmpty()) {
            e("$CLASS_NAME source is empty")
            return
        }
        mCurrentMediaId = radioStation.id
        preparePlayer(source)
    }

    private fun preparePlayer(url: String) {
        // Cache URL.
        mLastPlayedUrl = url
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        mPauseReason = PauseReason.DEFAULT

        // Release everything except ExoPlayer
        relaxResources(false)
        createMediaPlayerIfNeeded()
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        mExoPlayerORImpl!!.prepare(Uri.parse(url))
    }

    private fun handleClearCache() {
        mApiServiceProvider!!.clear()
        SafeToast.showAnyThread(this, getString(R.string.clear_completed))
    }

    private fun setPlayerVolume() {
        if (mExoPlayerORImpl == null) {
            e("$CLASS_NAME can not set player volume, player null")
            return
        }
        val volume = AppPreferencesManager.getMasterVolume(applicationContext) / 100.0f
        mExoPlayerORImpl!!.setVolume(volume)
    }

    /**
     * Handle a request to pause radio stream with reason provided.
     *
     * @param reason Reason to pause.
     */
    private fun handlePauseRequest(reason: PauseReason = PauseReason.DEFAULT) {
        mUiScope.launch { handlePauseRequestUiThread(reason) }
    }

    /**
     * Handle a request to pause radio stream with reason provided in UI thread.
     *
     * @param reason Reason to pause.
     */
    private fun handlePauseRequestUiThread(reason: PauseReason) {
        d("$CLASS_NAME HandlePauseRequest, state:${MediaItemHelper.playbackStateToString(mState)}")
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            mPauseReason = reason
            if (mExoPlayerORImpl != null && mExoPlayerORImpl!!.isPlaying) {
                mExoPlayerORImpl!!.pause()
            }
            // while paused, retain the ExoPlayer but give up audio focus
            relaxResources(false)
        }
        updatePlaybackState()
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error Error object to present to the user.
     */
    private fun updatePlaybackState(error: PlaybackStateError = PlaybackStateError()) {
        d("$CLASS_NAME set playback state to ${MediaItemHelper.playbackStateToString(mState)} error:$error")
        if (mSession == null) {
            e("$CLASS_NAME update playback with null media session")
            return
        }
        val stateBuilder = PlaybackStateCompat.Builder().setActions(availableActions)
        setCustomAction(stateBuilder)

        // If there is an error message, send it to the playback state:
        if (error.msg != null) {
            e("$CLASS_NAME update playback state, error: $error")
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error.msg)
            setPlaybackState(PlaybackStateCompat.STATE_ERROR)
            mLastKnownRS = null
            if (error.code !== PlaybackStateError.Code.UNRECOGNIZED_URL) {
                mLastPlayedUrl = null
            }
        }
        stateBuilder.setBufferedPosition(mBufferedPosition)
        stateBuilder.setState(mState, mPosition, 1.0f, SystemClock.elapsedRealtime())

        // Set the activeQueueItemId if the current index is valid.
        if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
            val item = currentQueueItem
            if (item != null) {
                // TODO: INVESTIGATE!!!
                stateBuilder.setActiveQueueItemId(mCurrentIndexOnQueue.toLong())
            }
        }

        // Update state only in case of play. Error cause "updatePlaybackState" which has "updateMetadata"
        // inside - infinite loop!
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            updateMetadata(getString(R.string.buffering_infinite))
        }
        try {
            // Try to address issue on Android 4.1.2:
            // IllegalStateException: beginBroadcast() called while already in a broadcast
            mSession!!.setPlaybackState(stateBuilder.build())
        } catch (e: IllegalStateException) {
            AnalyticsUtils.logException(e)
        }
        if (mState == PlaybackStateCompat.STATE_BUFFERING
                || mState == PlaybackStateCompat.STATE_PLAYING
                || mState == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotification!!.startNotification(applicationContext, getRadioStationByMediaId(mCurrentMediaId))
        }
    }// Always show Prev and Next buttons, play index is handling on each listener (for instance, to handle loop
    // once end or beginning reached).
    /**
     * Get available actions from media control buttons.
     *
     * @return Actions encoded in integer.
     */
    private val availableActions: Long
        get() {
            var actions = (PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                    or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                actions = actions or PlaybackStateCompat.ACTION_PAUSE
            }
            if (mRadioStationsStorage.size() <= 1) {
                return actions
            }
            // Always show Prev and Next buttons, play index is handling on each listener (for instance, to handle loop
            // once end or beginning reached).
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            return actions
        }

    /**
     * Handle a request to stop music in UI thread.
     *
     * @param error Playback error.
     */
    private fun handleStopRequestUiThread(error: PlaybackStateError) {
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            return
        }
        d("$CLASS_NAME handle stop request, state:${MediaItemHelper.playbackStateToString(mState)} error:$error")
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        mPauseReason = PauseReason.DEFAULT
        mNoisyAudioStreamReceiver.unregister(applicationContext)

        // let go of all resources...
        relaxResources(true)
        if (mMediaNotification != null) {
            mMediaNotification!!.stopNotification()
            updatePlaybackState(error)
        }
    }

    /**
     * Handle a request to stop music.
     *
     * @param error Playback error.
     */
    private fun handleStopRequest(error: PlaybackStateError = PlaybackStateError()) {
        mUiScope.launch { handleStopRequestUiThread(error) }
    }

    private fun onResult() {
        mUiScope.launch { onResultUiThread() }
    }

    private fun onResultUiThread() {
        if (!mCurrentMediaId.isNullOrEmpty() &&
                !mRadioStationsStorage.isEmpty) {
            mCurrentIndexOnQueue = mRadioStationsStorage.getIndex(mCurrentMediaId)
            d("$CLASS_NAME on result from command, index:$mCurrentIndexOnQueue, $mCurrentMediaId")
        }
        restoreActiveRadioStation()
    }

    private fun restoreActiveRadioStation() {
        val context = applicationContext
        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            if (mRestoredRS == null) {
                mRestoredRS = LatestRadioStationStorage[context]
            }
            if (mRestoredRS != null) {
                handlePlayFromMediaId(mRestoredRS!!.id)
            }
            mRestoredRS = null
        }
    }

    /**
     * Consume Radio Station by it's ID.
     *
     * @param mediaId ID of the Radio Station.
     */
    private fun handlePlayFromMediaId(mediaId: String) {
        if (mediaId == MediaSessionCompat.QueueItem.UNKNOWN_ID.toString()) {
            updatePlaybackState(
                    PlaybackStateError(getString(R.string.no_data_message), PlaybackStateError.Code.GENERAL)
            )
            return
        }
        mCurrentMediaId = mediaId
        if (!ConnectivityReceiver.checkConnectivityAndNotify(applicationContext)) {
            return
        }

        // Use this flag to compare indexes of the items later on.
        // Do not compare indexes if state is not play.
        if (mState == PlaybackStateCompat.STATE_PAUSED) {
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        }
        val tempIndexOnQueue = mRadioStationsStorage.getIndex(mCurrentMediaId)
        if (tempIndexOnQueue != MediaSessionCompat.QueueItem.UNKNOWN_ID) {
            mCurrentIndexOnQueue = tempIndexOnQueue
        }

        // Play Radio Station
        handlePlayRequest()
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        getCurrentPlayingRSAsync(
                object : RadioStationUpdateListener {
                    override fun onComplete(radioStation: RadioStation?) {
                        if (radioStation == null) {
                            return
                        }
                        var favoriteIcon = R.drawable.ic_favorite_off
                        if (FavoritesStorage.isFavorite(radioStation, applicationContext)) {
                            favoriteIcon = R.drawable.ic_favorite_on
                        }
                        stateBuilder.addCustomAction(
                                CUSTOM_ACTION_THUMBS_UP,
                                this@OpenRadioService.getString(R.string.favorite),
                                favoriteIcon
                        )
                    }
                }
        )
    }

    private fun handleConnectivityChange(isConnected: Boolean) {
        if (mState != PlaybackStateCompat.STATE_PLAYING) {
            return
        }
        if (isConnected) {
            handlePlayRequest()
        }
    }

    /**
     *
     */
    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {

        private val mClassName = MediaSessionCallback::class.java.simpleName + " "

        override fun onPlay() {
            super.onPlay()
            i("$mClassName play [ors:${this@OpenRadioService.hashCode()}]")
            if (mRadioStationsStorage.isEmpty) {
                // Start playing from the beginning of the queue.
                mCurrentIndexOnQueue = 0
            }
            handlePlayRequest()
        }

        override fun onSkipToQueueItem(id: Long) {
            super.onSkipToQueueItem(id)
            i("$mClassName skip to queue item, id:$id [ors:${this@OpenRadioService.hashCode()}]")
            if (mState == PlaybackStateCompat.STATE_PAUSED) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
            }
            if (mRadioStationsStorage.isEmpty) {
                return
            }

            // set the current index on queue from the music Id:
            mCurrentIndexOnQueue = mRadioStationsStorage.getIndex(id.toString())
            if (mCurrentIndexOnQueue == MediaSessionCompat.QueueItem.UNKNOWN_ID) {
                return
            }
            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue)

            // Play the Radio Station
            handlePlayRequest()
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            i(
                    "$mClassName play from media id:$mediaId extras:${IntentUtils.bundleToString(extras)} " +
                            "[ors:${this@OpenRadioService.hashCode()}]"
            )
            handlePlayFromMediaId(mediaId)
        }

        override fun onPause() {
            super.onPause()
            i("$mClassName pause [ors:${this@OpenRadioService.hashCode()}]")
            handlePauseRequest()
        }

        override fun onStop() {
            super.onStop()
            i("$mClassName stop [ors:${this@OpenRadioService.hashCode()}]")
            handleStopRequest()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            i(
                    "$mClassName $mCurrentIndexOnQueue skip to ${(mCurrentIndexOnQueue + 1)} " +
                            "[ors:${this@OpenRadioService.hashCode()}]"
            )
            val size = mRadioStationsStorage.size()
            mCurrentIndexOnQueue++
            if (mCurrentIndexOnQueue >= size) {
                mCurrentIndexOnQueue = 0
            }
            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue)
            if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                val rs = currentQueueItem
                if (rs != null) {
                    mCurrentMediaId = rs.id
                }
                handlePlayRequest()
            } else {
                e("$mClassName cannot skip to next, next index:$mCurrentIndexOnQueue queue size::$size}")
                handleStopRequest(
                        PlaybackStateError(getString(R.string.can_not_skip), PlaybackStateError.Code.GENERAL)
                )
            }
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            i("$mClassName skip to previous [ors:${this@OpenRadioService.hashCode()}]")
            mCurrentIndexOnQueue--
            val size = mRadioStationsStorage.size()
            if (mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = size - 1
            }
            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue)
            if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                val rs = currentQueueItem
                if (rs != null) {
                    mCurrentMediaId = rs.id
                }
                handlePlayRequest()
            } else {
                e("$mClassName cannot skip to previous, previous index:$mCurrentIndexOnQueue, queue size:$size")
                handleStopRequest(
                        PlaybackStateError(getString(R.string.can_not_skip), PlaybackStateError.Code.GENERAL)
                )
            }
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            super.onCustomAction(action, extras)
            i("$mClassName custom action:$action [ors:${this@OpenRadioService.hashCode()}]")
            if (CUSTOM_ACTION_THUMBS_UP == action) {
                getCurrentPlayingRSAsync(
                        object : RadioStationUpdateListener {
                            override fun onComplete(radioStation: RadioStation?) {
                                val context = this@OpenRadioService.applicationContext
                                if (radioStation != null) {
                                    val isFavorite = FavoritesStorage.isFavorite(
                                            radioStation, context
                                    )
                                    if (isFavorite) {
                                        FavoritesStorage.remove(radioStation, context)
                                    } else {
                                        FavoritesStorage.add(radioStation, context)
                                    }
                                }

                                // playback state needs to be updated because the "Favorite" icon on the
                                // custom action will change to reflect the new favorite state.
                                updatePlaybackState()
                            }
                        }
                )
            } else {
                e("$mClassName unsupported action:$action")
            }
        }

        override fun onPlayFromSearch(query: String, extras: Bundle) {
            i(
                    "$mClassName play from search:$query extras:${IntentUtils.bundleToString(extras)} " +
                            "[ors:${this@OpenRadioService.hashCode()}]"
            )
            super.onPlayFromSearch(query, extras)
            performSearch(query)
        }

        @Volatile
        private var mLastKeyEventTime: Long = 0
        override fun onMediaButtonEvent(intent: Intent): Boolean {
            // Prevent double event.
            // TODO: Need to investigate
            if (mLastKeyEventTime != 0L && System.currentTimeMillis() - mLastKeyEventTime <= 1000) {
                return true
            }
            mLastKeyEventTime = System.currentTimeMillis()
            i(
                    "$mClassName media btn evnt:$intent extra:${IntentUtils.intentBundleToString(intent)} " +
                            "[ors:${this@OpenRadioService.hashCode()}]"
            )
            val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
            val keyCode = event?.keyCode ?: Int.MIN_VALUE
            d(mClassName + "KeyCode:" + keyCode)
            return when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> {
                    onPlay()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (mExoPlayerORImpl == null) {
                        return false
                    }
                    if (mExoPlayerORImpl!!.isPlaying) {
                        onPause()
                    } else {
                        onPlay()
                    }
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    onPause()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> {
                    onStop()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    onSkipToNext()
                    true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    onSkipToPrevious()
                    true
                }
                else -> {
                    w("$mClassName Unhandled key code:$keyCode")
                    false
                }
            }
        }
    }

    private fun initInternals() {
        mLastKnownRS = null
        mLastPlayedUrl = null
    }

    private fun performSearch(query: String) {
        i("$CLASS_NAME search for:$query")
        if (query.isEmpty()) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            withTimeout(API_CALL_TIMEOUT_MS) {
                try {
                    executePerformSearch(query)
                } catch (e: Exception) {
                    e("$CLASS_NAME can not perform search for '$query', exception:$e")
                }
            }
        }
    }

    /**
     * Execute actual search.
     *
     * @param query Search query.
     */
    private fun executePerformSearch(query: String) {
        if (mApiServiceProvider == null) {
            e("$CLASS_NAME can not handle perform search, API provider is null")
            return
        }
        val list = mApiServiceProvider!!.getStations(mDownloader, UrlBuilder.getSearchUrl(query), CacheType.NONE)
        if (list.isEmpty()) {
            SafeToast.showAnyThread(applicationContext, applicationContext.getString(R.string.no_search_results))
            return
        }
        mUiScope.launch {
            i("$CLASS_NAME found ${list.size} items")
            mRadioStationsStorage.clearAndCopy(list)
            // immediately start playing from the beginning of the search results
            mCurrentIndexOnQueue = 0
            handlePlayRequest()
        }
    }

    /**
     * Dispatch broad cast event about changes on current playing Radio Station.
     *
     * @param index Index of the Radio Station in the queue.
     */
    private fun dispatchCurrentIndexOnQueue(index: Int) {
        if (!mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
            w("$CLASS_NAME can not dispatch curr index on queue")
            return
        }
        val item = currentQueueItem
        var mediaId = ""
        if (item != null) {
            mediaId = item.id
        }
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                AppLocalBroadcast.createIntentCurrentIndexOnQueue(index, mediaId)
        )
    }

    /**
     * This method executed in separate thread.
     *
     * @param command
     * @param intent
     */
    private fun handleMessageInternal(command: String, intent: Intent) {
        val context = applicationContext
        d("$CLASS_NAME rsv cmd:$command")
        when (command) {
            VALUE_NAME_GET_RADIO_STATION_COMMAND -> {
                if (mMediaNotification != null) {
                    mMediaNotification!!.notifyService("Update Favorite Radio Station")
                }
                val description = extractMediaDescription(intent) ?: return
                var rs = getRadioStationByMediaId(description.mediaId)
                // This can the a case when last known Radio Station is playing.
                // In this case it is not in a list of radio stations.
                // If it exists, let's compare its id with the id provided by intent.
                if (rs == null) {
                    if (mLastKnownRS != null && mLastKnownRS!!.id == description.mediaId) {
                        rs = RadioStation.makeCopyInstance(context, mLastKnownRS!!)
                    }
                    // We failed both cases, something went wrong ...
                    if (rs == null) {
                        return
                    }
                }
                // Update Favorites Radio station: whether add it or remove it from the storage
                val isFavorite = getIsFavoriteFromIntent(intent)
                if (isFavorite) {
                    FavoritesStorage.add(rs, context)
                } else {
                    FavoritesStorage.remove(rs, context)
                }
            }
            VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND -> {
                val mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID)
                val name = intent.getStringExtra(EXTRA_KEY_STATION_NAME)
                val url = intent.getStringExtra(EXTRA_KEY_STATION_STREAM_URL)
                val imageUrl = intent.getStringExtra(EXTRA_KEY_STATION_IMAGE_URL)
                val genre = intent.getStringExtra(EXTRA_KEY_STATION_GENRE)
                val country = intent.getStringExtra(EXTRA_KEY_STATION_COUNTRY)
                val addToFav = intent.getBooleanExtra(
                        EXTRA_KEY_STATION_ADD_TO_FAV, false
                )
                var imageUrlLocal = FileUtils.copyExtFileToIntDir(context, imageUrl!!)
                if (imageUrlLocal == null) {
                    imageUrlLocal = imageUrl
                } else {
                    FileUtils.deleteFile(imageUrl)
                }
                val result = LocalRadioStationsStorage.update(
                        mediaId, context, name, url, imageUrlLocal, genre, country, addToFav
                )
                if (result) {
                    notifyChildrenChanged(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSSuccess(
                                    "Radio Station updated successfully"
                            )
                    )
                } else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Can not update Radio Station"
                            )
                    )
                }
            }
            VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND -> {
                val rsToAdd = intent.getSerializableExtra(EXTRA_KEY_RS_TO_ADD) as RadioStationToAdd?
                if (rsToAdd == null) {
                    e("$CLASS_NAME Radio Station to add is null")
                    return
                }
                if (rsToAdd.name.isEmpty()) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's name is invalid"
                            )
                    )
                    return
                }
                val url = rsToAdd.url
                if (url.isEmpty()) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's url is invalid"
                            )
                    )
                    return
                }
                if (!NetUtils.checkResource(context, url)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's stream is invalid"
                            )
                    )
                    return
                }
                val imageWebUrl = rsToAdd.imageWebUrl
                if (!NetUtils.checkResource(context, imageWebUrl)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's web image is invalid"
                            )
                    )
                }
                val homePage = rsToAdd.homePage
                if (!NetUtils.checkResource(context, homePage)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's home page is invalid"
                            )
                    )
                }
                if (rsToAdd.isAddToServer) {
                    val urlData = UrlBuilder.addStation(rsToAdd)
                    val uri = urlData.first ?: return
                    val pairs = urlData.second ?: return
                    if (!mApiServiceProvider!!.addStation(mDownloader, uri, pairs, CacheType.NONE)) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                AppLocalBroadcast.createIntentValidateOfRSFailed(
                                        "Radio Station can not be added to server"
                                )
                        )
                    } else {
                        AppLocalBroadcast.createIntentValidateOfRSSuccess("Radio Station added to server")
                    }
                }
                var imageUrlLocal = FileUtils.copyExtFileToIntDir(context, rsToAdd.imageLocalUrl)
                if (imageUrlLocal == null) {
                    imageUrlLocal = rsToAdd.imageLocalUrl
                }
                val radioStation = RadioStation.makeDefaultInstance(
                        context, LocalRadioStationsStorage.getId(context)
                )
                radioStation.name = rsToAdd.name
                radioStation.mediaStream.setVariant(0, url)
                radioStation.imageUrl = imageUrlLocal
                radioStation.thumbUrl = imageUrlLocal
                radioStation.genre = rsToAdd.genre
                radioStation.country = rsToAdd.country
                radioStation.setIsLocal(true)
                LocalRadioStationsStorage.add(radioStation, context)
                if (rsToAdd.isAddToFav) {
                    FavoritesStorage.add(radioStation, context)
                }
                notifyChildrenChanged(MediaIdHelper.MEDIA_ID_ROOT)
                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        AppLocalBroadcast.createIntentValidateOfRSSuccess("Radio Station added to local device")
                )
            }
            VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND -> {
                val mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID)
                if (mediaId.isNullOrEmpty()) {
                    w("$CLASS_NAME can not remove Station, Media Id is empty")
                    return
                }
                val radioStation = mRadioStationsStorage.remove(mediaId)
                if (radioStation != null) {
                    FileUtils.deleteFile(radioStation.imageUrl)
                    LocalRadioStationsStorage.remove(radioStation, context)
                }
                notifyChildrenChanged(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)
            }
            VALUE_NAME_UPDATE_SORT_IDS -> {
                val mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_IDS)
                val sortId = intent.getIntExtra(EXTRA_KEY_SORT_IDS, 0)
                val categoryMediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID) ?: ""
                if (mediaId.isNullOrEmpty()) {
                    return
                }
                d("$CLASS_NAME sort set $mediaId to $sortId position [$categoryMediaId]")
                SortUtils.updateSortIds(applicationContext, mRadioStationsComparator, mediaId, sortId, categoryMediaId)
                notifyChildrenChanged(categoryMediaId)
                GlobalScope.launch(Dispatchers.Main) {
                    delay(100)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                            AppLocalBroadcast.createIntentSortIdChanged(mediaId, sortId)
                    )
                }
            }
            VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM -> {
                if (mMediaNotification != null) {
                    mMediaNotification!!.notifyService("Toggle last Radio Station")
                }
                when (mState) {
                    PlaybackStateCompat.STATE_PLAYING -> {
                        handlePauseRequest()
                    }
                    PlaybackStateCompat.STATE_PAUSED -> {
                        handlePlayRequest()
                    }
                    PlaybackStateCompat.STATE_STOPPED -> {
                        initInternals()
                        handlePlayRequest()
                    }
                    else -> {
                        w("$CLASS_NAME unhandled playback state:${MediaItemHelper.playbackStateToString(mState)}")
                    }
                }
            }
            VALUE_NAME_STOP_LAST_PLAYED_ITEM -> {
                if (mMediaNotification != null) {
                    mMediaNotification!!.notifyService("Stop play last Radio Station")
                }
                handlePauseRequest()
            }
            VALUE_NAME_PLAY_LAST_PLAYED_ITEM -> {
                if (mMediaNotification != null) {
                    mMediaNotification!!.notifyService("Play last Radio Station")
                }
                handlePlayRequest()
            }
            VALUE_NAME_UPDATE_EQUALIZER -> if (mExoPlayerORImpl != null) {
                mExoPlayerORImpl!!.loadEqualizerState()
            }
            VALUE_NAME_STOP_SERVICE -> {
                if (mMediaNotification != null) {
                    mMediaNotification!!.notifyService("Stop application")
                }
                mUiScope.launch {
                    initInternals()
                    handleStopRequest()
                    stopSelfResultInt()
                }
            }
            VALUE_NAME_SLEEP_TIMER -> {
                val enabled = intent.getBooleanExtra(EXTRA_KEY_SLEEP_TIMER_ENABLED, false)
                val time = intent.getLongExtra(EXTRA_KEY_SLEEP_TIMER_TIME, System.currentTimeMillis())
                mTimer.handle(enabled, time)
            }
            else -> w("$CLASS_NAME unknown command:$command")
        }
    }

    private fun stopSelfResultInt() {
        while (!mStartIds.isEmpty()) {
            val id = mStartIds.poll() ?: continue
            val result = stopSelfResult(id)
            i("$CLASS_NAME service " + (if (result) "stopped" else "not stopped") + " for $id")
        }
    }

    private fun setPlaybackState(state: Int) {
        d("$CLASS_NAME set state:${MediaItemHelper.playbackStateToString(state)}")
        mState = state
    }

    /**
     * Listener for Exo Player events.
     */
    private inner class ExoPlayerListener : ExoPlayerOpenRadioImpl.Listener {

        override fun onError(error: ExoPlaybackException) {
            e("$CLASS_NAME ExoPlayer exception:$error")
            handleStopRequest(
                    PlaybackStateError(getString(R.string.media_player_error), PlaybackStateError.Code.GENERAL)
            )
        }

        override fun onHandledError(error: ExoPlaybackException) {
            this@OpenRadioService.onHandledError(error)
        }

        override fun onPrepared() {
            this@OpenRadioService.onPrepared()
        }

        override fun onProgress(position: Long, bufferedPosition: Long, duration: Long) {
            mPosition = position
            mBufferedPosition = bufferedPosition
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            d("$CLASS_NAME OnPlayerStateChanged $playbackState")
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                    updatePlaybackState()
                }
                Player.STATE_READY -> {
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    updatePlaybackState()
                }
            }
        }
    }

    /**
     * An inner class that inherits from Handler and uses its
     * handleMessage() hook method to process Messages sent to
     * it from onStartCommand().
     */
    private inner class ServiceHandler
    /**
     * Class constructor initializes the Looper.
     *
     * @param looper The Looper that we borrow from HandlerThread.
     */
    constructor(looper: Looper) : Handler(looper) {
        /**
         * A factory method that creates a Message that contains
         * information of the command to perform.
         */
        fun makeMessage(intent: Intent): Message {
            val message = Message.obtain()
            message.obj = intent
            return message
        }

        /**
         * Hook method that process command sent from service.
         */
        override fun handleMessage(message: Message) {
            val intent = message.obj as Intent
            val bundle = intent.extras ?: return
            val command = bundle.getString(KEY_NAME_COMMAND_NAME)
            if (command == null || command.isEmpty()) {
                return
            }
            handleMessageInternal(command, intent)
        }
    }

    companion object {
        private lateinit var CLASS_NAME: String
        private const val KEY_NAME_COMMAND_NAME = "KEY_NAME_COMMAND_NAME"
        private const val VALUE_NAME_GET_RADIO_STATION_COMMAND = "VALUE_NAME_GET_RADIO_STATION_COMMAND"
        private const val VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND = "VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND"
        private const val VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND = "VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND"
        private const val VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND = "VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND"
        private const val VALUE_NAME_UPDATE_SORT_IDS = "VALUE_NAME_UPDATE_SORT_IDS"
        private const val VALUE_NAME_STOP_SERVICE = "VALUE_NAME_STOP_SERVICE"
        private const val VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM = "VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM"
        private const val VALUE_NAME_PLAY_LAST_PLAYED_ITEM = "VALUE_NAME_PLAY_LAST_PLAYED_ITEM"
        private const val VALUE_NAME_STOP_LAST_PLAYED_ITEM = "VALUE_NAME_STOP_LAST_PLAYED_ITEM"
        private const val VALUE_NAME_UPDATE_EQUALIZER = "VALUE_NAME_UPDATE_EQUALIZER"
        private const val VALUE_NAME_SLEEP_TIMER = "VALUE_NAME_SLEEP_TIMER"
        private const val EXTRA_KEY_MEDIA_DESCRIPTION = "EXTRA_KEY_MEDIA_DESCRIPTION"
        private const val EXTRA_KEY_IS_FAVORITE = "EXTRA_KEY_IS_FAVORITE"
        private const val EXTRA_KEY_STATION_NAME = "EXTRA_KEY_STATION_NAME"
        private const val EXTRA_KEY_STATION_STREAM_URL = "EXTRA_KEY_STATION_STREAM_URL"
        private const val EXTRA_KEY_STATION_IMAGE_URL = "EXTRA_KEY_STATION_IMAGE_URL"
        private const val EXTRA_KEY_STATION_THUMB_URL = "EXTRA_KEY_STATION_THUMB_URL"
        private const val EXTRA_KEY_STATION_GENRE = "EXTRA_KEY_STATION_GENRE"
        private const val EXTRA_KEY_STATION_COUNTRY = "EXTRA_KEY_STATION_COUNTRY"
        private const val EXTRA_KEY_STATION_ADD_TO_FAV = "EXTRA_KEY_STATION_ADD_TO_FAV"
        private const val EXTRA_KEY_MEDIA_ID = "EXTRA_KEY_MEDIA_ID"
        private const val EXTRA_KEY_MEDIA_IDS = "EXTRA_KEY_MEDIA_IDS"
        private const val EXTRA_KEY_SORT_IDS = "EXTRA_KEY_SORT_IDS"
        private const val EXTRA_KEY_RS_TO_ADD = "EXTRA_KEY_RS_TO_ADD"
        private const val EXTRA_KEY_SLEEP_TIMER_ENABLED = "EXTRA_KEY_SLEEP_TIMER_ENABLED"
        private const val EXTRA_KEY_SLEEP_TIMER_TIME = "EXTRA_KEY_SLEEP_TIMER_TIME"
        private const val BUNDLE_ARG_CATALOGUE_ID = "BUNDLE_ARG_CATALOGUE_ID"
        private const val BUNDLE_ARG_CURRENT_PLAYBACK_STATE = "BUNDLE_ARG_CURRENT_PLAYBACK_STATE"
        private const val BUNDLE_ARG_IS_RESTORE_STATE = "BUNDLE_ARG_IS_RESTORE_STATE"

        /**
         * Action to thumbs up a media item
         */
        private const val CUSTOM_ACTION_THUMBS_UP = "com.yuriy.openradio.share.service.THUMBS_UP"

        /**
         * Delay stop service by using a handler.
         */
        private const val STOP_DELAY = 30000

        const val API_CALL_TIMEOUT_MS = 3000L

        /**
         * Current media player state.
         */
        @Volatile
        var mState = 0

        var mCurrentParentId: String? = null

        var mIsRestoreState = false

        fun putCurrentParentId(bundle: Bundle?, currentParentId: String?) {
            if (bundle == null) {
                return
            }
            bundle.putString(BUNDLE_ARG_CATALOGUE_ID, currentParentId)
        }

        fun getCurrentParentId(bundle: Bundle?): String {
            return if (bundle == null) {
                ""
            } else bundle.getString(BUNDLE_ARG_CATALOGUE_ID, "")
        }

        fun putCurrentPlaybackState(bundle: Bundle?, value: Int) {
            if (bundle == null) {
                return
            }
            bundle.putInt(BUNDLE_ARG_CURRENT_PLAYBACK_STATE, value)
        }

        fun getCurrentPlaybackState(bundle: Bundle?): Int {
            return bundle?.getInt(BUNDLE_ARG_CURRENT_PLAYBACK_STATE, PlaybackStateCompat.STATE_NONE)
                    ?: PlaybackStateCompat.STATE_NONE
        }

        fun putRestoreState(bundle: Bundle?, value: Boolean) {
            if (bundle == null) {
                return
            }
            bundle.putBoolean(BUNDLE_ARG_IS_RESTORE_STATE, value)
        }

        fun getRestoreState(bundle: Bundle?): Boolean {
            return bundle?.getBoolean(BUNDLE_ARG_IS_RESTORE_STATE, false) ?: false
        }

        /**
         * Factory method to make intent to create custom [RadioStation].
         *
         * @return [Intent].
         */
        fun makeAddRadioStationIntent(context: Context?,
                                      value: RadioStationToAdd?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND)
            intent.putExtra(EXTRA_KEY_RS_TO_ADD, value)
            return intent
        }

        /**
         * Factory method to make intent to edit custom [RadioStation].
         *
         * @return [Intent].
         */
        fun makeEditRadioStationIntent(context: Context?,
                                       mediaId: String?,
                                       value: RadioStationToAdd): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND)
            intent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId)
            intent.putExtra(EXTRA_KEY_STATION_NAME, value.name)
            intent.putExtra(EXTRA_KEY_STATION_STREAM_URL, value.url)
            intent.putExtra(EXTRA_KEY_STATION_IMAGE_URL, value.imageLocalUrl)
            intent.putExtra(EXTRA_KEY_STATION_THUMB_URL, value.imageLocalUrl)
            intent.putExtra(EXTRA_KEY_STATION_GENRE, value.genre)
            intent.putExtra(EXTRA_KEY_STATION_COUNTRY, value.country)
            intent.putExtra(EXTRA_KEY_STATION_ADD_TO_FAV, value.isAddToFav)
            return intent
        }

        /**
         * Factory method to make Intent to remove custom [RadioStation].
         *
         * @param context Context of the callee.
         * @param mediaId Media Id of the Radio Station.
         * @return [Intent].
         */
        fun makeRemoveRadioStationIntent(context: Context?, mediaId: String?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND)
            intent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId)
            return intent
        }

        /**
         * Factory method to make Intent to update Sort Ids of the Radio Stations.
         *
         * @param context               Application context.
         * @param mediaId               Array of the Media Ids (of the Radio Stations).
         * @param sortId                Array of the corresponded Sort Ids.
         * @param parentCategoryMediaId ID of the current category ([etc ...][MediaIdHelper.MEDIA_ID_FAVORITES_LIST]).
         * @return [Intent].
         */
        fun makeUpdateSortIdsIntent(context: Context,
                                    mediaId: String,
                                    sortId: Int,
                                    parentCategoryMediaId: String): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_SORT_IDS)
            intent.putExtra(EXTRA_KEY_MEDIA_IDS, mediaId)
            intent.putExtra(EXTRA_KEY_SORT_IDS, sortId)
            intent.putExtra(EXTRA_KEY_MEDIA_ID, parentCategoryMediaId)
            return intent
        }

        /**
         * Make intent to stop service.
         *
         * @param context Context of the callee.
         * @return [Intent].
         */
        fun makeStopServiceIntent(context: Context?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_STOP_SERVICE)
            return intent
        }

        /**
         * Factory method to make [Intent] to update whether [RadioStation] is Favorite.
         *
         * @param context          Context of the callee.
         * @param mediaDescription [MediaDescriptionCompat] of the [RadioStation].
         * @param isFavorite       Whether Radio station is Favorite or not.
         * @return [Intent].
         */
        fun makeUpdateIsFavoriteIntent(context: Context?,
                                       mediaDescription: MediaDescriptionCompat?,
                                       isFavorite: Boolean): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_GET_RADIO_STATION_COMMAND)
            intent.putExtra(EXTRA_KEY_MEDIA_DESCRIPTION, mediaDescription)
            intent.putExtra(EXTRA_KEY_IS_FAVORITE, isFavorite)
            return intent
        }

        fun makeSleepTimerIntent(context: Context, enabled: Boolean, time: Long): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_SLEEP_TIMER)
            intent.putExtra(EXTRA_KEY_SLEEP_TIMER_ENABLED, enabled)
            intent.putExtra(EXTRA_KEY_SLEEP_TIMER_TIME, time)
            return intent
        }

        /**
         * @param context
         * @return
         */
        fun makeToggleLastPlayedItemIntent(context: Context?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM)
            return intent
        }

        fun makeStopLastPlayedItemIntent(context: Context?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_STOP_LAST_PLAYED_ITEM)
            return intent
        }

        fun makePlayLastPlayedItemIntent(context: Context?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_PLAY_LAST_PLAYED_ITEM)
            return intent
        }

        fun makeUpdateEqualizerIntent(context: Context?): Intent {
            val intent = Intent(context, OpenRadioService::class.java)
            intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_EQUALIZER)
            return intent
        }

        /**
         * Extract [.EXTRA_KEY_IS_FAVORITE] value from the [Intent].
         *
         * @param intent [Intent].
         * @return True in case of the key exists and it's value is True, False otherwise.
         */
        private fun getIsFavoriteFromIntent(intent: Intent?): Boolean {
            return (intent != null && intent.hasExtra(EXTRA_KEY_IS_FAVORITE)
                    && intent.getBooleanExtra(EXTRA_KEY_IS_FAVORITE, false))
        }

        private fun extractMediaDescription(intent: Intent?): MediaDescriptionCompat? {
            if (intent == null) {
                return MediaDescriptionCompat.Builder().build()
            }
            return if (!intent.hasExtra(EXTRA_KEY_MEDIA_DESCRIPTION)) {
                MediaDescriptionCompat.Builder().build()
            } else intent.getParcelableExtra(EXTRA_KEY_MEDIA_DESCRIPTION)
        }
    }

    /**
     * Default constructor.
     */
    init {
        CLASS_NAME = "ORS[" + hashCode() + "]"
        i(CLASS_NAME)
        setPlaybackState(PlaybackStateCompat.STATE_NONE)
        mRadioStationsComparator = RadioStationsComparator()
        mStartIds = ConcurrentLinkedQueue()
        mListener = ExoPlayerListener()
        mRadioStationsStorage = RadioStationsStorage()
        mDelayedStopHandler = DelayedStopHandler()
        mBTConnectionReceiver = BTConnectionReceiver(
                object : BTConnectionReceiver.Listener {
                    override fun onSameDeviceConnected() {
                        handleBTSameDeviceConnected()
                    }

                    override fun onDisconnected() {
                        handlePauseRequest(PauseReason.NOISY)
                    }
                }
        )
        mNoisyAudioStreamReceiver = BecomingNoisyReceiver(
                object : BecomingNoisyReceiver.Listener {
                    override fun onAudioBecomingNoisy() {
                        handlePauseRequest(PauseReason.NOISY)
                    }
                }
        )
        mConnectivityReceiver = ConnectivityReceiver(
                object : ConnectivityReceiver.Listener {
                    override fun onConnectivityChange(isConnected: Boolean) {
                        handleConnectivityChange(isConnected)
                    }
                }
        )
        mMasterVolumeBroadcastReceiver = MasterVolumeReceiver(
                object : MasterVolumeReceiverListener {
                    override fun onMasterVolumeChanged() {
                        setPlayerVolume()
                    }
                }
        )
        mClearCacheReceiver = ClearCacheReceiver(
                object : ClearCacheReceiverListener {
                    override fun onClearCache() {
                        handleClearCache()
                    }
                }
        )
        mDownloader = HTTPDownloaderImpl()
    }

    inner class SleepTimerListenerImpl : SleepTimerListener {

        override fun onComplete() {
            SleepTimerStorage.saveEnabled(applicationContext, false)
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                    AppLocalBroadcast.createIntentSleepTimer()
            )
            initInternals()
            handleStopRequest()
            stopSelfResultInt()
        }
    }
}
