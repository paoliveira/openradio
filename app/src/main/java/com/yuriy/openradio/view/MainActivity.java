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

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.yuriy.openradio.R;
import com.yuriy.openradio.business.MediaResourceManagerListener;
import com.yuriy.openradio.business.MediaResourcesManager;
import com.yuriy.openradio.business.PermissionStatusListener;
import com.yuriy.openradio.business.broadcast.AppLocalBroadcast;
import com.yuriy.openradio.business.broadcast.AppLocalReceiver;
import com.yuriy.openradio.business.broadcast.AppLocalReceiverCallback;
import com.yuriy.openradio.business.broadcast.ConnectivityReceiver;
import com.yuriy.openradio.business.broadcast.ScreenReceiver;
import com.yuriy.openradio.business.service.OpenRadioService;
import com.yuriy.openradio.business.storage.AppPreferencesManager;
import com.yuriy.openradio.business.storage.FavoritesStorage;
import com.yuriy.openradio.business.storage.LatestRadioStationStorage;
import com.yuriy.openradio.drive.GoogleDriveError;
import com.yuriy.openradio.drive.GoogleDriveManager;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.FabricUtils;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.ImageFetcherFactory;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.PermissionChecker;
import com.yuriy.openradio.utils.Utils;
import com.yuriy.openradio.view.list.MediaItemsAdapter;
import com.yuriy.openradio.vo.RadioStation;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 * <p>
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
public final class MainActivity extends AppCompatActivity {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MainActivity.class.getSimpleName() + " ";

    /**
     * Adapter for the representing media items in the list.
     */
    private MediaItemsAdapter mBrowserAdapter;

    /**
     * Handles loading the  image in a background thread.
     */
    private ImageFetcher mImageFetcher;

    private View mCurrentRadioStationView;

    @Nullable
    private MediaMetadataCompat mLastKnownMetadata;

    /**
     * Stack of the media items.
     * It is used when navigating back and forth via list.
     */
    private final List<String> mMediaItemsStack = new LinkedList<>();

    /**
     * Map of the last used list position for the given list of the media items.
     */
    private final Map<String, Integer> mListPositionMap = new Hashtable<>();

    /**
     * Key value for the Media Stack for the store Bundle.
     */
    private static final String BUNDLE_ARG_MEDIA_ITEMS_STACK = "BUNDLE_ARG_MEDIA_ITEMS_STACK";

    private static final String BUNDLE_ARG_LAST_KNOWN_METADATA = "BUNDLE_ARG_LAST_KNOWN_METADATA";

    /**
     * Key value for the List-Position map for the store Bundle.
     */
    private static final String BUNDLE_ARG_LIST_POSITION_MAP = "BUNDLE_ARG_LIST_POSITION_MAP";

    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 300;
    private static final int ACCOUNT_REQUEST_CODE = 400;

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
    private final AppLocalReceiver mAppLocalBroadcastReceiver
            = AppLocalReceiver.getInstance();

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
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentParentId = "";

    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentMediaId = "";

    /**
     * Listener for the List view click event.
     */
    private final AdapterView.OnItemClickListener mOnItemClickListener
            = new OnItemClickListener(this);

    /**
     * Listener for the List touch event.
     */
    private final OnTouchListener mOnTouchListener = new OnTouchListener(this);

    /**
     * Guardian field to prevent UI operation after addToLocals instance passed.
     */
    private volatile AtomicBoolean mIsOnSaveInstancePassed = new AtomicBoolean(false);

    /**
     * Current dragging item.
     */
    public MediaBrowserCompat.MediaItem mDragMediaItem;

    /**
     * Currently active item when drag started.
     */
    private MediaBrowserCompat.MediaItem mStartDragSelectedItem;

    /**
     * Drag and drop position.
     */
    private int mDropPosition = -1;

    public boolean mIsSortMode = false;

    /**
     * Receiver for the Screen OF/ON events.
     */
    private final ScreenReceiver mScreenBroadcastReceiver = new ScreenReceiver();

    /**
     *
     */
    private GoogleDriveManager mGoogleDriveManager;

    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private MediaResourcesManager mMediaResourcesManager;

    private TextView mBufferedTextView;

