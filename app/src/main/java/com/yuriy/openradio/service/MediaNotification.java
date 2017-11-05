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

package com.yuriy.openradio.service;

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
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.LruCache;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.BitmapHelper;
import com.yuriy.openradio.utils.FabricUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Keeps track of a notification and updates it automatically for a given
 * MediaSession. Maintaining a visible notification (usually) guarantees that the music service
 * won't be killed during playback.
 */
public class MediaNotification extends BroadcastReceiver {
    
    private static final String CLASS_NAME = MediaNotification.class.getSimpleName();

    private static final int NOTIFICATION_ID = 412;

    private static final String ACTION_PAUSE = "com.yuriy.openradio.pause";
    private static final String ACTION_PLAY = "com.yuriy.openradio.play";
    private static final String ACTION_PREV = "com.yuriy.openradio.prev";
    private static final String ACTION_NEXT = "com.yuriy.openradio.next";

    private static final int MAX_ALBUM_ART_CACHE_SIZE = 1024 * 1024;

    private final OpenRadioService mService;
    private MediaSessionCompat.Token mSessionToken;
    private MediaControllerCompat mController;
    private MediaControllerCompat.TransportControls mTransportControls;
    private final LruCache<String, Bitmap> mAlbumArtCache;

    private PlaybackStateCompat mPlaybackState;
    private MediaMetadataCompat mMetadata;

    private NotificationCompat.Builder mNotificationBuilder;
    private NotificationManagerCompat mNotificationManager;
    private NotificationCompat.Action mPlayPauseAction;

    private PendingIntent mPauseIntent, mPlayIntent, mPreviousIntent, mNextIntent;

    private int mNotificationColor;

    private boolean mStarted = false;

    public MediaNotification(final OpenRadioService service) {
        mService = service;
        updateSessionToken();

        // simple album art cache that holds no more than
        // MAX_ALBUM_ART_CACHE_SIZE bytes:
        mAlbumArtCache = new LruCache<String, Bitmap>(MAX_ALBUM_ART_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
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
            ApplicationInfo applicationInfo =
                    mService.getPackageManager().getApplicationInfo(packageName, 0);
            packageContext.setTheme(applicationInfo.theme);
            Resources.Theme theme = packageContext.getTheme();
            TypedArray ta = theme.obtainStyledAttributes(
                    new int[] {android.support.v7.appcompat.R.attr.colorPrimary});
            notificationColor = ta.getColor(0, Color.DKGRAY);
            ta.recycle();
        } catch (final Exception e) {
            FabricUtils.logException(e);
        }
        return notificationColor;
    }

