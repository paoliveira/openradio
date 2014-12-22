/*
 * Copyright 2014 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import android.util.Log;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.APIServiceProviderImpl;
import com.yuriy.openradio.api.CategoryVO;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.JSONDataParserImpl;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.HTTPDownloaderImpl;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.PackageValidator;
import com.yuriy.openradio.utils.QueueHelper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/13/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class OpenRadioService
        extends MediaBrowserService
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
                   MediaPlayer.OnErrorListener,
                   AudioManager.OnAudioFocusChangeListener {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = OpenRadioService.class.getSimpleName();

    public static final String ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead";

    /**
     * Delay stopSelf by using a handler.
     */
    private static final int STOP_DELAY = 30000;

    /**
     * The volume we set the media player to when we lose audio focus,
     * but are allowed to reduce the volume instead of stopping playback.
     */
    public static final float VOLUME_DUCK = 0.2f;

    /**
     * The volume we set the media player when we have audio focus.
     */
    public static final float VOLUME_NORMAL = 1.0f;

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
    private final List<MediaSession.QueueItem> playingQueue = new ArrayList<>();

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
    private ExecutorService apiCallExecutor = Executors.newSingleThreadExecutor();

    /**
     * Collection of All Categories.
     */
    private final List<CategoryVO> allCategories = new ArrayList<>();

    /**
     * Collection of the Child Categories.
     */
    private final List<CategoryVO> childCategories = new ArrayList<>();

    /**
     * Collection of the Radio Stations.
     */
    private final List<RadioStationVO> radioStations = new ArrayList<>();

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

    private Handler mDelayedStopHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if ((mMediaPlayer != null && mMediaPlayer.isPlaying()) || mPlayOnFocusGain) {
                Log.d(CLASS_NAME, "Ignoring delayed stop since the media player is in use.");
                return;
            }
            Log.d(CLASS_NAME, "Stopping service with delay handler.");
            stopSelf();
            mServiceStarted = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(CLASS_NAME, "On Create");

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
    public void onDestroy() {
        super.onDestroy();

        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // In particular, always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(final String clientPackageName, final int clientUid,
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
        if (ANDROID_AUTO_PACKAGE_NAME.equals(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
        }
        return new BrowserRoot(MediaIDHelper.MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(final String parentId,
                               final Result<List<MediaBrowser.MediaItem>> result) {

        Log.i(CLASS_NAME, "OnLoadChildren:" + parentId);

        final List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate parser (JSON one)
        final DataParser dataParser = new JSONDataParserImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = new APIServiceProviderImpl(dataParser);

        if (MediaIDHelper.MEDIA_ID_ROOT.equals(parentId)) {

            final String iconUrl = "android.resource://" +
                    getApplicationContext().getPackageName() + "/drawable/ic_all_categories";

            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES)
                            .setTitle(getString(R.string.all_categories_title))
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(getString(R.string.all_categories_sub_title))
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
            result.sendResult(mediaItems);
        } else if (MediaIDHelper.MEDIA_ID_ALL_CATEGORIES.equals(parentId)) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            apiCallExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {

                            // Load all categories into menu
                            loadAllCategories(
                                    serviceProvider,
                                    downloader,
                                    mediaItems,
                                    result
                            );
                        }
                    }
            );
        } else if (parentId.startsWith(MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES)) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            final String primaryMenuId
                    = parentId.replace(MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES, "");

            apiCallExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {

                            // Load child categories into menu
                            loadChildCategories(
                                    serviceProvider,
                                    downloader,
                                    primaryMenuId,
                                    mediaItems,
                                    result
                            );
                        }
                    }
            );
        } else if (parentId.startsWith(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES)) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            final String childMenuId
                    = parentId.replace(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES, "");

            apiCallExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {

                            // Load Radio Stations into menu
                            loadStationsInCategory(
                                    serviceProvider,
                                    downloader,
                                    childMenuId,
                                    mediaItems,
                                    result
                            );
                        }
                    });
        } else if (parentId.startsWith(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY)) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            final String radioStationId
                    = parentId.replace(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, "");

            apiCallExecutor.submit(
                    new Runnable() {

                        @Override
                        public void run() {

                            // Load Radio Station
                            loadStation(
                                    serviceProvider,
                                    downloader,
                                    radioStationId,
                                    mediaItems,
                                    result
                            );
                        }
                    }
            );
        } else {
            Log.w(CLASS_NAME, "Skipping unmatched parentId: " + parentId);
            result.sendResult(mediaItems);
        }
    }

    @Override
    public void onCompletion(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "On MediaPlayer completion");

        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (!playingQueue.isEmpty()) {
            // In this sample, we restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= playingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
        Log.e(CLASS_NAME, "MediaPlayer error: what=" + what + ", extra=" + extra);
        handleStopRequest("MediaPlayer error " + what + " (" + extra + ")");
        return true; // true indicates we handled the error
    }

    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "MediaPlayer prepared");

        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
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
     * Load All Categories into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadAllCategories(final APIServiceProvider serviceProvider,
                                   final Downloader downloader,
                                   final List<MediaBrowser.MediaItem> mediaItems,
                                   final Result<List<MediaBrowser.MediaItem>> result) {
        final List<CategoryVO> list = serviceProvider.getAllCategories(downloader,
                UrlBuilder.getAllCategoriesUrl(getApplicationContext()));

        QueueHelper.copyCollection(allCategories, list);

        final String iconUrl = "android.resource://" +
                getApplicationContext().getPackageName() + "/drawable/ic_child_categories";

        for (CategoryVO category : allCategories) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getName())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(category.getDescription())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }

    /**
     * Load Child Categories into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param primaryItemId   Id of the primary menu item.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadChildCategories(final APIServiceProvider serviceProvider,
                                     final Downloader downloader,
                                     final String primaryItemId,
                                     final List<MediaBrowser.MediaItem> mediaItems,
                                     final Result<List<MediaBrowser.MediaItem>> result) {
        final List<CategoryVO> list = serviceProvider.getChildCategories(downloader,
                UrlBuilder.getChildCategoriesUrl(getApplicationContext(), primaryItemId));

        QueueHelper.copyCollection(childCategories, list);

        final String iconUrl = "android.resource://" +
                getApplicationContext().getPackageName() + "/drawable/ic_child_categories";

        for (CategoryVO category : childCategories) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getName())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(category.getDescription())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }

    /**
     * Load Radio Stations into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param categoryId      Id of the Category.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadStationsInCategory(final APIServiceProvider serviceProvider,
                                        final Downloader downloader,
                                        final String categoryId,
                                        final List<MediaBrowser.MediaItem> mediaItems,
                                        final Result<List<MediaBrowser.MediaItem>> result) {
        final List<RadioStationVO> list = serviceProvider.getStationsInCategory(downloader,
                UrlBuilder.getStationsInCategory(getApplicationContext(), categoryId));

        QueueHelper.copyCollection(radioStations, list);

        final String iconUrl = "android.resource://" +
                getApplicationContext().getPackageName() + "/drawable/ic_child_categories";

        final String genre = QueueHelper.getGenreNameById(categoryId, childCategories);

        for (RadioStationVO radioStation : radioStations) {

            radioStation.setGenre(genre);

            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY
                                            + String.valueOf(radioStation.getId())
                            )
                            .setTitle(radioStation.getName())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(radioStation.getCountry())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }

    /**
     * Load Radio Station.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param radioStationId  Id of the Radio Station.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadStation(final APIServiceProvider serviceProvider,
                             final Downloader downloader,
                             final String radioStationId,
                             final List<MediaBrowser.MediaItem> mediaItems,
                             final Result<List<MediaBrowser.MediaItem>> result) {
        final RadioStationVO radioStation
                = serviceProvider.getStation(downloader,
                UrlBuilder.getStation(getApplicationContext(), radioStationId));

        MediaMetadata track;
        try {
            track = JSONDataParserImpl.buildMediaMetadataFromRadioStation(getApplicationContext(),
                    radioStation);
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Can not parse Media Metadata:" + e.getMessage());
            return;
        }

        final MediaDescription mediaDescription = track.getDescription();
        final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);
        mediaItems.add(mediaItem);

        result.sendResult(mediaItems);
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
     * Starts playing the current song in the playing queue.
     */
    private void playCurrentSong() {
        final MediaMetadata track = getCurrentPlayingRadioStation();
        if (track == null) {
            Log.e(CLASS_NAME, "Play Radio Station: ignoring request to play next song, " +
                    "because cannot find it." +
                    " CurrentIndex=" + mCurrentIndexOnQueue + "." +
                    " PlayQueue.size=" + playingQueue.size());
            return;
        }
        final String source = track.getString(JSONDataParserImpl.CUSTOM_METADATA_TRACK_SOURCE);
        Log.d(CLASS_NAME, "Play Radio Station: current (" + mCurrentIndexOnQueue + ") in playingQueue. " +
                " musicId=" + track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) +
                " source=" + source);

        mState = PlaybackState.STATE_STOPPED;

        // release everything except MediaPlayer
        relaxResources(false);

        createMediaPlayerIfNeeded();

        mState = PlaybackState.STATE_BUFFERING;

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(source);

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
            updatePlaybackState(ex.getMessage());
        }
    }

    private MediaMetadata getCurrentPlayingRadioStation() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, playingQueue)) {
            return null;
        }
        final MediaSession.QueueItem item = playingQueue.get(mCurrentIndexOnQueue);
        if (item == null) {
            return null;
        }
        final String mediaId = item.getDescription().getMediaId();
        final RadioStationVO radioStation = QueueHelper.getRadioStationById(mediaId, radioStations);
        Log.d(CLASS_NAME, "CurrentPlayingRadioStation for id=" + mediaId);
        MediaMetadata track = null;
        try {
            track = JSONDataParserImpl.buildMediaMetadataFromRadioStation(getApplicationContext(),
                    radioStation);
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Update metadata:" + e.getMessage());
        }
        return track;
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, playingQueue)) {
            Log.e(CLASS_NAME, "Can't retrieve current metadata.");
            mState = PlaybackState.STATE_ERROR;
            updatePlaybackState("No Metadata");
            return;
        }

        final MediaSession.QueueItem queueItem = playingQueue.get(mCurrentIndexOnQueue);
        final String mediaId = queueItem.getDescription().getMediaId();
        final RadioStationVO radioStation = QueueHelper.getRadioStationById(mediaId, radioStations);
        final MediaMetadata track;
        try {
            track = JSONDataParserImpl.buildMediaMetadataFromRadioStation(getApplicationContext(),
                    radioStation);
        } catch (JSONException e) {
            Log.e(CLASS_NAME, "Update metadata:" + e.getMessage());
            return;
        }
        final String trackId = track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
        if (!mediaId.equals(trackId)) {
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
     *            be released or not
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
            playCurrentSong();
        }
    }

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
     *
     */
    private void updatePlaybackState(String error) {
        Log.d(CLASS_NAME, "UpdatePlaybackState, setting session playback state to " + mState);

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
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, playingQueue)) {
            MediaSession.QueueItem item = playingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        Log.d(CLASS_NAME, "UpdatePlaybackState, state:" + mState);
        if (mState == PlaybackState.STATE_PLAYING || mState == PlaybackState.STATE_PAUSED) {
            mMediaNotification.startNotification();
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY
                | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackState.ACTION_PLAY_FROM_SEARCH;
        if (playingQueue.isEmpty()) {
            return actions;
        }
        if (mState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < playingQueue.size() - 1) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    /**
     * Try to get the system audio focus.
     */
    void tryToGetAudioFocus() {
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
    private void handleStopRequest(String withError) {
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

        /*MediaMetadata currentMusic = getCurrentPlayingRadioStation();
        if (currentMusic != null) {
            // Set appropriate "Favorite" icon on Custom action:
            String mediaId = currentMusic.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            int favoriteIcon = R.drawable.ic_star_off;
            if (mMusicProvider.isFavorite(mediaId)) {
                favoriteIcon = R.drawable.ic_star_on;
            }
            Log.d(CLASS_NAME, "updatePlaybackState, setting Favorite custom action of music ",
                    mediaId, " current favorite=" + mMusicProvider.isFavorite(mediaId));
            stateBuilder.addCustomAction(CUSTOM_ACTION_THUMBS_UP, getString(R.string.favorite),
                    favoriteIcon);
        }*/
    }

    private final class MediaSessionCallback extends MediaSession.Callback {

        private final String CLASS_NAME = MediaSessionCallback.class.getSimpleName();

        @Override
        public void onPlay() {
            super.onPlay();

            Log.i(CLASS_NAME, "On Play");

            if (playingQueue.isEmpty()) {
                //mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                //mSession.setQueue(mPlayingQueue);
                //mSession.setQueueTitle(getString(R.string.random_queue_title));

                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (!playingQueue.isEmpty()) {
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

            if (!playingQueue.isEmpty()) {

                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue
                        = QueueHelper.getRadioStationIndexOnQueue(playingQueue, id);

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

            QueueHelper.copyCollection(playingQueue, QueueHelper.getPlayingQueue(
                    getApplicationContext(),
                    radioStations)
            );

            mSession.setQueue(playingQueue);

            final String queueTitle = "Queue Title";
            mSession.setQueueTitle(queueTitle);

            if (playingQueue.isEmpty()) {
                return;
            }

            // Set the current index on queue from the Radio Station Id:
            mCurrentIndexOnQueue = QueueHelper.getRadioStationIndexOnQueue(playingQueue, mediaId);

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
            if (mCurrentIndexOnQueue >= playingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, playingQueue)) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(CLASS_NAME, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" + playingQueue.size());

                handleStopRequest("Cannot skip");
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
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, playingQueue)) {
                mState = PlaybackState.STATE_STOPPED;
                handlePlayRequest();
            } else {
                Log.e(CLASS_NAME, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" + playingQueue.size());

                handleStopRequest("Cannot skip");
            }
        }
    }
}
