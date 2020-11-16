/*
 * Copyright 2019-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.presenter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.shared.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.shared.model.media.MediaResourcesManager;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public final class MediaPresenter {

    private static final String CLASS_NAME = MediaPresenter.class.getSimpleName();
    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";
    private static final String BUNDLE_ARG_LIST_CLICKED_ID = "BUNDLE_ARG_LIST_CLICKED_ID";
    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private final MediaResourcesManager mMediaRsrMgr;
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
    private int mListFirstVisiblePosition = 0;
    private int mListLastVisiblePosition = 0;
    private int mListSavedClickedPosition = MediaSessionCompat.QueueItem.UNKNOWN_ID;
    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentParentId = "";
    @Nullable
    private MediaBrowserCompat.SubscriptionCallback mCallback;
    private Activity mActivity;
    private MediaPresenterListener mListener;
    private RecyclerView mListView;
    private final RecyclerView.OnScrollListener mScrollListener;
    /**
     * Adapter for the representing media items in the list.
     */
    private MediaItemsAdapter mAdapter;

    @Inject
    public MediaPresenter(@ApplicationContext final Context context) {
        super();
        mScrollListener = new ScrollListener();
        mMediaRsrMgr = new MediaResourcesManager(context, getClass().getSimpleName());
    }

    public void init(final Activity activity, final Bundle bundle, @NonNull final RecyclerView listView,
                     @NonNull final MediaItemsAdapter adapter, final MediaItemsAdapter.Listener itemAdapterListener,
                     @NonNull final MediaBrowserCompat.SubscriptionCallback mediaSubscriptionCallback,
                     final MediaPresenterListener listener) {
        AppLogger.d(CLASS_NAME + " init");
        mCallback = mediaSubscriptionCallback;
        mActivity = activity;
        mListener = listener;
        mListView = listView;
        mAdapter = adapter;
        // Listener of events provided by Media Resource Manager.
        final MediaResourceManagerListener mediaRsrMgrLst = new MediaResourceManagerListenerImpl();
        mMediaRsrMgr.init(activity, bundle, mediaRsrMgrLst);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        mListView.setLayoutManager(layoutManager);
        // Set adapter
        mListView.setAdapter(mAdapter);
        mListView.addOnScrollListener(mScrollListener);

        mAdapter.setListener(itemAdapterListener);

        if (!mMediaItemsStack.isEmpty()) {
            final String mediaId = mMediaItemsStack.get(mMediaItemsStack.size() - 1);
            AppLogger.d(CLASS_NAME + " current media id:" + mediaId);
            unsubscribeFromItem(mediaId);
            addMediaItemToStack(mediaId);
        }
    }

    public void clean() {
        AppLogger.d(CLASS_NAME + " clean");
        mMediaRsrMgr.clean();
        mCallback = null;
        mActivity = null;
        mListener = null;
        mAdapter.clear();
        mAdapter.removeListener();
    }

    public void destroy() {
        AppLogger.d(CLASS_NAME + " destroy");
        if (mListView != null) {
            mListView.removeOnScrollListener(mScrollListener);
        }
        // Disconnect Media Browser
        mMediaRsrMgr.disconnect();
    }

    public boolean handleBackPressed(final Context context) {
        AppLogger.d(CLASS_NAME + " back pressed start:" + mMediaItemsStack.size());

        // If there is root category - close activity
        if (mMediaItemsStack.size() == 1) {

            // Un-subscribe from item
            mMediaRsrMgr.unsubscribe(mMediaItemsStack.remove(mMediaItemsStack.size() - 1));
            // Clear stack
            mMediaItemsStack.clear();

            context.startService(OpenRadioService.makeStopServiceIntent(context));
            AppLogger.d(CLASS_NAME + " back pressed return true, stop service");
            return true;
        }

        int index = mMediaItemsStack.size() - 1;
        if (index >= 0) {
            // Get current media item and un-subscribe.
            final String currentMediaId = mMediaItemsStack.remove(index);
            mMediaRsrMgr.unsubscribe(currentMediaId);
        }

        // Un-subscribe from all items.
        for (final String mediaItemId : mMediaItemsStack) {
            mMediaRsrMgr.unsubscribe(mediaItemId);
        }

        // Subscribe to the previous item.
        index = mMediaItemsStack.size() - 1;
        if (index >= 0) {
            final String previousMediaId = mMediaItemsStack.get(index);
            if (!TextUtils.isEmpty(previousMediaId)) {
                if (mListener != null) {
                    mListener.showProgressBar();
                }
                mMediaRsrMgr.subscribe(previousMediaId, mCallback);
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
        mMediaRsrMgr.unsubscribe(mediaId);
    }

    public void addMediaItemToStack(final String mediaId) {
        if (mCallback == null) {
            AppLogger.e(CLASS_NAME + " add media id to stack, callback null");
            return;
        }
        if (TextUtils.isEmpty(mediaId)) {
            AppLogger.e(CLASS_NAME + " add empty media id to stack");
            return;
        }
        if (!mMediaItemsStack.contains(mediaId)) {
            mMediaItemsStack.add(mediaId);
        }
        if (mListener != null) {
            mListener.showProgressBar();
        }
        mMediaRsrMgr.subscribe(mediaId, mCallback);
    }

    /**
     * Sets the item on the provided index as active.
     *
     * @param position Position of the item in the list.
     */
    public void setActiveItem(final int position) {
        if (mListView == null) {
            return;
        }
        mAdapter.setActiveItemId(position);
        mAdapter.notifyDataSetChanged();
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

        if (!ConnectivityReceiver.checkConnectivityAndNotify(mActivity)) {
            return;
        }

        // Current selected media item
        if (item == null) {
            //TODO: Improve message
            SafeToast.showAnyThread(mActivity, mActivity.getString(R.string.can_not_play_station));
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
            //TODO: Improve!
            if (mPositions.containsKey(mediaItem)) {
                mPositions.get(mediaItem)[1] = position;
            } else {
                mPositions.put(mediaItem, new int[]{0, position});
            }
        }

        final String mediaId = item.getMediaId();

        // If it is browsable - then we navigate to the next category
        if (item.isBrowsable()) {
            addMediaItemToStack(mediaId);
        } else if (item.isPlayable()) {
            // Else - we play an item
            mMediaRsrMgr.playFromMediaId(mediaId);
        }
    }

    public void connect() {
        if (mMediaRsrMgr == null) {
            return;
        }
        mMediaRsrMgr.connect();
    }

    @NonNull
    public int[] getPositions(final String mediaItem) {
        // Restore clicked position for the Catalogue list.
        if (!TextUtils.isEmpty(mediaItem) && mPositions.containsKey(mediaItem)) {
            final int[] data = mPositions.get(mediaItem);
            if (data == null) {
                return createInitPositionEntry();
            }
            return data;
        }
        return createInitPositionEntry();
    }

    public void handleChildrenLoaded(@NonNull final String parentId,
                                     @NonNull final List<MediaBrowserCompat.MediaItem> children) {
        setCurrentParentId(parentId);

        // No need to go on if indexed list ended with last item.
        if (MediaItemHelper.isEndOfList(children)) {
            return;
        }

        mAdapter.setParentId(parentId);
        mAdapter.clearData();
        mAdapter.addAll(children);
        mAdapter.notifyDataSetChanged();

        restoreSelectedPosition();
    }

    public void setCurrentParentId(final String value) {
        mCurrentParentId = value;
    }

    public void restoreSelectedPosition() {
        int selectedPosition;
        int clickedPosition;
        if (mListFirstVisiblePosition != -1) {
            selectedPosition = mListFirstVisiblePosition;
            clickedPosition = mListSavedClickedPosition;
            mListFirstVisiblePosition = -1;
            mListSavedClickedPosition = -1;
        } else {
            // Restore positions for the Catalogue list.
            final int[] positions = getPositions(mCurrentParentId);
            clickedPosition = positions[1];
            selectedPosition = positions[0];
        }
        // This will make selected item highlighted.
        setActiveItem(clickedPosition);
        // This actually do scroll to the position.
        mListView.scrollToPosition(selectedPosition - 1);
    }

    public final void handleSaveInstanceState(@NonNull final Bundle outState) {
        OpenRadioService.putCurrentParentId(outState, mCurrentParentId);
        updateListVisiblePositions(mListView);
        // Save first visible ID of the List
        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, mListFirstVisiblePosition);
        outState.putInt(BUNDLE_ARG_LIST_CLICKED_ID, mAdapter.getActiveItemId());
    }

    public final void handleRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        mListFirstVisiblePosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        mListSavedClickedPosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_CLICKED_ID);
    }

    public void handleCurrentIndexOnQueueChanged(final String mediaId) {
        final int position = mAdapter.getIndexForMediaId(mediaId);
        if (position != -1) {
            setActiveItem(position);
        }
    }

    public String getCurrentParentId() {
        return mCurrentParentId;
    }

    private void handleMediaResourceManagerConnected() {
        final String mediaId = mMediaItemsStack.isEmpty()
                ? mMediaRsrMgr.getRoot()
                : mMediaItemsStack.get(mMediaItemsStack.size() - 1);
        addMediaItemToStack(mediaId);

        // Update metadata in case of UI started on and media service was already created and stream played.
        if (mListener != null) {
            mListener.handleMetadataChanged(mMediaRsrMgr.getMediaMetadata());
        }
    }

    private int[] createInitPositionEntry() {
        return new int[]{0, MediaSessionCompat.QueueItem.UNKNOWN_ID};
    }

    private void updateListVisiblePositions(@NonNull final RecyclerView recyclerView) {
        final LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
        if (layoutManager == null) {
            mListFirstVisiblePosition = 0;
            return;
        }
        mListFirstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        mListLastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
    }

    private void onScrolledToEnd() {
        if (MediaIdHelper.isMediaIdRefreshable(mCurrentParentId)) {
            unsubscribeFromItem(mCurrentParentId);
            addMediaItemToStack(mCurrentParentId);
        } else {
            AppLogger.w(CLASS_NAME + "Category " + mCurrentParentId + " is not refreshable");
        }
    }

    /**
     * Listener for the Media Resources related events.
     */
    private final class MediaResourceManagerListenerImpl implements MediaResourceManagerListener {

        /**
         * Default constructor.
         */
        private MediaResourceManagerListenerImpl() {
            super();
        }

        @Override
        public void onConnected() {
            AppLogger.i(CLASS_NAME + " Connected");
            MediaPresenter.this.handleMediaResourceManagerConnected();
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + " psc:" + state);
            final MediaPresenter activity = MediaPresenter.this;
            if (activity.mListener != null) {
                activity.mListener.handlePlaybackStateChanged(state);
            }
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + " qc:" + queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata,
                                      final List<MediaSessionCompat.QueueItem> queue) {
            if (metadata == null) {
                return;
            }
            final MediaPresenter activity = MediaPresenter.this;
            if (activity.mListener != null) {
                activity.mListener.handleMetadataChanged(metadata);
            }
        }
    }

    private final class ScrollListener extends RecyclerView.OnScrollListener {

        private ScrollListener() {
            super();
        }

        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                return;
            }
            MediaPresenter.this.updateListVisiblePositions(recyclerView);
            if (MediaPresenter.this.mListLastVisiblePosition == MediaPresenter.this.mAdapter.getItemCount() - 1) {
                MediaPresenter.this.onScrolledToEnd();
            }
        }
    }
}
