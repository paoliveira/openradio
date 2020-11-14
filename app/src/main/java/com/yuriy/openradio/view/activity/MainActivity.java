/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.yuriy.openradio.R;
import com.yuriy.openradio.broadcast.AppLocalReceiver;
import com.yuriy.openradio.broadcast.AppLocalReceiverCallback;
import com.yuriy.openradio.broadcast.ScreenReceiver;
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.shared.permission.PermissionChecker;
import com.yuriy.openradio.shared.permission.PermissionListener;
import com.yuriy.openradio.shared.permission.PermissionStatusListener;
import com.yuriy.openradio.shared.presenter.MediaPresenter;
import com.yuriy.openradio.shared.presenter.MediaPresenterListener;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.view.activity.PermissionsDialogActivity;
import com.yuriy.openradio.shared.view.dialog.AboutDialog;
import com.yuriy.openradio.shared.view.dialog.GeneralSettingsDialog;
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.shared.view.dialog.LogsDialog;
import com.yuriy.openradio.shared.view.dialog.StreamBufferingDialog;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.shared.vo.RadioStationToAdd;
import com.yuriy.openradio.view.dialog.AddStationDialog;
import com.yuriy.openradio.view.dialog.EditStationDialog;
import com.yuriy.openradio.view.dialog.EqualizerDialog;
import com.yuriy.openradio.view.dialog.RSSettingsDialog;
import com.yuriy.openradio.view.dialog.RemoveStationDialog;
import com.yuriy.openradio.view.dialog.SearchDialog;
import com.yuriy.openradio.view.list.MediaItemsAdapter;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 19.12.14
 * Time: 15:13
 * <p>
 * Main Activity class with represents the list of the categories: All, By Genre, Favorites, etc ...
 */