    /**
     * Posts the notification and starts tracking the session to keep it
     * updated. The notification will automatically be removed if the session is
     * destroyed before {@link #stopNotification} is called.
     */
    public void startNotification() {
        if (!mStarted) {
            mController.registerCallback(mCb);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_NEXT);
            filter.addAction(ACTION_PAUSE);
            filter.addAction(ACTION_PLAY);
            filter.addAction(ACTION_PREV);
            mService.registerReceiver(this, filter);

            mMetadata = mController.getMetadata();
            mPlaybackState = mController.getPlaybackState();

            mStarted = true;
            // The notification must be updated after setting started to true
            updateNotificationMetadata();
        }
    }

    /**
     * Removes the notification and stops tracking the session. If the session
     * was destroyed this has no effect.
     */
    public void stopNotification() {
        mStarted = false;
        mController.unregisterCallback(mCb);
        try {
            mNotificationManager.cancel(NOTIFICATION_ID);
            mService.unregisterReceiver(this);
        } catch (final IllegalArgumentException ex) {
            // ignore if the receiver is not registered.
        }
        mService.stopForeground(true);
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
                FabricUtils.logException(e);
                return;
            }
            mTransportControls = mController.getTransportControls();
            if (mStarted) {
                mController.registerCallback(mCb);
            }
        }
    }

    private final MediaControllerCompat.Callback mCb = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            mPlaybackState = state;
            AppLogger.d(CLASS_NAME + " Received new playback state:" + state);
            updateNotificationPlaybackState();
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mMetadata = metadata;
            AppLogger.d(CLASS_NAME + " Received new metadata:" + metadata);
            updateNotificationMetadata();
        }

        @Override
        public void onSessionDestroyed() {
            super.onSessionDestroyed();
            AppLogger.d(CLASS_NAME + " Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }
    };

    private void updateNotificationMetadata() {
        AppLogger.d(CLASS_NAME + " Update Notification Metadata : " + mMetadata);
        if (mMetadata == null || mPlaybackState == null) {
            return;
        }

        updatePlayPauseAction();

        mNotificationBuilder = new NotificationCompat.Builder(mService);
        int playPauseActionIndex = 0;

        // If skip to previous action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            mNotificationBuilder
                    .addAction(R.drawable.ic_skip_previous_white_24dp,
                            mService.getString(R.string.label_previous), mPreviousIntent);
            playPauseActionIndex = 1;
        }

        mNotificationBuilder.addAction(mPlayPauseAction);

        // If skip to next action is enabled
        if ((mPlaybackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            mNotificationBuilder.addAction(R.drawable.ic_skip_next_white_24dp,
                    mService.getString(R.string.label_next), mNextIntent);
        }

        final MediaDescriptionCompat description = mMetadata.getDescription();

        String fetchArtUrl = null;
        Bitmap art = description.getIconBitmap();
        if (art == null && description.getIconUri() != null) {
            // This sample assumes the iconUri will be a valid URL formatted String, but
            // it can actually be any valid Android Uri formatted String.
            // async fetch the album art icon
            final String artUrl = description.getIconUri().toString();
            art = mAlbumArtCache.get(artUrl);
            if (art == null) {
                fetchArtUrl = artUrl;
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(mService.getResources(), R.drawable.ic_radio_station);
            }
        }

        mNotificationBuilder
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(playPauseActionIndex)  // only show play/pause in compact view
                        .setMediaSession(mSessionToken))
                .setColor(mNotificationColor)
                .setSmallIcon(R.drawable.ic_notification)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setUsesChronometer(true)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(art);

        updateNotificationPlaybackState();

        mService.startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
        if (fetchArtUrl != null && !BitmapHelper.isUrlLocalResource(fetchArtUrl)) {
            fetchBitmapFromURLAsync(fetchArtUrl);
        }
    }

    private void updatePlayPauseAction() {
        AppLogger.d(CLASS_NAME + " updatePlayPauseAction");
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
        if (mPlayPauseAction == null) {
            mPlayPauseAction = new NotificationCompat.Action(icon, label, intent);
        } else {
            mPlayPauseAction.icon = icon;
            mPlayPauseAction.title = label;
            mPlayPauseAction.actionIntent = intent;
        }
    }

    private void updateNotificationPlaybackState() {
        AppLogger.d(CLASS_NAME + " updateNotificationPlaybackState. mPlaybackState=" + mPlaybackState);
        if (mPlaybackState == null || !mStarted) {
            AppLogger.d(CLASS_NAME + " updateNotificationPlaybackState. cancelling notification!");
            mService.stopForeground(true);
            return;
        }
        if (mNotificationBuilder == null) {
            AppLogger.d(CLASS_NAME + " updateNotificationPlaybackState. there is no notificationBuilder. Ignoring request to update state!");
            return;
        }
        if (mPlaybackState.getPosition() >= 0) {
            AppLogger.d(CLASS_NAME + " updateNotificationPlaybackState. updating playback position to " +
                    (System.currentTimeMillis() - mPlaybackState.getPosition()) / 1000 + " seconds");
            mNotificationBuilder
                    .setWhen(System.currentTimeMillis() - mPlaybackState.getPosition())
                    .setShowWhen(true)
                    .setUsesChronometer(true);
            mNotificationBuilder.setShowWhen(true);
        } else {
            AppLogger.d(CLASS_NAME + " updateNotificationPlaybackState. hiding playback position");
            mNotificationBuilder
                    .setWhen(0)
                    .setShowWhen(false)
                    .setUsesChronometer(false);
        }

        updatePlayPauseAction();

        // Make sure that the notification can be dismissed by the user when we are not playing:
        mNotificationBuilder.setOngoing(mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING);

        mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void fetchBitmapFromURLAsync(final String source) {
        AppLogger.d(CLASS_NAME + " getBitmapFromURLAsync: starting async task to fetch " + source);
        final AsyncTask<Void, Void, Bitmap> task = new FetchBitmapAsyncTask(this, source);
        task.execute();
    }

    /**
     * async task to fetch Bitmap from provided URL.
     */
    private static final class FetchBitmapAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        /**
         * Reference to the enclosing class.
         */
        private final WeakReference<MediaNotification> mReference;

        /**
         * Resource URL.
         */
        private final String mSource;

        /**
         * Main constructor.
         *
         * @param mediaNotification Reference to the enclosing class.
         * @param source            URL of the resource.
         */
        private FetchBitmapAsyncTask(final MediaNotification mediaNotification,
                                     final String source) {
            super();
            mReference = new WeakReference<>(mediaNotification);
            mSource = source;
        }

        @Override
        protected Bitmap doInBackground(final Void... params) {
            final MediaNotification reference = mReference.get();
            if (reference == null) {
                return  null;
            }

            Bitmap bitmap = null;
            try {
                bitmap = BitmapHelper.fetchAndRescaleBitmap(
                        mSource,
                        BitmapHelper.MEDIA_ART_BIG_WIDTH,
                        BitmapHelper.MEDIA_ART_BIG_HEIGHT
                );
            } catch (final IOException e) {
                FabricUtils.logException(e);
            }
            if (mSource != null && bitmap != null) {
                reference.mAlbumArtCache.put(mSource, bitmap);
            } else {
                AppLogger.e(CLASS_NAME + " Can not put data into AlbumArtCache, " +
                        "key == null || value == null");
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            final MediaNotification reference = mReference.get();
            if (reference == null) {
                return;
            }
            if (bitmap != null && reference.mMetadata != null
                    && reference.mNotificationBuilder != null
                    && reference.mMetadata.getDescription().getIconUri() != null
                    && !TextUtils.equals(mSource, reference.mMetadata.getDescription().getIconUri().toString())) {
                // If the media is still the same, update the notification:
                AppLogger.d(CLASS_NAME + " GetBitmapFromURLAsync: set bitmap to " + mSource);
                reference.mNotificationBuilder.setLargeIcon(bitmap);
                reference.mNotificationManager.notify(
                        NOTIFICATION_ID, reference.mNotificationBuilder.build()
                );
            }
        }
    }
}
