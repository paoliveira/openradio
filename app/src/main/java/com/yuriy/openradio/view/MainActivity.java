package com.yuriy.openradio.view;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.yuriy.openradio.R;
import com.yuriy.openradio.business.AppPreferencesManager;
import com.yuriy.openradio.service.AppLocalBroadcastReceiver;
import com.yuriy.openradio.service.AppLocalBroadcastReceiverCallback;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.view.list.MediaItemsAdapter;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 */

/**
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
public class MainActivity extends FragmentActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MainActivity.class.getSimpleName();

    /**
     * Browses media content offered by a link MediaBrowserService.
     * This object is not thread-safe.
     * All calls should happen on the thread on which the browser was constructed.
     */
    private MediaBrowser mMediaBrowser;

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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        // Set content.
        setContentView(R.layout.activity_main);

        // Register local receivers.
        registerReceivers();

        // Instantiate adapter
        mBrowserAdapter = new MediaItemsAdapter(this, null);

        // Initialize progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_view);

        hideProgressBar();

        // Initialize No Data text view
        mNoDataView = (TextView) findViewById(R.id.no_data_view);

        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);
        // Set adapter
        listView.setAdapter(mBrowserAdapter);
        // Set click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {

                // Current selected media item
                final MediaBrowser.MediaItem item
                        = (MediaBrowser.MediaItem) mBrowserAdapter.getItem(position);

                if (item.isBrowsable() && item.getDescription().getTitle().equals(
                        getString(R.string.category_empty))
                        ) {
                    return;
                }

                // Keep last selected position for the given category.
                // We will use it when back to this category
                listPositionMap.put(mediaItemsStack.get(mediaItemsStack.size() - 1), position);

                showProgressBar();

                // If it is browsable - then we navigate to the next category
                if (item.isBrowsable()) {
                    addMediaItemToStack(item.getMediaId());
                } else if (item.isPlayable()) {
                    // Else - we play an item
                    getMediaController().getTransportControls().playFromMediaId(
                            item.getMediaId(), null
                    );

                    // Call appropriate activity for the items playing
                    runOnUiThread(
                            new Runnable() {

                                @Override
                                public void run() {
                                    startActivity(QueueActivity.makeIntent(MainActivity.this));
                                }
                            }
                    );
                }
            }
        });

        // Instantiate media browser
        mMediaBrowser = new MediaBrowser(
                this,
                new ComponentName(this, OpenRadioService.class),
                connectionCallback, null
        );

        restoreState(savedInstanceState);

        mMediaBrowser.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideProgressBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister local receivers
        unregisterReceivers();
        // Disconnect Media Browser
        mMediaBrowser.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            // DialogFragment.show() will take care of adding the fragment
            // in a transaction.  We also want to remove any currently showing
            // dialog, so make our own transaction and take care of that here.
            final FragmentTransaction fragmentTransaction = getFragmentManager()
                    .beginTransaction();
            final Fragment fragmentByTag = getFragmentManager()
                    .findFragmentByTag(AboutDialog.DIALOG_TAG);
            if (fragmentByTag != null) {
                fragmentTransaction.remove(fragmentByTag);
            }
            fragmentTransaction.addToBackStack(null);

            // Show About Dialog
            final DialogFragment aboutDialog = AboutDialog.newInstance();
            aboutDialog.show(fragmentTransaction, AboutDialog.DIALOG_TAG);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {

        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);

        // Save Media Stack
        outState.putSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK, (Serializable) mediaItemsStack);

        // Save List-Position Map
        outState.putSerializable(BUNDLE_ARG_LIST_POSITION_MAP, (Serializable) listPositionMap);

        // Save first visible ID of the List
        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, listView.getFirstVisiblePosition());

        // Keep last selected position for the given category.
        // We will use it when back to this category
        listPositionMap.put(
                mediaItemsStack.get(mediaItemsStack.size() - 1), listView.getFirstVisiblePosition()
        );

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {

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

        // Pop up current media item
        final String currentMediaId = mediaItemsStack.remove(mediaItemsStack.size() - 1);

        // Un-subscribe from all items
        mMediaBrowser.unsubscribe(currentMediaId);
        for (String mediaItemId : mediaItemsStack) {
            mMediaBrowser.unsubscribe(mediaItemId);
        }

        // Disconnect and connect back to media browser
        mMediaBrowser.disconnect();
        mMediaBrowser.connect();
    }

    /**
     * Start request location procedure. Despite the fact that whether user enable Location or not,
     * just request Location via Android API and return result via Broadcast event.
     */
    public void processLocationCallback() {
        startService(OpenRadioService.makeRequestLocationIntent(this));
    }

    /**
     * Add {@link android.media.browse.MediaBrowser.MediaItem} to stack.
     *
     * @param mediaId Id of the {@link android.view.MenuItem}
     */
    private void addMediaItemToStack(final String mediaId) {
        Log.i(CLASS_NAME, "MediaItem Id added:" + mediaId);

        mediaItemsStack.add(mediaId);

        mMediaBrowser.subscribe(mediaId, subscriptionCallback);
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
            for (String item : mediaItemsStackRestored) {
                mediaItemsStack.add(item);
            }
        }

        // Restore List's position
        final int listFirstVisiblePosition
                = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        // Get list view reference from the inflated xml
        final ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setSelection(listFirstVisiblePosition);
    }

    /**
     * Register receiver for the application's local events.
     */
    private void registerReceivers() {

        mAppLocalBroadcastReceiver.registerListener(receiverCallback);

        // Create filter and add actions
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppLocalBroadcastReceiver.getActionLocationDisabled());
        intentFilter.addAction(AppLocalBroadcastReceiver.getActionLocationCountryCode());
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
     * Callback object for the subscription events
     */
    private final MediaBrowser.SubscriptionCallback subscriptionCallback
            = new MediaBrowser.SubscriptionCallback() {

        @Override
        public void onChildrenLoaded(final String parentId,
                                     final List<MediaBrowser.MediaItem> children) {
            Log.i(CLASS_NAME, "On children loaded:" + parentId);

            hideProgressBar();

            mBrowserAdapter.clear();
            mBrowserAdapter.notifyDataSetInvalidated();
            for (MediaBrowser.MediaItem item : children) {
                mBrowserAdapter.addItem(item);
            }
            mBrowserAdapter.notifyDataSetChanged();

            if (children.isEmpty()) {
                showNoDataMessage();
            }

            // Get list view reference from the inflated xml
            final ListView listView = (ListView) findViewById(R.id.list_view);
            if (listView == null) {
                return;
            }
            if (!listPositionMap.containsKey(parentId)) {
                return;
            }
            // Restore previous position for the given category
            listView.setSelection(listPositionMap.get(parentId));
        }

        @Override
        public void onError(final String id) {

            hideProgressBar();

            Toast.makeText(
                    MainActivity.this, R.string.error_loading_media, Toast.LENGTH_LONG
            ).show();
        }
    };

    /**
     * Callback object for the Media Browser connection events
     */
    private final MediaBrowser.ConnectionCallback connectionCallback
            = new MediaBrowser.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();

            Log.i(CLASS_NAME, "MediaBrowser connected, stack empty:" + mediaItemsStack.isEmpty());

            // If stack is empty - assume that this is a start point
            if (mediaItemsStack.isEmpty()) {
                addMediaItemToStack(mMediaBrowser.getRoot());
            }

            // If session token is null - throw exception
            if (mMediaBrowser.getSessionToken() == null) {
                throw new IllegalArgumentException("No Session token");
            }

            // Subscribe to the media item
            mMediaBrowser.subscribe(
                    mediaItemsStack.get(mediaItemsStack.size() - 1),
                    subscriptionCallback
            );

            if (getMediaController() != null) {
                return;
            }

            // Initialize media controller
            final MediaController mediaController = new MediaController(
                    MainActivity.this,
                    mMediaBrowser.getSessionToken()
            );

            // Set actual controller
            setMediaController(mediaController);
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();

            Log.w(CLASS_NAME, "MediaBrowser connection suspended");
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();

            Log.w(CLASS_NAME, "MediaBrowser connection failed");

            setMediaController(null);
        }
    };

    /**
     * Callback receiver of the local application's event.
     */
    private final AppLocalBroadcastReceiverCallback receiverCallback
            = new AppLocalBroadcastReceiverCallback() {

        @Override
        public void onLocationDisabled() {
            Log.i(CLASS_NAME, "LocationDisabled");

            if (AppPreferencesManager.isLocationDialogShown()) {
                return;
            }

            final BaseDialogFragment useLocationServiceDialog = BaseDialogFragment.newInstance(
                    UseLocationDialog.class.getName()
            );
            useLocationServiceDialog.setCancelable(false);
            useLocationServiceDialog.show(getFragmentManager(), UseLocationDialog.DIALOG_TAG);

            AppPreferencesManager.setIsLocationDialogShown(true);
        }

        @Override
        public void onLocationCountryCode(final String countryCode) {
            // Disconnect and connect back to media browser
            mMediaBrowser.disconnect();
            mMediaBrowser.connect();
        }
    };
}
