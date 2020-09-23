/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.yuriy.openradio.shared.notification;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.net.UrlBuilder;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.LruCacheObject;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.view.activity.MainActivity;
import com.yuriy.openradio.view.activity.TvMainActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public final class MediaNotification extends BroadcastReceiver {

    private static final String CLASS_NAME = MediaNotification.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;

    private static final String ACTION_PAUSE = "com.yuriy.openradio.pause";
    private static final String ACTION_PLAY = "com.yuriy.openradio.play";
    private static final String ACTION_PREV = "com.yuriy.openradio.prev";
    private static final String ACTION_NEXT = "com.yuriy.openradio.next";

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 1024 * 1024;

    @NonNull
    private final OpenRadioService mService;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private final LruCache<String, LruCacheObject> mAlbumArtCache;

    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMetadata;
    private final MediaControllerCompat.Callback mCb;

    private final NotificationManagerCompat mNotificationManager;
//    private NotificationCompat.Action mPlayPauseAction;

    private final PendingIntent mPauseIntent;
    private final PendingIntent mPlayIntent;
    private final PendingIntent mPreviousIntent;
    private final PendingIntent mNextIntent;

    private final int mNotificationColor;

    private final AtomicBoolean mStarted = new AtomicBoolean(false);
    private final NotificationChannelFactory mNotificationChannelFactory;

    public MediaNotification(@NonNull final OpenRadioService service) {
        super();

        mCb = new MediaControllerCompatCallback();
        mService = service;
        mNotificationChannelFactory = new NotificationChannelFactory(mService);
        updateSessionToken();

        // simple album art cache that holds no more than
        // MAX_ALBUM_ART_CACHE_SIZE bytes:
        mAlbumArtCache = new LruCache<String, LruCacheObject>(MAX_ALBUM_ART_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, LruCacheObject value) {
                return value.getBitmap() != null ? value.getBitmap().getByteCount() : 0;
            }
        };

        mNotificationColor = getNotificationColor();

        mNotificationManager = NotificationManagerCompat.from(mService);

        String pkg = mService.getPackageName();
        mPauseIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PAUSE).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPlayIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PLAY).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mPreviousIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_PREV).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
        mNextIntent = PendingIntent.getBroadcast(mService, 100,
                new Intent(ACTION_NEXT).setPackage(pkg), PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private int getNotificationColor() {
        int notificationColor = 0;
        String packageName = mService.getPackageName();
        try {
            Context packageContext = mService.createPackageContext(packageName, 0);
            ApplicationInfo applicationInfo = mService.getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(new int[]{android.R.attr.colorPrimary});
            notificationColor = ta.getColor(0, Color.DKGRAY);
            ta.recycle();
        } catch (final Exception e) {
            AnalyticsUtils.logException(e);
        }
        return notificationColor;
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification(final RadioStation radioStation) {
        if (mStarted.get()) {
            return;
        }
        mController.registerCallback(mCb);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PREV);
        mService.registerReceiver(this, filter);

        final MediaMetadataCompat metadata = mController.getMetadata();
        if (metadata != null) {
            mMetadata = metadata;
        } else {
            mMetadata = MediaItemHelper.metadataFromRadioStation(radioStation);
        }
        PlaybackStateCompat playbackState = mController.getPlaybackState();
        if (playbackState != null) {
            mPlaybackState = playbackState;
        } else {
            AppLogger.e("StartNotification with null playback state");
        }

        mStarted.set(true);
        // The notification must be updated after setting started to true
        handleNotification();
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        AppLogger.d(CLASS_NAME + " stop, ORS[" + mService.hashCode() + "]");
        mStarted.set(false);
        mController.unregisterCallback(mCb);
        mNotificationManager.cancelAll();
        try {
            mService.unregisterReceiver(this);
        } catch (final IllegalArgumentException ex) {
            AppLogger.e(CLASS_NAME + " error while unregister:" + ex);
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        AppLogger.d(CLASS_NAME + " Received intent with action " + action);
        if (TextUtils.isEmpty(action)) {
            return;
        }
        switch (action) {
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_PREV:
                mTransportControls.skipToPrevious();
                break;
        }
    }

    /**
     * Update the state based on a change on the session token. Called either when
     * we are running for the first time or when the media session owner has destroyed the session
     * (see {@link android.media.session.MediaController.Callback#onSessionDestroyed()})
     */
    private void updateSessionToken() {
        final MediaSessionCompat.Token freshToken = mService.getSessionToken();
        if (mSessionToken == null || !mSessionToken.equals(freshToken)) {
            if (mController != null) {
                mController.unregisterCallback(mCb);
            }
            mSessionToken = freshToken;
            try {
                mController = new MediaControllerCompat(mService, mSessionToken);
            } catch (final RemoteException e) {
                AnalyticsUtils.logException(e);
                return;
            }
            mTransportControls = mController.getTransportControls();
            if (mStarted.get()) {
                mController.registerCallback(mCb);
            }
        }
    }

    private final class MediaControllerCompatCallback extends MediaControllerCompat.Callback {

        private MediaControllerCompatCallback() {
            super();
        }

        @Override
        public void onPlaybackStateChanged(final @NonNull PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + " Received new playback state:" + state);
            mPlaybackState = state;
            handleNotification();
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            AppLogger.d(CLASS_NAME + " Received new metadata:" + metadata);
            if (metadata != null) {
                mMetadata = metadata;
            } else {
                AppLogger.e("OnMetadataChanged null metadata, prev metadata " + mMetadata);
            }
            handleNotification();
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.d(CLASS_NAME + " Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    }

    public void handleNotification() {
        if (mMetadata == null) {
            showNoStreamNotification();
            return;
        }
        if (mPlaybackState == null) {
            showNoStreamNotification();
            return;
        }
        if (mService == null) {
            showNoStreamNotification();
            return;
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mService, MediaNotificationData.CHANNEL_ID
        );

        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(new MediaNotificationData(mService, mMetadata));
        int playPauseActionIndex = 0;
        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(
                    R.drawable.ic_skip_previous_white_24dp,
                    mService.getString(R.string.label_previous),
                    mPreviousIntent
            );
            playPauseActionIndex = 1;
        }

        // Build the style.
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle
                = new androidx.media.app.NotificationCompat.MediaStyle()
                // only show play/pause in compact view
                .setShowActionsInCompactView(playPauseActionIndex)
                .setMediaSession(mSessionToken);

        final MediaDescriptionCompat description = mMetadata.getDescription();

        LruCacheObject cacheObject;
//        String fetchArtUrl = null;
        Bitmap art = description.getIconBitmap();
        if (art == null && description.getIconUri() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            final String artUrl = UrlBuilder.preProcessIconUrl(
                    description.getIconUri().toString()
            );
            cacheObject = mAlbumArtCache.get(artUrl);
            if (cacheObject == null) {
                mAlbumArtCache.put(artUrl, new LruCacheObject());
            }
            art = cacheObject != null ? cacheObject.getBitmap() : null;
            if (art == null) {
//                fetchArtUrl = artUrl;
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(
                        mService.getResources(), R.drawable.ic_radio_station
                );
            }
        }

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(
                    R.drawable.ic_skip_next_white_24dp,
                    mService.getString(R.string.label_next),
                    mNextIntent
            );
        }

        builder
                .setContentIntent(makePendingIntent())
                .addAction(getPlayPauseAction())
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        AppLogger.d(
                CLASS_NAME + " Update Notification " +
                        "state:" + mPlaybackState +
                        "title:" + description.getTitle() +
                        "subtitle:" + description.getSubtitle()
        );
        AppLogger.d(CLASS_NAME + " update, ORS[" + mService.hashCode() + "]");
        mService.startForeground(NOTIFICATION_ID, builder.build());
        // TODO: Fetch and update Notification.
//        if (fetchArtUrl != null && !BitmapUtils.isUrlLocalResource(fetchArtUrl)) {
//            if (cacheObject == null) {
//                fetchBitmapFromURLAsync(fetchArtUrl);
//            }
//        }
    }

    public void notifyServiceStarted() {
        final Bitmap art = BitmapFactory.decodeResource(
                mService.getResources(), R.drawable.ic_radio_station
        );
        // Build the style.
        final androidx.media.app.NotificationCompat.MediaStyle mediaStyle
                = new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mSessionToken);
        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(new ServiceStartedNotificationData(mService));

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mService, ServiceStartedNotificationData.CHANNEL_ID
        );
        builder
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("Open Radio")
                .setContentText("Open Radio just started")
                .setLargeIcon(art);
        AppLogger.d(CLASS_NAME + " show Just Started notification ORS[" + mService.hashCode() + "]");
        mService.startForeground(NOTIFICATION_ID, builder.build());
    }

    public void doInitialNotification(final RadioStation radioStation) {
        mMetadata = MediaItemHelper.metadataFromRadioStation(radioStation);
        if (mMetadata == null) {
            AppLogger.e(
                    "StartNotification null metadata, after created from RadioStation."
            );
        }
        handleNotification();
    }

    @Nullable
    private PendingIntent makePendingIntent() {
        if (mService == null) {
            return null;
        }
        return PendingIntent.getActivity(
                mService, 0,
                new Intent(mService, mService.isTv() ? TvMainActivity.class : MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private void showNoStreamNotification() {
        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannelNoStream(new NoMediaNotificationData(mService));

        // Build the style.
        androidx.media.app.NotificationCompat.MediaStyle mediaStyle
                = new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mSessionToken);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mService, NoMediaNotificationData.CHANNEL_ID
        );

        builder
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("Open Radio")
                .setContentText("No Radio Station selected")
                .setContentIntent(makePendingIntent())
                .setLargeIcon(BitmapFactory.decodeResource(
                        mService.getResources(), R.drawable.ic_radio_station
                ));
        AppLogger.d(CLASS_NAME + " No Radio Station, ORS[" + mService.hashCode() + "]");
        doNotifySafely(builder);
    }

    private NotificationCompat.Action getPlayPauseAction() {
        String label;
        int icon;
        PendingIntent intent;
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause_white_24dp;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow_white_24dp;
            intent = mPlayIntent;
        }
        return new NotificationCompat.Action(icon, label, intent);
    }

    private void doNotifySafely(@NonNull final NotificationCompat.Builder builder) {
        // Address NPE inside ApplicationPackageManager when build notification
        try {
            AppLogger.d(CLASS_NAME + " notify safely, ORS[" + mService.hashCode() + "]");
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        } catch (final Exception e) {
            AppLogger.e("Can not do notification:" + e);
        }
    }