    /**
     * Default constructor.
     */
    public MainActivity() {
        super();
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.d(CLASS_NAME + "OnCreate:" + savedInstanceState);

        // Set content.
        setContentView(R.layout.main_drawer);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    clearDialogs(fragmentTransaction);
                    menuItem.setChecked(false);
                    // Handle navigation view item clicks here.
                    final int id = menuItem.getItemId();
                    switch (id) {
                        case R.id.nav_general:
                            // Show Search Dialog
                            final DialogFragment settingsDialog = BaseDialogFragment.newInstance(
                                    GeneralSettingsDialog.class.getName()
                            );
                            settingsDialog.show(fragmentTransaction, GeneralSettingsDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_buffering:
                            // Show Stream Buffering Dialog
                            final DialogFragment streamBufferingDialog = BaseDialogFragment.newInstance(
                                    StreamBufferingDialog.class.getName()
                            );
                            streamBufferingDialog.show(fragmentTransaction, StreamBufferingDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_google_drive:
                            // Show Google Drive Dialog
                            final DialogFragment googleDriveDialog = BaseDialogFragment.newInstance(
                                    GoogleDriveDialog.class.getName()
                            );
                            googleDriveDialog.show(fragmentTransaction, GoogleDriveDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_logs:
                            // Show Application Logs Dialog
                            final DialogFragment applicationLogsDialog = BaseDialogFragment.newInstance(
                                    LogsDialog.class.getName()
                            );
                            applicationLogsDialog.show(fragmentTransaction, LogsDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_about:
                            // Show About Dialog
                            final DialogFragment aboutDialog = BaseDialogFragment.newInstance(
                                    AboutDialog.class.getName()
                            );
                            aboutDialog.show(fragmentTransaction, AboutDialog.DIALOG_TAG);
                            break;
                        default:

                            break;
                    }

                    drawer.closeDrawer(GravityCompat.START);
                    return true;
                }
        );

        final String versionText = AppUtils.getApplicationVersion(getApplicationContext()) + "." +
                AppUtils.getApplicationVersionCode(getApplicationContext());
        final TextView versionView = navigationView.getHeaderView(0).findViewById(R.id.drawer_ver_code_view);
        versionView.setText(versionText);

        mLastKnownMetadata = null;

        mGoogleDriveManager = new GoogleDriveManager(
                getApplicationContext(), new GoogleDriveManagerListenerImpl(this)
        );

        mMediaResourcesManager = new MediaResourcesManager(
                this,
                new MediaResourceManagerListenerImpl(this)
        );

        // Register local receivers.
        registerReceivers();

        // Add listener for the permissions status
        PermissionChecker.addPermissionStatusListener(mPermissionStatusListener);

        // Handles loading the  image in a background thread
        mImageFetcher = ImageFetcherFactory.getSmallImageFetcher(this);

        // Instantiate adapter
        mBrowserAdapter = new MediaItemsAdapter(this, mImageFetcher);

        // Initialize progress bar
        mProgressBar = findViewById(R.id.progress_bar_view);

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        mCurrentRadioStationView = findViewById(R.id.current_radio_station_view);
        mCurrentRadioStationView.setOnClickListener(
                v -> startService(OpenRadioService.makeToggleLastPlayedItemIntent(getApplicationContext()))
        );

        hideProgressBar();

        // Initialize No Data text view
        mNoDataView = findViewById(R.id.no_data_view);

        mBufferedTextView = findViewById(R.id.crs_buffered_view);
        updateBufferedTime(0);

        // Get list view reference from the inflated xml
        final ListView listView = findViewById(R.id.list_view);
        // Set adapter
        listView.setAdapter(mBrowserAdapter);
        // Set click listener
        listView.setOnItemClickListener(mOnItemClickListener);
        // Set touch listener.
        listView.setOnTouchListener(mOnTouchListener);
        // Set scroll listener.
        listView.setOnScrollListener(new OnScrollListener(this));

        // Handle Add Radio Station button.
        final FloatingActionButton addBtn = findViewById(R.id.add_station_btn);
        addBtn.setOnClickListener(
                view -> {
                    // Show Add Station Dialog
                    final FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    final DialogFragment dialog = AddStationDialog.newInstance();
                    dialog.show(transaction, AddStationDialog.DIALOG_TAG);
                }
        );

        mMediaResourcesManager.create(savedInstanceState);

        restoreState(savedInstanceState);

        mMediaResourcesManager.connect();
    }

    @Override
    protected void onPause() {

        mGoogleDriveManager.disconnect();

        super.onPause();

        // Get list view reference from the inflated xml
        final ListView listView = findViewById(R.id.list_view);
        if (listView != null) {
            unregisterForContextMenu(listView);
        }
    }

    @Override
    protected final void onResume() {
        super.onResume();

        // Get list view reference from the inflated xml
        final ListView listView = findViewById(R.id.list_view);
        if (listView != null) {
            registerForContextMenu(listView);
        }

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        // Hide any progress bar
        hideProgressBar();

        // Restore position for the Catalogue list.
        if (!TextUtils.isEmpty(mCurrentParentId)
                && mListPositionMap.containsKey(mCurrentParentId)) {
            final int position = mListPositionMap.get(mCurrentParentId);
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
        AppLogger.i(CLASS_NAME + "OnDestroy");

        PermissionChecker.removePermissionStatusListener(mPermissionStatusListener);

        // Unregister local receivers
        unregisterReceivers();
        // Disconnect Media Browser
        mMediaResourcesManager.disconnect();

        if (mGoogleDriveManager != null) {
            mGoogleDriveManager.release();
        }
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu,
                                    final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final MenuInflater inflater = getMenuInflater();
        if (MediaIDHelper.MEDIA_ID_FAVORITES_LIST.equals(mCurrentParentId)) {
            inflater.inflate(R.menu.context_menu_favorites_stations, menu);
        } else if (MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST.equals(mCurrentParentId)) {
            inflater.inflate(R.menu.context_menu_local_stations, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_radio_stations_menu:
                mIsSortMode = true;
                mBrowserAdapter.notifyDataSetChanged();
                break;
            case R.id.delete_radio_station_menu:
                if (mOnTouchListener.mPosition != -1) {
                    handleDeleteRadioStationMenu(mOnTouchListener.mPosition);
                }
                break;
            case R.id.edit_radio_station_menu:
                if (mOnTouchListener.mPosition != -1) {
                    handleEditRadioStationMenu(mOnTouchListener.mPosition);
                }
                break;
        }
        return super.onContextItemSelected(item);
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
        clearDialogs(fragmentTransaction);
        if (id == R.id.action_search) {
            // Show Search Dialog
            final DialogFragment searchDialog = BaseDialogFragment.newInstance(
                    SearchDialog.class.getName()
            );
            searchDialog.show(fragmentTransaction, SearchDialog.DIALOG_TAG);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected final void onSaveInstanceState(final Bundle outState) {
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true);

        // Get list view reference from the inflated xml
        final ListView listView = findViewById(R.id.list_view);

        // Save Media Stack
        outState.putSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK, (Serializable) mMediaItemsStack);

        // Save List-Position Map
        outState.putSerializable(BUNDLE_ARG_LIST_POSITION_MAP, (Serializable) mListPositionMap);

        if (mLastKnownMetadata != null) {
            outState.putParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA, mLastKnownMetadata);
        }

        // Get first visible item id
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        // Just in case ...
        if (firstVisiblePosition < 0) {
            firstVisiblePosition = 0;
        }

        // Save first visible ID of the List
        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, firstVisiblePosition);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                mGoogleDriveManager.connect();
                break;
            case ACCOUNT_REQUEST_CODE:
                final String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (TextUtils.isEmpty(email)) {
                    SafeToast.showAnyThread(
                            getApplicationContext(), getString(R.string.can_not_get_account_name)
                    );
                    break;
                }
                mGoogleDriveManager.connect(email);
                break;
        }
    }

    @Override
    public final void onBackPressed() {

        if (mIsSortMode) {
            mIsSortMode = false;
            mBrowserAdapter.notifyDataSetChanged();
            return;
        }

        hideNoDataMessage();
        hideProgressBar();

        // If there is root category - close activity
        if (mMediaItemsStack.size() == 1) {

            // Un-subscribe from item
            mMediaResourcesManager.unsubscribe(mMediaItemsStack.remove(mMediaItemsStack.size() - 1));
            // Clear stack
            mMediaItemsStack.clear();

            startService(OpenRadioService.makeStopServiceIntent(getApplicationContext()));

            // perform android frameworks lifecycle
            super.onBackPressed();
            return;
        }

        int location = mMediaItemsStack.size() - 1;
        if (location >= 0) {
            // Get current media item and un-subscribe.
            final String currentMediaId = mMediaItemsStack.remove(location);
            mMediaResourcesManager.unsubscribe(currentMediaId);
        }

        // Un-subscribe from all items.
        for (final String mediaItemId : mMediaItemsStack) {
            mMediaResourcesManager.unsubscribe(mediaItemId);
        }

        // Subscribe to the previous item.
        location = mMediaItemsStack.size() - 1;
        if (location >= 0) {
            final String previousMediaId = mMediaItemsStack.get(location);
            if (!TextUtils.isEmpty(previousMediaId)) {
                showProgressBar();
                AppLogger.d(CLASS_NAME + "Back to " + previousMediaId);
                mMediaResourcesManager.subscribe(previousMediaId, mMedSubscriptionCallback);
            }
        }
    }

    /**
     *
     * @param fragmentTransaction
     */
    private void clearDialogs(final FragmentTransaction fragmentTransaction) {
        Fragment fragmentByTag = getFragmentManager().findFragmentByTag(AboutDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getFragmentManager().findFragmentByTag(SearchDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getFragmentManager().findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentByTag = getFragmentManager().findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG);
        if (fragmentByTag != null) {
            fragmentTransaction.remove(fragmentByTag);
        }
        fragmentTransaction.addToBackStack(null);
    }

    /**
     * Start request location procedure. Despite the fact that whether user enable Location or not,
     * just request Location via Android API and return result via Broadcast event.
     */
    public final void processLocationCallback() {
        startService(OpenRadioService.makeRequestLocationIntent(getApplicationContext()));
    }

    /**
     * Process user's input in order to generate custom {@link RadioStation}.
     */
    public final void processAddStationCallback(final String name, final String url,
                                                final String imageUrl, final String genre,
                                                final String country, final boolean addToFav) {
        startService(OpenRadioService.makeAddRadioStationIntent(
                getApplicationContext(), name, url, imageUrl, genre, country, addToFav
        ));
    }

    /**
     * Process user's input in order to edit custom {@link RadioStation}.
     */
    public final void processEditStationCallback(final String mediaId, final String name, final String url,
                                                 final String imageUrl, final String genre,
                                                 final String country, final boolean addToFav) {
        startService(OpenRadioService.makeEditRadioStationIntent(
                getApplicationContext(), mediaId, name, url, imageUrl, genre, country, addToFav
        ));
    }

    /**
     * Process user's input in order to remove custom {@link RadioStation}.
     */
    public final void processRemoveStationCallback(final String mediaId) {
        startService(OpenRadioService.makeRemoveRadioStationIntent(getApplicationContext(), mediaId));
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

    /**
     *
     */
    public void uploadRadioStationsToGoogleDrive() {
        mGoogleDriveManager.uploadRadioStations();
    }

    /**
     *
     */
    public void downloadRadioStationsFromGoogleDrive() {
        mGoogleDriveManager.downloadRadioStations();
    }

    /**
     * @param connectionResult
     */
    private void requestGoogleDriveSignIn(@NonNull final ConnectionResult connectionResult) {
        try {
            connectionResult.startResolutionForResult(
                    this,
                    RESOLVE_CONNECTION_REQUEST_CODE
            );
        } catch (IntentSender.SendIntentException e) {
            // Unable to resolve, message user appropriately
            AppLogger.e(CLASS_NAME + "Google Drive unable to resolve failure:" + e);
        }
    }

    /**
     * Handle event of the list view item start drag.
     *
     * @param mediaItem Media Item associated with the start drag event.
     */
    private void startDrag(final MediaBrowserCompat.MediaItem mediaItem) {
        mDropPosition = -1;
        mDragMediaItem = mediaItem;
        final int activeItemId = mBrowserAdapter.getActiveItemId();
        if (activeItemId != MediaSessionCompat.QueueItem.UNKNOWN_ID) {
            mStartDragSelectedItem = mBrowserAdapter.getItem(activeItemId);
        }
        mBrowserAdapter.notifyDataSetChanged();
    }

    /**
     * Handle event of the list view item stop drag.
     */
    private void stopDrag() {
        final int itemsNumber = mBrowserAdapter.getCount();
        if (mStartDragSelectedItem != null) {
            MediaBrowserCompat.MediaItem mediaItem;
            for (int i = 0; i < itemsNumber; ++i) {
                mediaItem = mBrowserAdapter.getItem(i);
                if (mediaItem == null) {
                    continue;
                }
                if (TextUtils.equals(mediaItem.getMediaId(), mStartDragSelectedItem.getMediaId())) {
                    setSelectedItem(i);
                    break;
                }
            }
        }
        mDropPosition = -1;
        mDragMediaItem = null;
        mStartDragSelectedItem = null;
        mBrowserAdapter.notifyDataSetChanged();

        MediaBrowserCompat.MediaItem mediaItem;
        final String[] mediaIds = new String[itemsNumber];
        final int[] positions = new int[itemsNumber];
        for (int i = 0; i < itemsNumber; ++i) {
            mediaItem = mBrowserAdapter.getItem(i);
            if (mediaItem == null) {
                continue;
            }
            MediaItemHelper.updateSortIdField(mediaItem, i);
            mediaIds[i] = mediaItem.getMediaId();
            positions[i] = i;
        }

        startService(
                OpenRadioService.makeUpdateSortIdsIntent(
                        getApplicationContext(),
                        mediaIds,
                        positions,
                        mCurrentParentId
                )
        );
    }

    /**
     *
     */
    private void handleDragChangedEvent() {
        mBrowserAdapter.remove(mDragMediaItem);
        mBrowserAdapter.addAt(mDropPosition, mDragMediaItem);
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
        for (int i = 0; i < mMediaItemsStack.size(); i++) {
            if (mMediaItemsStack.get(i).equals(mediaItemId)) {
                mMediaItemsStack.remove(i);
                i--;
            }
        }

        // Un-subscribe from item
        mMediaResourcesManager.unsubscribe(mediaItemId);
    }

    /**
     * Add {@link android.media.browse.MediaBrowser.MediaItem} to stack.
     *
     * @param mediaId Id of the {@link android.view.MenuItem}
     */
    private void addMediaItemToStack(final String mediaId) {
        AppLogger.i(CLASS_NAME + "MediaItem Id added:" + mediaId);
        if (TextUtils.isEmpty(mediaId)) {
            return;
        }

        if (!mMediaItemsStack.contains(mediaId)) {
            mMediaItemsStack.add(mediaId);
        }
        showProgressBar();
        mMediaResourcesManager.subscribe(mediaId, mMedSubscriptionCallback);
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
            mListPositionMap.clear();
            for (String key : listPositionMapRestored.keySet()) {
                mListPositionMap.put(key, listPositionMapRestored.get(key));
            }
        }

        // Restore Media Items stack
        final List<String> mediaItemsStackRestored
                = (List<String>) savedInstanceState.getSerializable(BUNDLE_ARG_MEDIA_ITEMS_STACK);
        if (mediaItemsStackRestored != null) {
            mMediaItemsStack.clear();
            mMediaItemsStack.addAll(mediaItemsStackRestored);
        }

        // Restore List's position
        final int listFirstVisiblePosition
                = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        setSelectedItem(listFirstVisiblePosition);

        final MediaMetadataCompat lastKnownMetadata = savedInstanceState.getParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA);
        if (lastKnownMetadata != null) {
            handleMetadataChanged(lastKnownMetadata);
        }
    }

    /**
     * Sets the item on the provided index as selected.
     *
     * @param position Position of the item in the list.
     */
    private void setSelectedItem(final int position) {
        AppLogger.d(CLASS_NAME + "Set selected:" + position);
        // Get list view reference from the inflated xml
        final ListView listView = findViewById(R.id.list_view);
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
        intentFilter.addAction(AppLocalBroadcast.getActionLocationDisabled());
        intentFilter.addAction(AppLocalBroadcast.getActionLocationCountryCode());
        intentFilter.addAction(AppLocalBroadcast.getActionCurrentIndexOnQueueChanged());
        // Register receiver
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                mAppLocalBroadcastReceiver,
                intentFilter
        );

        mScreenBroadcastReceiver.register(getApplicationContext());
    }

