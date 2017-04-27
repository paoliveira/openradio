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

package com.yuriy.openradio.view;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.AppPreferencesManager;
import com.yuriy.openradio.business.PermissionStatusListener;
import com.yuriy.openradio.service.AppLocalBroadcastReceiver;
import com.yuriy.openradio.service.AppLocalBroadcastReceiverCallback;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.CrashlyticsUtils;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.ImageFetcherFactory;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.PermissionChecker;
import com.yuriy.openradio.utils.Utils;
import com.yuriy.openradio.view.list.MediaItemsAdapter;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 *
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
public final class MainActivity extends AppCompatActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    /**
     * Browses media content offered by a link MediaBrowserService.
     * This object is not thread-safe.
     * All calls should happen on the thread on which the browser was constructed.
     */
    private MediaBrowserCompat mMediaBrowser;

    /**
     * Adapter for the representing media items in the list.
     */
    private MediaItemsAdapter mBrowserAdapter;

    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private final List<String> mediaItemsStack = new LinkedList<>();

    /**
     * Map of the last used list position for the given list of the media items.
     */
    private final Map<String, Integer> listPositionMap = new Hashtable<>();

    /**
     * Key value for the Media Stack for the store Bundle.
     */
    private static final String BUNDLE_ARG_MEDIA_ITEMS_STACK = "BUNDLE_ARG_MEDIA_ITEMS_STACK";

    /**
     * Key value for the List-Position map for the store Bundle.
     */
    private static final String BUNDLE_ARG_LIST_POSITION_MAP = "BUNDLE_ARG_LIST_POSITION_MAP";

    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";

    /**
     * Progress Bar view to indicate that data is loading.
     */
    private ProgressBar mProgressBar;

    /**
     * Text View to display that data has not been loaded.
     */
    private TextView mNoDataView;

    /**
     * Receiver for the local application;s events
     */
    private final AppLocalBroadcastReceiver mAppLocalBroadcastReceiver
            = AppLocalBroadcastReceiver.getInstance();

    /**
     * Listener of the Permissions status changes.
     */
    private final PermissionStatusListener mPermissionStatusListener = new PermissionListener(this);

    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private final LocalBroadcastReceiverCallback mLocalBroadcastReceiverCallback
            = new LocalBroadcastReceiverCallback(this);

    /**
     * Listener for the Media Browser Subscription callback
     */
    private final MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback
            = new MediaBrowserSubscriptionCallback(this);

    /**
     *
     */
    private String mCurrentParentId = "";

    /**
     *
     */
    private String mCurrentMediaId = "";

    /**
     * Listener for the List view click event.
     */
    private final AdapterView.OnItemClickListener mOnItemClickListener
            = new OnItemClickListener(this);

    /**
     * Listener for the List view long click event.
     */
    private final AdapterView.OnItemLongClickListener mOnItemLongClickListener
            = new OnItemLongClickListener(this);

    private volatile AtomicBoolean mIsOnSaveInstancePassed = new AtomicBoolean(false);

    private boolean mSortable = false;
    public MediaBrowserCompat.MediaItem mDragMediaItem;
    private int mDropPosition = -1;
    private int mStartDragPosition = -1;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content.
        setContentView(R.layout.activity_main);

        // Register local receivers.
        registerReceivers();

        // Add listener for the permissions status
        PermissionChecker.addPermissionStatusListener(mPermissionStatusListener);

        // Handles loading the  image in a background thread
        final ImageFetcher imageFetcher = ImageFetcherFactory.getSmallImageFetcher(this);

        // Instantiate adapter
        mBrowserAdapter = new MediaItemsAdapter(this, imageFetcher);

        // Initialize progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_view);

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        hideProgressBar();

        // Initialize No Data text view
        mNoDataView = (TextView) findViewById(R.id.no_data_view);

        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);
        // Set adapter
        listView.setAdapter(mBrowserAdapter);
        // Set click listener
        listView.setOnItemClickListener(mOnItemClickListener);
        // Set long click listener.
        // Return true in order to prevent click event been invoked.
        listView.setOnItemLongClickListener(mOnItemLongClickListener);
        //
        listView.setOnTouchListener(
                (v, event) -> {
                    if (!mSortable) {
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            final int position = listView.pointToPosition(
                                    (int) event.getX(), (int) event.getY()
                            );
                            if (position < 0) {
                                break;
                            }
                            if (position != mDropPosition) {
                                mDropPosition = position;
                                mBrowserAdapter.remove(mDragMediaItem);
                                mBrowserAdapter.addAt(mDropPosition, mDragMediaItem);
                                mBrowserAdapter.notifyDataSetChanged();
                            }
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_OUTSIDE: {
                            stopDrag();
                            return true;
                        }
                    }
                    return false;
                }
        );

        final FloatingActionButton addBtn = (FloatingActionButton) findViewById(R.id.add_station_btn);
        addBtn.setOnClickListener(
                view -> {
                    // Show Add Station Dialog
                    final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    final DialogFragment dialog = AddStationDialog.newInstance();
                    dialog.show(transaction, AddStationDialog.DIALOG_TAG);
                }
        );

        // Instantiate media browser
        mMediaBrowser = new MediaBrowserCompat(
                this,
                new ComponentName(this, OpenRadioService.class),
                new MediaBrowserConnectionCallback(this), null
        );

        restoreState(savedInstanceState);

        mMediaBrowser.connect();
    }

    @Override
    protected final void onResume() {
        super.onResume();

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        // Hide any progress bar
        hideProgressBar();

        // Restore position for the Catalogue list.
        if (!TextUtils.isEmpty(mCurrentParentId)
                && listPositionMap.containsKey(mCurrentParentId)) {
            final int position = listPositionMap.get(mCurrentParentId);
            setSelectedItem(position);
        }
        // Restore position for the Catalogue of the Playable items
        if (!TextUtils.isEmpty(mCurrentMediaId)) {
            final int position = mBrowserAdapter.getIndexForMediaId(mCurrentMediaId);
            if (position != -1) {
                setSelectedItem(position);
            }
        }
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();

        PermissionChecker.removePermissionStatusListener(mPermissionStatusListener);

        // Unregister local receivers
        unregisterReceivers();
        // Disconnect Media Browser
        mMediaBrowser.disconnect();
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        final Fragment fragmentByTag = getFragmentManager().findFragmentByTag(AboutDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentTransaction.addToBackStack(null);

        if (id == R.id.action_about) {

            // Show About Dialog
            final DialogFragment aboutDialog = AboutDialog.newInstance();
            aboutDialog.show(fragmentTransaction, AboutDialog.DIALOG_TAG);
            return true;
        } else if (id == R.id.action_search) {

            // Show Search Dialog
            final DialogFragment searchDialog = SearchDialog.newInstance();
            searchDialog.show(fragmentTransaction, SearchDialog.DIALOG_TAG);
            return true;
        } else if (id == R.id.action_settings) {

            // Show Search Dialog
            final DialogFragment settingsDialog = SettingsDialog.newInstance();
            settingsDialog.show(fragmentTransaction, SettingsDialog.DIALOG_TAG);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected final void onSaveInstanceState(final Bundle outState) {
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true);

        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);

        // Save Media Stack
        outState.putSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK, (Serializable) mediaItemsStack);

        // Save List-Position Map
        outState.putSerializable(BUNDLE_ARG_LIST_POSITION_MAP, (Serializable) listPositionMap);

        // Get first visible item id
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        // Just in case ...
        if (firstVisiblePosition < 0) {
            firstVisiblePosition = 0;
        }

        // Save first visible ID of the List
        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, firstVisiblePosition);

        // Keep last selected position for the given category.
        // We will use it when back to this category. Only if collection is not empty.
        /*if (!mediaItemsStack.isEmpty()) {
            listPositionMap.put(
                    mediaItemsStack.get(mediaItemsStack.size() - 1), firstVisiblePosition
            );
        }*/

        super.onSaveInstanceState(outState);
    }

    @Override
    public final void onBackPressed() {

        hideNoDataMessage();
        hideProgressBar();

        // If there is root category - close activity
        if (mediaItemsStack.size() == 1) {

            // Un-subscribe from item
            mMediaBrowser.unsubscribe(mediaItemsStack.remove(mediaItemsStack.size() - 1));
            // Clear stack
            mediaItemsStack.clear();

            // perform android framework lifecycle
            super.onBackPressed();
            return;
        }

        int location = mediaItemsStack.size() - 1;
        if (location >= 0) {
            // Get current media item and un-subscribe.
            final String currentMediaId = mediaItemsStack.remove(location);
            mMediaBrowser.unsubscribe(currentMediaId);
        }

        // Un-subscribe from all items.
        for (final String mediaItemId : mediaItemsStack) {
            mMediaBrowser.unsubscribe(mediaItemId);
        }

        // Subscribe to the previous item.
        location = mediaItemsStack.size() - 1;
        if (location >= 0) {
            final String previousMediaId = mediaItemsStack.get(location);
            if (!TextUtils.isEmpty(previousMediaId)) {
                AppLogger.d("Back to " + previousMediaId);
                mMediaBrowser.subscribe(previousMediaId, mMedSubscriptionCallback);
            }
        }
    }

    /**
     * Start request location procedure. Despite the fact that whether user enable Location or not,
     * just request Location via Android API and return result via Broadcast event.
     */
    public final void processLocationCallback() {
        startService(OpenRadioService.makeRequestLocationIntent(this));
    }

    /**
     * Process user's input in order to generate custom {@link RadioStationVO}.
     */
    public final void processAddStationCallback(final String name, final String url,
                                                final String imageUrl, final String genre,
                                                final String country) {
        startService(OpenRadioService.makeAddRadioStationIntent(
                this, name, url, imageUrl, genre, country
        ));
    }

    /**
     * Process user's input in order to remove custom {@link RadioStationVO}.
     */
    public final void processRemoveStationCallback(final String mediaId) {
        startService(OpenRadioService.makeRemoveRadioStationIntent(this, mediaId));
    }

    /**
     * Process call back from the Search Dialog.
     *
     * @param queryString String to query for.
     */
    public void onSearchDialogClick(final String queryString) {
        // Un-subscribe from previous Search
        unsubscribeFromItem(MediaIDHelper.MEDIA_ID_SEARCH_FROM_APP);

        // Save search query string, retrieve it later in the service
        Utils.setSearchQuery(queryString);
        addMediaItemToStack(MediaIDHelper.MEDIA_ID_SEARCH_FROM_APP);
    }

    public void startDrag(final int position, final MediaBrowserCompat.MediaItem mediaItem) {
        mStartDragPosition = position;
        mDropPosition = -1;
        mSortable = true;
        mDragMediaItem = mediaItem;
        mBrowserAdapter.notifyDataSetChanged();
    }

    private void stopDrag() {
        if (mStartDragPosition == mBrowserAdapter.getActiveItemId()) {
            setSelectedItem(mDropPosition);
        }
        if (mDropPosition == mBrowserAdapter.getActiveItemId()) {
            setSelectedItem(mStartDragPosition);
        }
        mDropPosition = -1;
        mSortable = false;
        mDragMediaItem = null;
        mBrowserAdapter.notifyDataSetChanged();
    }

    /**
     * Remove provided Media Id from the collection. Reconnect {@link MediaBrowserCompat}.
     *
     * @param mediaItemId Media Id.
     */
    private void unsubscribeFromItem(final String mediaItemId) {
        hideNoDataMessage();
        hideProgressBar();

        // Remove provided media item (and it's duplicates, if any)
        for (int i = 0; i < mediaItemsStack.size(); i++) {
            if (mediaItemsStack.get(i).equals(mediaItemId)) {
                mediaItemsStack.remove(i);
                i--;
            }
        }

        // Un-subscribe from item
        mMediaBrowser.unsubscribe(mediaItemId);
    }

    /**
     * Add {@link android.media.browse.MediaBrowser.MediaItem} to stack.
     *
     * @param mediaId Id of the {@link android.view.MenuItem}
     */
    private void addMediaItemToStack(final String mediaId) {
        AppLogger.i(CLASS_NAME + " MediaItem Id added:" + mediaId);
        if (TextUtils.isEmpty(mediaId)) {
            return;
        }

        if (!mediaItemsStack.contains(mediaId)) {
            mediaItemsStack.add(mediaId);
        }

        mMediaBrowser.subscribe(mediaId, mMedSubscriptionCallback);
    }

    /**
     * Show progress bar.
     */
    private void showProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress bar.
     */
    private void hideProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * Show "No data" text view.
     */
    private void showNoDataMessage() {
        if (mNoDataView == null) {
            return;
        }
        mNoDataView.setVisibility(View.VISIBLE);
    }

    /**
     * Hide "No data" text view.
     */
    private void hideNoDataMessage() {
        if (mNoDataView == null) {
            return;
        }
        mNoDataView.setVisibility(View.GONE);
    }

    @SuppressWarnings("unchecked")
    private void restoreState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Nothing to restore
            return;
        }

        // Restore map of the List - Position values
        final Map<String, Integer> listPositionMapRestored
                = (Map<String, Integer>) savedInstanceState.getSerializable(BUNDLE_ARG_LIST_POSITION_MAP);
        if (listPositionMapRestored != null) {
            listPositionMap.clear();
            for (String key : listPositionMapRestored.keySet()) {
                listPositionMap.put(key, listPositionMapRestored.get(key));
            }
        }

        // Restore Media Items stack
        final List<String> mediaItemsStackRestored
                = (List<String>) savedInstanceState.getSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK);
        if (mediaItemsStackRestored != null) {
            mediaItemsStack.clear();
            mediaItemsStack.addAll(mediaItemsStackRestored);
        }

        // Restore List's position
        final int listFirstVisiblePosition
                = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        setSelectedItem(listFirstVisiblePosition);
    }

    /**
     * Sets the item on the provided index as selected.
     *
     * @param position Position of the item in the list.
     */
    private void setSelectedItem(final int position) {
        AppLogger.d(CLASS_NAME + " Set selected:" + position);
        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);
        if (listView == null) {
            return;
        }

        listView.setSelection(position);
        listView.smoothScrollToPositionFromTop(position, 0);

        mBrowserAdapter.notifyDataSetInvalidated();
        mBrowserAdapter.setActiveItemId(position);
        mBrowserAdapter.notifyDataSetChanged();
    }

    /**
     * Register receiver for the application's local events.
     */
    private void registerReceivers() {

        mAppLocalBroadcastReceiver.registerListener(mLocalBroadcastReceiverCallback);

        // Create filter and add actions
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppLocalBroadcastReceiver.getActionLocationDisabled());
        intentFilter.addAction(AppLocalBroadcastReceiver.getActionLocationCountryCode());
        intentFilter.addAction(AppLocalBroadcastReceiver.getActionCurrentIndexOnQueueChanged());
        // Register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mAppLocalBroadcastReceiver,
                intentFilter
        );
    }

    /**
     * Unregister receiver for the application's local events.
     */
    private void unregisterReceivers() {
        mAppLocalBroadcastReceiver.unregisterListener();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mAppLocalBroadcastReceiver);
    }

    /**
     * Callback receiver of the local application's event.
     */
    private static final class LocalBroadcastReceiverCallback implements AppLocalBroadcastReceiverCallback {

        private static final String CLASS_NAME = LocalBroadcastReceiverCallback.class.getSimpleName();
        /**
         * Member field to keep reference to the outer class.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor.
         *
         * @param reference The reference to the outer class.
         */
        private LocalBroadcastReceiverCallback(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onLocationDisabled() {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            if (AppPreferencesManager.isLocationDialogShown(reference.getApplicationContext())) {
                return;
            }

            if (reference.mIsOnSaveInstancePassed.get()) {
                AppLogger.w(CLASS_NAME + " Can not show Dialog after OnSaveInstanceState");
                return;
            }

            final BaseDialogFragment useLocationServiceDialog = BaseDialogFragment.newInstance(
                    UseLocationDialog.class.getName()
            );
            useLocationServiceDialog.setCancelable(false);
            useLocationServiceDialog.show(reference.getFragmentManager(), UseLocationDialog.DIALOG_TAG);

            AppPreferencesManager.setLocationDialogShown(reference.getApplicationContext(), true);
        }

        @Override
        public void onLocationCountryCode(final String countryCode) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            // Disconnect and connect back to media browser
            reference.mMediaBrowser.disconnect();
            reference.mMediaBrowser.connect();
        }

        @Override
        public void onCurrentIndexOnQueueChanged(final int index, final String mediaId) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            reference.mCurrentMediaId = mediaId;

            final int position = reference.mBrowserAdapter.getIndexForMediaId(mediaId);
            if (position != -1) {
                reference.setSelectedItem(position);
            }
        }
    }

    /**
     * Listener of the Permissions Status changes.
     */
    private static final class PermissionListener implements PermissionStatusListener {

        private final WeakReference<Context> mReference;
        private final Map<String, Double> mMap = new ConcurrentHashMap<>();
        private static final int DELTA = 2000;

        private PermissionListener(final Context reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onPermissionRequired(final String permissionName) {
            if (mReference.get() == null) {
                return;
            }

            final double currentTime = System.currentTimeMillis();

            if (mMap.containsKey(permissionName)) {
                if (currentTime - mMap.get(permissionName) < DELTA) {
                    return;
                }
            }

            mMap.put(permissionName, currentTime);

            mReference.get().startActivity(
                    PermissionsDialogActivity.getIntent(mReference.get(), permissionName)
            );
        }
    }

    /**
     * Callback object for the Media Browser connection events
     */
    private static final class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserConnectionCallback(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected() {
            AppLogger.d(CLASS_NAME + " On Connected");

            final MainActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            AppLogger.i(CLASS_NAME + " Stack empty:" + activity.mediaItemsStack.isEmpty());

            // If stack is empty - assume that this is a start point
            if (activity.mediaItemsStack.isEmpty()) {
                activity.addMediaItemToStack(activity.mMediaBrowser.getRoot());
            }

            // If session token is null - throw exception
            //if (mMediaBrowser.getSessionToken() == null) {
            //    throw new IllegalArgumentException("No Session token");
            //}

            // Subscribe to the media item
            activity.mMediaBrowser.subscribe(
                    activity.mediaItemsStack.get(activity.mediaItemsStack.size() - 1),
                    activity.mMedSubscriptionCallback
            );

            // (Re)-Initialize media controller
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(
                        activity.getApplicationContext(),
                        activity.mMediaBrowser.getSessionToken()
                );
            } catch (final RemoteException e) {
                AppLogger.e(
                        CLASS_NAME + " Can not init MediaController\n:" + Log.getStackTraceString(e)
                );
                CrashlyticsUtils.logException(e);
            }

            // Set actual controller
            if (mediaController != null) {
                activity.setSupportMediaController(mediaController);
            }
        }

        @Override
        public void onConnectionFailed() {
            AppLogger.w(CLASS_NAME + " On Connection Failed");
        }

        @Override
        public void onConnectionSuspended() {
            AppLogger.w(CLASS_NAME + " On Connection Suspended");
            final MainActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            activity.setSupportMediaController(null);
        }
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onChildrenLoaded(@NonNull final String parentId,
                                     @NonNull final List<MediaBrowserCompat.MediaItem> children) {
            AppLogger.i(CLASS_NAME + " On children loaded:" + parentId);

            final MainActivity activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + " On children loaded -> activity ref is null");
                return;
            }

            activity.mCurrentParentId = parentId;
            activity.hideProgressBar();

            final FloatingActionButton addBtn
                    = (FloatingActionButton) activity.findViewById(R.id.add_station_btn);
            if (parentId.equals(MediaIDHelper.MEDIA_ID_ROOT)) {
                addBtn.setVisibility(View.VISIBLE);
            } else {
                addBtn.setVisibility(View.GONE);
            }

            activity.mBrowserAdapter.clear();
            activity.mBrowserAdapter.notifyDataSetInvalidated();
            activity.mBrowserAdapter.addAll(children);
            activity.mBrowserAdapter.notifyDataSetChanged();

            if (children.isEmpty()) {
                activity.showNoDataMessage();
            }

            if (!activity.listPositionMap.containsKey(parentId)) {
                AppLogger.d(CLASS_NAME + " No key");
                activity.mBrowserAdapter.notifyDataSetInvalidated();
                activity.mBrowserAdapter.setActiveItemId(MediaSessionCompat.QueueItem.UNKNOWN_ID);
                activity.mBrowserAdapter.notifyDataSetInvalidated();
                return;
            }

            // Restore position for the Catalogue list
            int position = activity.listPositionMap.get(parentId);
            activity.setSelectedItem(position);
            // Restore position for the Catalogue of the Playable items
            if (!TextUtils.isEmpty(activity.mCurrentMediaId)) {
                position = activity.mBrowserAdapter.getIndexForMediaId(activity.mCurrentMediaId);
                if (position != -1) {
                    activity.setSelectedItem(position);
                }
            }
        }

        @Override
        public void onError(@NonNull final String id) {

            final MainActivity activity = mReference.get();
            if (activity == null) {
                return;
            }

            activity.hideProgressBar();

            Toast.makeText(
                    activity.getApplicationContext(),
                    R.string.error_loading_media, Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Listener of the List Item click event.
     */
    private static final class OnItemClickListener implements AdapterView.OnItemClickListener {

        /**
         * Weak reference to the Main Activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor.
         *
         * @param mainActivity Reference to the Main Activity.
         */
        private OnItemClickListener(final MainActivity mainActivity) {
            super();
            mReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void onItemClick(final AdapterView<?> parent, final View view, final int position,
                                final long id) {
            final MainActivity mainActivity = mReference.get();
            if (mainActivity == null) {
                AppLogger.w(CLASS_NAME + " OnItemClick return, reference to MainActivity is null");
                return;
            }
            // Current selected media item
            final MediaBrowserCompat.MediaItem item
                    = (MediaBrowserCompat.MediaItem) mainActivity.mBrowserAdapter.getItem(position);

            if (item.isBrowsable()) {
                if (item.getDescription().getTitle() != null
                        && item.getDescription().getTitle()
                        .equals(mainActivity.getString(R.string.category_empty))) {
                    return;
                }
            }

            // Keep last selected position for the given category.
            // We will use it when back to this category
            final int mediaItemsStackSize = mainActivity.mediaItemsStack.size();
            if (mediaItemsStackSize >= 1) {
                final String children = mainActivity.mediaItemsStack.get(mediaItemsStackSize - 1);
                AppLogger.d(CLASS_NAME + " Children:" + children + " pos:" + position);
                mainActivity.listPositionMap.put(children, position);
            }

            mainActivity.showProgressBar();

            final String mediaId = item.getMediaId();

            // If it is browsable - then we navigate to the next category
            if (item.isBrowsable()) {
                mainActivity.addMediaItemToStack(mediaId);
            } else if (item.isPlayable()) {
                // Else - we play an item
                mainActivity.getSupportMediaController().getTransportControls().playFromMediaId(
                        mediaId, null
                );

                // Call appropriate activity for the items playing
                mainActivity.startActivity(
                        QueueActivity.makeIntent(mainActivity, mediaId)
                );
            }
        }
    }

    /**
     * Listener of the List Item long click event.
     */
    private static final class OnItemLongClickListener implements AdapterView.OnItemLongClickListener {

        private static final String CLASS_NAME = OnItemLongClickListener.class.getSimpleName();
        /**
         * Weak reference to the Main Activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor.
         *
         * @param mainActivity Reference to the Main Activity.
         */
        private OnItemLongClickListener(final MainActivity mainActivity) {
            super();
            mReference = new WeakReference<>(mainActivity);
        }

        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, final View view,
                                       final int position, final long id) {
            final MainActivity mainActivity = mReference.get();
            if (mainActivity == null) {
                AppLogger.w(CLASS_NAME + " OnItemLongClick return, reference to MainActivity is null");
                return true;
            }

            final MediaBrowserCompat.MediaItem item
                    = (MediaBrowserCompat.MediaItem) parent.getItemAtPosition(position);
            if (item == null) {
                return true;
            }

            // If Item is not Local Radio Station - skipp farther processing
            if (!MediaItemHelper.isLocalRadioStationField(item)) {
                return true;
            }

            String name = "";
            if (item.getDescription().getTitle() != null) {
                name = item.getDescription().getTitle().toString();
            }

            if (mainActivity.mIsOnSaveInstancePassed.get()) {
                AppLogger.w(CLASS_NAME + " Can not show Dialog after OnSaveInstanceState");
                return true;
            }

            // Show Remove Station Dialog
            final FragmentTransaction transaction = mainActivity.getFragmentManager().beginTransaction();
            final DialogFragment dialog = RemoveStationDialog.newInstance(
                    item.getMediaId(), name
            );
            dialog.show(transaction, RemoveStationDialog.DIALOG_TAG);

            return true;
        }
    }
}
