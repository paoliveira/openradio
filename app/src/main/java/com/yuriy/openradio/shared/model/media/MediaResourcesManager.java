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
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.shared.utils.AppLogger;

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
    private final MediaBrowserCompat mMediaBrowser;

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
    private Activity mActivity;

    /**
     * Listener for the media resources events. Acts as proxy between this manager and callee Activity.
     */
    private MediaResourceManagerListener mListener;

    private final Set<String> mSubscribed;

    /**
     * Constructor.
     *
     */
    public MediaResourcesManager(@NonNull final Context context,
                                 @NonNull final String className) {
        super();
        CLASS_NAME = "MdRsrcsMgr " + className + " ";
        mMediaSessionCallback = new MediaSessionCallback();
        mSubscribed = new HashSet<>();
        // Initialize Media Browser
        final MediaBrowserCompat.ConnectionCallback callback = new MediaBrowserConnectionCallback();
        mMediaBrowser = new MediaBrowserCompat(
                context,
                new ComponentName(context, OpenRadioService.class),
                callback,
                null
        );
    }

    /**
     * Creates Media Browser, assigns listener.
     */
    public void init(@NonNull final Activity activity,
                     Bundle bundle,
                     @NonNull final MediaResourceManagerListener listener) {
        mActivity = activity;
        mListener = listener;
        //TODO: Simple solution that needs to be revised.
        OpenRadioService.mCurrentParentId = OpenRadioService.getCurrentParentId(bundle);
        OpenRadioService.mIsRestoreState = OpenRadioService.getRestoreState(bundle);
        OpenRadioService.mState = OpenRadioService.getCurrentPlaybackState(bundle);
    }

    /**
     * Connects to the Media Browse service.
     */
    public void connect() {
        if (mMediaBrowser.isConnected()) {
            AppLogger.w(CLASS_NAME + "Connect aborted, already connected");
            // Register callbacks
            mMediaController.registerCallback(mMediaSessionCallback);
            // Set actual media controller
            MediaControllerCompat.setMediaController(mActivity, mMediaController);
            return;
        }
        try {
            mMediaBrowser.connect();
            AppLogger.i(CLASS_NAME + "Connected");
        } catch (final IllegalStateException e) {
            AppLogger.e(CLASS_NAME + "Can not connect:" + e);
        }
    }

    /**
     * Disconnects from the Media Browse service. After this, no more callbacks will be received.
     */
    public void disconnect() {
        if (!mMediaBrowser.isConnected()) {
            AppLogger.w(CLASS_NAME + "Disconnect aborted, already disconnected");
            return;
        }
        mMediaBrowser.disconnect();
        AppLogger.i(CLASS_NAME + "Disconnected");
    }

    public void clean() {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaSessionCallback);
        }
        if (mActivity != null) {
            MediaControllerCompat.setMediaController(mActivity, null);
        }
        mActivity = null;
        mListener = null;
    }

    /**
     * Queries for information about the media items that are contained within the specified id and subscribes to
     * receive updates when they change.
     *
     * @param parentId The id of the parent media item whose list of children will be subscribed.
     * @param callback The callback to receive the list of children.
     */
    public void subscribe(final @NonNull String parentId,
                          final @Nullable MediaBrowserCompat.SubscriptionCallback callback) {
        AppLogger.i(CLASS_NAME + "Subscribe:" + parentId);
        if (callback == null) {
            AppLogger.e(CLASS_NAME + " subscribe listener is null");
            return;
        }
        if (mSubscribed.contains(parentId)) {
            AppLogger.w(CLASS_NAME + "already subscribed");
            return;
        }
        mSubscribed.add(parentId);
        mMediaBrowser.subscribe(parentId, callback);
    }

    /**
     * Unsubscribe for changes to the children of the specified media id.
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

    /**
     *
     * @return
     */
    public MediaMetadataCompat getMediaMetadata() {
        return mMediaController != null ? mMediaController.getMetadata() : null;
    }

    /**
     *
     * @param mediaId
     */
    public void playFromMediaId(final String mediaId) {
        if (mTransportControls != null) {
            mTransportControls.playFromMediaId(mediaId, null);
        }
    }

    private static Bundle createRootHints(final Bundle savedInstance) {
        final Bundle bundle = new Bundle();
        OpenRadioService.putCurrentParentId(bundle, OpenRadioService.getCurrentParentId(savedInstance));
        OpenRadioService.putCurrentPlaybackState(bundle, OpenRadioService.getCurrentPlaybackState(savedInstance));
        OpenRadioService.putRestoreState(bundle, OpenRadioService.getRestoreState(savedInstance));
        return bundle;
    }
    
    private void handleMediaBrowserConnected() {
        AppLogger.d(CLASS_NAME + "Session token " + mMediaBrowser.getSessionToken());
        if (mActivity == null) {
            AppLogger.e(CLASS_NAME + " media browser connected when context is null, disconnect");
            disconnect();
            return;
        }

        // Initialize Media Controller
        try {
            mMediaController = new MediaControllerCompat(
                    mActivity,
                    mMediaBrowser.getSessionToken()
            );
        } catch (final RemoteException e) {
            AnalyticsUtils.logException(e);
            return;
        }

        // Initialize Transport Controls
        mTransportControls = mMediaController.getTransportControls();
        // Register callbacks
        mMediaController.registerCallback(mMediaSessionCallback);

        // Set actual media controller
        MediaControllerCompat.setMediaController(mActivity, mMediaController);

        if (mListener != null) {
            mListener.onConnected();
        } else {
            AppLogger.e(CLASS_NAME + " handle media browser connected, listener is null");
        }
    }

    /**
     * Callback object for the Media Browser connection events.
     */
    private final class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        /**
         * Default constructor.
         */
        private MediaBrowserConnectionCallback() {
            super();
        }

        @Override
        public void onConnected() {
            AppLogger.i(CLASS_NAME + "Connected");
            MediaResourcesManager.this.handleMediaBrowserConnected();
        }

        @Override
        public void onConnectionSuspended() {
            AppLogger.w(CLASS_NAME + "Connection Suspended");
            final MediaResourcesManager manager = MediaResourcesManager.this;
            manager.mMediaController.unregisterCallback(manager.mMediaSessionCallback);
            manager.mTransportControls = null;
            manager.mMediaController = null;
            MediaControllerCompat.setMediaController(manager.mActivity, null);
        }

        @Override
        public void onConnectionFailed() {
            AppLogger.e(CLASS_NAME + "Connection Failed");
        }
    }

    /**
     * Receive callbacks from the {@link MediaControllerCompat}.<br>
     * Here we update our state such as which queue is being shown,
     * the current title and description and the {@link PlaybackStateCompat}.
     */
    private final class MediaSessionCallback extends MediaControllerCompat.Callback {

        /**
         * Default constructor.
         */
        private MediaSessionCallback() {
            super();
        }

        @Override
        public void onSessionDestroyed() {
            AppLogger.i(CLASS_NAME + "Session destroyed. Need to fetch a new Media Session");
        }

        @Override
        public void onPlaybackStateChanged(final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + "PlaybackStateChanged:" + state);
            if (MediaResourcesManager.this.mListener == null) {
                AppLogger.e(CLASS_NAME + "PlaybackStateChanged listener null");
                return;
            }
            MediaResourcesManager.this.mListener.onPlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + "Queue changed:" + queue);
            if (MediaResourcesManager.this.mListener == null) {
                AppLogger.e(CLASS_NAME + "Queue changed listener null");
                return;
            }
            MediaResourcesManager.this.mListener.onQueueChanged(queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata) {
            AppLogger.d(CLASS_NAME + "Metadata changed:" + metadata);
            if (MediaResourcesManager.this.mListener == null) {
                AppLogger.e(CLASS_NAME + "Metadata changed listener null");
                return;
            }
            if (MediaResourcesManager.this.mMediaController == null) {
                AppLogger.e(CLASS_NAME + "Metadata changed media controller null");
                return;
            }
            MediaResourcesManager.this.mListener.onMetadataChanged(
                    metadata, MediaResourcesManager.this.mMediaController.getQueue()
            );
        }
    }
}