    /**
     * Unregister receiver for the application's local events.
     */
    private void unregisterReceivers() {
        mAppLocalBroadcastReceiver.unregisterListener();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                mAppLocalBroadcastReceiver
        );

        mScreenBroadcastReceiver.unregister(getApplicationContext());
    }

    /**
     * Handle a click event on the List View item.
     *
     * @param position Position of the clicked item.
     */
    private void handleOnItemClick(final int position) {
        if (!ConnectivityReceiver.checkConnectivityAndNotify(getApplicationContext())) {
            return;
        }

        // Current selected media item
        final MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);

        if (item.isBrowsable()) {
            if (item.getDescription().getTitle() != null
                    && item.getDescription().getTitle().equals(getString(R.string.category_empty))) {
                return;
            }
        }

        // Keep last selected position for the given category.
        // We will use it when back to this category
        final int mediaItemsStackSize = mMediaItemsStack.size();
        if (mediaItemsStackSize >= 1) {
            final String children = mMediaItemsStack.get(mediaItemsStackSize - 1);
            mListPositionMap.put(children, position);
        }

        final String mediaId = item.getMediaId();

        // If it is browsable - then we navigate to the next category
        if (item.isBrowsable()) {
            addMediaItemToStack(mediaId);
        } else if (item.isPlayable()) {
            // Else - we play an item
            boolean result = false;
            final MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
            if (mediaController != null) {
                final MediaControllerCompat.TransportControls transportControls = mediaController.getTransportControls();
                if (transportControls != null) {
                    transportControls.playFromMediaId(mediaId, null);
                    // Call appropriate activity for the items playing
                    startActivity(
                            QueueActivity.makeIntent(getApplicationContext())
                    );
                    result = true;
                }
            }
            if (!result) {
                SafeToast.showAnyThread(getApplicationContext(), getString(R.string.can_not_play_station));
            }
        }
    }

    /**
     * Handles action of the Radio Station deletion.
     *
     * @param position Position (in the List) of the Radio Station to be deleted.
     */
    private void handleDeleteRadioStationMenu(final int position) {
        final MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);
        if (item == null) {
            return;
        }

        // If Item is not Local Radio Station - skip farther processing
        if (!MediaItemHelper.isLocalRadioStationField(item)) {
            return;
        }

        String name = "";
        if (item.getDescription().getTitle() != null) {
            name = item.getDescription().getTitle().toString();
        }

        if (mIsOnSaveInstancePassed.get()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState");
            return;
        }

        // Show Remove Station Dialog
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final Bundle bundle = RemoveStationDialog.createBundle(item.getMediaId(), name);
        final DialogFragment dialog = BaseDialogFragment.newInstance(
                RemoveStationDialog.class.getName(), bundle
        );
        dialog.show(transaction, RemoveStationDialog.DIALOG_TAG);
    }

    /**
     * Handles action of the Radio Station edition.
     *
     * @param position Position (in the List) of the Radio Station to be edited.
     */
    private void handleEditRadioStationMenu(final int position) {
        final MediaBrowserCompat.MediaItem item = mBrowserAdapter.getItem(position);
        if (item == null) {
            return;
        }

        // If Item is not Local Radio Station - skip farther processing
        if (!MediaItemHelper.isLocalRadioStationField(item)) {
            return;
        }

        if (mIsOnSaveInstancePassed.get()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState");
            return;
        }

        // Show Edit Station Dialog
        final FragmentTransaction transaction = getFragmentManager().beginTransaction();
        final DialogFragment dialog = EditStationDialog.newInstance(item.getMediaId());
        dialog.show(transaction, EditStationDialog.DIALOG_TAG);
    }

    private RadioStation getLastKnowRadioStationAndUpdateView() {
        if (mCurrentRadioStationView == null) {
            return null;
        }

        final RadioStation radioStation = LatestRadioStationStorage.load(getApplicationContext());
        if (radioStation == null) {
            mCurrentRadioStationView.setVisibility(View.GONE);
            return null;
        }
        mCurrentRadioStationView.setVisibility(View.VISIBLE);
        return radioStation;
    }

    private void handlePlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
        final View playBtn = findViewById(R.id.crs_play_btn_view);
        final View pauseBtn = findViewById(R.id.crs_pause_btn_view);
        final ProgressBar progressBar = findViewById(R.id.crs_progress_view);

        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                playBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.VISIBLE);
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                playBtn.setVisibility(View.VISIBLE);
                pauseBtn.setVisibility(View.GONE);
                break;
        }

        progressBar.setVisibility(View.GONE);

        final double bufferedDuration = (state.getBufferedPosition() - state.getPosition()) / 1000.0;
        updateBufferedTime(bufferedDuration);
    }

    /**
     * Updates buffered value of the currently playing radio station.
     *
     * @param value Buffered time in seconds.
     *
     * TODO: Reuse the same code here and in QueueActivity
     */
    private void updateBufferedTime(double value) {
        if (mBufferedTextView == null) {
            return;
        }
        if (value < 0) {
            value = 0;
        }

        final double finalValue = value;
        runOnUiThread(
                () -> {
                    mBufferedTextView.setVisibility(finalValue > 0 ? View.VISIBLE : View.INVISIBLE);
                    mBufferedTextView.setText(String.format(Locale.getDefault(), "Buffered %.2f sec", finalValue));
                }
        );
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private void handleMetadataChanged(@Nullable final MediaMetadataCompat metadata) {
        mLastKnownMetadata = metadata;
        // TODO: Probably no need to have this check as currently playing Radio Station is the only one relates to
        //       metadata change.
        final RadioStation radioStation = getLastKnowRadioStationAndUpdateView();
        if (radioStation == null) {
            return;
        }

        final MediaDescriptionCompat description = mLastKnownMetadata != null
                ? mLastKnownMetadata.getDescription()
                : MediaItemHelper.buildMediaDescriptionFromRadioStation(getApplicationContext(), radioStation);

        final TextView nameView = findViewById(R.id.crs_name_view);
        if (nameView != null) {
            nameView.setText(description.getTitle());
        }
        final TextView descriptionView = findViewById(R.id.crs_description_view);
        if (descriptionView != null) {
            descriptionView.setText(description.getSubtitle());
        }
        final ImageView imageView = findViewById(R.id.crs_img_view);
        if (imageView != null) {
            MediaItemsAdapter.updateImage(description, true, imageView, mImageFetcher);
        }
        final CheckBox favoriteCheckView = findViewById(R.id.crs_favorite_check_view);
        if (favoriteCheckView != null) {
            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    MediaItemHelper.buildMediaDescriptionFromRadioStation(getApplicationContext(), radioStation),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            );
            MediaItemHelper.updateFavoriteField(
                    mediaItem,
                    FavoritesStorage.isFavorite(radioStation, getApplicationContext())
            );
            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, this);
        }
    }

    @Nullable
    private GoogleDriveDialog getGoogleDriveDialog() {
        final Fragment fragment = getFragmentManager().findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragment instanceof GoogleDriveDialog) {
            return (GoogleDriveDialog) fragment;
        }
        return null;
    }

    /**
     * Callback receiver of the local application's event.
     */
    private static final class LocalBroadcastReceiverCallback implements AppLocalReceiverCallback {

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
                AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState");
                return;
            }

            final DialogFragment useLocationServiceDialog = BaseDialogFragment.newInstance(
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
            reference.mMediaResourcesManager.disconnect();
            reference.mMediaResourcesManager.connect();
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

        /**
         * Reference to the enclosing class.
         */
        private final WeakReference<Context> mReference;
        private final Map<String, Double> mMap = new ConcurrentHashMap<>();
        private static final int DELTA = 2000;

        /**
         * Main constructor.
         *
         * @param reference Reference to the enclosing class.
         */
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
            AppLogger.i(
                    CLASS_NAME + "Children loaded:" + parentId + ", children:" + children.size()
            );

            final MainActivity activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "On children loaded -> activity ref is null");
                return;
            }

            // In case of Catalog is sortable and user do not know about it - show
            // help dialog to guide through functionality.
            if (MediaIDHelper.isMediaIdSortable(parentId)) {
                final boolean isSortDialogShown = AppPreferencesManager.isSortDialogShown(
                        activity.getApplicationContext()
                );
                if (!isSortDialogShown) {
                    final DialogFragment featureSortDialog = BaseDialogFragment.newInstance(
                            FeatureSortDialog.class.getName()
                    );
                    featureSortDialog.setCancelable(false);
                    featureSortDialog.show(activity.getFragmentManager(), FeatureSortDialog.DIALOG_TAG);
                }
            }

            activity.mCurrentParentId = parentId;
            activity.hideProgressBar();

            final FloatingActionButton addBtn = activity.findViewById(R.id.add_station_btn);
            if (parentId.equals(MediaIDHelper.MEDIA_ID_ROOT)) {
                addBtn.setVisibility(View.VISIBLE);
            } else {
                addBtn.setVisibility(View.GONE);
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            activity.mBrowserAdapter.clear();
            activity.mBrowserAdapter.notifyDataSetInvalidated();
            activity.mBrowserAdapter.addAll(children);
            activity.mBrowserAdapter.notifyDataSetChanged();

            if (children.isEmpty()) {
                activity.showNoDataMessage();
            }

            activity.mMediaResourcesManager.dump();

            if (!activity.mListPositionMap.containsKey(parentId)) {
                AppLogger.d(CLASS_NAME + "No key");
                activity.mBrowserAdapter.notifyDataSetInvalidated();
                activity.mBrowserAdapter.setActiveItemId(MediaSessionCompat.QueueItem.UNKNOWN_ID);
                activity.mBrowserAdapter.notifyDataSetInvalidated();
                return;
            }

            // Restore position for the Catalogue list
            int position = activity.mListPositionMap.get(parentId);
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
                AppLogger.w(CLASS_NAME + "OnItemClick return, reference to MainActivity is null");
                return;
            }
            mainActivity.handleOnItemClick(position);
        }
    }

    /**
     * Touch listener for the List View.
     */
    private static final class OnTouchListener implements AdapterView.OnTouchListener {

        /**
         * Weak reference to the Main Activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Position of the item under the touch event.
         */
        private int mPosition = -1;

        /**
         * Constructor.
         *
         * @param mainActivity Reference to the Main Activity.
         */
        private OnTouchListener(final MainActivity mainActivity) {
            super();
            mReference = new WeakReference<>(mainActivity);
        }

        @Override
        public boolean onTouch(final View listView, final MotionEvent event) {
            mPosition = ((ListView) listView).pointToPosition(
                    (int) event.getX(), (int) event.getY()
            );

            final MainActivity mainActivity = mReference.get();
            if (mainActivity == null) {
                AppLogger.w(CLASS_NAME + "OnItemTouch return, reference to MainActivity is null");
                return false;
            }

            if (!mainActivity.mIsSortMode) {
                return false;
            }

            // Do drag and drop sort only for Favorites and Local Radio Stations
            if (!MediaIDHelper.isMediaIdSortable(mainActivity.mCurrentParentId)) {
                return false;
            }

            if (mPosition < 0) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mainActivity.startDrag(mainActivity.mBrowserAdapter.getItem(mPosition));
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mPosition < 0) {
                        break;
                    }
                    if (mPosition != mainActivity.mDropPosition) {
                        mainActivity.mDropPosition = mPosition;
                        mainActivity.handleDragChangedEvent();
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_OUTSIDE: {
                    mainActivity.stopDrag();
                    return true;
                }
            }
            return true;
        }
    }

    /**
     * Update List only if parent is Root or Favorites or Locals.
     */
    private void updateListAfterDownloadFromGoogleDrive() {
        if (TextUtils.equals(mCurrentParentId, MediaIDHelper.MEDIA_ID_ROOT)
                || TextUtils.equals(mCurrentParentId, MediaIDHelper.MEDIA_ID_FAVORITES_LIST)
                || TextUtils.equals(mCurrentParentId, MediaIDHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)) {
            mMediaResourcesManager.disconnect();
            mMediaResourcesManager.connect();
        }
    }

    private void onScrolledToEnd() {
        if (MediaIDHelper.isMediaIdRefreshable(mCurrentParentId)) {
            unsubscribeFromItem(mCurrentParentId);
            addMediaItemToStack(mCurrentParentId);
        } else {
            AppLogger.w(CLASS_NAME + "Category " + mCurrentParentId + " is not refreshable");
        }
    }

    /**
     * Listener for the Media Resources related events.
     */
    private static final class MediaResourceManagerListenerImpl implements MediaResourceManagerListener {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MainActivity> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaResourceManagerListenerImpl(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(final List<MediaSessionCompat.QueueItem> queue) {
            final MainActivity activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onConnected reference to MainActivity is null");
                return;
            }

            AppLogger.i(CLASS_NAME + "Stack empty:" + activity.mMediaItemsStack.isEmpty());

            // If stack is empty - assume that this is a start point
            if (activity.mMediaItemsStack.isEmpty()) {
                activity.addMediaItemToStack(activity.mMediaResourcesManager.getRoot());
            }

            activity.showProgressBar();
            // Subscribe to the media item
            activity.mMediaResourcesManager.subscribe(
                    activity.mMediaItemsStack.get(activity.mMediaItemsStack.size() - 1),
                    activity.mMedSubscriptionCallback
            );

            // Update metadata in case of UI started on and media service was already created and stream played.
            activity.handleMetadataChanged(activity.mMediaResourcesManager.getMediaMetadata());
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + "PlaybackStateChanged:" + state);
            final MainActivity activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onPlaybackStateChanged reference to MainActivity is null");
                return;
            }
            activity.handlePlaybackStateChanged(state);
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + "Queue changed to:" + queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata,
                                      final List<MediaSessionCompat.QueueItem> queue) {
            final MainActivity activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onMetadataChanged reference to MainActivity is null");
                return;
            }
            if (metadata == null) {
                return;
            }
            activity.handleMetadataChanged(metadata);
        }
    }

    private static final class GoogleDriveManagerListenerImpl implements GoogleDriveManager.Listener {

        private final WeakReference<MainActivity> mReference;

        private GoogleDriveManagerListenerImpl(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnectionFailed(@Nullable final ConnectionResult connectionResult) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            if (connectionResult != null) {
                reference.requestGoogleDriveSignIn(connectionResult);
            } else {
                SafeToast.showAnyThread(
                        reference.getApplicationContext(), reference.getString(R.string.google_drive_conn_error)
                );
            }

            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().hideTitleProgress();
                }
            });
        }

        @Override
        public void onStart(final GoogleDriveManager.Command command) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().showProgress(command);
                }
            });
        }

        @Override
        public void onSuccess(final GoogleDriveManager.Command command) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            String message = null;
            switch (command) {
                case UPLOAD:
                    message = reference.getString(R.string.google_drive_data_saved);
                    break;
                case DOWNLOAD:
                    message = reference.getString(R.string.google_drive_data_read);
                    reference.updateListAfterDownloadFromGoogleDrive();
                    break;
            }
            if (!TextUtils.isEmpty(message)) {
                SafeToast.showAnyThread(reference.getApplication(), message);
            }

            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().hideProgress(command);
                }
            });
        }

        @Override
        public void onError(final GoogleDriveManager.Command command, final GoogleDriveError error) {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }
            String message = null;
            switch (command) {
                case UPLOAD:
                    message = reference.getString(R.string.google_drive_error_when_save);
                    break;
                case DOWNLOAD:
                    message = reference.getString(R.string.google_drive_error_when_read);
                    break;
            }
            if (!TextUtils.isEmpty(message)) {
                SafeToast.showAnyThread(reference.getApplication(), message);
            }

            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().hideProgress(command);
                }
            });
        }

        @Override
        public void onConnect() {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }

            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().showTitleProgress();
                }
            });
        }

        @Override
        public void onConnected() {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }

            reference.runOnUiThread(() -> {
                if (reference.getGoogleDriveDialog() != null) {
                    reference.getGoogleDriveDialog().hideTitleProgress();
                }
            });
        }

        @Override
        public void onAccountRequested() {
            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }

            try {
                reference.startActivityForResult(
                        AccountPicker.newChooseAccountIntent(
                                null,
                                null,
                                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                                true, null, null, null, null
                        ),
                        ACCOUNT_REQUEST_CODE
                );
            } catch (final ActivityNotFoundException e) {
                FabricUtils.logException(e);
                reference.mGoogleDriveManager.connect(null);
            }
        }
    }

    private static final class OnScrollListener implements AbsListView.OnScrollListener {

        private final WeakReference<MainActivity> mReference;
        private int mScrollState;
        private int mFirstVisibleItem;
        private int mVisibleItemCount;
        private int mTotalItemCount;

        private OnScrollListener(final MainActivity reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onScrollStateChanged(final AbsListView view, final int scrollState) {
            mScrollState = scrollState;
            reportEndOfScroll();
        }

        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem,
                             final int visibleItemCount, final int totalItemCount) {
            mFirstVisibleItem = firstVisibleItem;
            mVisibleItemCount = visibleItemCount;
            mTotalItemCount = totalItemCount;
        }

        private void reportEndOfScroll() {
            if (mScrollState != SCROLL_STATE_IDLE) {
                return;
            }

            if (mFirstVisibleItem + mVisibleItemCount != mTotalItemCount) {
                return;
            }

            final MainActivity reference = mReference.get();
            if (reference == null) {
                return;
            }

            reference.onScrolledToEnd();
        }
    }
}