@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity {

    /**
     * Tag string to use in logging message.
     */
    private final String CLASS_NAME;
    /**
     * Adapter for the representing media items in the list.
     */
    private MediaItemsAdapter mBrowserAdapter;
    private View mCurrentRadioStationView;
    @Nullable
    private MediaMetadataCompat mLastKnownMetadata;
    private static final String BUNDLE_ARG_LAST_KNOWN_METADATA = "BUNDLE_ARG_LAST_KNOWN_METADATA";
    /**
     * Key value for the first visible ID in the List for the store Bundle
     */
    private static final String BUNDLE_ARG_LIST_1_VISIBLE_ID = "BUNDLE_ARG_LIST_1_VISIBLE_ID";
    private static final String BUNDLE_ARG_LIST_CLICKED_ID = "BUNDLE_ARG_LIST_CLICKED_ID";
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
    private final AppLocalReceiver mAppLocalBroadcastRcvr;
    /**
     * Listener of the Permissions status changes.
     */
    private final PermissionStatusListener mPermissionStatusLstnr;
    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private final LocalBroadcastReceiverCallback mLocalBroadcastReceiverCb;
    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentParentId = "";
    private int mCurrentPlaybackState = PlaybackStateCompat.STATE_NONE;
    private int mListFirstVisiblePosition = 0;
    private int mListLastVisiblePosition = 0;
    private int mListSavedClickedPosition = MediaSessionCompat.QueueItem.UNKNOWN_ID;
    /**
     * Listener for the List view click event.
     */
    private final MediaItemsAdapter.Listener mMediaItemListener;
    /**
     * Guardian field to prevent UI operation after addToLocals instance passed.
     */
    private final AtomicBoolean mIsOnSaveInstancePassed;
    /**
     * Receiver for the Screen OF/ON events.
     */
    private final ScreenReceiver mScreenBroadcastRcvr;
    private TextView mBufferedTextView;
    private RecyclerView mListView;
    private View mPlayBtn;
    private View mPauseBtn;
    private ProgressBar mProgressBarCrs;
    @Inject
    MediaPresenter mMediaPresenter;
    private final RecyclerView.OnScrollListener mScrollListener;

    /**
     * Default constructor.
     */
    public MainActivity() {
        super();
        CLASS_NAME = MainActivity.class.getSimpleName() + " " + hashCode() + " ";
        mAppLocalBroadcastRcvr = AppLocalReceiver.getInstance();
        mPermissionStatusLstnr = new PermissionListener(this);
        mLocalBroadcastReceiverCb = new LocalBroadcastReceiverCallback();
        mMediaItemListener = new MediaItemListenerImpl();
        mIsOnSaveInstancePassed = new AtomicBoolean(false);
        mScreenBroadcastRcvr = new ScreenReceiver();
        mScrollListener = new ScrollListener();
    }

    @SuppressLint({"ClickableViewAccessibility", "NonConstantResourceId"})
    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.d(CLASS_NAME + "OnCreate:" + savedInstanceState);

        // Set content.
        setContentView(R.layout.main_drawer);

        mPlayBtn = findViewById(R.id.crs_play_btn_view);
        mPauseBtn = findViewById(R.id.crs_pause_btn_view);
        mProgressBarCrs = findViewById(R.id.crs_progress_view);

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
                    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    clearDialogs(transaction);
                    menuItem.setChecked(false);
                    // Handle navigation view item clicks here.
                    final int id = menuItem.getItemId();
                    switch (id) {
                        case R.id.nav_general:
                            // Show Search Dialog
                            final DialogFragment settingsDialog = BaseDialogFragment.newInstance(
                                    GeneralSettingsDialog.class.getName()
                            );
                            settingsDialog.show(transaction, GeneralSettingsDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_buffering:
                            // Show Stream Buffering Dialog
                            final DialogFragment streamBufferingDialog = BaseDialogFragment.newInstance(
                                    StreamBufferingDialog.class.getName()
                            );
                            streamBufferingDialog.show(transaction, StreamBufferingDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_google_drive:
                            // Show Google Drive Dialog
                            final DialogFragment googleDriveDialog = BaseDialogFragment.newInstance(
                                    GoogleDriveDialog.class.getName()
                            );
                            googleDriveDialog.show(transaction, GoogleDriveDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_logs:
                            // Show Application Logs Dialog
                            final DialogFragment applicationLogsDialog = BaseDialogFragment.newInstance(
                                    LogsDialog.class.getName()
                            );
                            applicationLogsDialog.show(transaction, LogsDialog.DIALOG_TAG);
                            break;
                        case R.id.nav_about:
                            // Show About Dialog
                            final DialogFragment aboutDialog = BaseDialogFragment.newInstance(
                                    AboutDialog.class.getName()
                            );
                            aboutDialog.show(transaction, AboutDialog.DIALOG_TAG);
                            break;
                        default:

                            break;
                    }

                    drawer.closeDrawer(GravityCompat.START);
                    return true;
                }
        );

        final Context context = getApplicationContext();
        final String versionText = AppUtils.getApplicationVersion(context) + "." +
                AppUtils.getApplicationVersionCode(context);
        final TextView versionView = navigationView.getHeaderView(0).findViewById(
                R.id.drawer_ver_code_view
        );
        versionView.setText(versionText);

        mLastKnownMetadata = null;

        final MediaBrowserCompat.SubscriptionCallback medSubscriptionCb = new MediaBrowserSubscriptionCallback();
        final MediaPresenterListener mediaPresenterLstnr = new MediaPresenterListenerImpl();
        mMediaPresenter.init(
                this,
                savedInstanceState,
                medSubscriptionCb,
                mediaPresenterLstnr
        );

        // Register local receivers.
        registerReceivers();

        // Add listener for the permissions status
        PermissionChecker.addPermissionStatusListener(mPermissionStatusLstnr);

        // Instantiate adapter
        mBrowserAdapter = new MediaItemsAdapter(getApplicationContext());

        // Initialize progress bar
        mProgressBar = findViewById(R.id.progress_bar_view);

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        mCurrentRadioStationView = findViewById(R.id.current_radio_station_view);
        mCurrentRadioStationView.setOnClickListener(
                v -> startService(OpenRadioService.makeToggleLastPlayedItemIntent(context))
        );

        hideProgressBar();

        // Initialize No Data text view
        mNoDataView = findViewById(R.id.no_data_view);

        mBufferedTextView = findViewById(R.id.crs_buffered_view);
        updateBufferedTime(0);

        // Get list view reference from the inflated xml
        mListView = findViewById(R.id.list_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        mListView.setLayoutManager(layoutManager);
        // Set adapter
        mListView.setAdapter(mBrowserAdapter);
        mListView.addOnScrollListener(mScrollListener);
        mBrowserAdapter.setListener(mMediaItemListener);

        // Handle Add Radio Station button.
        final FloatingActionButton addBtn = findViewById(R.id.add_station_btn);
        addBtn.setOnClickListener(
                view -> {
                    // Show Add Station Dialog
                    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    final DialogFragment dialog = BaseDialogFragment.newInstance(
                            AddStationDialog.class.getName()
                    );
                    dialog.show(transaction, AddStationDialog.DIALOG_TAG);
                }
        );

        restoreState(savedInstanceState);
        connectToMediaBrowser();

        if (!AppUtils.hasLocation(context)) {
            return;
        }

        if (PermissionsDialogActivity.isLocationDenied(getIntent())) {
            return;
        }

        final boolean isLocationPermissionGranted = PermissionChecker.isGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (isLocationPermissionGranted) {
            LocationService.doEnqueueWork(context);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Get list view reference from the inflated xml
        if (mListView != null) {
            unregisterForContextMenu(mListView);
        }
    }

    @Override
    protected final void onResume() {
        super.onResume();

        // Get list view reference from the inflated xml
        if (mListView != null) {
            registerForContextMenu(mListView);
        }

        // Set OnSaveInstanceState to false
        mIsOnSaveInstancePassed.set(false);

        // Hide any progress bar
        hideProgressBar();
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
        AppLogger.i(CLASS_NAME + "OnDestroy");

        if (mListView != null) {
            mListView.removeOnScrollListener(mScrollListener);
        }
        mMediaPresenter.clean();
        if (!mIsOnSaveInstancePassed.get()) {
            mMediaPresenter.destroy();
        }
        PermissionChecker.removePermissionStatusListener(mPermissionStatusLstnr);

        // Unregister local receivers
        unregisterReceivers();

        mBrowserAdapter.clear();

        ContextCompat.startForegroundService(
                getApplicationContext(),
                OpenRadioService.makeStopServiceIntent(getApplicationContext())
        );
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu,
                                    final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final MenuInflater inflater = getMenuInflater();
        if (MediaIdHelper.MEDIA_ID_FAVORITES_LIST.equals(mCurrentParentId)) {
            inflater.inflate(R.menu.context_menu_favorites_stations, menu);
        } else if (MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST.equals(mCurrentParentId)) {
            inflater.inflate(R.menu.context_menu_local_stations, menu);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_radio_stations_menu:
                mBrowserAdapter.notifyDataSetChanged();
                break;
            case R.id.delete_radio_station_menu:
//                if (mOnTouchLstnr.mPosition != -1) {
//                    handleDeleteRadioStationMenu(mOnTouchLstnr.mPosition);
//                }
                break;
            case R.id.edit_radio_station_menu:
//                if (mOnTouchLstnr.mPosition != -1) {
//                    handleEditRadioStationMenu(mOnTouchLstnr.mPosition);
//                }
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        clearDialogs(transaction);
        switch (id) {
            case R.id.action_search: {
                // Show Search Dialog
                final DialogFragment dialog = BaseDialogFragment.newInstance(
                        SearchDialog.class.getName()
                );
                dialog.show(transaction, SearchDialog.DIALOG_TAG);
                return true;
            }
            case R.id.action_eq: {
                // Show Equalizer Dialog
                final DialogFragment dialog = BaseDialogFragment.newInstance(
                        EqualizerDialog.class.getName()
                );
                dialog.show(transaction, EqualizerDialog.DIALOG_TAG);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected final void onSaveInstanceState(@NonNull final Bundle outState) {
        AppLogger.d(CLASS_NAME + "OnSaveInstance:" + outState);
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true);

        if (mLastKnownMetadata != null) {
            outState.putParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA, mLastKnownMetadata);
        }

        OpenRadioService.putCurrentParentId(outState, mCurrentParentId);
        OpenRadioService.putCurrentPlaybackState(outState, mCurrentPlaybackState);
        OpenRadioService.putRestoreState(outState, true);

        updateListVisiblePositions(mListView);
        // Save first visible ID of the List
        outState.putInt(BUNDLE_ARG_LIST_1_VISIBLE_ID, mListFirstVisiblePosition);
        outState.putInt(BUNDLE_ARG_LIST_CLICKED_ID, mBrowserAdapter.getActiveItemId());
        super.onSaveInstanceState(outState);
    }

    @Override
    public final void onBackPressed() {

        hideNoDataMessage();
        hideProgressBar();

        if (mMediaPresenter.handleBackPressed(getApplicationContext())) {
            // perform android frameworks lifecycle
            super.onBackPressed();
        }
    }

    private void connectToMediaBrowser() {
        mMediaPresenter.connect();
    }

    private void restoreSelectedPosition() {
        int selectedPosition;
        int clickedPosition;
        if (mListFirstVisiblePosition != -1) {
            selectedPosition = mListFirstVisiblePosition;
            clickedPosition = mListSavedClickedPosition;
            mListFirstVisiblePosition = -1;
            mListSavedClickedPosition = -1;
        } else {
            // Restore positions for the Catalogue list.
            final int[] positions = mMediaPresenter.getPositions(mCurrentParentId);
            clickedPosition = positions[1];
            selectedPosition = positions[0];
        }
        // This will make selected item highlighted.
        setActiveItem(clickedPosition);
        // This actually do scroll to the position.
        mListView.scrollToPosition(selectedPosition - 1);
    }

    /**
     * Clears any active dialog.
     *
     * @param transaction Instance of Fragment transaction.
     */
    private void clearDialogs(final FragmentTransaction transaction) {
        final FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(AboutDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(SearchDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(EqualizerDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(GoogleDriveDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(GeneralSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(RSSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        fragment = manager.findFragmentByTag(EditStationDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        transaction.commitNow();
    }

    /**
     * Process user's input in order to edit custom {@link RadioStation}.
     */
    public final void processEditStationCallback(final String mediaId, final RadioStationToAdd radioStationToAdd) {
        startService(OpenRadioService.makeEditRadioStationIntent(
                this, mediaId, radioStationToAdd
        ));
    }

    /**
     * Process user's input in order to remove custom {@link RadioStation}.
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
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
        // Save search query string, retrieve it later in the service
        AppUtils.setSearchQuery(queryString);
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
    }

    /**
     * Remove provided Media Id from the collection. Reconnect {@link MediaBrowserCompat}.
     *
     * @param mediaItemId Media Id.
     */
    private void unsubscribeFromItem(final String mediaItemId) {
        hideNoDataMessage();
        hideProgressBar();

        mMediaPresenter.unsubscribeFromItem(mediaItemId);
    }

    /**
     * Updates root view is there was changes in collection.
     * Should be call only if current media id is {@link MediaIdHelper#MEDIA_ID_ROOT}.
     */
    private void updateRootView() {
        unsubscribeFromItem(MediaIdHelper.MEDIA_ID_ROOT);
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_ROOT);
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

    private void restoreState(final Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // Nothing to restore
            return;
        }

        mCurrentParentId = OpenRadioService.getCurrentParentId(savedInstanceState);

        // Restore List's position
        mListFirstVisiblePosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_1_VISIBLE_ID);
        mListSavedClickedPosition = savedInstanceState.getInt(BUNDLE_ARG_LIST_CLICKED_ID);

        final MediaMetadataCompat lastKnownMetadata = savedInstanceState.getParcelable(BUNDLE_ARG_LAST_KNOWN_METADATA);
        if (lastKnownMetadata != null) {
            handleMetadataChanged(lastKnownMetadata);
        }
    }

    /**
     * Sets the item on the provided index as active.
     *
     * @param position Position of the item in the list.
     */
    private void setActiveItem(final int position) {
        if (mListView == null) {
            return;
        }
        mBrowserAdapter.setActiveItemId(position);
        mBrowserAdapter.notifyDataSetChanged();
    }

    /**
     * Register receiver for the application's local events.
     */
    private void registerReceivers() {

        mAppLocalBroadcastRcvr.registerListener(mLocalBroadcastReceiverCb);

        // Create filter and add actions
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppLocalBroadcast.getActionLocationChanged());
        intentFilter.addAction(AppLocalBroadcast.getActionCurrentIndexOnQueueChanged());
        // Register receiver
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                mAppLocalBroadcastRcvr,
                intentFilter
        );

        mScreenBroadcastRcvr.register(getApplicationContext());
    }

    /**
     * Unregister receiver for the application's local events.
     */
    private void unregisterReceivers() {
        mAppLocalBroadcastRcvr.unregisterListener();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                mAppLocalBroadcastRcvr
        );

        mScreenBroadcastRcvr.unregister(getApplicationContext());
    }

    public void onRemoveRSClick(final View view) {
        final MediaBrowserCompat.MediaItem item = (MediaBrowserCompat.MediaItem) view.getTag();
        if (item == null) {
            return;
        }
        handleRemoveRadioStationMenu(item);
    }

    /**
     * Handles action of the Radio Station deletion.
     *
     * @param item Media item related to the Radio Station to be deleted.
     */
    private void handleRemoveRadioStationMenu(@NonNull final MediaBrowserCompat.MediaItem item) {
        String name = "";
        if (item.getDescription().getTitle() != null) {
            name = item.getDescription().getTitle().toString();
        }

        if (mIsOnSaveInstancePassed.get()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState");
            return;
        }

        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        clearDialogs(transaction);

        // Show Remove Station Dialog
        final Bundle bundle = RemoveStationDialog.createBundle(item.getMediaId(), name);
        final DialogFragment dialog = BaseDialogFragment.newInstance(
                RemoveStationDialog.class.getName(), bundle
        );
        dialog.show(transaction, RemoveStationDialog.DIALOG_TAG);
    }

    public void onEditRSClick(final View view) {
        final MediaBrowserCompat.MediaItem item = (MediaBrowserCompat.MediaItem) view.getTag();
        if (item == null) {
            return;
        }
        handleEditRadioStationMenu(item);
    }

    /**
     * Handles edit of the Radio Station action.
     *
     * @param item Media item related to the Radio Station to be edited.
     */
    private void handleEditRadioStationMenu(@NonNull final MediaBrowserCompat.MediaItem item) {
        if (mIsOnSaveInstancePassed.get()) {
            AppLogger.w(CLASS_NAME + "Can not show Dialog after OnSaveInstanceState");
            return;
        }

        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        clearDialogs(transaction);

        // Show Edit Station Dialog
        final DialogFragment dialog = EditStationDialog.newInstance(item.getMediaId());
        dialog.show(transaction, EditStationDialog.DIALOG_TAG);
    }

    @MainThread
    private void handlePlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
        mCurrentPlaybackState = state.getState();
        switch (mCurrentPlaybackState) {
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayBtn.setVisibility(View.GONE);
                mPauseBtn.setVisibility(View.VISIBLE);
                break;
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayBtn.setVisibility(View.VISIBLE);
                mPauseBtn.setVisibility(View.GONE);
                break;
        }
        mProgressBarCrs.setVisibility(View.GONE);
        hideProgressBar();

        final long bufferedDuration = (state.getBufferedPosition() - state.getPosition()) / 1000;
        updateBufferedTime(bufferedDuration);
    }

    /**
     * Updates buffered value of the currently playing radio station.
     *
     * @param value Buffered time in seconds.
     */
    private void updateBufferedTime(long value) {
        if (mBufferedTextView == null) {
            return;
        }
        if (value < 0) {
            value = 0;
        }

        mBufferedTextView.setVisibility(value > 0 ? View.VISIBLE : View.INVISIBLE);
        mBufferedTextView.setText(String.format(Locale.getDefault(), "Buffered %d sec", value));
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private void handleMetadataChanged(@Nullable final MediaMetadataCompat metadata) {
        if (metadata != null) {
            if (mCurrentRadioStationView.getVisibility() != View.VISIBLE) {
                mCurrentRadioStationView.setVisibility(View.VISIBLE);
            }
        }
        mLastKnownMetadata = metadata;
        final Context context = this;

        final RadioStation radioStation = LatestRadioStationStorage.get(context);
        if (radioStation == null) {
            return;
        }

        final MediaDescriptionCompat description = mLastKnownMetadata != null
                ? mLastKnownMetadata.getDescription()
                : MediaItemHelper.buildMediaDescriptionFromRadioStation(context, radioStation);

        final TextView nameView = findViewById(R.id.crs_name_view);
        if (nameView != null) {
            nameView.setText(description.getTitle());
        }
        final TextView descriptionView = findViewById(R.id.crs_description_view);
        if (descriptionView != null) {
            descriptionView.setText(description.getDescription());
        }
        final ImageView imgView = findViewById(R.id.crs_img_view);
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station);
        MediaItemsAdapter.updateImage(description, imgView);
        MediaItemsAdapter.updateBitrateView(
                radioStation.getMediaStream().getVariant(0).getBitrate(),
                findViewById(R.id.crs_bitrate_view),
                true
        );
        final CheckBox favoriteCheckView = findViewById(R.id.crs_favorite_check_view);
        if (favoriteCheckView != null) {
            favoriteCheckView.setButtonDrawable(
                    AppCompatResources.getDrawable(this, R.drawable.src_favorite)
            );
            favoriteCheckView.setChecked(false);
            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    MediaItemHelper.buildMediaDescriptionFromRadioStation(context, radioStation),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            );
            MediaItemHelper.updateFavoriteField(
                    mediaItem,
                    FavoritesStorage.isFavorite(radioStation, context)
            );
            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, context);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppLogger.d(CLASS_NAME + "OnActivityResult: request:" + requestCode + " result:" + resultCode);
        final GoogleDriveDialog gDriveDialog = GoogleDriveDialog.findGoogleDriveDialog(getSupportFragmentManager());
        if (gDriveDialog != null) {
            gDriveDialog.onActivityResult(requestCode, resultCode, data);
        }

        final LogsDialog logsDialog = LogsDialog.findLogsDialog(getSupportFragmentManager());
        if (logsDialog != null) {
            logsDialog.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Callback receiver of the local application's event.
     */
    private final class LocalBroadcastReceiverCallback implements AppLocalReceiverCallback {

        /**
         * Constructor.
         */
        private LocalBroadcastReceiverCallback() {
            super();
        }

        @Override
        public void onLocationChanged() {
            if (mIsOnSaveInstancePassed.get()) {
                AppLogger.w(CLASS_NAME + "Can not do Location Changed after OnSaveInstanceState");
                return;
            }

            AppLogger.d(CLASS_NAME + "Location Changed received");
            if (TextUtils.equals(MainActivity.this.mCurrentParentId, MediaIdHelper.MEDIA_ID_ROOT)) {
                MainActivity.this.updateRootView();
            }
        }

        @Override
        public void onCurrentIndexOnQueueChanged(final int index, final String mediaId) {
            final int position = MainActivity.this.mBrowserAdapter.getIndexForMediaId(mediaId);
            if (position != -1) {
                MainActivity.this.setActiveItem(position);
            }
        }
    }

    private final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Constructor.
         */
        private MediaBrowserSubscriptionCallback() {
            super();
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onChildrenLoaded(@NonNull final String parentId,
                                     @NonNull final List<MediaBrowserCompat.MediaItem> children) {
            AppLogger.i(
                    CLASS_NAME + "Children loaded:" + parentId + ", children:" + children.size()
            );

            if (MainActivity.this.mIsOnSaveInstancePassed.get()) {
                AppLogger.w(CLASS_NAME + "Can perform on children loaded after OnSaveInstanceState");
                return;
            }

            MainActivity.this.mCurrentParentId = parentId;
            MainActivity.this.hideProgressBar();

            final FloatingActionButton addBtn = findViewById(R.id.add_station_btn);
            if (parentId.equals(MediaIdHelper.MEDIA_ID_ROOT)) {
                addBtn.setVisibility(View.VISIBLE);
            } else {
                addBtn.setVisibility(View.GONE);
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            MainActivity.this.mBrowserAdapter.setParentId(parentId);
            MainActivity.this.mBrowserAdapter.clearData();
            MainActivity.this.mBrowserAdapter.addAll(children);
            MainActivity.this.mBrowserAdapter.notifyDataSetChanged();

            if (children.isEmpty()) {
                MainActivity.this.showNoDataMessage();
            }

            MainActivity.this.restoreSelectedPosition();
        }

        @Override
        public void onError(@NonNull final String id) {
            MainActivity.this.hideProgressBar();
            SafeToast.showAnyThread(
                    MainActivity.this, MainActivity.this.getString(R.string.error_loading_media)
            );
        }
    }

    /**
     * Listener of the List Item events.
     */
    private final class MediaItemListenerImpl implements MediaItemsAdapter.Listener {

        /**
         * Constructor.
         */
        private MediaItemListenerImpl() {
            super();
        }

        @Override
        public void onItemSettings(@NonNull final MediaBrowserCompat.MediaItem item, final int position) {
            final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            clearDialogs(transaction);
            final Bundle bundle = new Bundle();
            if (MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST.equals(mCurrentParentId)) {
                RSSettingsDialog.provideMediaItem(bundle, item);
            }
            final DialogFragment fragment = BaseDialogFragment.newInstance(RSSettingsDialog.class.getName(), bundle);
            fragment.show(transaction, RSSettingsDialog.DIALOG_TAG);
        }

        @Override
        public void onItemTap(final MediaBrowserCompat.MediaItem item, final int position) {
            MainActivity.this.setActiveItem(position);
            MainActivity.this.mMediaPresenter.handleItemClick(item, position);
        }
    }

    /**
     * Update List only if parent is Root or Favorites or Locals.
     */
    private void updateListAfterDownloadFromGoogleDrive() {
        if (TextUtils.equals(mCurrentParentId, MediaIdHelper.MEDIA_ID_ROOT)
                || TextUtils.equals(mCurrentParentId, MediaIdHelper.MEDIA_ID_FAVORITES_LIST)
                || TextUtils.equals(mCurrentParentId, MediaIdHelper.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST)) {
            updateRootView();
        }
    }

    private final class MediaPresenterListenerImpl implements MediaPresenterListener {

        private MediaPresenterListenerImpl() {
            super();
        }

        @Override
        public void showProgressBar() {
            MainActivity.this.showProgressBar();
        }

        @Override
        public void handleMetadataChanged(final MediaMetadataCompat metadata) {
            MainActivity.this.handleMetadataChanged(metadata);
        }

        @Override
        public void handlePlaybackStateChanged(final PlaybackStateCompat state) {
            MainActivity.this.handlePlaybackStateChanged(state);
        }
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
            mMediaPresenter.addMediaItemToStack(mCurrentParentId);
        } else {
            AppLogger.w(CLASS_NAME + "Category " + mCurrentParentId + " is not refreshable");
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
            MainActivity.this.updateListVisiblePositions(recyclerView);
            if (MainActivity.this.mListLastVisiblePosition == MainActivity.this.mBrowserAdapter.getItemCount() - 1) {
                MainActivity.this.onScrolledToEnd();
            }
        }
    }
}
