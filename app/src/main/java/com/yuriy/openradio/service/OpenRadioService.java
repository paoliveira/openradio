/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.APIServiceProviderImpl;
import com.yuriy.openradio.api.CategoryVO;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.AppPreferencesManager;
import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.business.MediaItemAllCategories;
import com.yuriy.openradio.business.MediaItemChildCategories;
import com.yuriy.openradio.business.MediaItemCommand;
import com.yuriy.openradio.business.MediaItemCountriesList;
import com.yuriy.openradio.business.MediaItemFavoritesList;
import com.yuriy.openradio.business.MediaItemParentCategories;
import com.yuriy.openradio.business.MediaItemRoot;
import com.yuriy.openradio.business.MediaItemSearchFromApp;
import com.yuriy.openradio.business.MediaItemStationsInCategory;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.PackageValidator;
import com.yuriy.openradio.utils.QueueHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/13/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class OpenRadioService
        extends MediaBrowserService
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener,
                   AudioManager.OnAudioFocusChangeListener {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = OpenRadioService.class.getSimpleName();

    private static final String ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead";

    private static final String KEY_NAME_COMMAND_NAME = "KEY_NAME_COMMAND_NAME";

    private static final String VALUE_NAME_REQUEST_LOCATION_COMMAND
            = "VALUE_NAME_REQUEST_LOCATION_COMMAND";

    private static final String VALUE_NAME_GET_RADIO_STATION_COMMAND
            = "VALUE_NAME_GET_RADIO_STATION_COMMAND";

    private static final String EXTRA_KEY_MEDIA_DESCRIPTION = "EXTRA_KEY_MEDIA_DESCRIPTION";

    private static final String EXTRA_KEY_MESSAGES_HANDLER = "EXTRA_KEY_MESSAGES_HANDLER";

    private static final String EXTRA_KEY_RADIO_STATION = "EXTRA_KEY_RADIO_STATION";

    private static final String EXTRA_KEY_IS_FAVORITE = "EXTRA_KEY_IS_FAVORITE";

    /**
     * Reserved init value.
     */
    private static final int MSG_INIT = 100;

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
     * Player instance to play Radio stream.
     */
    private MediaPlayer mMediaPlayer;

    /**
     * Media Session
     */
    private MediaSession mSession;

    /**
     * Index of the current playing song.
     */
    private int mCurrentIndexOnQueue;

    /**
     * Queue of the Radio Stations in the Category
     */
    private final List<MediaSession.QueueItem> mPlayingQueue = new ArrayList<>();

    /**
     * Current local media player state
     */
    private int mState = PlaybackState.STATE_NONE;

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
    private ExecutorService mApiCallExecutor = Executors.newSingleThreadExecutor();

    /**
     * Collection of the Child Categories.
     */
    private final List<CategoryVO> mChildCategories = new ArrayList<>();

    /**
     * Collection of the Radio Stations.
     */
    private final List<RadioStationVO> mRadioStations = new ArrayList<>();

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
     * Handler to handle incoming to the Service messages.
     */
    //private MessagesHandler mMessagesHandler;

    /**
     * Id of the current Category. It is used for example when back from an empty Category.
     */
    private String mCurrentCategory = "";

    /**
     * Service class to provide information about current location.
     */
    private final LocationService mLocationService = LocationService.getInstance();

    private final MediaItemCommand.IUpdatePlaybackState mPlaybackStateListener = new PlaybackStateListener(this);

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

    private final Handler mDelayedStopHandler = new DelayedStopHandler(this);

    /**
     * Map of the Media Item commands that responsible for the Media Items List creation.
     */
    private final Map<String, MediaItemCommand> mMediaItemCommands = new HashMap<>();

    private static class DelayedStopHandler extends Handler {

        private final OpenRadioService mReference;

        public DelayedStopHandler(final OpenRadioService reference) {
            mReference = reference;
        }

        @Override
        public void handleMessage(final Message msg) {
            if (mReference == null) {
                return;
            }
            if ((mReference.mMediaPlayer != null && mReference.mMediaPlayer.isPlaying())
                    || mReference.mPlayOnFocusGain) {
                Log.d(CLASS_NAME, "Ignoring delayed stop since the media player is in use.");
                return;
            }
            Log.d(CLASS_NAME, "Stopping service with delay handler.");

            mReference.stopSelf();

            mReference.mServiceStarted = false;
        }
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        Log.i(CLASS_NAME, "On Create");

        // Set application's context for the Preferences.
        AppPreferencesManager.setContext(this);

        // Add Media Items implementations to the map
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_ROOT, new MediaItemRoot());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES, new MediaItemAllCategories());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_COUNTRIES_LIST, new MediaItemCountriesList());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES, new MediaItemParentCategories());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES, new MediaItemChildCategories());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, new MediaItemStationsInCategory());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_FAVORITES_LIST, new MediaItemFavoritesList());
        mMediaItemCommands.put(MediaIDHelper.MEDIA_ID_SEARCH_FROM_APP, new MediaItemSearchFromApp());

        // Create and start a background HandlerThread since by
        // default a Service runs in the UI Thread, which we don't
        // want to block.
        final HandlerThread thread = new HandlerThread(
                OpenRadioService.class.getSimpleName() + "-MessagesThread"
        );
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler.
        //mMessagesHandler = new MessagesHandler(thread.getLooper());
        //mMessagesHandler.setReference(this);

        mLocationService.checkLocationEnable(this);
        mLocationService.requestCountryCodeLastKnown(this);

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "OpenRadio_lock");

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Start a new MediaSession
        mSession = new MediaSession(this, "OpenRadioService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaNotification = new MediaNotification(this);
    }

    @Override
    public final int onStartCommand(final Intent intent, final int flags, final int startId) {

        Log.i(CLASS_NAME, "On Start Command: " + intent);

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

        if (command.equals(VALUE_NAME_REQUEST_LOCATION_COMMAND)) {
            mLocationService.requestCountryCode(this, new LocationServiceListener() {

                @Override
                public void onCountryCodeLocated(final String countryCode) {
                    LocalBroadcastManager.getInstance(OpenRadioService.this).sendBroadcast(
                            AppLocalBroadcastReceiver.createIntentLocationCountryCode(
                                    countryCode
                            )
                    );
                }
            });
        } else if (command.equals(VALUE_NAME_GET_RADIO_STATION_COMMAND)) {
            // Update Favorites Radio station: whether add it or remove it from the storage
            final boolean isFavorite = getIsFavoriteFromIntent(intent);
            final MediaDescription mediaDescription = extractMediaDescription(intent);
            if (mediaDescription == null) {
                return super.onStartCommand(intent, flags, startId);
            }
            final RadioStationVO radioStation = QueueHelper.getRadioStationById(
                    mediaDescription.getMediaId(), mRadioStations
            );
            if (radioStation == null) {
                if (!isFavorite) {
                    removeFromFavorites(mediaDescription.getMediaId());
                }
                return super.onStartCommand(intent, flags, startId);
            }
            if (isFavorite) {
                FavoritesStorage.addToFavorites(
                        radioStation, getApplicationContext()
                );
            } else {
                removeFromFavorites(String.valueOf(radioStation.getId()));
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();

        Log.d(CLASS_NAME, "On Destroy");

        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }

    @Override
    public final BrowserRoot onGetRoot(@NonNull final String clientPackageName, final int clientUid,
                                       final Bundle rootHints) {
        Log.d(CLASS_NAME, "OnGetRoot: clientPackageName=" + clientPackageName
                + ", clientUid=" + clientUid + ", rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!PackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            Log.w(CLASS_NAME, "OnGetRoot: IGNORING request from untrusted package "
                    + clientPackageName);
            return null;
        }
        //noinspection StatementWithEmptyBody
        if (ANDROID_AUTO_PACKAGE_NAME.equals(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
            Log.i(CLASS_NAME, "Package name is Android Auto");
        } else {
            Log.i(CLASS_NAME, "Package name is not Android Auto");
        }
        return new BrowserRoot(MediaIDHelper.MEDIA_ID_ROOT, null);
    }

    @Override
    public final void onLoadChildren(@NonNull final String parentId,
                                     @NonNull final Result<List<MediaBrowser.MediaItem>> result) {

        Log.i(CLASS_NAME, "OnLoadChildren:" + parentId);

        final List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = getServiceProvider();

        final MediaItemCommand command = mMediaItemCommands.get(parentId);
        if (command != null) {
            command.create(
                    getApplicationContext(), mLocationService.getCountryCode(),
                    downloader, serviceProvider, result, mediaItems,
                    mPlaybackStateListener
            );
        } else {
            Log.w(CLASS_NAME, "Skipping unmatched parentId: " + parentId);
            result.sendResult(mediaItems);
        }
    }

    @Override
    public final void onCompletion(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "On MediaPlayer completion");

        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (!mPlayingQueue.isEmpty()) {
            // In this sample, we restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public final boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
        Log.e(CLASS_NAME, "MediaPlayer error: what=" + what + ", extra=" + extra);
        handleStopRequest(getString(R.string.media_player_error));
        return true; // true indicates we handled the error
    }

    @Override
    public final void onPrepared(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "MediaPlayer prepared");

        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    @Override
    public final void onAudioFocusChange(int focusChange) {
        Log.d(CLASS_NAME, "On AudioFocusChange. focusChange=" + focusChange);
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
            if (mState == PlaybackState.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Log.e(CLASS_NAME, "OnAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
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
     * Factory method to make {@link Intent} to update Favorite {@link RadioStationVO}.
     *
     * @param context          Context of the callee.
     * @param mediaDescription {@link MediaDescription} of the {@link RadioStationVO}.
     * @param isFavorite       Whether Radio station is Favorite or not.
     * @return {@link Intent}.
     */
    public static Intent makeUpdateFavoriteIntent(final Context context,
                                                  final MediaDescription mediaDescription,
                                                  final boolean isFavorite) {
        final Intent intent = new Intent(context, OpenRadioService.class);
        intent.putExtra(KEY_NAME_COMMAND_NAME, VALUE_NAME_GET_RADIO_STATION_COMMAND);
        intent.putExtra(EXTRA_KEY_MEDIA_DESCRIPTION, mediaDescription);
        intent.putExtra(EXTRA_KEY_IS_FAVORITE, isFavorite);
        return intent;
    }

    /**
     * Extract {@link RadioStationVO} from the {@link Message} that has been received from the
     * {@link OpenRadioService} as a result of the {@link RadioStationVO} retrieving.
     *
     * @param message {@link Message} receiving from the {@link OpenRadioService}.
     * @return {@link RadioStationVO} object or null in case of any error.
     */
    public static RadioStationVO getRadioStationFromMessage(final Message message) {
        if (message == null) {
            return null;
        }
        final Bundle data = message.getData();
        if (data == null) {
            return null;
        }
        final RadioStationVO radioStation = (RadioStationVO) data.getSerializable(
                EXTRA_KEY_RADIO_STATION
        );
        if (radioStation == null) {
            return null;
        }
        return radioStation;
    }

    /**
     * Extract {@link #EXTRA_KEY_IS_FAVORITE} value from the {@link Message} that has been
     * received from the {@link OpenRadioService} as a result of the {@link RadioStationVO}
     * retrieving.
     *
     * @param message {@link Message} receiving from the {@link OpenRadioService}.
     * @return True in case of the key exists and it's value is True, False otherwise.
     */
    public static boolean getIsFavoriteFromMessage(final Message message) {
        boolean isFavorite = false;
        if (message == null) {
            return false;
        }
        final Bundle data = message.getData();
        if (data == null) {
            return false;
        }
        if (data.containsKey(EXTRA_KEY_IS_FAVORITE)) {
            isFavorite = data.getBoolean(EXTRA_KEY_IS_FAVORITE, false);
        }
        return isFavorite;
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

    private static MediaDescription extractMediaDescription(final Intent intent) {
        if (intent == null) {
            return new MediaDescription.Builder().build();
        }
        if (!intent.hasExtra(EXTRA_KEY_MEDIA_DESCRIPTION)) {
            return new MediaDescription.Builder().build();
        }
        return intent.getParcelableExtra(EXTRA_KEY_MEDIA_DESCRIPTION);
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            Log.d(CLASS_NAME, "Create MediaPlayer");

            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
        } else {
            Log.d(CLASS_NAME, "Reset MediaPlayer");

            mMediaPlayer.reset();
        }
    }

    /**
     * Retrieve currently selected Radio Station. If the URl is not yet obtained via API the
     * it will be retrieved as well, appropriate event will be dispatched via listener.
     *
     * @param listener {@link RadioStationUpdateListener}
     */
    private void getCurrentPlayingRadioStation(final RadioStationUpdateListener listener) {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        final MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
        if (item == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }
        final String mediaId = item.getDescription().getMediaId();

        RadioStationVO radioStation;
        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
        }

        // Make a copy of the Radio Station
        final RadioStationVO radioStationCopy = radioStation;

        if (radioStationCopy == null) {
            if (listener != null) {
                listener.onComplete(null);
            }
            return;
        }

        // If there is no detailed information about Radio Station - download it here and
        // update model.

        if (radioStationCopy.getStreamURL() != null && !radioStationCopy.getStreamURL().isEmpty()) {
            radioStationCopy.setIsUpdated(true);
        }

        if (!radioStationCopy.getIsUpdated()) {

            mApiCallExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {

                            // Start download information about Radio Station
                            final RadioStationVO radioStationUpdated = getServiceProvider().getStation(
                                    new HTTPDownloaderImpl(),
                                    UrlBuilder.getStation(
                                            getApplicationContext(),
                                            String.valueOf(radioStationCopy.getId())
                                    )
                            );
                            radioStationCopy.setStreamURL(radioStationUpdated.getStreamURL());
                            radioStationCopy.setBitRate(radioStationUpdated.getBitRate());
                            radioStationCopy.setIsUpdated(true);

                            if (listener != null) {
                                listener.onComplete(
                                        buildMetadata(radioStationCopy)
                                );
                            }
                        }
                    }
            );
        } else {
            if (listener != null) {
                listener.onComplete(
                        buildMetadata(radioStationCopy)
                );
            }
        }
    }

    private MediaMetadata buildMetadata(final RadioStationVO radioStation) {
        if (radioStation.getStreamURL() == null || radioStation.getStreamURL().isEmpty()) {
            updatePlaybackState(getString(R.string.no_data_message));
        }

        //Log.d(CLASS_NAME, "CurrentPlayingRadioStation for id=" + mediaId);
        return MediaItemHelper.buildMediaMetadataFromRadioStation(
                getApplicationContext(),
                radioStation
        );
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            Log.e(CLASS_NAME, "Can't retrieve current metadata.");
            mState = PlaybackState.STATE_ERROR;
            updatePlaybackState(getString(R.string.no_metadata));
            return;
        }

        final MediaSession.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        // TODO : getDescription() can return null
        final String mediaId = queueItem.getDescription().getMediaId();
        final RadioStationVO radioStation = QueueHelper.getRadioStationById(mediaId, mRadioStations);
        final MediaMetadata track = MediaItemHelper.buildMediaMetadataFromRadioStation(
                getApplicationContext(),
                radioStation
        );
        final String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (mediaId == null || trackId == null || !mediaId.equals(trackId)) {
            throw new IllegalStateException("track ID (" + trackId + ") " +
                    "should match mediaId (" + mediaId + ")");
        }
        Log.d(CLASS_NAME, "Updating metadata for MusicID= " + mediaId);
        mSession.setMetadata(track);
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(CLASS_NAME, "RelaxResources. releaseMediaPlayer=" + releaseMediaPlayer);
        // stop being a foreground service
        stopForeground(true);

        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
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
        Log.d(CLASS_NAME, "Handle PlayRequest: mState=" + mState);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(CLASS_NAME, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), OpenRadioService.class));
            mServiceStarted = true;
        }

        mPlayOnFocusGain = true;
        tryToGetAudioFocus();

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        // actually play the song
        if (mState == PlaybackState.STATE_PAUSED) {
            // If we're paused, just continue playback and restore the
            // 'foreground service' state.
            configMediaPlayerState();
        } else {
            // If we're stopped or playing a song,
            // just go ahead to the new song and (re)start playing
            getCurrentPlayingRadioStation(radioStationUpdateListener);
        }
    }

    /**
     * Listener for the getting current playing Radio Station data event.
     */
    private final RadioStationUpdateListener radioStationUpdateListener
            = new RadioStationUpdateListener() {

        @Override
        public void onComplete(final MediaMetadata track) {
            if (track == null) {
                Log.e(CLASS_NAME, "Play Radio Station: ignoring request to play next song, " +
                        "because cannot find it." +
                        " CurrentIndex=" + mCurrentIndexOnQueue + "." +
                        " PlayQueue.size=" + mPlayingQueue.size());
                return;
            }
            final String source = track.getString(MediaItemHelper.CUSTOM_METADATA_TRACK_SOURCE);
            Log.d(CLASS_NAME, "Play Radio Station: current (" + mCurrentIndexOnQueue + ") in mPlayingQueue. " +
                    " musicId=" + track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) +
                    " source=" + source);

            mState = PlaybackState.STATE_STOPPED;

            // release everything except MediaPlayer
            relaxResources(false);

            createMediaPlayerIfNeeded();

            mState = PlaybackState.STATE_BUFFERING;

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            final Map<String, String> headers = new HashMap<>();
            headers.put("Connection", "keep-alive");
            headers.put("Keep-Alive", "timeout=2000");

            try {
                mMediaPlayer.setDataSource(
                        getApplicationContext(), Uri.parse(source), headers
                );

                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mMediaPlayer.prepareAsync();

                // If we are streaming from the internet, we want to hold a
                // Wifi lock, which prevents the Wifi radio from going to
                // sleep while the song is playing.
                mWifiLock.acquire();

                updatePlaybackState(null);
                updateMetadata();

            } catch (IOException ex) {
                Log.e(CLASS_NAME, "IOException playing song:" + ex.getMessage());
                updatePlaybackState(getString(R.string.can_not_play_station));
            }
        }
    };

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a context where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        Log.d(CLASS_NAME, "ConfigAndStartMediaPlayer. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AudioFocus.NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (mState == PlaybackState.STATE_PLAYING) {
                handlePauseRequest();
            }
        } else {
            if (mMediaPlayer == null) {
                return;
            }
            // we have audio focus:
            if (mAudioFocus == AudioFocus.NO_FOCUS_CAN_DUCK) {
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);     // we'll be relatively quiet
            } else {
                mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (!mMediaPlayer.isPlaying()) {
                    Log.d(CLASS_NAME, "ConfigAndStartMediaPlayer startMediaPlayer");
                    mMediaPlayer.start();
                }
                mPlayOnFocusGain = false;
                Log.d(CLASS_NAME, "ConfigAndStartMediaPlayer set state playing");
                mState = PlaybackState.STATE_PLAYING;
            }
        }

        updatePlaybackState(null);
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        Log.d(CLASS_NAME, "HandlePauseRequest: mState=" + mState);

        if (mState == PlaybackState.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            mState = PlaybackState.STATE_PAUSED;
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }
        updatePlaybackState(null);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(final String error) {
        Log.d(CLASS_NAME, "UpdatePlaybackState, setting session playback state to " + mState);

        // Start timeout handler for the new Radio Station
        if (mState == PlaybackState.STATE_BUFFERING) {
            mRadioStationTimeoutHandler.postDelayed(
                    radioStationTimeoutRunnable, RADIO_STATION_BUFFERING_TIMEOUT
            );
            // Or cancel it in case of Success or Error
        } else {
            mRadioStationTimeoutHandler.removeCallbacks(radioStationTimeoutRunnable);
        }

        long position = PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            position = mMediaPlayer.getCurrentPosition();
        }

        final PlaybackState.Builder stateBuilder
                = new PlaybackState.Builder().setActions(getAvailableActions());

        setCustomAction(stateBuilder);

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            mState = PlaybackState.STATE_ERROR;
        }
        stateBuilder.setState(mState, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSession.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        Log.d(CLASS_NAME, "UpdatePlaybackState, state:" + mState);
        if (mState == PlaybackState.STATE_PLAYING || mState == PlaybackState.STATE_PAUSED) {
            mMediaNotification.startNotification();
        }
    }

    /**
     * Runnable for the Radio Station buffering timeout.
     */
    private final Runnable radioStationTimeoutRunnable = new Runnable() {

        @Override
        public void run() {

            handleStopRequest(null);
            handleStopRequest(getString(R.string.can_not_play_station));
        }
    };

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        Log.d(CLASS_NAME, "Try To Get Audio Focus, current focus:" + mAudioFocus);
        if (mAudioFocus == AudioFocus.FOCUSED) {
            return;
        }

        final int result = mAudioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        );

        //Log.d(CLASS_NAME, "Audio Focus result:" + result);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        Log.i(CLASS_NAME, "Audio Focus focused");
        mAudioFocus = AudioFocus.FOCUSED;
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        Log.d(CLASS_NAME, "Give Up Audio Focus");
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
        Log.d(CLASS_NAME, "Handle stop request: state=" + mState + " error=" + withError);

        mState = PlaybackState.STATE_STOPPED;

        // let go of all resources...
        relaxResources(true);
        giveUpAudioFocus();
        updatePlaybackState(withError);

        mMediaNotification.stopNotification();

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private void setCustomAction(final PlaybackState.Builder stateBuilder) {
        getCurrentPlayingRadioStation(
                new RadioStationUpdateListener() {

                    @Override
                    public void onComplete(final MediaMetadata currentMusic) {

                        if (currentMusic == null) {
                            return;
                        }
                        // Set appropriate "Favorite" icon on Custom action:
                        final String mediaId = currentMusic.getString(
                                MediaMetadata.METADATA_KEY_MEDIA_ID
                        );
                        final RadioStationVO radioStation = QueueHelper.getRadioStationById(
                                mediaId, mRadioStations
                        );
                        int favoriteIcon = R.drawable.ic_star_off;
                        if (FavoritesStorage.isFavorite(radioStation, getApplicationContext())) {
                            favoriteIcon = R.drawable.ic_star_on;
                        }
                        /*Log.d(
                                CLASS_NAME,
                                "UpdatePlaybackState, setting Favorite custom action of music ",
                                mediaId, " current favorite=" + mMusicProvider.isFavorite(mediaId)
                        );*/
                        stateBuilder.addCustomAction(
                                CUSTOM_ACTION_THUMBS_UP,
                                getString(R.string.favorite),
                                favoriteIcon
                        );
                    }
                }
        );
    }

    /**
     * @return Implementation of the {@link com.yuriy.openradio.api.APIServiceProvider} interface.
     */
    private static APIServiceProvider getServiceProvider() {
        // Instantiate appropriate parser (JSON one)
        final DataParser dataParser = new JSONDataParserImpl();
        // Instantiate appropriate API service provider
        return new APIServiceProviderImpl(dataParser);
    }

    /**
     * Remove {@link RadioStationVO} from the Favorites store by the provided Media Id.
     *
     * @param mediaId Media Id of the {@link RadioStationVO}.
     */
    private void removeFromFavorites(final String mediaId) {
        FavoritesStorage.removeFromFavorites(
                mediaId,
                getApplicationContext()
        );
    }

    private final class MediaSessionCallback extends MediaSession.Callback {

        private final String CLASS_NAME = MediaSessionCallback.class.getSimpleName();

        @Override
        public void onPlay() {
            super.onPlay();

            Log.i(CLASS_NAME, "On Play");

            if (mPlayingQueue.isEmpty()) {
                //mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                //mSession.setQueue(mPlayingQueue);
                //mSession.setQueueTitle(getString(R.string.random_queue_title));

                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (!mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(final long id) {
            super.onSkipToQueueItem(id);

            Log.i(CLASS_NAME, "On Skip to queue item, id:" + id);

            if (mState == PlaybackState.STATE_PAUSED) {
                mState = PlaybackState.STATE_STOPPED;
            }

            if (!mPlayingQueue.isEmpty()) {

                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue = QueueHelper.getRadioStationIndexOnQueue(mPlayingQueue, id);

                if (mCurrentIndexOnQueue == -1) {
                    return;
                }

                // Play the Radio Station
                handlePlayRequest();
            }
        }

        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);

            Log.i(CLASS_NAME, "On Play from media id:" + mediaId + " extras:" + extras);

            if (mState == PlaybackState.STATE_PAUSED) {
                mState = PlaybackState.STATE_STOPPED;
            }

            if (mediaId.equals("-1")) {
                updatePlaybackState(getString(R.string.no_data_message));
                return;
            }

            synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
                QueueHelper.copyCollection(mPlayingQueue, QueueHelper.getPlayingQueue(
                                getApplicationContext(),
                                mRadioStations)
                );
            }

            mSession.setQueue(mPlayingQueue);

            final String queueTitle = "Queue Title";
            mSession.setQueueTitle(queueTitle);

            if (mPlayingQueue.isEmpty()) {
                return;
            }

            // Set the current index on queue from the Radio Station Id:
            mCurrentIndexOnQueue = QueueHelper.getRadioStationIndexOnQueue(mPlayingQueue, mediaId);

            if (mCurrentIndexOnQueue == -1) {
                return;
            }

            // Play Radio Station
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            super.onPause();

            Log.i(CLASS_NAME, "On Pause");

            handlePauseRequest();
        }

        @Override
        public void onStop() {
            super.onStop();

            Log.i(CLASS_NAME, "On Stop");

            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            Log.i(CLASS_NAME, "On Skip to next");

            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(CLASS_NAME, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" + mPlayingQueue.size());

                handleStopRequest(getString(R.string.can_not_skip));
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            Log.i(CLASS_NAME, "On Skip to previous");

            mCurrentIndexOnQueue--;
            if (mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(CLASS_NAME, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" + mPlayingQueue.size());

                handleStopRequest(getString(R.string.can_not_skip));
            }
        }

        @Override
        public void onCustomAction(@NonNull final String action, final Bundle extras) {
            super.onCustomAction(action, extras);

            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                getCurrentPlayingRadioStation(
                        new RadioStationUpdateListener() {
                            @Override
                            public void onComplete(final MediaMetadata track) {

                                if (track != null) {
                                    final String mediaId = track.getString(
                                            MediaMetadata.METADATA_KEY_MEDIA_ID
                                    );
                                    final RadioStationVO radioStation = QueueHelper.getRadioStationById(
                                            mediaId, mRadioStations
                                    );

                                    if (radioStation == null) {
                                        Log.w(CLASS_NAME, "OnCustomAction radioStation is null");
                                        return;
                                    }

                                    final boolean isFavorite = FavoritesStorage.isFavorite(
                                            radioStation, getApplicationContext()
                                    );
                                    if (isFavorite) {
                                        removeFromFavorites(String.valueOf(radioStation.getId()));
                                    } else {
                                        FavoritesStorage.addToFavorites(
                                                radioStation, getApplicationContext()
                                        );
                                    }
                                }

                                // playback state needs to be updated because the "Favorite" icon on the
                                // custom action will change to reflect the new favorite state.
                                updatePlaybackState(null);
                            }
                        }
                );
            } else {
                Log.e(CLASS_NAME, "Unsupported action: " + action);
            }
        }

        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            super.onPlayFromSearch(query, extras);

            performSearch(query);
        }
    }

    private void performSearch(final String query) {
        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            mRadioStations.clear();
            mPlayingQueue.clear();
        }

        Log.i(CLASS_NAME, "Search for:" + query);
        if (TextUtils.isEmpty(query)) {
            // A generic search like "Play music" sends an empty query
            // and it's expected that we start playing something.
            // TODO
            return;
        }

        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = getServiceProvider();

        mApiCallExecutor.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        final List<RadioStationVO> list = serviceProvider.getStations(
                                downloader,
                                UrlBuilder.getSearchQuery(getApplicationContext(), query)
                        );

                        if (list == null || list.isEmpty()) {
                            // if nothing was found, we need to warn the user and stop playing
                            handleStopRequest(getString(R.string.no_search_results));
                            // TODO
                            return;
                        }

                        Log.i(CLASS_NAME, "Found " + list.size() + " items");

                        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
                            QueueHelper.copyCollection(mRadioStations, list);

                            QueueHelper.copyCollection(mPlayingQueue, QueueHelper.getPlayingQueue(
                                            getApplicationContext(),
                                            mRadioStations)
                            );
                        }

                        mSession.setQueue(mPlayingQueue);

                        // immediately start playing from the beginning of the search results
                        mCurrentIndexOnQueue = 0;

                        handlePlayRequest();
                    }
                }
        );
    }

    private static final class PlaybackStateListener implements MediaItemCommand.IUpdatePlaybackState {

        private final WeakReference<OpenRadioService> mReference;

        public PlaybackStateListener(final OpenRadioService reference) {
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

    // Template for the future

    /**
     * An inner class that inherits from {@link android.os.Handler} and uses its
     * {@link #handleMessage(android.os.Message)} hook method to process Messages sent to
     * it from {@link #onStartCommand(Intent, int, int)} (android.content.Intent)} that indicate which
     * action to perform.
     */
    /*private static final class MessagesHandler extends Handler {

        *//**
         * String tag to use in the logging.
         *//*
        private static final String CLASS_NAME = MessagesHandler.class.getSimpleName();

        *//**
         * Reference to the outer class (service).
         *//*
        private OpenRadioService mReference;

        *//**
         * Class constructor initializes the Looper.
         *
         * @param looper The Looper that we borrow from HandlerThread.
         *//*
        public MessagesHandler(final Looper looper) {
            super(looper);
        }

        *//**
         * Hook method that process incoming commands.
         *//*
        @Override
        public void handleMessage(final Message message) {

            final int what = message.what;
            final Intent intent = (Intent) message.obj;
            switch (what) {
                default:
                    Log.w(CLASS_NAME, "Handle unknown msg:" + what);
            }
        }

        *//**
         * Set the reference to the outer class (service).
         * @param value Instance of the {@link OpenRadioService}
         *//*
        public void setReference(final OpenRadioService value) {
            mReference = value;
        }
    }*/
}
