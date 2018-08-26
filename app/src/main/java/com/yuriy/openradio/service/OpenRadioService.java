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

package com.yuriy.openradio.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.APIServiceProviderImpl;
import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.business.RemoteControlReceiver;
import com.yuriy.openradio.business.mediaitem.MediaItemAllCategories;
import com.yuriy.openradio.business.mediaitem.MediaItemChildCategories;
import com.yuriy.openradio.business.mediaitem.MediaItemCommand;
import com.yuriy.openradio.business.mediaitem.MediaItemCountriesList;
import com.yuriy.openradio.business.mediaitem.MediaItemCountryStations;
import com.yuriy.openradio.business.mediaitem.MediaItemFavoritesList;
import com.yuriy.openradio.business.mediaitem.MediaItemLocalsList;
import com.yuriy.openradio.business.mediaitem.MediaItemParentCategories;
import com.yuriy.openradio.business.mediaitem.MediaItemPopularStations;
import com.yuriy.openradio.business.mediaitem.MediaItemRecentlyAddedStations;
import com.yuriy.openradio.business.mediaitem.MediaItemRoot;
import com.yuriy.openradio.business.mediaitem.MediaItemSearchFromApp;
import com.yuriy.openradio.business.mediaitem.MediaItemShareObject;
import com.yuriy.openradio.business.mediaitem.MediaItemStation;
import com.yuriy.openradio.business.storage.AppPreferencesManager;
import com.yuriy.openradio.business.storage.FavoritesStorage;
import com.yuriy.openradio.business.storage.LatestRadioStationStorage;
import com.yuriy.openradio.business.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.exo.ExoPlayerOpenRadioImpl;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.PackageValidator;
import com.yuriy.openradio.utils.QueueHelper;
import com.yuriy.openradio.utils.RadioStationChecker;
import com.yuriy.openradio.vo.RadioStation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = OpenRadioService.class.getSimpleName();

    private static final String ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead";

    private static final String KEY_NAME_COMMAND_NAME = "KEY_NAME_COMMAND_NAME";

    private static final String VALUE_NAME_REQUEST_LOCATION_COMMAND
            = "VALUE_NAME_REQUEST_LOCATION_COMMAND";

    private static final String VALUE_NAME_GET_RADIO_STATION_COMMAND
            = "VALUE_NAME_GET_RADIO_STATION_COMMAND";

    private static final String VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND
            = "VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND";

    private static final String VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND
            = "VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND";

    private static final String VALUE_NAME_UPDATE_SORT_IDS = "VALUE_NAME_UPDATE_SORT_IDS";

    private static final String EXTRA_KEY_MEDIA_DESCRIPTION = "EXTRA_KEY_MEDIA_DESCRIPTION";

    private static final String EXTRA_KEY_IS_FAVORITE = "EXTRA_KEY_IS_FAVORITE";

    private static final String EXTRA_KEY_ADD_STATION_NAME = "EXTRA_KEY_ADD_STATION_NAME";

    private static final String EXTRA_KEY_ADD_STATION_STREAM_URL
            = "EXTRA_KEY_ADD_STATION_STREAM_URL";

    private static final String EXTRA_KEY_ADD_STATION_IMAGE_URL
            = "EXTRA_KEY_ADD_STATION_IMAGE_URL";

    private static final String EXTRA_KEY_ADD_STATION_THUMB_URL
            = "EXTRA_KEY_ADD_STATION_THUMB_URL";

    private static final String EXTRA_KEY_ADD_STATION_GENRE
            = "EXTRA_KEY_ADD_STATION_GENRE";

    private static final String EXTRA_KEY_ADD_STATION_COUNTRY
            = "EXTRA_KEY_ADD_STATION_COUNTRY";

    private static final String EXTRA_KEY_ADD_STATION_ADD_TO_FAV
            = "EXTRA_KEY_ADD_STATION_ADD_TO_FAV";

    private static final String EXTRA_KEY_MEDIA_ID = "EXTRA_KEY_MEDIA_ID";

    private static final String EXTRA_KEY_MEDIA_IDS = "EXTRA_KEY_MEDIA_IDS";

    private static final String EXTRA_KEY_SORT_IDS = "EXTRA_KEY_SORT_IDS";

    /**
     * Action to thumbs up a media item
     */
    private static final String CUSTOM_ACTION_THUMBS_UP = "com.yuriy.openradio.service.THUMBS_UP";

    /**
     * Timeout for the response from the Radio Station's stream, in milliseconds.
     */
    private static final int RADIO_STATION_BUFFERING_TIMEOUT = 8000;

    /**
     * Delay stopSelf by using a handler.
     */
    private static final int STOP_DELAY = 30000;

    /**
     * The volume we set the media player to when we lose audio focus,
     * but are allowed to reduce the volume instead of stopping playback.
     */
    private static final float VOLUME_DUCK = 0.2f;

    /**
     * The volume we set the media player when we have audio focus.
     */
    private static final float VOLUME_NORMAL = 1.0f;

    /**
     * ExoPlayer's implementation to play Radio stream..
     */
    private ExoPlayerOpenRadioImpl mExoPlayer;

    /**
     * Listener of the ExoPlayer's event.
     */
    private final ExoPlayerOpenRadioImpl.Listener mListener = new ExoPlayerListener(this);

    /**
     * Media Session
     */
    private MediaSessionCompat mSession;

    /**
     * Index of the current playing song.
     */
    private int mCurrentIndexOnQueue = -1;

    /**
     * Queue of the Radio Stations in the Category
     */
    private final List<MediaSessionCompat.QueueItem> mPlayingQueue = new ArrayList<>();

    /**
     * Current local media player state
     */
    private int mState = PlaybackStateCompat.STATE_NONE;

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
     * Executor of the API requests.
     */
    private final ExecutorService mApiCallExecutor = Executors.newCachedThreadPool();

    /**
     * Collection of the Radio Stations.
     */
    private final List<RadioStation> mRadioStations = new ArrayList<>();

    /**
     * Indicates if we should start playing immediately after we gain focus.
     */
    private boolean mPlayOnFocusGain;

    /**
     * Indicates whether the service was started.
     */
    private boolean mServiceStarted;

    /**
     * Notification object.
     */
    private MediaNotification mMediaNotification;

    /**
     * Handler to manage response for the Radio Station's stream.
     */
    private Handler mRadioStationTimeoutHandler = new Handler();

    /**
     * Service class to provide information about current location.
     */
    private final LocationService mLocationService = LocationService.getInstance(mApiCallExecutor);

    /**
     * Listener of the Playback State changes.
     */
    private final MediaItemCommand.IUpdatePlaybackState mPlaybackStateListener = new PlaybackStateListener(this);

    /**
     * Flag that indicates whether application runs over normal Android or Auto version.
     */
    private boolean mIsAndroidAuto = false;

    /**
     * Indicates whether {@link #onBind(Intent)} has been called.
     */
    private boolean mIsBind;

    /**
     *
     */
    private final AtomicBoolean mIsPlayWhenReady = new AtomicBoolean(true);

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

    /**
     *
     */
    private final Handler mDelayedStopHandler = new DelayedStopHandler(this);

    /**
     * Map of the Media Item commands that responsible for the Media Items List creation.
     */
    private final Map<String, MediaItemCommand> mMediaItemCommands = new HashMap<>();

    /**
     *
     */
    private final RadioStationUpdateListener mRadioStationUpdateListener
            = new RadioStationUpdateListenerImpl(this);

    private long mPosition;

    private long mBufferedPosition;

    private String mLastPlayedUrl;

    private String mCurrentParentId;

    private MasterVolumeBroadcastReceiver mMasterVolumeBroadcastReceiver;

    /**
     * Default constructor.
     */
    public OpenRadioService() {
        super();
        final MasterVolumeBroadcastReceiverListener listener = new MasterVolumeEventListener(this);
        mMasterVolumeBroadcastReceiver = new MasterVolumeBroadcastReceiver(listener);
    }

    /**
     * Interface to link command implementation and Open Radio service.
     */
    public interface RemotePlay {

        /**
         * Play from known Media Id.
         *
         * @param mediaId Media Id of the Radio Station.
         */
        void playFromMediaId(final String mediaId);
    }

    /**
     *
     */
    private static class DelayedStopHandler extends Handler {

        /**
         * Reference to enclosing class.
         */
        private final OpenRadioService mReference;

        /**
         * Main constructor.
         *
         * @param reference Reference to enclosing class.
         */
        private DelayedStopHandler(final OpenRadioService reference) {
            mReference = reference;
        }

        @Override
        public void handleMessage(final Message msg) {
            if (mReference == null) {
                return;
            }
            if ((mReference.mExoPlayer != null && mReference.mExoPlayer.isPlaying())
                    || mReference.mPlayOnFocusGain) {
                AppLogger.d(CLASS_NAME + " Ignoring delayed stop since the media player is in use.");
                return;
            }
            AppLogger.d(CLASS_NAME + " Stopping service with delay handler.");

            mReference.stopSelf();

            mReference.mServiceStarted = false;
        }
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        AppLogger.i(CLASS_NAME + " On Create");

        // Add Media Items implementations to the map
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_ROOT, new MediaItemRoot());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES, new MediaItemAllCategories());
        //mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_ALL_STATIONS, new MediaItemAllStations());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_COUNTRIES_LIST, new MediaItemCountriesList());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_COUNTRY_STATIONS, new MediaItemCountryStations());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES, new MediaItemParentCategories());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES, new MediaItemChildCategories());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, new MediaItemStation());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_FAVORITES_LIST, new MediaItemFavoritesList());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST, new MediaItemLocalsList());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_SEARCH_FROM_APP, new MediaItemSearchFromApp());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_POPULAR_STATIONS, new MediaItemPopularStations());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_RECENT_ADDED_STATIONS, new MediaItemRecentlyAddedStations());

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread, which we don't
        // want to block.
        //final HandlerThread thread = new HandlerThread(
        //        OpenRadioService.class.getSimpleName() + "-MessagesThread"
        //);
        //thread.start();

        // Get the HandlerThread's Looper and use it for our Handler.
        //mMessagesHandler = new MessagesHandler(thread.getLooper());
        //mMessagesHandler.setReference(this);

        mCurrentIndexOnQueue = -1;

        final Context context = getApplicationContext();
        mLocationService.checkLocationEnable(context);
        mLocationService.requestCountryCodeLastKnownSync(context, mApiCallExecutor);

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) context
                .getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "OpenRadio_lock");

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        final ComponentName mediaButtonReceiver = new ComponentName(context, RemoteControlReceiver.class);

        // Start a new MediaSession
        mSession = new MediaSessionCompat(context, "OpenRadioService", mediaButtonReceiver, null);
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback(this));
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaNotification = new MediaNotification(this);
        mMasterVolumeBroadcastReceiver.register(context);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        AppLogger.i(CLASS_NAME + " On Bind: " + intent);
        mIsBind = true;
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        AppLogger.i(CLASS_NAME + " On Unbind: " + intent);
        if (mIsBind) {
            final Handler handler = new Handler();
            handler.post(this::stopService);
            mIsBind = false;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        AppLogger.i(CLASS_NAME + " On Config changed: " + newConfig);
    }

    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {

        AppLogger.i(CLASS_NAME + " On Start Command: " + intent);

        if (intent == null) {
            return super.onStartCommand(null, flags, startId);
        }

        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        final String command = bundle.getString(KEY_NAME_COMMAND_NAME);

        if (command == null || command.isEmpty()) {
            return super.onStartCommand(intent, flags, startId);
        }
        final Context context = getApplicationContext();
        switch (command) {
            case VALUE_NAME_REQUEST_LOCATION_COMMAND:
                mLocationService.requestCountryCode(
                        context,
                        countryCode -> LocalBroadcastManager.getInstance(context).sendBroadcast(
                                AppLocalBroadcast.createIntentLocationCountryCode(
                                        countryCode
                                )
                        )
                );
                break;
            case VALUE_NAME_GET_RADIO_STATION_COMMAND: {
                // Update Favorites Radio station: whether add it or remove it from the storage
                final boolean isFavorite = getIsFavoriteFromIntent(intent);
                final MediaDescriptionCompat mediaDescription = extractMediaDescription(intent);
                if (mediaDescription == null) {
                    return super.onStartCommand(intent, flags, startId);
                }
                final RadioStation radioStation = QueueHelper.getRadioStationById(
                        mediaDescription.getMediaId(), mRadioStations
                );
                if (radioStation == null) {
                    if (!isFavorite) {
                        removeFromFavorites(mediaDescription.getMediaId());
                    }
                    return super.onStartCommand(intent, flags, startId);
                }
                if (isFavorite) {
                    FavoritesStorage.addToFavorites(radioStation, context);
                } else {
                    removeFromFavorites(radioStation.getIdAsString());
                }
                break;
            }
            case VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND: {
                final String name = intent.getStringExtra(EXTRA_KEY_ADD_STATION_NAME);
                final String url = intent.getStringExtra(EXTRA_KEY_ADD_STATION_STREAM_URL);
                final String imageUrl = intent.getStringExtra(EXTRA_KEY_ADD_STATION_IMAGE_URL);
                final String genre = intent.getStringExtra(EXTRA_KEY_ADD_STATION_GENRE);
                final String country = intent.getStringExtra(EXTRA_KEY_ADD_STATION_COUNTRY);
                final boolean addToFav = intent.getBooleanExtra(
                        EXTRA_KEY_ADD_STATION_ADD_TO_FAV, false
                );

                if (!TextUtils.isEmpty(name)
                        && !TextUtils.isEmpty(url)) {
                    final RadioStation radioStationLocal = RadioStation.makeDefaultInstance();

                    radioStationLocal.setId(LocalRadioStationsStorage.getId(context));
                    radioStationLocal.setName(name);
                    radioStationLocal.setStreamURL(url);
                    radioStationLocal.setImageUrl(imageUrl);
                    radioStationLocal.setThumbUrl(imageUrl);
                    radioStationLocal.setGenre(genre);
                    radioStationLocal.setCountry(country);
                    radioStationLocal.setIsLocal(true);

                    LocalRadioStationsStorage.addToLocal(radioStationLocal, context);
                    if (addToFav) {
                        FavoritesStorage.addToFavorites(radioStationLocal, context);
                    }

                    notifyChildrenChanged(MediaIDHelper.MEDIA_ID_ROOT);

                    AppLogger.d(CLASS_NAME + " Add:" + radioStationLocal);
                } else {
                    AppLogger.w(CLASS_NAME + " Can not add Station, Name or url are empty");
                }
                break;
            }
            case VALUE_NAME_REMOVE_CUSTOM_RADIO_STATION_COMMAND: {
                final String mediaId = intent.getStringExtra(EXTRA_KEY_MEDIA_ID);
                if (TextUtils.isEmpty(mediaId)) {
                    AppLogger.w(CLASS_NAME + " Can not remove Station, Media Id is empty");
                    break;
                }
                LocalRadioStationsStorage.removeFromLocal(mediaId, context);
                QueueHelper.removeRadioStation(mediaId, mRadioStations);

                notifyChildrenChanged(MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST);

                AppLogger.d(CLASS_NAME + " Remove:" + mediaId);
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
            default:
                AppLogger.w(CLASS_NAME + " Unknown command:" + command);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        AppLogger.d(CLASS_NAME + " On Destroy");
        super.onDestroy();

        mMasterVolumeBroadcastReceiver.unregister(getApplicationContext());
        stopService();

        final ExecutorService executorService = getApiCallExecutor();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public final BrowserRoot onGetRoot(@NonNull final String clientPackageName, final int clientUid,
                                       final Bundle rootHints) {
        AppLogger.d(CLASS_NAME + " OnGetRoot: clientPackageName=" + clientPackageName
                + ", clientUid=" + clientUid + ", rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!PackageValidator.isCallerAllowed(getApplicationContext(), clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            AppLogger.w(CLASS_NAME + " OnGetRoot: IGNORING request from untrusted package "
                    + clientPackageName);
            return null;
        }
        //noinspection StatementWithEmptyBody
        if (ANDROID_AUTO_PACKAGE_NAME.equals(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
            AppLogger.i(CLASS_NAME + " Package name is Android Auto");
            mIsAndroidAuto = true;
        } else {
            AppLogger.i(CLASS_NAME + " Package name is not Android Auto");
            mIsAndroidAuto = false;
        }
        return new BrowserRoot(MediaIDHelper.MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadItem(final String itemId, @NonNull final Result<MediaBrowserCompat.MediaItem> result) {
        super.onLoadItem(itemId, result);

        AppLogger.i(CLASS_NAME + " OnLoadItem:" + itemId + ", res:" + result);
    }

    @Override
    public final void onLoadChildren(@NonNull final String parentId,
                                     @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {

        AppLogger.i(CLASS_NAME + " OnLoadChildren:" + parentId + ", res:" + result);

        boolean isSameCatalogue = false;
        // Check whether category had changed.
        if (TextUtils.equals(mCurrentParentId, parentId)) {
            isSameCatalogue = true;
        }

        mCurrentParentId = parentId;
        final List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = getServiceProvider(getApplicationContext());

        // If Parent Id contains Country Code - use it in the API.
        String countryCode = MediaIDHelper.getCountryCode(mCurrentParentId);
        if (TextUtils.isEmpty(countryCode)) {
            // If no Country Code founded - use device native one.
            countryCode = mLocationService.getCountryCode();
            AppLogger.d(CLASS_NAME + " country code:" + countryCode);
        }

        final MediaItemCommand command = mMediaItemCommands.get(MediaIDHelper.getId(mCurrentParentId));
        if (command != null) {

            final MediaItemShareObject shareObject = MediaItemShareObject.getDefaultInstance();
            shareObject.setContext(getApplicationContext());
            shareObject.setCountryCode(countryCode);
            shareObject.setDownloader(downloader);
            shareObject.setServiceProvider(serviceProvider);
            shareObject.setResult(result);
            shareObject.setMediaItems(mediaItems);
            shareObject.setParentId(mCurrentParentId);
            shareObject.setRadioStations(mRadioStations);
            shareObject.setIsAndroidAuto(mIsAndroidAuto);
            shareObject.isSameCatalogue(isSameCatalogue);
            shareObject.setRemotePlay(this::handleLastRadioStation);

            command.create(mPlaybackStateListener, shareObject);
        } else {
            AppLogger.w(CLASS_NAME + " Skipping unmatched parentId: " + mCurrentParentId);
            result.sendResult(mediaItems);
        }
    }

    private void onError(final ExoPlaybackException exception) {
        AppLogger.e(CLASS_NAME + " ExoPlayer exception:" + exception);
        handleStopRequest(getString(R.string.media_player_error));
    }

    private void onHandledError(final ExoPlaybackException exception) {
        AppLogger.e(CLASS_NAME + " ExoPlayer handled exception:" + exception);
        final Throwable throwable = exception.getCause();
        if (throwable != null && throwable instanceof UnrecognizedInputFormatException) {
            handleUnrecognizedInputFormatException();
        }
    }

    private void handleUnrecognizedInputFormatException() {
        handleStopRequest(null);

        mApiCallExecutor.submit(
                () -> {
                    final String[] urls = extractUrlsFromPlaylist(OpenRadioService.this.mLastPlayedUrl);
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(
                            () -> OpenRadioService.this.handlePlayListUrlsExtracted(urls)
                    );
                }
        );
    }

    private void handlePlayListUrlsExtracted(final String[] urls) {
        if (urls.length == 0) {
            handleStopRequest(getString(R.string.media_player_error));
            return;
        }

        final MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
        final String mediaId = item.getDescription().getMediaId();

        RadioStation radioStation;
        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
        }
        if (radioStation == null) {
            handleStopRequest(getString(R.string.media_player_error));
            return;
        }

        radioStation.setStreamURL(urls[0]);
        QueueHelper.updateRadioStation(radioStation, mRadioStations);
        OpenRadioService.this.handlePlayRequest();
    }

    private String[] extractUrlsFromPlaylist(final String playlistUrl) {
        URL url;
        HttpURLConnection conn = null;
        InputStream is = null;
        String[] result = null;

        try {
            url = new URL(playlistUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(RadioStationChecker.TIME_OUT);
            conn.setReadTimeout(RadioStationChecker.TIME_OUT);
            conn.setRequestMethod("GET");

            final String contentType = conn.getContentType();
            is = conn.getInputStream();

            final AutoDetectParser parser = new AutoDetectParser(RadioStationChecker.TIME_OUT);
            final Playlist playlist = new Playlist();
            parser.parse(url.toString(), contentType, is, playlist);

            final int length = playlist.getPlaylistEntries().size();
            result = new String[length];
            AppLogger.d("Found " + length + " streams associated with " + playlistUrl);
            for (int i = 0; i < length; i++) {
                final PlaylistEntry entry = playlist.getPlaylistEntries().get(i);
                result[i] = entry.get(PlaylistEntry.URI);
                AppLogger.d(" - " + result[i]);
            }
        } catch (final IOException | JPlaylistParserException e) {
            final String errorMessage = "Can not get urls from playlist at " + playlistUrl;
            FabricUtils.logException(new JPlaylistParserException(errorMessage, e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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
        AppLogger.i(CLASS_NAME + " ExoPlayer prepared (mIsPlayWhenReady):" + mIsPlayWhenReady);

        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        if (mIsPlayWhenReady.get()) {
            configMediaPlayerState();
        } else {
            handleStopRequest(null);
            mIsPlayWhenReady.set(true);
        }
    }

    @Override
    public final void onAudioFocusChange(int focusChange) {
        AppLogger.d(CLASS_NAME + " On AudioFocusChange. focusChange=" + focusChange);
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
            AppLogger.e(CLASS_NAME + " OnAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }

        configMediaPlayerState();
    }

    /**
     * Factory method to make intent to use for the request location procedure.
     *
     * @param context Context of the callee.
     * @return {@link Intent}.
     */
    public static Intent makeRequestLocationIntent(final Context context) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_REQUEST_LOCATION_COMMAND);
        return intent;
    }

    /**
     * Factory method to make intent to create custom {@link RadioStation}.
     *
     * @return {@link Intent}.
     */
    public static Intent makeAddRadioStationIntent(final Context context,
                                                   final String name, final String url,
                                                   final String imageUrl, final String genre,
                                                   final String country, final boolean addToFav) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_ADD_CUSTOM_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_ADD_STATION_NAME, name);
        intent.putExtra(EXTRA_KEY_ADD_STATION_STREAM_URL, url);
        intent.putExtra(EXTRA_KEY_ADD_STATION_IMAGE_URL, imageUrl);
        intent.putExtra(EXTRA_KEY_ADD_STATION_THUMB_URL, imageUrl);
        intent.putExtra(EXTRA_KEY_ADD_STATION_GENRE, genre);
        intent.putExtra(EXTRA_KEY_ADD_STATION_COUNTRY, country);
        intent.putExtra(EXTRA_KEY_ADD_STATION_ADD_TO_FAV, addToFav);
        return intent;
    }

    /**
     * Factory method to make Intent to remove custom {@link RadioStation}.
     *
     * @param context Context of the callee.
     * @param mediaId Media Id of the Radio Station.
     *
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
     *                         ({@link MediaIDHelper#MEDIA_ID_FAVORITES_LIST, etc ...}).
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
     * Updates Radio Station with the Sort Id by the given Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @param sortId  Sort Id to update to.
     */
    private void updateSortId(final String mediaId, final int sortId, final String categoryMediaId) {
        RadioStation radioStation;
        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
            if (radioStation != null) {
                radioStation.setSortId(sortId);
            }
        }
        if (radioStation != null) {
            // This call just overrides existing Radio Station in the storage.
            if (TextUtils.equals(MediaIDHelper.MEDIA_ID_FAVORITES_LIST, categoryMediaId)) {
                FavoritesStorage.addToFavorites(radioStation, getApplicationContext());
            } else if (TextUtils.equals(MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST, categoryMediaId)) {
                LocalRadioStationsStorage.addToLocal(radioStation, getApplicationContext());
            }
        }
    }

    private void stopService() {
        AppLogger.d(CLASS_NAME + " stop Service");
        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();

        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
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
        if (mExoPlayer == null) {
            AppLogger.d(CLASS_NAME + " Create ExoPlayer");

            mExoPlayer = new ExoPlayerOpenRadioImpl(
                    getApplicationContext(),
                    mListener,
                    metadata -> {
                        AppLogger.d("Metadata map:" + metadata);
                        String streamTitle = metadata.get("StreamTitle");
                        if (TextUtils.isEmpty(streamTitle)) {
                            streamTitle = "";
                        }
                        updateMetadata(streamTitle);
                    }
            );

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mExoPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            AppLogger.d(CLASS_NAME + " ExoPlayer prepared");
        } else {
            AppLogger.d(CLASS_NAME + " Reset ExoPlayer");

            mExoPlayer.reset();
        }
    }

    /**
     * Retrieve currently selected Radio Station asynchronously.<br>
     * If the URl is not yet obtained via API the it will be retrieved as well,
     * appropriate event will be dispatched via listener.
     *
     * @param listener {@link RadioStationUpdateListener}
     */
    private void getCurrentPlayingRadioStationAsync(final RadioStationUpdateListener listener) {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        final MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
        if (item == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        final String mediaId = item.getDescription().getMediaId();

        RadioStation radioStation;
        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
        }

        // Make a copy of the Radio Station
        final RadioStation radioStationCopy = radioStation;

        if (radioStationCopy == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        // Save latest selected Radio Station.
        // Use it in Android Auto mode to display in the side menu as Latest Radio Station.
        LatestRadioStationStorage.addToLocals(radioStation, getApplicationContext());

        // If there is no detailed information about Radio Station - download it here and
        // update model.

        if (radioStationCopy.getStreamURL() != null && !radioStationCopy.getStreamURL().isEmpty()) {
            radioStationCopy.setIsUpdated(true);
        }

        if (!radioStationCopy.getIsUpdated()) {

            final ExecutorService executorService = getApiCallExecutor();
            if (executorService != null) {
                executorService.submit(
                        () -> {
                            // Start download information about Radio Station
                            final RadioStation radioStationUpdated = getServiceProvider(OpenRadioService.this.getApplicationContext()).getStation(
                                    new HTTPDownloaderImpl(),
                                    UrlBuilder.getStation(
                                            getApplicationContext(),
                                            radioStationCopy.getIdAsString()
                                    )
                            );
                            radioStationCopy.setStreamURL(radioStationUpdated.getStreamURL());
                            radioStationCopy.setBitRate(radioStationUpdated.getBitRate());
                            radioStationCopy.setIsUpdated(true);

                            if (listener != null) {
                                listener.onComplete(buildMetadata(radioStationCopy));
                            }
                        }
                );
            }
        } else {
            if (listener != null) {
                listener.onComplete(buildMetadata(radioStationCopy));
            }
        }
    }

    private MediaMetadataCompat buildMetadata(final RadioStation radioStation) {
        if (radioStation.getStreamURL() == null || radioStation.getStreamURL().isEmpty()) {
            updatePlaybackState(getString(R.string.no_data_message));
        }

        return MediaItemHelper.buildMediaMetadataFromRadioStation(
                getApplicationContext(),
                radioStation
        );
    }

    /**
     * Updates Metadata for the currently playing Radio Station. This method terminates without
     * throwing exception if one of the stream parameters is invalid.
     */
    private void updateMetadata(final String streamTitle) {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            AppLogger.e(
                    CLASS_NAME + " Can't retrieve current metadata, curIndx:"
                            + mCurrentIndexOnQueue + " queueSize:" + mPlayingQueue.size()
            );
            mState = PlaybackStateCompat.STATE_ERROR;
            updatePlaybackState(getString(R.string.no_metadata));
            return;
        }

        final MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        if (queueItem == null) {
            AppLogger.w(CLASS_NAME + " Can not update Metadata - QueueItem is null");
            return;
        }
        if (queueItem.getDescription() == null) {
            AppLogger.w(CLASS_NAME + " Can not update Metadata - Description of the QueueItem is null");
            return;
        }
        final String mediaId = queueItem.getDescription().getMediaId();
        if (TextUtils.isEmpty(mediaId)) {
            AppLogger.w(CLASS_NAME + " Can not update Metadata - MediaId is null");
            return;
        }
        final RadioStation radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
        if (radioStation == null) {
            AppLogger.w(CLASS_NAME + " Can not update Metadata - Radio Station is null");
            return;
        }
        final MediaMetadataCompat track = MediaItemHelper.buildMediaMetadataFromRadioStation(
                getApplicationContext(),
                radioStation,
                streamTitle
        );
        if (track == null) {
            AppLogger.w(CLASS_NAME + " Can not update Metadata - MediaMetadata is null");
            return;
        }
        final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (!mediaId.equals(trackId)) {
            AppLogger.w(CLASS_NAME + " track ID '" + trackId + "' should match mediaId '" + mediaId + "'");
            return;
        }
        AppLogger.d(
                CLASS_NAME +
                " Updating metadata for MusicId:" + mediaId + ", title:" + streamTitle
        );
        mSession.setMetadata(track);
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the ExoPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        AppLogger.d(CLASS_NAME + " RelaxResources. releaseMediaPlayer=" + releaseMediaPlayer);
        // stop being a foreground service
        stopForeground(true);

        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }

        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    /**
     * Handle a request to play Radio Station
     */
    private void handlePlayRequest() {
        AppLogger.d(CLASS_NAME + " Handle PlayRequest: mState=" + mState + " started:" + mServiceStarted);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            AppLogger.i(CLASS_NAME + " Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            final Context context = getApplicationContext();
            ContextCompat.startForegroundService(
                    context,
                    new Intent(context, OpenRadioService.class)
            );
            mServiceStarted = true;
            mMediaNotification.updateNotificationMetadata();
        }

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
            // just go ahead to the new song and (re)start playing
            getCurrentPlayingRadioStationAsync(mRadioStationUpdateListener);
        }
    }

    /**
     * Listener for the getting current playing Radio Station data event.
     */
    private static final class RadioStationUpdateListenerImpl implements RadioStationUpdateListener {

        /**
         * Reference to enclosing class.
         */
        private final WeakReference<OpenRadioService> mReference;

        /**
         * Main constructor.
         *
         * @param reference Reference to enclosing class.
         */
        private RadioStationUpdateListenerImpl(final OpenRadioService reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onComplete(final MediaMetadataCompat track) {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                AppLogger.e(CLASS_NAME + " RS Update can not proceed farther, service is null");
                return;
            }
            if (track == null) {
                AppLogger.e(CLASS_NAME + " Play Radio Station: ignoring request to play next song, " +
                        "because cannot find it." +
                        " CurrentIndex=" + service.mCurrentIndexOnQueue + "." +
                        " PlayQueue.size=" + service.mPlayingQueue.size());
                return;
            }
            final String source = track.getString(MediaItemHelper.CUSTOM_METADATA_TRACK_SOURCE);
            AppLogger.d(
                    CLASS_NAME + " Play Radio Station: current (" + service.mCurrentIndexOnQueue
                            + ") in mPlayingQueue. " +
                    " musicId=" + track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) +
                    " source=" + source
            );

            service.preparePlayer(source);
        }
    }

    private void preparePlayer(final String url) {
        // Cache URL.
        mLastPlayedUrl = url;
        mState = PlaybackStateCompat.STATE_STOPPED;

        // release everything except ExoPlayer
        relaxResources(false);

        createMediaPlayerIfNeeded();

        mState = PlaybackStateCompat.STATE_BUFFERING;

        AppLogger.d("Prepare " + mLastPlayedUrl);
        mExoPlayer.prepare(Uri.parse(mLastPlayedUrl));

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
        AppLogger.d(CLASS_NAME + " ConfigAndStartMediaPlayer. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AudioFocus.NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                handlePauseRequest();
            }
        } else {
            if (mExoPlayer == null) {
                return;
            }
            // we have audio focus:
            setPlayerVolume();
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (!mExoPlayer.isPlaying()) {
                    AppLogger.d(CLASS_NAME + " ConfigAndStartMediaPlayer startMediaPlayer");
                    mExoPlayer.play();
                }
                mPlayOnFocusGain = false;
                AppLogger.d(CLASS_NAME + " ConfigAndStartMediaPlayer set state playing");
                mState = PlaybackStateCompat.STATE_PLAYING;
            }
        }

        updatePlaybackState();
    }

    private void setPlayerVolume() {
        if (mAudioFocus == AudioFocus.NO_FOCUS_CAN_DUCK) {
            mExoPlayer.setVolume(getNormalVolume() * 0.2F); // we'll be relatively quiet
        } else {
            mExoPlayer.setVolume(getNormalVolume()); // we can be loud again
        }
    }

    private float getNormalVolume() {
        final int masterVolume = AppPreferencesManager.getMasterVolume(getApplicationContext());
        return masterVolume / 100.0F;
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        AppLogger.d(CLASS_NAME + " HandlePauseRequest: mState=" + mState);

        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mState = PlaybackStateCompat.STATE_PAUSED;
            if (mExoPlayer.isPlaying()) {
                mExoPlayer.pause();
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
        updatePlaybackState(null);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error Error message to present to the user.
     */
    private void updatePlaybackState(final String error) {
        AppLogger.d(CLASS_NAME + " set playback state to " + mState + " with error:" + error);

        // Start timeout handler for the Radio Station.
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mRadioStationTimeoutHandler.postDelayed(
                    mTimeoutRunnable, RADIO_STATION_BUFFERING_TIMEOUT
            );
        // Or cancel it in case of Success or Error.
        } else {
            mRadioStationTimeoutHandler.removeCallbacks(mTimeoutRunnable);
        }

        final PlaybackStateCompat.Builder stateBuilder
                = new PlaybackStateCompat.Builder().setActions(getAvailableActions());

        setCustomAction(stateBuilder);

        // If there is an error message, send it to the playback state:
        if (error != null) {
            AppLogger.e(CLASS_NAME + " UpdatePlaybackState, error: " + error);
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            // TODO: Provide proper error code
            stateBuilder.setErrorMessage(0, error);
            mState = PlaybackStateCompat.STATE_ERROR;
        }

        stateBuilder.setBufferedPosition(mBufferedPosition);
        stateBuilder.setState(mState, mPosition, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        AppLogger.d(CLASS_NAME + " state:" + mState);
        if (mState == PlaybackStateCompat.STATE_PLAYING || mState == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotification.startNotification();
        }
    }

    /**
     * Runnable for the Radio Station buffering timeout.
     */
    private final Runnable mTimeoutRunnable = new TimeoutRunnable(this);

    private static final class TimeoutRunnable implements Runnable {

        private final WeakReference<OpenRadioService> mReference;

        private TimeoutRunnable(final OpenRadioService reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void run() {
            final OpenRadioService service = mReference.get();
            if (service != null) {
                service.handleStopRequest(null);
                service.handleStopRequest(service.getString(R.string.can_not_play_station));
            }
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        AppLogger.d(CLASS_NAME + " Try To Get Audio Focus, current focus:" + mAudioFocus);
        if (mAudioFocus == AudioFocus.FOCUSED) {
            return;
        }

        final int result = mAudioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        );

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        AppLogger.i(CLASS_NAME + " Audio Focus focused");
        mAudioFocus = AudioFocus.FOCUSED;
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        AppLogger.d(CLASS_NAME + " Give Up Audio Focus " + mAudioFocus);
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
    private void handleStopRequest(final String withError) {
        AppLogger.d(CLASS_NAME + " Handle stop request: state=" + mState + " error=" + withError);

        mState = PlaybackStateCompat.STATE_STOPPED;

        // let go of all resources...
        relaxResources(true);
        giveUpAudioFocus();
        updatePlaybackState(withError);

        mMediaNotification.stopNotification();

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private void handleLastRadioStation(final String mediaId) {
        mIsPlayWhenReady.set(!mIsAndroidAuto);
        handlePlayFromMediaId(mediaId);
    }

    /**
     * Consume Radio Station by it's ID.
     *
     * @param mediaId ID of the Radio Station.
     */
    private void handlePlayFromMediaId(final String mediaId) {
        if (mState == PlaybackStateCompat.STATE_PAUSED) {
            mState = PlaybackStateCompat.STATE_STOPPED;
        }

        if (mediaId.equals("-1")) {
            updatePlaybackState(getString(R.string.no_data_message));
            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.clearAndCopyCollection(mPlayingQueue, QueueHelper.getPlayingQueue(
                    getApplicationContext(),
                    mRadioStations)
            );
        }

        final String queueTitle = getString(R.string.queue);
        mSession.setQueueTitle(queueTitle);

        final int tempIndexOnQueue = QueueHelper.getRadioStationIndexOnQueue(
                mPlayingQueue, mediaId
        );
        if (mCurrentIndexOnQueue == tempIndexOnQueue) {
            AppLogger.w("Skip play request, same id");
            return;
        }

        // Set the current index on queue from the Radio Station Id:
        mCurrentIndexOnQueue = tempIndexOnQueue;

        if (mCurrentIndexOnQueue == -1) {
            AppLogger.w("Skip play request, negative id");
            return;
        }

        mSession.setQueue(mPlayingQueue);

        if (mPlayingQueue.isEmpty()) {
            return;
        }

        // Play Radio Station
        handlePlayRequest();
    }

    private void setCustomAction(final PlaybackStateCompat.Builder stateBuilder) {
        getCurrentPlayingRadioStationAsync(
                (track) -> {

                    if (track == null) {
                        return;
                    }
                    // Set appropriate "Favorite" icon on Custom action:
                    final String mediaId = track.getString(
                            MediaMetadataCompat.METADATA_KEY_MEDIA_ID
                    );
                    final RadioStation radioStation = QueueHelper.getRadioStationById(
                            mediaId, mRadioStations
                    );
                    if (radioStation == null) {
                        final String msg = "Set custom action on null Radio Station. MediaId:" + mediaId
                                + ". " + QueueHelper.queueToString(mRadioStations);
                        FabricUtils.logException(new RuntimeException(msg));
                        return;
                    }

                    int favoriteIcon = R.drawable.ic_star_off;
                    if (FavoritesStorage.isFavorite(radioStation, OpenRadioService.this.getApplicationContext())) {
                        favoriteIcon = R.drawable.ic_star_on;
                    }
                    stateBuilder.addCustomAction(
                            CUSTOM_ACTION_THUMBS_UP,
                            OpenRadioService.this.getString(R.string.favorite),
                            favoriteIcon
                    );
                }
        );
    }

    /**
     * @return Implementation of the {@link APIServiceProvider} interface.
     */
    private static APIServiceProvider getServiceProvider(final Context context) {
        // Instantiate appropriate parser (JSON one)
        final DataParser dataParser = new JSONDataParserImpl();
        // Instantiate appropriate API service provider
        return new APIServiceProviderImpl(context, dataParser);
    }

    /**
     * Remove {@link RadioStation} from the Favorites store by the provided Media Id.
     *
     * @param mediaId Media Id of the {@link RadioStation}.
     */
    private void removeFromFavorites(final String mediaId) {
        FavoritesStorage.removeFromFavorites(
                mediaId,
                getApplicationContext()
        );
    }

    /**
     *
     */
    private static final class MediaSessionCallback extends MediaSessionCompat.Callback {

        /**
         *
         */
        private final String CLASS_NAME = MediaSessionCallback.class.getSimpleName();

        /**
         * Reference to the enclosing class.
         */
        private final WeakReference<OpenRadioService> mService;

        /**
         * Main constructor.
         *
         * @param service Reference to the enclosing class.
         */
        private MediaSessionCallback(OpenRadioService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void onPlay() {
            super.onPlay();

            AppLogger.i(CLASS_NAME + " On Play");

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }

            if (service.mPlayingQueue.isEmpty()) {
                // start playing from the beginning of the queue
                service.mCurrentIndexOnQueue = 0;
            }

            if (!service.mPlayingQueue.isEmpty()) {
                service.handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(final long id) {
            super.onSkipToQueueItem(id);

            AppLogger.i(CLASS_NAME + " On Skip to queue item, id:" + id);

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }

            if (service.mState == PlaybackStateCompat.STATE_PAUSED) {
                service.mState = PlaybackStateCompat.STATE_STOPPED;
            }

            if (!service.mPlayingQueue.isEmpty()) {

                // set the current index on queue from the music Id:
                service.mCurrentIndexOnQueue = QueueHelper.getRadioStationIndexOnQueue(
                        service.mPlayingQueue, id
                );
                service.dispatchCurrentIndexOnQueue(service.mCurrentIndexOnQueue);

                if (service.mCurrentIndexOnQueue == -1) {
                    return;
                }

                // Play the Radio Station
                service.handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);

            AppLogger.i(CLASS_NAME + " On Play from media id:" + mediaId + " extras:" + extras);

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }

            service.handlePlayFromMediaId(mediaId);
        }

        @Override
        public void onPause() {
            super.onPause();

            AppLogger.i(CLASS_NAME + " On Pause");

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }
            service.handlePauseRequest();
        }

        @Override
        public void onStop() {
            super.onStop();

            AppLogger.i(CLASS_NAME + " On Stop");

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }
            service.handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            AppLogger.i(CLASS_NAME + " On Skip to next");

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }
            service.mCurrentIndexOnQueue++;
            if (service.mCurrentIndexOnQueue >= service.mPlayingQueue.size()) {
                service.mCurrentIndexOnQueue = 0;
            }
            service.dispatchCurrentIndexOnQueue(service.mCurrentIndexOnQueue);
            if (QueueHelper.isIndexPlayable(service.mCurrentIndexOnQueue, service.mPlayingQueue)) {
                service.mState = PlaybackStateCompat.STATE_STOPPED;
                service.handlePlayRequest();
            } else {
                AppLogger.e(CLASS_NAME + " skipToNext: cannot skip to next. next Index=" +
                        service.mCurrentIndexOnQueue + " queue length=" + service.mPlayingQueue.size());

                service.handleStopRequest(service.getString(R.string.can_not_skip));
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            AppLogger.i(CLASS_NAME + " On Skip to previous");

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }
            service.mCurrentIndexOnQueue--;
            if (service.mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                service.mCurrentIndexOnQueue = 0;
            }
            service.dispatchCurrentIndexOnQueue(service.mCurrentIndexOnQueue);
            if (QueueHelper.isIndexPlayable(service.mCurrentIndexOnQueue, service.mPlayingQueue)) {
                service.mState = PlaybackStateCompat.STATE_STOPPED;
                service.handlePlayRequest();
            } else {
                AppLogger.e(CLASS_NAME + " skipToPrevious: cannot skip to previous. previous Index=" +
                        service.mCurrentIndexOnQueue + " queue length=" + service.mPlayingQueue.size());

                service.handleStopRequest(service.getString(R.string.can_not_skip));
            }
        }

        @Override
        public void onCustomAction(@NonNull final String action, final Bundle extras) {
            super.onCustomAction(action, extras);

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }

            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                service.getCurrentPlayingRadioStationAsync(
                        (track) -> {

                            if (track != null) {
                                final String mediaId = track.getString(
                                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID
                                );
                                final RadioStation radioStation = QueueHelper.getRadioStationById(
                                        mediaId, service.mRadioStations
                                );

                                if (radioStation == null) {
                                    AppLogger.w(CLASS_NAME + " OnCustomAction radioStation is null");
                                    return;
                                }

                                final boolean isFavorite = FavoritesStorage.isFavorite(
                                        radioStation, service.getApplicationContext()
                                );
                                if (isFavorite) {
                                    service.removeFromFavorites(radioStation.getIdAsString());
                                } else {
                                    FavoritesStorage.addToFavorites(
                                            radioStation, service.getApplicationContext()
                                    );
                                }
                            }

                            // playback state needs to be updated because the "Favorite" icon on the
                            // custom action will change to reflect the new favorite state.
                            service.updatePlaybackState();
                        }
                );
            } else {
                AppLogger.e(CLASS_NAME + " Unsupported action: " + action);
            }
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            AppLogger.i(CLASS_NAME + " OnPlayFromSearch:" + query + ", extras:" + extras.toString());
            super.onPlayFromSearch(query, extras);

            final OpenRadioService service = mService.get();
            if (service == null) {
                return;
            }
            service.performSearch(query);
        }
    }

    private void performSearch(final String query) {
        AppLogger.i(CLASS_NAME + " Search for:" + query);

        if (TextUtils.isEmpty(query)) {
            // A generic search like "Play music" sends an empty query
            // and it's expected that we start playing something.
            // TODO
            handleStopRequest(getString(R.string.no_search_results));
            return;
        }

        final ExecutorService executorService = getApiCallExecutor();
        if (executorService != null) {
            executorService.submit(
                    () -> {
                        try {
                            executePerformSearch(query);
                        } catch (final Exception e) {
                            handleStopRequest(getString(R.string.no_search_results));
                            FabricUtils.logException(e);
                        }
                    }
            );
        } else {
            handleStopRequest(getString(R.string.no_search_results));
        }
    }

    /**
     * Execute actual search.
     *
     * @param query Search query.
     */
    private void executePerformSearch(final String query) {
        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = getServiceProvider(getApplicationContext());

        final List<RadioStation> list = serviceProvider.getStations(
                downloader,
                UrlBuilder.getSearchUrl(getApplicationContext()),
                APIServiceProviderImpl.getSearchQueryParameters(query)
        );

        if (list == null || list.isEmpty()) {
            // if nothing was found, we need to warn the user and stop playing
            handleStopRequest(getString(R.string.no_search_results));
            // TODO
            return;
        }

        AppLogger.i(CLASS_NAME + " Found " + list.size() + " items");

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.clearAndCopyCollection(mRadioStations, list);
            QueueHelper.clearAndCopyCollection(mPlayingQueue, QueueHelper.getPlayingQueue(
                    getApplicationContext(),
                    mRadioStations)
            );
        }

        mSession.setQueue(mPlayingQueue);

        // immediately start playing from the beginning of the search results
        mCurrentIndexOnQueue = 0;

        final Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(
                this::handlePlayRequest
        );
    }

    /**
     * Dispatch broad cast event about changes on current playing Radio Station.
     *
     * @param index Index of the Radio Station in the queue.
     */
    private void dispatchCurrentIndexOnQueue(final int index) {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            AppLogger.w(CLASS_NAME + " Can not dispatch curr index on queue");
            return;
        }
        final MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
        String mediaId = "";
        if (item != null && item.getDescription() != null) {
            mediaId = item.getDescription().getMediaId();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                AppLocalBroadcast.createIntentCurrentIndexOnQueue(
                        index, mediaId
                )
        );
    }

    /**
     *
     * @return
     */
    @Nullable
    private ExecutorService getApiCallExecutor() {
        if (mApiCallExecutor == null) {
            FabricUtils.logCustomEvent(
                    FabricUtils.EVENT_NAME_API_EXEC, "Error", "API executor is null"
            );
            return null;
        }
        if (mApiCallExecutor.isShutdown()) {
            FabricUtils.logCustomEvent(
                    FabricUtils.EVENT_NAME_API_EXEC, "Error", "API executor is shut down"
            );
            return null;
        }
        return mApiCallExecutor;
    }

    /**
     * Listener class of the Playback State changes.
     */
    private static final class PlaybackStateListener implements MediaItemCommand.IUpdatePlaybackState {

        /**
         * Reference to the outer class.
         */
        private final WeakReference<OpenRadioService> mReference;

        /**
         * private constructor.
         *
         * @param reference Reference to the outer class.
         */
        private PlaybackStateListener(final OpenRadioService reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void updatePlaybackState(final String error) {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.updatePlaybackState(error);
        }
    }

    /**
     * Listener for Exo Player events.
     */
    private static final class ExoPlayerListener implements ExoPlayerOpenRadioImpl.Listener {

        /**
         * Reference to the outer class.
         */
        private final WeakReference<OpenRadioService> mReference;

        /**
         * Constructor.
         *
         * @param reference
         */
        private ExoPlayerListener(final OpenRadioService reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public final void onError(final ExoPlaybackException error) {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.onError(error);
        }

        @Override
        public void onHandledError(final ExoPlaybackException error) {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.onHandledError(error);
        }

        @Override
        public void onPrepared() {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.onPrepared();
        }

        @Override
        public void onProgress(final long position, final long bufferedPosition, final long duration) {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.mPosition = position;
            service.mBufferedPosition = bufferedPosition;
            service.updatePlaybackState();
        }
    }

    private static final class MasterVolumeEventListener implements MasterVolumeBroadcastReceiverListener {

        private final WeakReference<OpenRadioService> mReference;

        private MasterVolumeEventListener(final OpenRadioService service) {
            super();
            mReference = new WeakReference<>(service);
        }

        @Override
        public void onMasterVolumeChanged() {
            final OpenRadioService service = mReference.get();
            if (service == null) {
                return;
            }
            service.setPlayerVolume();
        }
    }
}
