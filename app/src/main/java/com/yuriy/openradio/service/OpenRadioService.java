package com.yuriy.openradio.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaDescription;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
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
                   MediaPlayer.OnErrorListener {

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = OpenRadioService.class.getSimpleName();

    public static final String ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead";

    /**
     * Player instance to play Radio stream.
     */
    private MediaPlayer mMediaPlayer;

    private MediaSession mSession;

    /**
     * Current local media player state
     */
    private int mState = PlaybackState.STATE_NONE;

    /**
     * Wifi lock that we hold when streaming files from the internet,
     * in order to prevent the device from shutting off the Wifi radio.
     */
    private WifiManager.WifiLock mWifiLock;

    // Type of audio focus we have:
    private AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

    private AudioManager mAudioManager;

    private ExecutorService apiCallExecutor = Executors.newSingleThreadExecutor();

    private enum AudioFocus {
        /**
         * There is no audio focus, and no possible to "duck"
         */
        NoFocusNoDuck,

        /**
         * There is no focus, but can play at a low volume ("ducking")
         */
        NoFocusCanDuck,

        /**
         * There is full audio focus
         */
        Focused
    }

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        final List<MediaBrowser.MediaItem> mediaItems = new ArrayList<>();

        // Instantiate appropriate downloader (HTTP one)
        final Downloader downloader = new HTTPDownloaderImpl();
        // Instantiate appropriate parser (JSON one)
        final DataParser dataParser = new JSONDataParserImpl();
        // Instantiate appropriate API service provider
        final APIServiceProvider serviceProvider = new APIServiceProviderImpl(dataParser);

        if (MediaIDHelper.MEDIA_ID_ROOT.equals(parentId)) {
            Log.d(CLASS_NAME, "OnLoadChildren.ROOT");
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(MediaIDHelper.MEDIA_ID_ALL_CATEGORIES)
                            .setTitle(getString(R.string.all_categories_title))
                            .setIconUri(Uri.parse("android.resource://" +
                                    "com.example.android.mediabrowserservice/drawable/ic_by_genre"))
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

        } else {
            Log.w(CLASS_NAME, "Skipping unmatched parentId: " + parentId);
            result.sendResult(mediaItems);
        }
    }

    @Override
    public void onCompletion(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "On MediaPlayer completion");
    }

    @Override
    public boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
        Log.e(CLASS_NAME, "On MediaPlayer error");
        return false;
    }

    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {
        Log.i(CLASS_NAME, "On MediaPlayer prepared");

        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }

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
        final List<CategoryVO> allCategories
                = serviceProvider.getAllCategories(downloader,
                UrlBuilder.getAllCategoriesUrl(getApplicationContext()));

        for (CategoryVO category : allCategories) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getName())
                            .setIconUri(Uri.parse("android.resource://" +
                                    "com.example.android.mediabrowserservice/drawable/ic_by_genre"))
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
        final List<CategoryVO> childCategories
                = serviceProvider.getChildCategories(downloader,
                UrlBuilder.getChildCategoriesUrl(getApplicationContext(), primaryItemId));

        for (CategoryVO category : childCategories) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getName())
                            .setIconUri(Uri.parse("android.resource://" +
                                    "com.example.android.mediabrowserservice/drawable/ic_by_genre"))
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
        final List<RadioStationVO> radioStations
                = serviceProvider.getStationsInCategory(downloader,
                UrlBuilder.getStationsInCategory(getApplicationContext(), categoryId));

        for (RadioStationVO radioStation : radioStations) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY
                                            + String.valueOf(radioStation.getId())
                            )
                            .setTitle(radioStation.getName())
                            .setIconUri(Uri.parse("android.resource://" +
                                    "com.example.android.mediabrowserservice/drawable/ic_by_genre"))
                            .setSubtitle(radioStation.getCountry())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

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
    void playCurrentSong() {
        /*MediaMetadata track = getCurrentPlayingMusic();
        if (track == null) {
            LogHelper.e(CLASS_NAME, "playSong:  ignoring request to play next song, because cannot" +
                    " find it." +
                    " currentIndex=" + mCurrentIndexOnQueue +
                    " playQueue.size=" + (mPlayingQueue==null?"null": mPlayingQueue.size()));
            return;
        }*/
        String source = /*track.getString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE)*/
                "http://online-radioroks.tavrmedia.ua/RadioROKS";
        //LogHelper.d(CLASS_NAME, "playSong:  current (" + mCurrentIndexOnQueue + ") in playingQueue. " +
        //        " musicId=" + track.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) +
        //        " source=" + source);

        mState = PlaybackState.STATE_STOPPED;

        // release everything except MediaPlayer
        relaxResources(false);

        try {
            createMediaPlayerIfNeeded();

            //mState = PlaybackState.STATE_BUFFERING;

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
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

            //updatePlaybackState(null);
            //updateMetadata();

        } catch (IOException ex) {
            Log.e(CLASS_NAME, "IOException playing song:" + ex.getMessage());
            //updatePlaybackState(ex.getMessage());
        }
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
        //mDelayedStopHandler.removeCallbacksAndMessages(null);
        //mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

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

    private final class MediaSessionCallback extends MediaSession.Callback {

        private final String CLASS_NAME = MediaSessionCallback.class.getSimpleName();

        @Override
        public void onPlay() {
            super.onPlay();

            Log.i(CLASS_NAME, "On Play");
        }

        @Override
        public void onSkipToQueueItem(final long id) {
            super.onSkipToQueueItem(id);

            Log.i(CLASS_NAME, "On Skip to queue item, id:" + id);
        }

        @Override
        public void onPlayFromMediaId(final String mediaId, final Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);

            Log.i(CLASS_NAME, "On Play from media id:" + mediaId + " extras:" + extras);
        }

        @Override
        public void onPause() {
            super.onPause();

            Log.i(CLASS_NAME, "On Pause");
        }

        @Override
        public void onStop() {
            super.onStop();

            Log.i(CLASS_NAME, "On Stop");
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();

            Log.i(CLASS_NAME, "On Skip to next");
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();

            Log.i(CLASS_NAME, "On Skip to previous");
        }
    }
}