//    private void fetchBitmapFromURLAsync(@NonNull final String source) {
//        AppLogger.d(CLASS_NAME + " getBitmapFromURLAsync: starting async task to fetch " + source);
//        if (!BitmapUtils.isImageUrl(source)) {
//            // If url is not an image url - do not start async task.
//            return;
//        }
//        final AsyncTask<Void, Void, Bitmap> task = new FetchBitmapAsyncTask(this, source);
//        try {
//            task.execute();
//        } catch (final Exception e) {
//            AnalyticsUtils.logException(e);
//        }
//    }

    /**
     * async task to fetch Bitmap from provided URL.
     */
//    private static final class FetchBitmapAsyncTask extends AsyncTask<Void, Void, Bitmap> {
//
//        /**
//         * Reference to the enclosing class.
//         */
//        private final WeakReference<MediaNotification> mReference;
//
//        /**
//         * Resource URL.
//         */
//        @NonNull
//        private final String mSource;
//
//        /**
//         * Main constructor.
//         *
//         * @param mediaNotification Reference to the enclosing class.
//         * @param source            URL of the resource.
//         */
//        private FetchBitmapAsyncTask(final MediaNotification mediaNotification,
//                                     @NonNull final String source) {
//            super();
//            mReference = new WeakReference<>(mediaNotification);
//            mSource = source;
//        }
//
//        @Override
//        protected Bitmap doInBackground(final Void... params) {
//            final MediaNotification reference = mReference.get();
//            if (reference == null) {
//                return null;
//            }
//
//            Bitmap bitmap = null;
//            try {
//                bitmap = BitmapUtils.fetchAndRescaleBitmap(
//                        mSource,
//                        BitmapUtils.MEDIA_ART_BIG_WIDTH,
//                        BitmapUtils.MEDIA_ART_BIG_HEIGHT
//                );
//            } catch (final SocketTimeoutException e) {
//                AnalyticsUtils.logException(e);
//            } catch (final IOException e) {
//                AnalyticsUtils.logException(e);
//            }
//            if (bitmap != null) {
//                reference.mAlbumArtCache.get(mSource).setBitmap(bitmap);
//            } else {
//                AppLogger.e(CLASS_NAME + " Can not put data into AlbumArtCache, " +
//                        "key == null || value == null");
//            }
//            return bitmap;
//        }
//
//        @Override
//        protected void onPostExecute(final Bitmap bitmap) {
//            final MediaNotification reference = mReference.get();
//            if (reference == null) {
//                return;
//            }
//            if (!reference.mStarted.get()) {
//                // Service is stopped. If notification is go on the service will start again.
//                return;
//            }
//            if (bitmap != null && reference.mMetadata != null) {
//                // If the media is still the same, update the notification:
//                AppLogger.d(CLASS_NAME + " GetBitmapFromURLAsync: set bitmap to " + mSource);
//                // TODO: Update notification with image
//                // reference.handleNotification();
//            }
//        }
//    }
}
