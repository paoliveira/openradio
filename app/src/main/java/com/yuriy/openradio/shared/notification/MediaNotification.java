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
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.LruCacheObject;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.mobile.view.activity.MainActivity;
import com.yuriy.openradio.tv.view.activity.TvMainActivity;

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

    private MediaMetadataCompat mMetadata;
    private final MediaControllerCompatCallback mCb;

    private final NotificationManagerCompat mNotificationManager;

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
            if (AppUtils.hasVersionLollipop()) {
                TypedArray ta = theme.obtainStyledAttributes(new int[]{android.R.attr.colorPrimary});
                notificationColor = ta.getColor(0, Color.DKGRAY);
                ta.recycle();
            } else {
                notificationColor = 0xff444444;
            }
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
    public void startNotification(final Context context, final RadioStation radioStation) {
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
            mMetadata = MediaItemHelper.metadataFromRadioStation(context, radioStation);
        }
        mStarted.set(true);
        // The notification must be updated after setting started to true
        handleNotification(mController.getPlaybackState());
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

        private PlaybackStateCompat mPlaybackState;

        private MediaControllerCompatCallback() {
            super();
        }

        @Override
        public void onPlaybackStateChanged(final @NonNull PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + " Received new playback state:" + state);
            boolean doNotify;
            if (mPlaybackState == null) {
                doNotify = true;
            } else {
                doNotify = doHandleState(mPlaybackState.getState(), state.getState());
            }
            if (doNotify) {
                handleNotification(state);
            }
            mPlaybackState = state;
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            AppLogger.d(CLASS_NAME + " Received new metadata:" + metadata);
            if (metadata != null) {
                mMetadata = metadata;
            } else {
                AppLogger.e("OnMetadataChanged null metadata, prev metadata " + mMetadata);
            }
            handleNotification(mPlaybackState);
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.d(CLASS_NAME + " Session was destroyed, resetting to the new session token");
            updateSessionToken();
        }

        private boolean doHandleState(final int curState, final int newState) {
            if (newState == curState) {
                return false;
            }
            return true;
        }
    }

    public void handleNotification(final PlaybackStateCompat playbackState) {
        if (mMetadata == null) {
            return;
        }
        if (playbackState == null) {
            return;
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mService, MediaNotificationData.CHANNEL_ID
        );

        // Create/Retrieve Notification Channel for O and beyond devices (26+).
        mNotificationChannelFactory.createChannel(new MediaNotificationData(mService, mMetadata));
        int playPauseActionIndex = 0;
        // If skip to previous action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0) {
            builder.addAction(
                    R.drawable.ic_skip_prev,
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
                // use a placeholder art while the remote art is being downloaded
                art = BitmapFactory.decodeResource(
                        mService.getResources(), R.drawable.ic_radio_station
                );
            }
        }

        // If skip to next action is enabled
        if ((playbackState.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0) {
            builder.addAction(
                    R.drawable.ic_skip_next,
                    mService.getString(R.string.label_next),
                    mNextIntent
            );
        }

        int smallIcon = AppUtils.hasVersionLollipop() ?
                R.drawable.ic_notification :
                R.drawable.ic_notification_drawable;

        builder
                .setContentIntent(makePendingIntent())
                .addAction(getPlayPauseAction(playbackState))
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setLargeIcon(art)
                .setSmallIcon(smallIcon)
                .setOngoing(true)
                .setDefaults(0)
                .setSound(null)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle());

        AppLogger.d(
                CLASS_NAME + " Update Notification " +
                        "state:" + playbackState +
                        "title:" + description.getTitle() +
                        "subtitle:" + description.getSubtitle()
        );
        AppLogger.d(CLASS_NAME + " update, ORS[" + mService.hashCode() + "]");
//        mNotificationChannelFactory.updateChannel(NOTIFICATION_ID, builder.build());
        mService.startForeground(NOTIFICATION_ID, builder.build());
        // TODO: Fetch and update Notification.
//        if (fetchArtUrl != null && !BitmapUtils.isUrlLocalResource(fetchArtUrl)) {
//            if (cacheObject == null) {
//                fetchBitmapFromURLAsync(fetchArtUrl);
//            }
//        }
    }

    public void notifyService(final String message) {
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
        int smallIcon = AppUtils.hasVersionLollipop() ?
                R.drawable.ic_notification :
                R.drawable.ic_notification_drawable;
        builder
                .setContentIntent(makePendingIntent())
                .setStyle(mediaStyle)
                .setColor(mNotificationColor)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(mService.getString(R.string.app_name))
                .setContentText(message)
                .setSmallIcon(smallIcon)
                .setLargeIcon(art);
        AppLogger.d(CLASS_NAME + " show Just Started notification ORS[" + mService.hashCode() + "]");
        mService.startForeground(NOTIFICATION_ID, builder.build());
    }

    @Nullable
    private PendingIntent makePendingIntent() {
        return PendingIntent.getActivity(
                mService, 0,
                new Intent(mService, mService.isTv() ? TvMainActivity.class : MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private NotificationCompat.Action getPlayPauseAction(final PlaybackStateCompat playbackState) {
        String label;
        int icon;
        PendingIntent intent;
        if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            label = mService.getString(R.string.label_pause);
            icon = R.drawable.ic_pause;
            intent = mPauseIntent;
        } else {
            label = mService.getString(R.string.label_play);
            icon = R.drawable.ic_play_arrow;
            intent = mPlayIntent;
        }
        return new NotificationCompat.Action(icon, label, intent);
    }
}
