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

package com.yuriy.openradio.business;

import android.app.Activity;
import android.content.ComponentName;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.yuriy.openradio.business.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 29/06/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaResourcesManager {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MediaResourcesManager.class.getSimpleName();

    /**
     * Browses media content offered by a {@link android.service.media.MediaBrowserService}.
     */
    private MediaBrowserCompat mMediaBrowser;

    /**
     * Controller of media content offered by a {@link android.service.media.MediaBrowserService}.
     */
    private MediaControllerCompat mMediaController;

    /**
     * Listener of the media Controllers callbacks.
     */
    private final MediaControllerCompat.Callback mMediaSessionCallback = new MediaSessionCallback(this);

    /**
     * Transport controls of the Media Controller.
     */
    private MediaControllerCompat.TransportControls mTransportControls;

    /**
     * Callee {@link Activity}.
     */
    private final Activity mActivity;

    /**
     * Listener for the media resources events. Acts as proxy between this manager and callee Activity.
     */
    private final MediaResourceManagerListener mListener;

    /**
     * Constructor.
     *
     * @param activity Callee {@link Activity}.
     * @param listener Listener for the media resources events.
     */
    public MediaResourcesManager(@NonNull final Activity activity,
                                 @NonNull final MediaResourceManagerListener listener) {
        super();
        mActivity = activity;
        mListener = listener;
    }

    /**
     * Creates Media Browser, assigns listener.
     */
    public void create() {
        // Initialize Media Browser
        mMediaBrowser = new MediaBrowserCompat(
                mActivity.getApplicationContext(),
                new ComponentName(mActivity.getApplicationContext(), OpenRadioService.class),
                new MediaBrowserConnectionCallback(this), null
        );
    }

    /**
     * Connects to the Media Browse service.
     */
    public void connect() {
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    /**
     * Disconnects from the Media Browse service. After this, no more callbacks will be received.
     */
    public void disconnect() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaSessionCallback);
        }
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
    }

    /**
     * Queries for information about the media items that are contained within the specified id and subscribes to
     * receive updates when they change.
     *
     * @param parentId The id of the parent media item whose list of children will be subscribed.
     * @param callback The callback to receive the list of children.
     */
    public void subscribe(@NonNull String parentId, @NonNull MediaBrowserCompat.SubscriptionCallback callback) {
        if (mMediaBrowser != null) {
            mMediaBrowser.subscribe(parentId, callback);
        }
    }

    /**
     * Unsubscribes for changes to the children of the specified media id.
     *
     * @param parentId The id of the parent media item whose list of children will be unsubscribed.
     */
    public void unsubscribe(@NonNull String parentId) {
        if (mMediaBrowser != null) {
            mMediaBrowser.unsubscribe(parentId);
        }
    }

    /**
     * Gets the root id.<br>
     * Note that the root id may become invalid or change when when the browser is disconnected.
     *
     * @return Root Id.
     */
    public String getRoot() {
        if (mMediaBrowser == null) {
            return "";
        }
        return mMediaBrowser.getRoot();
    }

    public void transportControlsSkipToQueueItem(final long itemId) {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.skipToQueueItem(itemId);
    }

    public void transportControlsPlay() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.play();
    }

    public void transportControlsPause() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.pause();
    }

    public void transportControlsStop() {
        if (mTransportControls == null) {
            return;
        }
        mTransportControls.stop();
    }

    public PlaybackStateCompat getPlaybackState() {
        return mMediaController.getPlaybackState();
    }

    public MediaMetadataCompat getMediaMetadata() {
        return mMediaController != null ? mMediaController.getMetadata() : null;
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private static final class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        /**
         * Weak reference to the outer manager.
         */
        private final WeakReference<MediaResourcesManager> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the manager.
         */
        private MediaBrowserConnectionCallback(final MediaResourcesManager reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected() {
            AppLogger.d(CLASS_NAME + " On Connected");

            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }

            AppLogger.d(CLASS_NAME + " Session token " + manager.mMediaBrowser.getSessionToken());

            // Initialize Media Controller
            try {
                manager.mMediaController = new MediaControllerCompat(
                        manager.mActivity.getApplicationContext(),
                        manager.mMediaBrowser.getSessionToken()
                );
            } catch (final RemoteException e) {
                FabricUtils.logException(e);
                return;
            }

            // Initialize Transport Controls
            manager.mTransportControls = manager.mMediaController.getTransportControls();
            // Register callbacks
            manager.mMediaController.registerCallback(manager.mMediaSessionCallback);

            // Set actual media controller
            MediaControllerCompat.setMediaController(manager.mActivity, manager.mMediaController);

            // Update queue
            final List<MediaSessionCompat.QueueItem> queue = manager.mMediaController.getQueue();

            manager.mListener.onConnected(queue);
        }

        @Override
        public void onConnectionSuspended() {
            AppLogger.w(CLASS_NAME + " On Connection Suspended");
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            manager.mMediaController.unregisterCallback(manager.mMediaSessionCallback);
            manager.mTransportControls = null;
            manager.mMediaController = null;
            MediaControllerCompat.setMediaController(manager.mActivity, null);
        }

        @Override
        public void onConnectionFailed() {
            AppLogger.w(CLASS_NAME + " On Connection Failed");
        }
    }

    /**
     * Receive callbacks from the {@link MediaControllerCompat}.<br>
     * Here we update our state such as which queue is being shown,
     * the current title and description and the {@link PlaybackStateCompat}.
     */
    private static final class MediaSessionCallback extends MediaControllerCompat.Callback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MediaResourcesManager> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaSessionCallback(final MediaResourcesManager reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.d(CLASS_NAME + " Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + " Received playback state change to state " + state);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            manager.mListener.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + " On Queue Changed: " + queue);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            manager.mListener.onQueueChanged(queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            AppLogger.d(CLASS_NAME + " On Metadata Changed: " + metadata);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            final List<MediaSessionCompat.QueueItem> queue = manager.mMediaController.getQueue();
            manager.mListener.onMetadataChanged(metadata, queue);
        }
    }
}
