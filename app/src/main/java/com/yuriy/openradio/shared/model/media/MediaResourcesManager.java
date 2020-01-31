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

package com.yuriy.openradio.shared.model.media;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final String CLASS_NAME;

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
    private final MediaControllerCompat.Callback mMediaSessionCallback;

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

    private Set<String> mSubscribed;

    /**
     * Constructor.
     *
     * @param activity Callee {@link Activity}.
     * @param listener Listener for the media resources events.
     */
    public MediaResourcesManager(@NonNull final Activity activity,
                                 @NonNull final MediaResourceManagerListener listener) {
        super();
        CLASS_NAME = "MdRsrcsMgr " + activity.getClass().getSimpleName() + " " + activity.hashCode() + " ";
        mMediaSessionCallback = new MediaSessionCallback(this);
        mSubscribed = new HashSet<>();
        mActivity = activity;
        mListener = listener;
    }

    /**
     * Creates Media Browser, assigns listener.
     */
    public void create(final Bundle savedInstance) {
        // Initialize Media Browser
        mMediaBrowser = new MediaBrowserCompat(
                mActivity.getApplicationContext(),
                new ComponentName(mActivity.getApplicationContext(), OpenRadioService.class),
                new MediaBrowserConnectionCallback(this),
                savedInstance != null ? createRootHints(savedInstance) : null
        );
    }

    /**
     * Connects to the Media Browse service.
     */
    public void connect() {
        AppLogger.i(CLASS_NAME + "Connect");
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    /**
     * Disconnects from the Media Browse service. After this, no more callbacks will be received.
     */
    public void disconnect() {
        AppLogger.i(CLASS_NAME + "Disconnect");
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
    public void subscribe(final @NonNull String parentId,
                          final @NonNull MediaBrowserCompat.SubscriptionCallback callback) {
        if (mSubscribed.contains(parentId)) {
            return;
        }
        AppLogger.i(CLASS_NAME + "Subscribe:" + parentId);
        if (mMediaBrowser != null) {
            mSubscribed.add(parentId);
            mMediaBrowser.subscribe(parentId, callback);
        }
    }

    /**
     * Unsubscribes for changes to the children of the specified media id.
     *
     * @param parentId The id of the parent media item whose list of children will be unsubscribed.
     */
    public void unsubscribe(@NonNull String parentId) {
        if (!mSubscribed.contains(parentId)) {
            return;
        }
        AppLogger.i(CLASS_NAME + "Unsubscribe:" + parentId + ", " + mMediaBrowser);
        if (mMediaBrowser != null) {
            mSubscribed.remove(parentId);
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

    private static Bundle createRootHints(final Bundle savedInstance) {
        final Bundle bundle = new Bundle();
        OpenRadioService.putCurrentParentId(bundle, OpenRadioService.getCurrentParentId(savedInstance));
        OpenRadioService.putCurrentPlaybackState(bundle, OpenRadioService.getCurrentPlaybackState(savedInstance));
        return bundle;
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private static final class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        /**
         * Weak reference to the outer manager.
         */
        private final WeakReference<MediaResourcesManager> mReference;

        private final String CLASS_NAME;

        /**
         * Constructor.
         *
         * @param reference Reference to the manager.
         */
        private MediaBrowserConnectionCallback(final MediaResourcesManager reference) {
            super();
            CLASS_NAME = reference.CLASS_NAME;
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected() {
            AppLogger.d(CLASS_NAME + "Connected");

            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                AppLogger.w(CLASS_NAME + "Enclosing reference is null");
                return;
            }

            AppLogger.d(CLASS_NAME + "Session token " + manager.mMediaBrowser.getSessionToken());

            // Initialize Media Controller
            try {
                manager.mMediaController = new MediaControllerCompat(
                        manager.mActivity.getApplicationContext(),
                        manager.mMediaBrowser.getSessionToken()
                );
            } catch (final RemoteException e) {
                AnalyticsUtils.logException(e);
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
            AppLogger.w(CLASS_NAME + "Connection Suspended");
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
            AppLogger.w(CLASS_NAME + "Connection Failed");
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

        private final String CLASS_NAME;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaSessionCallback(final MediaResourcesManager reference) {
            super();
            CLASS_NAME = reference.CLASS_NAME;
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.d(CLASS_NAME + "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + "PlaybackStateChanged:" + state);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            if (state == null) {
                AppLogger.e(CLASS_NAME + "Received invalid playback state");
                return;
            }
            manager.mListener.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + "Queue Changed:" + queue);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                AppLogger.w(CLASS_NAME + "Manager is null when queue changed");
                return;
            }
            manager.mListener.onQueueChanged(queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            AppLogger.d(CLASS_NAME + "Metadata Changed:" + metadata);
            final MediaResourcesManager manager = mReference.get();
            if (manager == null) {
                return;
            }
            final List<MediaSessionCompat.QueueItem> queue = manager.mMediaController.getQueue();
            manager.mListener.onMetadataChanged(metadata, queue);
        }
    }

    public void dump() {
        AppLogger.i(
                CLASS_NAME
                        + "Dump queue:" + (mMediaController.getQueue() != null ? mMediaController.getQueue().size() : "null")
        );
    }
}
