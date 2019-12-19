/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.model.media.MediaResourcesManager;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.view.SafeToast;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class MediaPresenter {

    private static final String CLASS_NAME = MediaPresenter.class.getSimpleName();

    /**
     * Key value for the Media Stack for the store Bundle.
     */
    private static final String BUNDLE_ARG_MEDIA_ITEMS_STACK = "BUNDLE_ARG_MEDIA_ITEMS_STACK";
    /**
     * Key value for the List-Position map for the store Bundle.
     */
    private static final String BUNDLE_ARG_LIST_POSITION_MAP = "BUNDLE_ARG_LIST_POSITION_MAP";

    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private MediaResourcesManager mMediaResourcesManager;
    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private final List<String> mMediaItemsStack = new LinkedList<>();
    /**
     * Map of the selected and clicked positions for lists of the media items.
     * Contract is - array of integer has 2 elements {selected position, clicked position}.
     */
    private final Map<String, int[]> mPositions = new Hashtable<>();

    @Nullable
    private MediaBrowserCompat.SubscriptionCallback mCallback;
    private Activity mActivity;
    private MediaPresenterListener mListener;

    public MediaPresenter() {
        super();
    }

    public void init(final Activity activity,
                     final Bundle savedInstanceState,
                     @NonNull
                     final MediaBrowserCompat.SubscriptionCallback mediaSubscriptionCallback,
                     final MediaPresenterListener listener) {
        AppLogger.d(CLASS_NAME + " init");
        mCallback = mediaSubscriptionCallback;
        mActivity = activity;
        mListener = listener;
        mMediaResourcesManager = new MediaResourcesManager(
                mActivity,
                new MediaResourceManagerListenerImpl(this)
        );
        mMediaResourcesManager.create(savedInstanceState);
    }

    public void destroy() {
        AppLogger.d(CLASS_NAME + " destroy");
        // Disconnect Media Browser
        mMediaResourcesManager.disconnect();
        mCallback = null;
        mActivity = null;
        mListener = null;
    }

    public int getNumItemsInStack() {
        return mMediaItemsStack.size();
    }

    public boolean handleBackPressed(final Activity activity) {
        AppLogger.d(CLASS_NAME + " back pressed start:" + mMediaItemsStack.size());

        // If there is root category - close activity
        if (mMediaItemsStack.size() == 1) {

            // Un-subscribe from item
            mMediaResourcesManager.unsubscribe(mMediaItemsStack.remove(mMediaItemsStack.size() - 1));
            // Clear stack
            mMediaItemsStack.clear();

            activity.startService(OpenRadioService.makeStopServiceIntent(activity.getApplicationContext()));
            AppLogger.d(CLASS_NAME + " back pressed return true, stop service");
            return true;
        }

        int index = mMediaItemsStack.size() - 1;
        if (index >= 0) {
            // Get current media item and un-subscribe.
            final String currentMediaId = mMediaItemsStack.remove(index);
            mMediaResourcesManager.unsubscribe(currentMediaId);
        }

        // Un-subscribe from all items.
        for (final String mediaItemId : mMediaItemsStack) {
            mMediaResourcesManager.unsubscribe(mediaItemId);
        }

        // Subscribe to the previous item.
        index = mMediaItemsStack.size() - 1;
        if (index >= 0) {
            final String previousMediaId = mMediaItemsStack.get(index);
            if (!TextUtils.isEmpty(previousMediaId)) {
                if (mListener != null) {
                    mListener.showProgressBar();
                }
                mMediaResourcesManager.subscribe(previousMediaId, mCallback);
            }
        } else {
            AppLogger.d(CLASS_NAME + " back pressed return true");
            return true;
        }
        AppLogger.d(CLASS_NAME + " back pressed end:" + mMediaItemsStack.size());
        return false;
    }

    public void unsubscribeFromItem(final String mediaId) {
        // Remove provided media item (and it's duplicates, if any)
        for (int i = 0; i < mMediaItemsStack.size(); i++) {
            if (mMediaItemsStack.get(i).equals(mediaId)) {
                mMediaItemsStack.remove(i);
                i--;
            }
        }

        // Un-subscribe from item
        mMediaResourcesManager.unsubscribe(mediaId);
    }

    public void addMediaItemToStack(final String mediaId) {
        AppLogger.i(CLASS_NAME + " MediaItem Id added:" + mediaId);
        if (TextUtils.isEmpty(mediaId)) {
            return;
        }

        if (!mMediaItemsStack.contains(mediaId)) {
            mMediaItemsStack.add(mediaId);
        }
        if (mListener != null) {
            mListener.showProgressBar();
        }
        mMediaResourcesManager.subscribe(mediaId, mCallback);
    }

    public void restoreState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        // Restore map of the List - Position values
        final Map<String, int[]> listPositionMapRestored
                = (Map<String, int[]>) savedInstanceState.getSerializable(BUNDLE_ARG_LIST_POSITION_MAP);
        if (listPositionMapRestored != null) {
            mPositions.clear();
            for (String key : listPositionMapRestored.keySet()) {
                mPositions.put(key, listPositionMapRestored.get(key));
            }
        }

        // Restore Media Items stack
        final List<String> mediaItemsStackRestored
                = (List<String>) savedInstanceState.getSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK);
        if (mediaItemsStackRestored != null) {
            mMediaItemsStack.clear();
            mMediaItemsStack.addAll(mediaItemsStackRestored);
        }
    }

    public void saveState(final Bundle outState) {
        // Save Media Stack
        outState.putSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK, (Serializable) mMediaItemsStack);
        // Save List-Position Map
        outState.putSerializable(BUNDLE_ARG_LIST_POSITION_MAP, (Serializable) mPositions);
    }

    public void handleItemSelect(final MediaBrowserCompat.MediaItem item, final int position) {
        // Keep last selected position for the given category.
        // We will use it when back to this category
        final int size = mMediaItemsStack.size();
        if (size >= 1) {
            final String mediaItem = mMediaItemsStack.get(size - 1);
            int[] data = mPositions.remove(mediaItem);
            if (data == null) {
                data = createInitPositionEntry();
            }
            data[0] = position;
            mPositions.put(mediaItem, data);
        }
    }

    public void handleItemClick(final MediaBrowserCompat.MediaItem item, final int position) {
        if (mActivity == null) {
            return;
        }

        if (!ConnectivityReceiver.checkConnectivityAndNotify(mActivity.getApplicationContext())) {
            return;
        }

        // Current selected media item
        if (item == null) {
            //TODO: Improve message
            SafeToast.showAnyThread(
                    mActivity.getApplicationContext(), mActivity.getString(R.string.can_not_play_station)
            );
            return;
        }
        if (item.isBrowsable()) {
            if (item.getDescription().getTitle() != null
                    && item.getDescription().getTitle().equals(mActivity.getString(R.string.category_empty))) {
                return;
            }
        }

        // Keep last selected position for the given category.
        // We will use it when back to this category
        final int size = mMediaItemsStack.size();
        if (size >= 1) {
            final String mediaItem = mMediaItemsStack.get(size - 1);
            mPositions.put(mediaItem, new int[]{0, position});
        }

        final String mediaId = item.getMediaId();

        // If it is browsable - then we navigate to the next category
        if (item.isBrowsable()) {
            addMediaItemToStack(mediaId);
        } else if (item.isPlayable()) {
            // Else - we play an item
            final MediaControllerCompat controller = MediaControllerCompat.getMediaController(mActivity);
            if (controller != null) {
                final MediaControllerCompat.TransportControls controls = controller.getTransportControls();
                if (controls != null) {
                    controls.playFromMediaId(mediaId, null);
                }
            }
        }
    }

    public void update() {
        mMediaResourcesManager.disconnect();
        mMediaResourcesManager.connect();
    }

    public void connect() {
        if (mMediaResourcesManager == null) {
            return;
        }
        mMediaResourcesManager.connect();
    }

    @NonNull
    public int[] getPositions(final String mediaItem) {
        // Restore clicked position for the Catalogue list.
        if (!TextUtils.isEmpty(mediaItem)
                && mPositions.containsKey(mediaItem)) {
            final int[] data = mPositions.get(mediaItem);
            if (data == null) {
                return createInitPositionEntry();
            }
            return data;
        }
        return createInitPositionEntry();
    }

    /**
     * Listener for the Media Resources related events.
     */
    private static final class MediaResourceManagerListenerImpl implements MediaResourceManagerListener {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MediaPresenter> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaResourceManagerListenerImpl(final MediaPresenter reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(final List<MediaSessionCompat.QueueItem> queue) {
            final MediaPresenter activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onConnected reference to MainActivity is null");
                return;
            }

            AppLogger.i(CLASS_NAME + "Stack empty:" + activity.mMediaItemsStack.isEmpty());

            // If stack is empty - assume that this is a start point
            if (activity.mMediaItemsStack.isEmpty()) {
                activity.addMediaItemToStack(activity.mMediaResourcesManager.getRoot());
            }

            if (activity.mListener != null) {
                activity.mListener.showProgressBar();
            }
            // Subscribe to the media item
            activity.mMediaResourcesManager.subscribe(
                    activity.mMediaItemsStack.get(activity.mMediaItemsStack.size() - 1),
                    activity.mCallback
            );

            // Update metadata in case of UI started on and media service was already created and stream played.
            if (activity.mListener != null) {
                activity.mListener.handleMetadataChanged(activity.mMediaResourcesManager.getMediaMetadata());
            }
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + "PlaybackStateChanged:" + state);
            final MediaPresenter activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onPlaybackStateChanged reference to MainActivity is null");
                return;
            }
            if (activity.mListener != null) {
                activity.mListener.handlePlaybackStateChanged(state);
            }
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + "Queue changed to:" + queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata,
                                      final List<MediaSessionCompat.QueueItem> queue) {
            final MediaPresenter activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onMetadataChanged reference to MainActivity is null");
                return;
            }
            if (metadata == null) {
                return;
            }
            if (activity.mListener != null) {
                activity.mListener.handleMetadataChanged(metadata);
            }
        }
    }

    private int[] createInitPositionEntry() {
        return new int[]{0, MediaSessionCompat.QueueItem.UNKNOWN_ID};
    }
}
