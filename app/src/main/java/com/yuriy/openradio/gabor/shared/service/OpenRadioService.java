/*
 * Copyright 2017 - 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.gabor.shared.service;

import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.broadcast.AbstractReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.AppLocalBroadcast;
import com.yuriy.openradio.gabor.shared.broadcast.BTConnectionReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.BecomingNoisyReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.MasterVolumeReceiver;
import com.yuriy.openradio.gabor.shared.broadcast.RemoteControlReceiver;
import com.yuriy.openradio.gabor.shared.exo.ExoPlayerOpenRadioImpl;
import com.yuriy.openradio.gabor.shared.model.api.ApiServiceProvider;
import com.yuriy.openradio.gabor.shared.model.api.ApiServiceProviderImpl;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemAllCategories;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemChildCategories;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemCommand;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemCommandDependencies;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemCountriesList;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemCountryStations;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemFavoritesList;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemLocalsList;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemPopularStations;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemRecentlyAddedStations;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemRoot;
import com.yuriy.openradio.gabor.shared.model.media.item.MediaItemSearchFromApp;
import com.yuriy.openradio.gabor.shared.model.net.Downloader;
import com.yuriy.openradio.gabor.shared.model.net.HTTPDownloaderImpl;
import com.yuriy.openradio.gabor.shared.model.net.UrlBuilder;
import com.yuriy.openradio.gabor.shared.model.parser.JsonDataParserImpl;
import com.yuriy.openradio.gabor.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.gabor.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.gabor.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.gabor.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.gabor.shared.model.storage.LocationPreferencesManager;
import com.yuriy.openradio.gabor.shared.model.storage.RadioStationsStorage;
import com.yuriy.openradio.gabor.shared.model.storage.ServiceLifecyclePreferencesManager;
import com.yuriy.openradio.gabor.shared.model.storage.cache.CacheType;
import com.yuriy.openradio.gabor.shared.notification.MediaNotification;
import com.yuriy.openradio.gabor.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;
import com.yuriy.openradio.gabor.shared.utils.ConcurrentUtils;
import com.yuriy.openradio.gabor.shared.utils.FileUtils;
import com.yuriy.openradio.gabor.shared.utils.IntentUtils;
import com.yuriy.openradio.gabor.shared.utils.MediaIdHelper;
import com.yuriy.openradio.gabor.shared.utils.MediaItemHelper;
import com.yuriy.openradio.gabor.shared.utils.NetUtils;
import com.yuriy.openradio.gabor.shared.utils.PackageValidator;
import com.yuriy.openradio.gabor.shared.vo.PlaybackStateError;
import com.yuriy.openradio.gabor.shared.vo.RadioStation;
import com.yuriy.openradio.gabor.shared.vo.RadioStationToAdd;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import wseemann.media.jplaylistparser.exception.JPlaylistParserException;
import wseemann.media.jplaylistparser.parser.AutoDetectParser;
import wseemann.media.jplaylistparser.playlist.Playlist;
import wseemann.media.jplaylistparser.playlist.PlaylistEntry;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/13/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class OpenRadioService extends MediaBrowserServiceCompat
        implements AudioManager.OnAudioFocusChangeListener {

    private static String CLASS_NAME;
    private static final String KEY_NAME_COMMAND_NAME = "KEY_NAME_COMMAND_NAME";
    private static final String VALUE_NAME_GET_RADIO_STATION_COMMAND
            = "VALUE_NAME_GET_RADIO_STATION_COMMAND";
    private static final String VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND
            = "VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND";
    private static final String VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND
            = "VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND";
    private static final String VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND
            = "VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND";
    private static final String VALUE_NAME_UPDATE_SORT_IDS = "VALUE_NAME_UPDATE_SORT_IDS";
    private static final String VALUE_NAME_STOP_SERVICE = "VALUE_NAME_STOP_SERVICE";
    private static final String VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM = "VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM";
    private static final String VALUE_NAME_PLAY_LAST_PLAYED_ITEM = "VALUE_NAME_PLAY_LAST_PLAYED_ITEM";
    private static final String VALUE_NAME_STOP_LAST_PLAYED_ITEM = "VALUE_NAME_STOP_LAST_PLAYED_ITEM";
    private static final String VALUE_NAME_UPDATE_EQUALIZER = "VALUE_NAME_UPDATE_EQUALIZER";
    private static final String EXTRA_KEY_MEDIA_DESCRIPTION = "EXTRA_KEY_MEDIA_DESCRIPTION";
    private static final String EXTRA_KEY_IS_FAVORITE = "EXTRA_KEY_IS_FAVORITE";
    private static final String EXTRA_KEY_STATION_NAME = "EXTRA_KEY_STATION_NAME";
    private static final String EXTRA_KEY_STATION_STREAM_URL = "EXTRA_KEY_STATION_STREAM_URL";
    private static final String EXTRA_KEY_STATION_IMAGE_URL = "EXTRA_KEY_STATION_IMAGE_URL";
    private static final String EXTRA_KEY_STATION_THUMB_URL = "EXTRA_KEY_STATION_THUMB_URL";
    private static final String EXTRA_KEY_STATION_GENRE = "EXTRA_KEY_STATION_GENRE";
    private static final String EXTRA_KEY_STATION_COUNTRY = "EXTRA_KEY_STATION_COUNTRY";
    private static final String EXTRA_KEY_STATION_ADD_TO_FAV = "EXTRA_KEY_STATION_ADD_TO_FAV";
    private static final String EXTRA_KEY_MEDIA_ID = "EXTRA_KEY_MEDIA_ID";
    private static final String EXTRA_KEY_MEDIA_IDS = "EXTRA_KEY_MEDIA_IDS";
    private static final String EXTRA_KEY_SORT_IDS = "EXTRA_KEY_SORT_IDS";
    private static final String EXTRA_KEY_RS_TO_ADD = "EXTRA_KEY_RS_TO_ADD";
    private static final String BUNDLE_ARG_CATALOGUE_ID = "BUNDLE_ARG_CATALOGUE_ID";
    private static final String BUNDLE_ARG_CURRENT_PLAYBACK_STATE = "BUNDLE_ARG_CURRENT_PLAYBACK_STATE";
    private static final String BUNDLE_ARG_IS_RESTORE_STATE = "BUNDLE_ARG_IS_RESTORE_STATE";
    /**
     * Action to thumbs up a media item
     */
    private static final String CUSTOM_ACTION_THUMBS_UP = "com.yuriy.openradio.gabor.share.service.THUMBS_UP";
    /**
     * Delay stop service by using a handler.
     */
    private static final int STOP_DELAY = 30000;
    /**
     * ExoPlayer's implementation to play Radio stream.
     */
    private ExoPlayerOpenRadioImpl mExoPlayerORImpl;
    /**
     * Listener of the ExoPlayer's event.
     */
    private final ExoPlayerOpenRadioImpl.Listener mListener;
    /**
     * Media Session.
     */
    private MediaSessionCompat mSession;
    /**
     * Callback listener to listen media session events.
     */
    private MediaSessionCompat.Callback mMediaSessionCb;
    // TODO: reconsider Queue fields. This queue was intended to handle music files, not live stream.
    //       It has no sense in live stream.
    /**
     * Index of the current playing song.
     */
    private int mCurrentIndexOnQueue = -1;
    private String mCurrentStreamTitle;
    /**
     * Current media player state.
     */
    public static volatile int mState;
    private PauseReason mPauseReason = PauseReason.DEFAULT;
    /**
     * Wifi lock that we hold when streaming files from the internet,
     * in order to prevent the device from shutting off the Wifi radio.
     */
    private WifiManager.WifiLock mWifiLock;
    /**
     * Type of audio focus we have
     */
    private AudioFocus mAudioFocus = AudioFocus.NO_FOCUS_NO_DUCK;
    /**
     * audio manager object.
     */
    private AudioManager mAudioManager;
    /**
     * Collection of the Radio Stations.
     */
    private final RadioStationsStorage mRadioStationsStorage;
    private String mCurrentMediaId;
    /**
     * Indicates if we should start playing immediately after we gain focus.
     */
    private boolean mPlayOnFocusGain;
    /**
     * Notification object.
     */
    private MediaNotification mMediaNotification;
    /**
     * Flag that indicates whether application runs over normal Android or Auto version.
     */
    private boolean mIsAndroidAuto = false;
    /**
     * Flag that indicates whether application runs over normal Android or Android TV.
     */
    private boolean mIsTv = false;

    /**
     * Enumeration for the Audio Focus states.
     */
    private enum AudioFocus {
        /**
         * There is no audio focus, and no possible to "duck"
         */
        NO_FOCUS_NO_DUCK,
        /**
         * There is no focus, but can play at a low volume ("ducking")
         */
        NO_FOCUS_CAN_DUCK,
        /**
         * There is full audio focus
         */
        FOCUSED
    }

    private enum PauseReason {
        DEFAULT, NOISY
    }

    /**
     *
     */
    private final Handler mDelayedStopHandler;
    /**
     * Map of the Media Item commands that responsible for the Media Items List creation.
     */
    private final Map<String, MediaItemCommand> mMediaItemCommands = new HashMap<>();
    private long mPosition;
    private long mBufferedPosition;
    private String mLastPlayedUrl;
    public static String mCurrentParentId;
    public static boolean mIsRestoreState;
    private final MasterVolumeReceiver mMasterVolumeBroadcastReceiver;
    /**
     * The BroadcastReceiver that tracks network connectivity changes.
     */
    private final AbstractReceiver mConnectivityReceiver;
    private final BecomingNoisyReceiver mNoisyAudioStreamReceiver;
    private final BTConnectionReceiver mBTConnectionReceiver;
    /**
     * Track last selected Radio Station. This filed used when AA uses buffering/duration and the "Last Played"
     * Radio Station is not actually in any lists, it is single entity.
     */
    @Nullable
    private RadioStation mLastKnownRS;
    private RadioStation mRestoredRS;
    private ApiServiceProvider mApiServiceProvider;
    /**
     * Processes Messages sent to it from onStartCommand() that
     * indicate which command to process.
     */
    private volatile ServiceHandler mServiceHandler;
    private final Handler mMainHandler;
    /**
     *
     */
    @NonNull
    private final Downloader mDownloader;
    private final ConcurrentLinkedQueue<Integer> mStartIds;

    /**
     * Default constructor.
     */
    public OpenRadioService() {
        super();
        CLASS_NAME = "ORS[" + hashCode() + "] ";
        AppLogger.i(CLASS_NAME);
        setPlaybackState(PlaybackStateCompat.STATE_NONE);
        mStartIds = new ConcurrentLinkedQueue<>();
        mListener = new ExoPlayerListener();
        mRadioStationsStorage = new RadioStationsStorage();
        mDelayedStopHandler = new DelayedStopHandler();
        mMainHandler = new Handler(Looper.getMainLooper());
        mBTConnectionReceiver = new BTConnectionReceiver(
                new BTConnectionReceiver.Listener() {
                    @Override
                    public void onSameDeviceConnected() {
                        OpenRadioService.this.handleBTSameDeviceConnected();
                    }

                    @Override
                    public void onDisconnected() {
                        OpenRadioService.this.handlePauseRequest(PauseReason.NOISY);
                    }
                }
        );
        mNoisyAudioStreamReceiver = new BecomingNoisyReceiver(() -> handlePauseRequest(PauseReason.NOISY));
        mConnectivityReceiver = new ConnectivityReceiver(this::handleConnectivityChange);
        mMasterVolumeBroadcastReceiver = new MasterVolumeReceiver(this::setPlayerVolume);
        mDownloader = new HTTPDownloaderImpl();
    }

    public interface ResultListener {

        void onResult();
    }

    /**
     *
     */
    @SuppressLint("HandlerLeak")
    private final class DelayedStopHandler extends Handler {

        /**
         * Main constructor.
         */
        private DelayedStopHandler() {
            super();
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
            if ((mExoPlayerORImpl != null && mExoPlayerORImpl.isPlaying())) {
                AppLogger.d(CLASS_NAME + "Ignoring delayed stop since ExoPlayerORImpl in use.");
                return;
            }
            if (mPlayOnFocusGain) {
                AppLogger.d(CLASS_NAME + "Ignoring delayed stop since PlayOnFocusGain.");
                return;
            }
            AppLogger.d(CLASS_NAME + "Stopping service with delay handler.");
            stopSelfResultInt();
        }
    }

    @Override
    public final void onCreate() {
        final long start = System.currentTimeMillis();
        super.onCreate();

        AppLogger.i(CLASS_NAME + "On Create");
        final Context context = getApplicationContext();

        final UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            AppLogger.d(CLASS_NAME + "running on a TV Device");
            mIsTv = true;
        } else {
            AppLogger.d(CLASS_NAME + "running on a non-TV Device");
        }

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread, which we don't
        // want to block.
        final HandlerThread thread = new HandlerThread("ORS-Thread");
        thread.start();
        // Looper associated with the HandlerThread.
        final Looper looper = thread.getLooper();
        // Get the HandlerThread's Looper and use it for our Handler.
        mServiceHandler = new ServiceHandler(looper);

        mApiServiceProvider = new ApiServiceProviderImpl(context, new JsonDataParserImpl());

        mBTConnectionReceiver.register(context);
        mBTConnectionReceiver.locateDevice(context);

        // Add Media Items implementations to the map
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_ROOT, new MediaItemRoot());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_ALL_CATEGORIES, new MediaItemAllCategories());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_COUNTRIES_LIST, new MediaItemCountriesList());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_COUNTRY_STATIONS, new MediaItemCountryStations());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES, new MediaItemChildCategories());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_FAVORITES_LIST, new MediaItemFavoritesList());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST, new MediaItemLocalsList());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP, new MediaItemSearchFromApp());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_POPULAR_STATIONS, new MediaItemPopularStations());
        mMediaItemCommands.put(MediaIdHelper.MEDIA_ID_RECENT_ADDED_STATIONS, new MediaItemRecentlyAddedStations());

        mCurrentIndexOnQueue = -1;

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) context
                .getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "OpenRadio_lock");

        // Need this component for API 20 and earlier.
        // I wish to get rid of this because it keeps listen to broadcast even after application is destroyed :-(
        final ComponentName mediaButtonReceiver = new ComponentName(
                context, RemoteControlReceiver.class
        );

        // Start a new MediaSession
        mSession = new MediaSessionCompat(
                context,
                "OpenRadioService",
                mediaButtonReceiver,
                null
        );
        setSessionToken(mSession.getSessionToken());
        mMediaSessionCb = new MediaSessionCallback();
        mSession.setCallback(mMediaSessionCb);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaNotification = new MediaNotification(this);
        AnalyticsUtils.logMessage("OpenRadioService[" + this.hashCode() + "]->onCreate");
        mMediaNotification.notifyService("Application just started");

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mMasterVolumeBroadcastReceiver.register(context);

        ServiceLifecyclePreferencesManager.isServiceActive(context, true);

        AppLogger.i(CLASS_NAME + "Created in " + (System.currentTimeMillis() - start) + " ms");
    }

    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {
        AppLogger.i(CLASS_NAME + "On Start Command:" + intent + ", id:" + startId);
        AnalyticsUtils.logMessage(
                "OpenRadioService[" + this.hashCode() + "]->onStartCommand:" + intent
                        + ", " + IntentUtils.intentBundleToString(intent)
        );

        mStartIds.add(startId);

        if (intent != null) {
            sendMessage(intent);
        }

        AnalyticsUtils.logMessage("OpenRadioService[" + this.hashCode() + "]->onStartCommand:" + intent + " competed");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        AppLogger.d(CLASS_NAME + "On Destroy:" + hashCode());
        super.onDestroy();

        final Context context = getApplicationContext();

        ServiceLifecyclePreferencesManager.isServiceActive(context, false);

        mPlayOnFocusGain = false;
        if (mServiceHandler != null) {
            mServiceHandler.getLooper().quit();
        }
        mBTConnectionReceiver.unregister(context);
        mConnectivityReceiver.unregister(context);
        mNoisyAudioStreamReceiver.unregister(context);
        mMasterVolumeBroadcastReceiver.unregister(context);
        if (mApiServiceProvider instanceof ApiServiceProviderImpl) {
            ((ApiServiceProviderImpl) mApiServiceProvider).close();
        }

        stopService();
    }

    @Override
    public final BrowserRoot onGetRoot(@NonNull final String clientPackageName, final int clientUid,
                                       final Bundle rootHints) {
        AppLogger.d(CLASS_NAME + "clientPackageName=" + clientPackageName
                + ", clientUid=" + clientUid + ", rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!PackageValidator.isCallerAllowed(getApplicationContext(), clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            AppLogger.w(CLASS_NAME + "IGNORING request from untrusted package " + clientPackageName);
            return null;
        }
        if (AppUtils.isAutomotive(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
            AppLogger.i(CLASS_NAME + "Package name is Android Auto");
            mIsAndroidAuto = true;
        } else {
            AppLogger.i(CLASS_NAME + "Package name is not Android Auto");
            mIsAndroidAuto = false;
        }
        mCurrentParentId = getCurrentParentId(rootHints);
        mIsRestoreState = getRestoreState(rootHints);
        setPlaybackState(getCurrentPlaybackState(rootHints));

        return new BrowserRoot(MediaIdHelper.MEDIA_ID_ROOT, null);
    }

    @Override
    public final void onLoadChildren(@NonNull final String parentId,
                                     @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        AppLogger.i(CLASS_NAME + "OnLoadChildren " + parentId);
        boolean isSameCatalogue = false;
        // Check whether category had changed.
        if (TextUtils.equals(mCurrentParentId, parentId)) {
            isSameCatalogue = true;
        }

        mCurrentParentId = parentId;

        // If Parent Id contains Country Code - use it in the API.
        String countryCode = MediaIdHelper.getCountryCode(mCurrentParentId);
        if (TextUtils.isEmpty(countryCode)) {
            // Otherwise, use whatever is stored in preferences.
            countryCode = LocationPreferencesManager.getLastCountryCode(getApplicationContext());
        }

        final Context context = getApplicationContext();

        final MediaItemCommand command = mMediaItemCommands.get(MediaIdHelper.getId(mCurrentParentId));
        final MediaItemCommandDependencies dependencies = new MediaItemCommandDependencies(
                context, mDownloader, result, mRadioStationsStorage, mApiServiceProvider,
                countryCode, mCurrentParentId, mIsAndroidAuto, isSameCatalogue, mIsRestoreState,
                this::onResult
        );
        mIsRestoreState = false;
        if (command != null) {
            command.execute(
                    error -> OpenRadioService.this.updatePlaybackState(),
                    dependencies
            );
        } else {
            AppLogger.w(CLASS_NAME + "Skipping unmatched parentId: " + mCurrentParentId);
            result.sendResult(dependencies.getMediaItems());
        }
        // Registers BroadcastReceiver to track network connection changes.
        mConnectivityReceiver.register(context);
    }

    public boolean isTv() {
        return mIsTv;
    }

    /**
     * @param intent
     */
    private void sendMessage(final Intent intent) {
        // Create a Message that will be sent to ServiceHandler.
        final Message message = mServiceHandler.makeMessage(intent);
        // Send the Message to ServiceHandler.
        mServiceHandler.sendMessage(message);
    }

    /**
     * @param exception
     */
    private void onHandledError(final ExoPlaybackException exception) {
        AppLogger.e(CLASS_NAME + "ExoPlayer handled exception:" + exception);
        final Throwable throwable = exception.getCause();
        if (throwable instanceof UnrecognizedInputFormatException) {
            handleUnrecognizedInputFormatException();
        }
    }

    /**
     * Handles exception related to unrecognized url. Try to parse url deeply to extract actual stream one from
     * playlist.
     */
    private void handleUnrecognizedInputFormatException() {
        handleStopRequest(
                new PlaybackStateError("Can not get play url.", PlaybackStateError.Code.UNRECOGNIZED_URL)
        );
        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    final String[] urls = extractUrlsFromPlaylist(OpenRadioService.this.mLastPlayedUrl);
                    mMainHandler.post(
                            () -> {
                                // Silently clear last references and try to restart:
                                OpenRadioService.this.mLastKnownRS = null;
                                OpenRadioService.this.mLastPlayedUrl = null;
                                OpenRadioService.this.handlePlayListUrlsExtracted(urls);
                            }
                    );
                }
        );
    }

    private void handlePlayListUrlsExtracted(final String[] urls) {
        if (urls.length == 0) {
            handleStopRequest(new PlaybackStateError(getString(R.string.media_player_error)));
            return;
        }

        final RadioStation radioStation = getCurrentPlayingRadioStation();
        if (radioStation == null) {
            handleStopRequest(new PlaybackStateError(getString(R.string.media_player_error)));
            return;
        }
        // TODO: Refactor
        radioStation.getMediaStream().clear();
        radioStation.getMediaStream().setVariant(0, urls[0]);
        handlePlayRequest();
    }

    private String[] extractUrlsFromPlaylist(final String playlistUrl) {
        final HttpURLConnection connection = NetUtils.getHttpURLConnection(playlistUrl, "GET");
        if (connection == null) {
            return new String[0];
        }
        InputStream is = null;
        String[] result = null;

        try {
            final String contentType = connection.getContentType();
            is = connection.getInputStream();

            final AutoDetectParser parser = new AutoDetectParser(AppUtils.TIME_OUT);
            final Playlist playlist = new Playlist();
            parser.parse(playlistUrl, contentType, is, playlist);

            final int length = playlist.getPlaylistEntries().size();
            result = new String[length];
            AppLogger.d(CLASS_NAME + "Found " + length + " streams associated with " + playlistUrl);
            for (int i = 0; i < length; i++) {
                final PlaylistEntry entry = playlist.getPlaylistEntries().get(i);
                result[i] = entry.get(PlaylistEntry.URI);
                AppLogger.d(CLASS_NAME + " - " + result[i]);
            }
        } catch (final SocketTimeoutException e) {
            final String errorMessage = "Can not get urls from playlist at " + playlistUrl;
            AnalyticsUtils.logException(new Exception(errorMessage, e));
        } catch (final IOException | JPlaylistParserException e) {
            final String errorMessage = "Can not get urls from playlist at " + playlistUrl;
            AnalyticsUtils.logException(new Exception(errorMessage, e));
        } finally {
            NetUtils.closeHttpURLConnection(connection);
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    /**/
                }
            }
        }
        return result == null ? new String[0] : result;
    }

    private void onPrepared() {
        AppLogger.i(CLASS_NAME + "ExoPlayer prepared");

        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        final RadioStation radioStation = getCurrentPlayingRadioStation();
        // Save latest selected Radio Station.
        // Use it in Android Auto mode to display in the side menu as Latest Radio Station.
        if (radioStation != null) {
            LatestRadioStationStorage.add(radioStation, getApplicationContext());
        }
        configMediaPlayerState();
        updateMetadata(mCurrentStreamTitle);
    }

    @Override
    public final void onAudioFocusChange(int focusChange) {
        AppLogger.d(CLASS_NAME + "On AudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AudioFocus.FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AudioFocus.NO_FOCUS_CAN_DUCK : AudioFocus.NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we addToLocals the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            AppLogger.e(CLASS_NAME + "OnAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }

        configMediaPlayerState();
    }

    public static void putCurrentParentId(final Bundle bundle, final String currentParentId) {
        if (bundle == null) {
            return;
        }
        bundle.putString(BUNDLE_ARG_CATALOGUE_ID, currentParentId);
    }

    public static String getCurrentParentId(final Bundle bundle) {
        if (bundle == null) {
            return "";
        }
        return bundle.getString(BUNDLE_ARG_CATALOGUE_ID, "");
    }

    public static void putCurrentPlaybackState(final Bundle bundle, final int value) {
        if (bundle == null) {
            return;
        }
        bundle.putInt(BUNDLE_ARG_CURRENT_PLAYBACK_STATE, value);
    }

    public static int getCurrentPlaybackState(final Bundle bundle) {
        if (bundle == null) {
            return PlaybackStateCompat.STATE_NONE;
        }
        return bundle.getInt(BUNDLE_ARG_CURRENT_PLAYBACK_STATE, PlaybackStateCompat.STATE_NONE);
    }

    public static void putRestoreState(final Bundle bundle, final boolean value) {
        if (bundle == null) {
            return;
        }
        bundle.putBoolean(BUNDLE_ARG_IS_RESTORE_STATE, value);
    }

    public static boolean getRestoreState(final Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        return bundle.getBoolean(BUNDLE_ARG_IS_RESTORE_STATE, false);
    }

    /**
     * Factory method to make intent to create custom {@link RadioStation}.
     *
     * @return {@link Intent}.
     */
    public static Intent makeAddRadioStationIntent(final Context context,
                                                   final RadioStationToAdd value) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_RS_TO_ADD, value);
        return intent;
    }

    /**
     * Factory method to make intent to edit custom {@link RadioStation}.
     *
     * @return {@link Intent}.
     */
    public static Intent makeEditRadioStationIntent(final Context context,
                                                    final String mediaId,
                                                    final RadioStationToAdd value) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId);
        intent.putExtra(EXTRA_KEY_STATION_NAME, value.getName());
        intent.putExtra(EXTRA_KEY_STATION_STREAM_URL, value.getUrl());
        intent.putExtra(EXTRA_KEY_STATION_IMAGE_URL, value.getImageLocalUrl());
        intent.putExtra(EXTRA_KEY_STATION_THUMB_URL, value.getImageLocalUrl());
        intent.putExtra(EXTRA_KEY_STATION_GENRE, value.getGenre());
        intent.putExtra(EXTRA_KEY_STATION_COUNTRY, value.getCountry());
        intent.putExtra(EXTRA_KEY_STATION_ADD_TO_FAV, value.isAddToFav());
        return intent;
    }

    /**
     * Factory method to make Intent to remove custom {@link RadioStation}.
     *
     * @param context Context of the callee.
     * @param mediaId Media Id of the Radio Station.
     * @return {@link Intent}.
     */
    public static Intent makeRemoveRadioStationIntent(final Context context, final String mediaId) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_MEDIA_ID, mediaId);
        return intent;
    }

    /**
     * Factory method to make Intent to update Sort Ids of the Radio Stations.
     *
     * @param context          Application context.
     * @param mediaIds         Array of the Media Ids (of the Radio Stations).
     * @param sortIds          Array of the corresponded Sort Ids.
     * @param mCategoryMediaId ID of the current category
     *                         ({@link MediaIdHelper#MEDIA_ID_FAVORITES_LIST, etc ...}).
     * @return {@link Intent}.
     */
    public static Intent makeUpdateSortIdsIntent(final Context context,
                                                 final String[] mediaIds,
                                                 final int[] sortIds,
                                                 final String mCategoryMediaId) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_SORT_IDS);
        intent.putExtra(EXTRA_KEY_MEDIA_IDS, mediaIds);
        intent.putExtra(EXTRA_KEY_SORT_IDS, sortIds);
        intent.putExtra(EXTRA_KEY_MEDIA_ID, mCategoryMediaId);
        return intent;
    }

    /**
     * Make intent to stop service.
     *
     * @param context Context of the callee.
     * @return {@link Intent}.
     */
    public static Intent makeStopServiceIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_STOP_SERVICE);
        return intent;
    }

    /**
     * Factory method to make {@link Intent} to update whether {@link RadioStation} is Favorite.
     *
     * @param context          Context of the callee.
     * @param mediaDescription {@link MediaDescriptionCompat} of the {@link RadioStation}.
     * @param isFavorite       Whether Radio station is Favorite or not.
     * @return {@link Intent}.
     */
    public static Intent makeUpdateIsFavoriteIntent(final Context context,
                                                    final MediaDescriptionCompat mediaDescription,
                                                    final boolean isFavorite) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_GET_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_MEDIA_DESCRIPTION, mediaDescription);
        intent.putExtra(EXTRA_KEY_IS_FAVORITE, isFavorite);
        return intent;
    }

    /**
     * @param context
     * @return
     */
    public static Intent makeToggleLastPlayedItemIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM);
        return intent;
    }

    public static Intent makeStopLastPlayedItemIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_STOP_LAST_PLAYED_ITEM);
        return intent;
    }

    public static Intent makePlayLastPlayedItemIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_PLAY_LAST_PLAYED_ITEM);
        return intent;
    }

    public static Intent makeUpdateEqualizerIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_UPDATE_EQUALIZER);
        return intent;
    }

    /**
     * Updates Radio Station with the Sort Id by the given Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @param sortId  Sort Id to update to.
     */
    private void updateSortId(final String mediaId, final int sortId, final String categoryMediaId) {
        final RadioStation radioStation = mRadioStationsStorage.getById(mediaId);
        if (radioStation == null) {
            return;
        }
        radioStation.setSortId(sortId);
        // This call just overrides existing Radio Station in the storage.
        if (TextUtils.equals(MediaIdHelper.MEDIA_ID_FAVORITES_LIST, categoryMediaId)) {
            FavoritesStorage.add(radioStation, getApplicationContext());
        } else if (TextUtils.equals(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST, categoryMediaId)) {
            LocalRadioStationsStorage.add(radioStation, getApplicationContext());
        }
    }

    private void stopService() {
        if (AppUtils.isUiThread()) {
            stopServiceUiThread();
        } else {
            mMainHandler.postAtFrontOfQueue(this::stopServiceUiThread);
        }
    }

    private void stopServiceUiThread() {
        AppLogger.d(CLASS_NAME + "stop Service");
        // Service is being killed, so make sure we release our resources
        handleStopRequest();
        releaseExoPlayer();
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        if (mSession != null) {
            AppLogger.d(CLASS_NAME + "clear media session");
            mSession.setActive(false);
            mSession.setMediaButtonReceiver(null);
            mSession.setCallback(null);
            mSession.release();
            mSession = null;
            mMediaSessionCb = null;
        }
    }

    /**
     * Clear Exo Player and associated resources.
     */
    private void releaseExoPlayer() {
        mCurrentStreamTitle = null;
        if (mExoPlayerORImpl == null) {
            return;
        }
        mExoPlayerORImpl.release();
        mExoPlayerORImpl = null;
    }

    /**
     * Extract {@link #EXTRA_KEY_IS_FAVORITE} value from the {@link Intent}.
     *
     * @param intent {@link Intent}.
     * @return True in case of the key exists and it's value is True, False otherwise.
     */
    private static boolean getIsFavoriteFromIntent(final Intent intent) {
        return intent != null
                && intent.hasExtra(EXTRA_KEY_IS_FAVORITE)
                && intent.getBooleanExtra(EXTRA_KEY_IS_FAVORITE, false);
    }

    private static MediaDescriptionCompat extractMediaDescription(final Intent intent) {
        if (intent == null) {
            return new MediaDescriptionCompat.Builder().build();
        }
        if (!intent.hasExtra(EXTRA_KEY_MEDIA_DESCRIPTION)) {
            return new MediaDescriptionCompat.Builder().build();
        }
        return intent.getParcelableExtra(EXTRA_KEY_MEDIA_DESCRIPTION);
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mExoPlayerORImpl == null) {
            AppLogger.d(CLASS_NAME + "Create ExoPlayer");

            mExoPlayerORImpl = new ExoPlayerOpenRadioImpl(
                    getApplicationContext(),
                    mListener,
                    metadata -> {
                        AppLogger.d(CLASS_NAME + "Metadata title:" + metadata);
                        updateMetadata(metadata);
                    }
            );

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mExoPlayerORImpl.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            AppLogger.d(CLASS_NAME + "ExoPlayer prepared");
        } else {
            AppLogger.d(CLASS_NAME + "Reset ExoPlayer");

            mExoPlayerORImpl.reset();
        }
    }

    /**
     * Retrieve currently selected Radio Station asynchronously.<br>
     * If the URl is not yet obtained via API the it will be retrieved as well,
     * appropriate event will be dispatched via listener.
     *
     * @param listener {@link RadioStationUpdateListener}
     */
    private void getCurrentPlayingRSAsync(@NonNull final RadioStationUpdateListener listener) {
        final RadioStation radioStation = getCurrentPlayingRadioStation();
        if (radioStation == null) {
            listener.onComplete(null);
            return;
        }

        // This indicates that Radio Station's url was not downloaded.
        // Currently, when list of the stations received they comes without stream url
        // and bitrate, upon selecting one - it is necessary to load additional data.
        if (!radioStation.isMediaStreamEmpty()) {
            listener.onComplete(radioStation);
            return;
        }

        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    if (mApiServiceProvider == null) {
                        listener.onComplete(null);
                        return;
                    }
                    // Start download information about Radio Station
                    final RadioStation radioStationUpdated = mApiServiceProvider
                            .getStation(
                                    mDownloader,
                                    UrlBuilder.getStation(radioStation.getId()),
                                    CacheType.NONE
                            );
                    if (radioStationUpdated == null) {
                        AppLogger.e("Can not get Radio Station from internet");
                        listener.onComplete(radioStation);
                        return;
                    }
                    radioStation.setMediaStream(radioStationUpdated.getMediaStream());
                    listener.onComplete(radioStation);
                }
        );
    }

    private MediaMetadataCompat buildMetadata(final RadioStation radioStation) {
        if (radioStation.isMediaStreamEmpty()) {
            updatePlaybackState(new PlaybackStateError(getString(R.string.no_data_message)));
        }

        return MediaItemHelper.metadataFromRadioStation(getApplicationContext(), radioStation);
    }

    //TODO: Translate
    private static final String BUFFERING_STR = "Buffering...";

    /**
     * Updates Metadata for the currently playing Radio Station. This method terminates without
     * throwing exception if one of the stream parameters is invalid.
     */
    private void updateMetadata(final String streamTitle) {
        if (mSession == null) {
            AppLogger.e(CLASS_NAME + "update metadata with null media session");
            return;
        }
        if (!TextUtils.equals(BUFFERING_STR, streamTitle)) {
            mCurrentStreamTitle = streamTitle;
        }

        final RadioStation radioStation = getCurrentPlayingRadioStation();
        if (radioStation == null) {
            AppLogger.w(CLASS_NAME + "Can not update Metadata - Radio Station is null");
            setPlaybackState(PlaybackStateCompat.STATE_ERROR);
            updatePlaybackState(new PlaybackStateError(getString(R.string.no_metadata)));
            return;
        }
        final MediaMetadataCompat metadata = MediaItemHelper.metadataFromRadioStation(
                getApplicationContext(), radioStation, streamTitle
        );
        if (metadata == null) {
            AppLogger.w(CLASS_NAME + "Can not update Metadata - MediaMetadata is null");
            return;
        }
        final String trackId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        // TODO: Check whether we can use media id from Radio Station
        if (!TextUtils.equals(radioStation.getId(), trackId)) {
            AppLogger.w(
                    CLASS_NAME + "track ID '" + trackId
                            + "' should match mediaId '" + radioStation.getId() + "'"
            );
            return;
        }
        AppLogger.d(CLASS_NAME + "Updating metadata for MusicId:" + radioStation.getId() + ", title:" + streamTitle);
        try {
            mSession.setMetadata(metadata);
        } catch (final IllegalStateException e) {
            AppLogger.e(CLASS_NAME + "Can not set metadata:" + e);
        }
    }

    /**
     * Return current active Radio Station object.
     *
     * @return {@link RadioStation} or {@code null}.
     */
    @Nullable
    private RadioStation getCurrentPlayingRadioStation() {
        RadioStation radioStation = mRadioStationsStorage.getById(mCurrentMediaId);
        if (radioStation == null) {
            radioStation = mLastKnownRS;
        }
        if (radioStation == null) {
            radioStation = mRestoredRS;
        }
        return radioStation;
    }

    /**
     * Returns current queue item.
     *
     * @return
     */
    @Nullable
    private RadioStation getCurrentQueueItem() {
        return mRadioStationsStorage.getAt(mCurrentIndexOnQueue);
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the ExoPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        AppLogger.d(CLASS_NAME + "RelaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        // stop being a foreground service
        stopForeground(true);
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            releaseExoPlayer();
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * Handles event when Bluetooth connected to same device within application lifetime.
     */
    private void handleBTSameDeviceConnected() {
        final boolean autoPlay = AppPreferencesManager.isBtAutoPlay(getApplicationContext());
        AppLogger.d(
                CLASS_NAME + "BTSameDeviceConnected, do auto play:" + autoPlay
                        + ", state:" + MediaItemHelper.playbackStateToString(mState)
                        + ", pause reason:" + mPauseReason
        );
        if (!autoPlay) {
            return;
        }
        // Restore playback if it was paused by noisy receiver.
        if (mState == PlaybackStateCompat.STATE_PAUSED && mPauseReason == PauseReason.NOISY) {
            handlePlayRequest();
        }
    }

    /**
     * Handle a request to play Radio Station.
     */
    private void handlePlayRequest() {
        if (AppUtils.isUiThread()) {
            handlePlayRequestUiThread();
        } else {
            mMainHandler.post(this::handlePlayRequestUiThread);
        }
    }

    private void handlePlayRequestUiThread() {
        AppLogger.d(
                CLASS_NAME + "Handle PlayRequest: state=" + MediaItemHelper.playbackStateToString(mState)
        );
        if (mSession == null) {
            AppLogger.e(CLASS_NAME + "handle play with null media session");
            return;
        }
        mCurrentStreamTitle = null;
        final Context context = getApplicationContext();
        if (!ConnectivityReceiver.checkConnectivityAndNotify(context)) {
            return;
        }

        mDelayedStopHandler.removeCallbacksAndMessages(null);

        mPlayOnFocusGain = true;
        tryToGetAudioFocus();

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        // actually play the song
        if (mState == PlaybackStateCompat.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            configMediaPlayerState();
        } else {
            // If we're stopped or playing a song,
            // just go ahead to the new song and (re)start playing.
            getCurrentPlayingRSAsync(this::getCurrentPlayingRSAsyncCb);
        }

        mNoisyAudioStreamReceiver.register(context);
    }

    private void getCurrentPlayingRSAsyncCb(final RadioStation radioStation) {
        if (radioStation == null) {
            AppLogger.e(CLASS_NAME + "Play. Ignoring request to play next song, " +
                    "because cannot find it." +
                    " idx " + mCurrentIndexOnQueue);
            return;
        }
        if (mLastKnownRS != null && mLastKnownRS.equals(radioStation)) {
            AppLogger.e(CLASS_NAME + "Play. Ignoring request to play next song, " +
                    "because last known is the same as requested. Try to resume playback.");
            updatePlaybackState();
            return;
        }

        mLastKnownRS = radioStation;
        final MediaMetadataCompat metadata = buildMetadata(radioStation);
        if (metadata == null) {
            AppLogger.e(CLASS_NAME + "play. Ignoring request to play next song, " +
                    "because cannot find metadata." +
                    " idx " + mCurrentIndexOnQueue);
            return;
        }
        final String source = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI);
        AppLogger.d(
                CLASS_NAME + "play. idx " + mCurrentIndexOnQueue
                        + " id " + metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) +
                        " source " + source
        );
        if (TextUtils.isEmpty(source)) {
            AppLogger.e(CLASS_NAME + " source is empty");
            return;
        }

        mCurrentMediaId = radioStation.getId();

        preparePlayer(source);
    }

    private void preparePlayer(final String url) {
        if (url == null) {
            AppLogger.e(CLASS_NAME + " url is null");
            return;
        }

        // Cache URL.
        mLastPlayedUrl = url;
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        mPauseReason = PauseReason.DEFAULT;

        // release everything except ExoPlayer
        relaxResources(false);

        createMediaPlayerIfNeeded();

        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);

        AppLogger.d(CLASS_NAME + "Prepare " + mLastPlayedUrl);
        mExoPlayerORImpl.prepare(Uri.parse(mLastPlayedUrl));

        // If we are streaming from the internet, we want to hold a
        // Wifi lock, which prevents the Wifi radio from going to
        // sleep while the song is playing.
        mWifiLock.acquire();

        updatePlaybackState();
    }

    /**
     * Reconfigures ExoPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the ExoPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * ExoPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        AppLogger.d(CLASS_NAME + "ConfigAndStartMediaPlayer. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AudioFocus.NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                handlePauseRequest();
            }
        } else {
            if (mExoPlayerORImpl == null) {
                return;
            }
            // we have audio focus:
            setPlayerVolume();
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (!mExoPlayerORImpl.isPlaying()) {
                    AppLogger.d(CLASS_NAME + "ConfigAndStartMediaPlayer startMediaPlayer");
                    mExoPlayerORImpl.play();
                }
                mPlayOnFocusGain = false;
                AppLogger.d(CLASS_NAME + "ConfigAndStartMediaPlayer set state playing");
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            }
        }

        updatePlaybackState();
    }

    private void setPlayerVolume() {
        if (mExoPlayerORImpl == null) {
            AppLogger.e(CLASS_NAME + "can not set player volume, player null");
            return;
        }
        float volume = AppPreferencesManager.getMasterVolume(getApplicationContext()) / 100.0F;
        if (mAudioFocus == AudioFocus.NO_FOCUS_CAN_DUCK) {
            volume = (volume * 0.2F);
        }
        mExoPlayerORImpl.setVolume(volume);
    }

    /**
     * Handle a request to pause radio stream.
     */
    private void handlePauseRequest() {
        if (AppUtils.isUiThread()) {
            handlePauseRequest(PauseReason.DEFAULT);
        } else {
            mMainHandler.post(() -> handlePauseRequest(PauseReason.DEFAULT));
        }
    }

    /**
     * Handle a request to pause radio stream with reason provided.
     *
     * @param reason Reason to pause.
     */
    private void handlePauseRequest(final PauseReason reason) {
        AppLogger.d(CLASS_NAME + "HandlePauseRequest: state=" + MediaItemHelper.playbackStateToString(mState));

        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
            mPauseReason = reason;
            if (mExoPlayerORImpl != null && mExoPlayerORImpl.isPlaying()) {
                mExoPlayerORImpl.pause();
            }
            // while paused, retain the ExoPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }
        updatePlaybackState();
    }

    /**
     * Update the current media player state, optionally showing an error message.
     */
    private void updatePlaybackState() {
        updatePlaybackState(new PlaybackStateError());
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error Error object to present to the user.
     */
    private void updatePlaybackState(@NonNull final PlaybackStateError error) {
        AppLogger.d(
                CLASS_NAME + "set playback state to "
                        + MediaItemHelper.playbackStateToString(mState) + " error:" + error.toString()
        );
        if (mSession == null) {
            AppLogger.e(CLASS_NAME + "update playback with null media session");
            return;
        }
        final PlaybackStateCompat.Builder stateBuilder
                = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        setCustomAction(stateBuilder);

        // If there is an error message, send it to the playback state:
        if (error.getMsg() != null) {
            AppLogger.e(CLASS_NAME + "UpdatePlaybackState, error: " + error.toString());
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error.getMsg());
            setPlaybackState(PlaybackStateCompat.STATE_ERROR);
            mLastKnownRS = null;
            if (error.getCode() != PlaybackStateError.Code.UNRECOGNIZED_URL) {
                mLastPlayedUrl = null;
            }
        }

        stateBuilder.setBufferedPosition(mBufferedPosition);
        stateBuilder.setState(mState, mPosition, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
            final RadioStation item = getCurrentQueueItem();
            if (item != null) {
                // TODO: INVESTIGATE!!!
                stateBuilder.setActiveQueueItemId(mCurrentIndexOnQueue);
            }
        }

        // Update state only in case of play. Error cause "updatePlaybackState" which has "updateMetadata"
        // inside - infinite loop!
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            updateMetadata(BUFFERING_STR);
        }
        try {
            // Try to address issue on Android 4.1.2:
            // IllegalStateException: beginBroadcast() called while already in a broadcast
            mSession.setPlaybackState(stateBuilder.build());
        } catch (final IllegalStateException e) {
            AnalyticsUtils.logException(e);
        }

        if (mState == PlaybackStateCompat.STATE_BUFFERING
                || mState == PlaybackStateCompat.STATE_PLAYING
                || mState == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotification.startNotification(getApplicationContext(), getCurrentPlayingRadioStation());
        }
    }

    /**
     * Get available actions from media control buttons.
     *
     * @return Actions encoded in integer.
     */
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mRadioStationsStorage.size() <= 1) {
            return actions;
        }
        // Always show Prev and Next buttons, play index is handling on each listener (for instance, to handle loop
        // once end or beginning reached).
        actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        return actions;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        AppLogger.d(CLASS_NAME + "Try To Get Audio Focus, current focus:" + mAudioFocus);
        if (mAudioFocus == AudioFocus.FOCUSED) {
            return;
        }

        final int result = mAudioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        );

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        AppLogger.i(CLASS_NAME + "Audio Focus focused");
        mAudioFocus = AudioFocus.FOCUSED;
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        AppLogger.d(CLASS_NAME + "Give Up Audio Focus " + mAudioFocus);
        if (mAudioFocus != AudioFocus.FOCUSED) {
            return;
        }
        if (mAudioManager.abandonAudioFocus(this) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }
        mAudioFocus = AudioFocus.NO_FOCUS_NO_DUCK;
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest() {
        handleStopRequest(new PlaybackStateError());
    }

    /**
     * Handle a request to stop music.
     */
    private void handleStopRequest(@NonNull final PlaybackStateError error) {
        if (mState == PlaybackStateCompat.STATE_STOPPED) {
            return;
        }
        AppLogger.d(
                CLASS_NAME + "Handle stop request: state="
                        + MediaItemHelper.playbackStateToString(mState) + " error=" + error.toString()
        );

        setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        mPauseReason = PauseReason.DEFAULT;

        mNoisyAudioStreamReceiver.unregister(getApplicationContext());

        // let go of all resources...
        relaxResources(true);
        giveUpAudioFocus();

        if (mMediaNotification != null) {
            mMediaNotification.stopNotification();
            updatePlaybackState(error);
        }
    }

    private void onResult() {
        if (AppUtils.isUiThread()) {
            onResultUiThread();
        } else {
            mMainHandler.post(this::onResultUiThread);
        }
    }

    private void onResultUiThread() {
        if (!TextUtils.isEmpty(mCurrentMediaId) &&
                !mRadioStationsStorage.isEmpty()) {
            mCurrentIndexOnQueue = mRadioStationsStorage.getIndex(mCurrentMediaId);
            AppLogger.d(CLASS_NAME + "On result from command, index:" + mCurrentIndexOnQueue + ", " + mCurrentMediaId);
        }

        restoreActiveRadioStation();
    }

    private void restoreActiveRadioStation() {
        final Context context = getApplicationContext();
        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            if (mRestoredRS == null) {
                mRestoredRS = LatestRadioStationStorage.get(context);
            }
            if (mRestoredRS != null) {
                handlePlayFromMediaId(mRestoredRS.getId());
            }
            mRestoredRS = null;
        }
    }

    /**
     * Consume Radio Station by it's ID.
     *
     * @param mediaId ID of the Radio Station.
     */
    private void handlePlayFromMediaId(final String mediaId) {
        if (mediaId.equals("-1")) {
            updatePlaybackState(new PlaybackStateError(getString(R.string.no_data_message)));
            return;
        }

        mCurrentMediaId = mediaId;

        if (!ConnectivityReceiver.checkConnectivityAndNotify(getApplicationContext())) {
            return;
        }

        // Use this flag to compare indexes of the items later on.
        // Do not compare indexes if state is not play.
        if (mState == PlaybackStateCompat.STATE_PAUSED) {
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        }

        final int tempIndexOnQueue = mRadioStationsStorage.getIndex(mCurrentMediaId);
        if (tempIndexOnQueue != -1) {
            mCurrentIndexOnQueue = tempIndexOnQueue;
        }

        // Play Radio Station
        handlePlayRequest();
    }

    private void setCustomAction(final PlaybackStateCompat.Builder stateBuilder) {
        getCurrentPlayingRSAsync(
                radioStation -> {
                    if (radioStation == null) {
                        return;
                    }

                    int favoriteIcon = R.drawable.ic_favorite_off;
                    if (FavoritesStorage.isFavorite(radioStation, getApplicationContext())) {
                        favoriteIcon = R.drawable.ic_favorite_on;
                    }
                    stateBuilder.addCustomAction(
                            CUSTOM_ACTION_THUMBS_UP,
                            OpenRadioService.this.getString(R.string.favorite),
                            favoriteIcon
                    );
                }
        );
    }

    private void handleConnectivityChange(final boolean isConnected) {
        if (mState != PlaybackStateCompat.STATE_PLAYING) {
            return;
        }
        if (isConnected) {
            handlePlayRequest();
        }
    }

    /**
     *
     */
    private final class MediaSessionCallback extends MediaSessionCompat.Callback {

        private final String CLASS_NAME = MediaSessionCallback.class.getSimpleName() + " ";

        /**
         * Main constructor.
         */
        private MediaSessionCallback() {
            super();
        }

        @Override
        public void onPlay() {
            super.onPlay();
            AppLogger.i(CLASS_NAME + "On Play" + " [ors:" + OpenRadioService.this.hashCode() + "]");
            if (mRadioStationsStorage.isEmpty()) {
                // Start playing from the beginning of the queue.
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        }

        @Override
        public void onSkipToQueueItem(final long id) {
            super.onSkipToQueueItem(id);
            AppLogger.i(
                    CLASS_NAME + "On Skip to queue item, id:" + id
                            + " [ors:" + OpenRadioService.this.hashCode() + "]"
            );

            if (mState == PlaybackStateCompat.STATE_PAUSED) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
            }

            if (mRadioStationsStorage.isEmpty()) {
                return;
            }

            // set the current index on queue from the music Id:
            mCurrentIndexOnQueue = mRadioStationsStorage.getIndex(String.valueOf(id));
            if (mCurrentIndexOnQueue == RadioStationsStorage.UNKNOWN_INDEX) {
                return;
            }

            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue);

            // Play the Radio Station
            handlePlayRequest();
        }

        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            AppLogger.i(
                    CLASS_NAME + "On Play from media id:" + mediaId
                            + " extras:" + IntentUtils.bundleToString(extras)
                            + " [ors:" + OpenRadioService.this.hashCode() + "]"
            );
            handlePlayFromMediaId(mediaId);
        }

        @Override
        public void onPause() {
            super.onPause();
            AppLogger.i(CLASS_NAME + "On Pause" + " [ors:" + OpenRadioService.this.hashCode() + "]");
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            super.onStop();
            AppLogger.i(CLASS_NAME + "On Stop" + " [ors:" + OpenRadioService.this.hashCode() + "]");
            handleStopRequest();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            AppLogger.i(
                    CLASS_NAME + mCurrentIndexOnQueue
                            + " skip to " + (mCurrentIndexOnQueue + 1)
                            + " [ors:" + OpenRadioService.this.hashCode() + "]"
            );
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mRadioStationsStorage.size()) {
                mCurrentIndexOnQueue = 0;
            }
            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue);
            if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED);

                RadioStation rs = getCurrentQueueItem();
                if (rs != null) {
                    mCurrentMediaId = rs.getId();
                }

                handlePlayRequest();
            } else {
                AppLogger.e(CLASS_NAME + "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" + mRadioStationsStorage.size());

                handleStopRequest(new PlaybackStateError(getString(R.string.can_not_skip)));
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            AppLogger.i(CLASS_NAME + "skip to previous" + " [ors:" + OpenRadioService.this.hashCode() + "]");

            mCurrentIndexOnQueue--;
            if (mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = mRadioStationsStorage.size() - 1;
            }
            dispatchCurrentIndexOnQueue(mCurrentIndexOnQueue);
            if (mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED);

                RadioStation rs = getCurrentQueueItem();
                if (rs != null) {
                    mCurrentMediaId = rs.getId();
                }

                handlePlayRequest();
            } else {
                AppLogger.e(CLASS_NAME + "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" + mRadioStationsStorage.size());

                handleStopRequest(new PlaybackStateError(getString(R.string.can_not_skip)));
            }
        }

        @Override
        public void onCustomAction(@NonNull final String action, final Bundle extras) {
            super.onCustomAction(action, extras);
            AppLogger.i(CLASS_NAME + "custom Action:" + action + " [ors:" + OpenRadioService.this.hashCode() + "]");

            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                getCurrentPlayingRSAsync(
                        radioStation -> {
                            final Context context = OpenRadioService.this.getApplicationContext();
                            if (radioStation != null) {
                                final boolean isFavorite = FavoritesStorage.isFavorite(
                                        radioStation, context
                                );
                                if (isFavorite) {
                                    FavoritesStorage.remove(radioStation, context);
                                } else {
                                    FavoritesStorage.add(radioStation, context);
                                }
                            }

                            // playback state needs to be updated because the "Favorite" icon on the
                            // custom action will change to reflect the new favorite state.
                            updatePlaybackState();
                        }
                );
            } else {
                AppLogger.e(CLASS_NAME + "Unsupported action: " + action);
            }
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            AppLogger.i(
                    CLASS_NAME + "play from search:" + query
                            + " extras:" + IntentUtils.bundleToString(extras)
                            + " [ors:" + OpenRadioService.this.hashCode() + "]"
            );
            super.onPlayFromSearch(query, extras);

            performSearch(query);
        }

        private volatile long mLastKeyEventTime = 0;

        @Override
        public boolean onMediaButtonEvent(final Intent intent) {
            // Prevent double event.
            // TODO: Need to investigate
            if (mLastKeyEventTime != 0 && System.currentTimeMillis() - mLastKeyEventTime <= 1000) {
                return true;
            }
            mLastKeyEventTime = System.currentTimeMillis();

            AppLogger.i(
                    CLASS_NAME + "media btn evnt:" + intent
                            + " extra:" + IntentUtils.intentBundleToString(intent)
                            + " [ors:" + OpenRadioService.this.hashCode() + "]"
            );

            final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            final int keyCode = event != null ? event.getKeyCode() : Integer.MIN_VALUE;
            AppLogger.d(CLASS_NAME + "KeyCode:" + keyCode);
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    onPlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (mExoPlayerORImpl == null) {
                        return false;
                    }
                    if (mExoPlayerORImpl.isPlaying()) {
                        onPause();
                    } else {
                        onPlay();
                    }
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    onPause();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    onStop();
                    return true;
                default:
                    AppLogger.w(CLASS_NAME + " Unhandled key code:" + keyCode);
                    return false;
            }
        }
    }

    private void performSearch(final String query) {
        AppLogger.i(CLASS_NAME + "Search for:" + query);

        if (TextUtils.isEmpty(query)) {
            // A generic search like "Play music" sends an empty query
            // and it's expected that we start playing something.
            // TODO
            handleStopRequest(new PlaybackStateError(getString(R.string.no_search_results)));
            return;
        }

        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    try {
                        executePerformSearch(query);
                    } catch (final Exception e) {
                        handleStopRequest(new PlaybackStateError(getString(R.string.no_search_results)));
                        AnalyticsUtils.logException(e);
                    }
                }
        );
    }

    /**
     * Execute actual search.
     *
     * @param query Search query.
     */
    private void executePerformSearch(final String query) {
        if (mApiServiceProvider == null) {
            handleStopRequest(new PlaybackStateError(getString(R.string.no_search_results)));
            // TODO
            return;
        }

        final List<RadioStation> list = mApiServiceProvider.getStations(
                mDownloader,
                UrlBuilder.getSearchUrl(query),
                CacheType.NONE
        );

        if (list == null || list.isEmpty()) {
            // if nothing was found, we need to warn the user and stop playing
            handleStopRequest(new PlaybackStateError(getString(R.string.no_search_results)));
            // TODO
            return;
        }

        AppLogger.i(CLASS_NAME + "Found " + list.size() + " items");

        mRadioStationsStorage.clearAndCopy(list);

        // immediately start playing from the beginning of the search results
        mCurrentIndexOnQueue = 0;

        handlePlayRequest();
    }

    /**
     * Dispatch broad cast event about changes on current playing Radio Station.
     *
     * @param index Index of the Radio Station in the queue.
     */
    private void dispatchCurrentIndexOnQueue(final int index) {
        if (!mRadioStationsStorage.isIndexPlayable(mCurrentIndexOnQueue)) {
            AppLogger.w(CLASS_NAME + "Can not dispatch curr index on queue");
            return;
        }
        final RadioStation item = getCurrentQueueItem();
        String mediaId = "";
        if (item != null) {
            mediaId = item.getId();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                AppLocalBroadcast.createIntentCurrentIndexOnQueue(index, mediaId)
        );
    }

    /**
     * This method executed in separate thread.
     *
     * @param command
     * @param intent
     */
    private void handleMessageInternal(@NonNull final String command, @NonNull final Intent intent) {
        final Context context = getApplicationContext();
        AppLogger.d(CLASS_NAME + "rsv cmd:" + command);
        switch (command) {
            case VALUE_NAME_GET_RADIO_STATION_COMMAND: {
                if (mMediaNotification != null) {
                    mMediaNotification.notifyService("Update Favorite Radio Station");
                }
                final MediaDescriptionCompat description = extractMediaDescription(intent);
                if (description == null) {
                    break;
                }
                RadioStation rs = mRadioStationsStorage.getById(description.getMediaId());
                // This can the a case when last known Radio Station is playing.
                // In this case it is not in a list of radio stations.
                // If it exists, let's compare its id with the id provided by intent.
                if (rs == null) {
                    if (mLastKnownRS != null
                            && TextUtils.equals(mLastKnownRS.getId(), description.getMediaId())) {
                        rs = RadioStation.makeCopyInstance(context, mLastKnownRS);
                    }
                    // We failed both cases, something went wrong ...
                    if (rs == null) {
                        break;
                    }

                }
                // Update Favorites Radio station: whether add it or remove it from the storage
                final boolean isFavorite = getIsFavoriteFromIntent(intent);
                if (isFavorite) {
                    FavoritesStorage.add(rs, context);
                } else {
                    FavoritesStorage.remove(rs, context);
                }
                break;
            }
            case VALUE_NAME_EDIT_CUSTOM_RADIO_STATION_COMMAND: {
                final String mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID);
                final String name = intent.getStringExtra(EXTRA_KEY_STATION_NAME);
                final String url = intent.getStringExtra(EXTRA_KEY_STATION_STREAM_URL);
                final String imageUrl = intent.getStringExtra(EXTRA_KEY_STATION_IMAGE_URL);
                final String genre = intent.getStringExtra(EXTRA_KEY_STATION_GENRE);
                final String country = intent.getStringExtra(EXTRA_KEY_STATION_COUNTRY);
                final boolean addToFav = intent.getBooleanExtra(
                        EXTRA_KEY_STATION_ADD_TO_FAV, false
                );

                String imageUrlLocal = FileUtils.copyExtFileToIntDir(context, imageUrl);
                if (imageUrlLocal == null) {
                    imageUrlLocal = imageUrl;
                } else {
                    FileUtils.deleteFile(imageUrl);
                }

                final boolean result = LocalRadioStationsStorage.update(
                        mediaId, context, name, url, imageUrlLocal, genre, country, addToFav
                );

                if (result) {
                    notifyChildrenChanged(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSSuccess(
                                    "Radio Station updated successfully"
                            )
                    );
                } else {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Can not update Radio Station"
                            )
                    );
                }
                break;
            }
            case VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND: {
                final RadioStationToAdd rsToAdd
                        = (RadioStationToAdd) intent.getSerializableExtra(EXTRA_KEY_RS_TO_ADD);
                if (rsToAdd == null) {
                    AppLogger.e(CLASS_NAME + " Radio Station to add is null");
                    break;
                }

                if (TextUtils.isEmpty(rsToAdd.getName())) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's name is invalid"
                            )
                    );
                    break;
                }

                final String url = rsToAdd.getUrl();
                if (TextUtils.isEmpty(url)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's url is invalid"
                            )
                    );
                    break;
                }

                if (!NetUtils.checkResource(url)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's stream is invalid"
                            )
                    );
                    break;
                }

                final String imageWebUrl = rsToAdd.getImageWebUrl();
                if (!NetUtils.checkResource(imageWebUrl)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's web image is invalid"
                            )
                    );
                }

                final String homePage = rsToAdd.getHomePage();
                if (!NetUtils.checkResource(homePage)) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(
                            AppLocalBroadcast.createIntentValidateOfRSFailed(
                                    "Radio Station's home page is invalid"
                            )
                    );
                }

                if (rsToAdd.isAddToServer()) {
                    final Pair<Uri, List<Pair<String, String>>> urlData = UrlBuilder.addStation(rsToAdd);
                    if (!mApiServiceProvider.addStation(
                            mDownloader, urlData.first, urlData.second, CacheType.NONE)) {
                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                AppLocalBroadcast.createIntentValidateOfRSFailed(
                                        "Radio Station can not be added to server"
                                )
                        );
                    } else {
                        AppLocalBroadcast.createIntentValidateOfRSSuccess("Radio Station added to server");
                    }
                }

                String imageUrlLocal = FileUtils.copyExtFileToIntDir(context, rsToAdd.getImageLocalUrl());
                if (imageUrlLocal == null) {
                    imageUrlLocal = rsToAdd.getImageLocalUrl();
                }

                final RadioStation radioStation = RadioStation.makeDefaultInstance(
                        context, LocalRadioStationsStorage.getId(context)
                );

                radioStation.setName(rsToAdd.getName());
                radioStation.getMediaStream().setVariant(0, url);
                radioStation.setImageUrl(imageUrlLocal);
                radioStation.setThumbUrl(imageUrlLocal);
                radioStation.setGenre(rsToAdd.getGenre());
                radioStation.setCountry(rsToAdd.getCountry());
                radioStation.setIsLocal(true);

                LocalRadioStationsStorage.add(radioStation, context);
                if (rsToAdd.isAddToFav()) {
                    FavoritesStorage.add(radioStation, context);
                }

                notifyChildrenChanged(MediaIdHelper.MEDIA_ID_ROOT);

                LocalBroadcastManager.getInstance(context).sendBroadcast(
                        AppLocalBroadcast.createIntentValidateOfRSSuccess("Radio Station added to local device")
                );

                break;
            }
            case VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND: {
                final String mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID);
                if (TextUtils.isEmpty(mediaId)) {
                    AppLogger.w(CLASS_NAME + "Can not remove Station, Media Id is empty");
                    break;
                }
                final RadioStation radioStation = mRadioStationsStorage.remove(mediaId);
                if (radioStation != null) {
                    FileUtils.deleteFile(radioStation.getImageUrl());
                    LocalRadioStationsStorage.remove(radioStation, context);
                }

                notifyChildrenChanged(MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST);
                break;
            }
            case VALUE_NAME_UPDATE_SORT_IDS:
                final String[] mediaIds = intent.getStringArrayExtra(EXTRA_KEY_MEDIA_IDS);
                final int[] sortIds = intent.getIntArrayExtra(EXTRA_KEY_SORT_IDS);
                final String categoryMediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID);
                if (mediaIds == null || sortIds == null) {
                    break;
                }
                // TODO: Optimize this algorithm, could be done in single iteration
                int counter = 0;
                for (final String mediaId : mediaIds) {
                    updateSortId(mediaId, sortIds[counter++], categoryMediaId);
                }
                break;
            case VALUE_NAME_TOGGLE_LAST_PLAYED_ITEM:
                if (mMediaNotification != null) {
                    mMediaNotification.notifyService("Toggle last Radio Station");
                }
                if (mState == PlaybackStateCompat.STATE_PLAYING) {
                    handlePauseRequest();
                } else if (mState == PlaybackStateCompat.STATE_PAUSED) {
                    handlePlayRequest();
                } else if (mState == PlaybackStateCompat.STATE_STOPPED) {
                    mLastKnownRS = null;
                    mLastPlayedUrl = null;
                    handlePlayRequest();
                } else {
                    AppLogger.w(
                            CLASS_NAME + "unhandled playback state:"
                                    + MediaItemHelper.playbackStateToString(mState)
                    );
                }
                break;
            case VALUE_NAME_STOP_LAST_PLAYED_ITEM:
                if (mMediaNotification != null) {
                    mMediaNotification.notifyService("Stop play last Radio Station");
                }
                handlePauseRequest();
                break;
            case VALUE_NAME_PLAY_LAST_PLAYED_ITEM:
                if (mMediaNotification != null) {
                    mMediaNotification.notifyService("Play last Radio Station");
                }
                handlePlayRequest();
                break;
            case VALUE_NAME_UPDATE_EQUALIZER:
                if (mExoPlayerORImpl != null) {
                    mExoPlayerORImpl.updateEqualizer();
                    mExoPlayerORImpl.saveState();
                }
                break;
            case VALUE_NAME_STOP_SERVICE:
                if (mMediaNotification != null) {
                    mMediaNotification.notifyService("Stop application");
                }
                mMainHandler.postAtFrontOfQueue(() -> {
                    mPlayOnFocusGain = false;
                    mLastPlayedUrl = null;
                    mLastKnownRS = null;
                    handleStopRequest();
                    stopSelfResultInt();
                });
                break;
            default:
                AppLogger.w(CLASS_NAME + "Unknown command:" + command);
        }
    }

    private void stopSelfResultInt() {
        while (!mStartIds.isEmpty()) {
            final int id = mStartIds.poll();
            final boolean result = stopSelfResult(id);
            AppLogger.i(CLASS_NAME + "service " + (result ? "stopped" : "not stopped") + " for " + id);
        }
    }

    private void setPlaybackState(final int state) {
        AppLogger.d(CLASS_NAME + "Set state " + MediaItemHelper.playbackStateToString(state));
        mState = state;
    }

    /**
     * Listener for Exo Player events.
     */
    private final class ExoPlayerListener implements ExoPlayerOpenRadioImpl.Listener {

        /**
         * Constructor.
         */
        private ExoPlayerListener() {
            super();
        }

        @Override
        public final void onError(final ExoPlaybackException exception) {
            AppLogger.e(CLASS_NAME + "ExoPlayer exception:" + exception);
            OpenRadioService.this.handleStopRequest(new PlaybackStateError(getString(R.string.media_player_error)));
        }

        @Override
        public void onHandledError(final ExoPlaybackException error) {
            OpenRadioService.this.onHandledError(error);
        }

        @Override
        public void onPrepared() {
            OpenRadioService.this.onPrepared();
        }

        @Override
        public void onProgress(final long position, final long bufferedPosition, final long duration) {
            mPosition = position;
            mBufferedPosition = bufferedPosition;
            updatePlaybackState();
        }

        @Override
        public void onPlaybackStateChanged(final int playbackState) {
            AppLogger.d(CLASS_NAME + "OnPlayerStateChanged " + playbackState);
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    setPlaybackState(PlaybackStateCompat.STATE_BUFFERING);
                    updatePlaybackState();
                    break;
                case Player.STATE_READY:
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
                    updatePlaybackState();
                default:
                    break;
            }
        }
    }

    /**
     * An inner class that inherits from Handler and uses its
     * handleMessage() hook method to process Messages sent to
     * it from onStartCommand().
     */
    private final class ServiceHandler extends Handler {

        /**
         * Class constructor initializes the Looper.
         *
         * @param looper The Looper that we borrow from HandlerThread.
         */
        private ServiceHandler(final Looper looper) {
            super(looper);
        }

        /**
         * A factory method that creates a Message that contains
         * information of the command to perform.
         */
        private Message makeMessage(final Intent intent) {
            final Message message = Message.obtain();
            message.obj = intent;
            return message;
        }

        /**
         * Hook method that process command sent from service.
         */
        @Override
        public void handleMessage(final Message message) {
            final Intent intent = (Intent) message.obj;
            if (intent == null) {
                return;
            }
            final Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }
            final String command = bundle.getString(KEY_NAME_COMMAND_NAME);
            if (command == null || command.isEmpty()) {
                return;
            }
            OpenRadioService.this.handleMessageInternal(command, intent);
        }
    }
}