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

package com.yuriy.openradio.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuriy.openradio.R;
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
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.shared.view.dialog.LogsDialog;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.view.dialog.AddStationDialog;
import com.yuriy.openradio.view.dialog.EqualizerDialog;
import com.yuriy.openradio.view.dialog.TvSettingsDialog;
import com.yuriy.openradio.view.list.MediaItemsAdapter;
import com.yuriy.openradio.view.list.TvMediaItemsAdapter;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/*
 * Main TV Activity class that loads main TV fragment.
 */
@AndroidEntryPoint
public final class TvMainActivity extends FragmentActivity {

    private static final String CLASS_NAME = TvMainActivity.class.getSimpleName() + " ";
    /**
     * Progress Bar view to indicate that data is loading.
     */
    private ProgressBar mProgressBar;
    @Inject
    MediaPresenter mMediaPresenter;
    private RecyclerView mListView;
    /**
     * Adapter for the representing media items in the list.
     */
    private TvMediaItemsAdapter mAdapter;
    /**
     * Listener of the Permissions status changes.
     */
    private PermissionStatusListener mPermissionStatusListener;
    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentParentId = "";
    private final TvMediaItemsAdapter.Listener mListener;
    private View mPlayBtn;
    private View mPauseBtn;
    private View mCurrentRadioStationView;
    private final RecyclerView.OnScrollListener mScrollListener;
    private int mListFirstVisiblePosition = 0;
    private int mListLastVisiblePosition = 0;
    private int mListSavedClickedPosition = MediaSessionCompat.QueueItem.UNKNOWN_ID;

    public TvMainActivity() {
        super();
        mListener = new TvMediaItemsAdapterListenerImpl();
        mScrollListener = new ScrollListener();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_main);

        setUpAddBtn();
        setUpSearchBtn();
        setUpSettingsBtn();
        setUpEqualizerBtn();

        final Context context = getApplicationContext();

        mProgressBar = findViewById(R.id.progress_bar_tv_view);
        mPlayBtn = findViewById(R.id.tv_crs_play_btn_view);
        mPauseBtn = findViewById(R.id.tv_crs_pause_btn_view);
        mCurrentRadioStationView = findViewById(R.id.tv_current_radio_station_view);
        mCurrentRadioStationView.setOnClickListener(
                v -> startService(OpenRadioService.makeToggleLastPlayedItemIntent(context))
        );

        // Instantiate adapter
        mAdapter = new TvMediaItemsAdapter(context);
        mAdapter.setListener(mListener);

        // Get list view reference from the inflated xml
        mListView = findViewById(R.id.tv_list_view);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        mListView.setLayoutManager(layoutManager);
        mListView.addOnScrollListener(mScrollListener);
        // Set adapter
        mListView.setAdapter(mAdapter);

        final MediaBrowserCompat.SubscriptionCallback subscriptionCb = new MediaBrowserSubscriptionCallback();
        final MediaPresenterListener listener = new MediaPresenterListenerImpl();
        mMediaPresenter.init(
                this,
                savedInstanceState,
                subscriptionCb,
                listener
        );

        mMediaPresenter.connect();

        if (!AppUtils.hasLocation(context)) {
            return;
        }
        mPermissionStatusListener = new PermissionListener(context);
        // Add listener for the permissions status
        PermissionChecker.addPermissionStatusListener(mPermissionStatusListener);
        final boolean isLocationPermissionGranted = PermissionChecker.isGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (isLocationPermissionGranted) {
            LocationService.doEnqueueWork(context);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.removeListener();
        }
        if (mMediaPresenter != null) {
            mMediaPresenter.destroy();
        }
        if (mListView != null) {
            mListView.removeOnScrollListener(mScrollListener);
        }
        PermissionChecker.removePermissionStatusListener(mPermissionStatusListener);
        ContextCompat.startForegroundService(
                getApplicationContext(),
                OpenRadioService.makeStopServiceIntent(getApplicationContext())
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AppLogger.d(CLASS_NAME + "on activity result, rqst:" + requestCode + " rslt:" + resultCode);
        final GoogleDriveDialog gDriveDialog = GoogleDriveDialog.findGoogleDriveDialog(getSupportFragmentManager());
        if (gDriveDialog != null) {
            gDriveDialog.onActivityResult(requestCode, resultCode, data);
        }

        final LogsDialog logsDialog = LogsDialog.findLogsDialog(getSupportFragmentManager());
        if (logsDialog != null) {
            logsDialog.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE) {
            onSearchDialogClick();
        }
    }

    /**
     * Process call back from the Search Dialog.
     */
    public void onSearchDialogClick() {
        if (mMediaPresenter == null) {
            return;
        }
        mMediaPresenter.unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
    }

    /**
     * Show progress bar.
     */
    public void showProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress bar.
     */
    public void hideProgressBar() {
        if (mProgressBar == null) {
            return;
        }
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        hideProgressBar();
        if (mMediaPresenter.handleBackPressed(getApplicationContext())) {
            // perform android frameworks lifecycle
            super.onBackPressed();
        }
    }

    @MainThread
    private void handlePlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mPlayBtn.setVisibility(View.GONE);
                mPauseBtn.setVisibility(View.VISIBLE);
                break;
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayBtn.setVisibility(View.VISIBLE);
                mPauseBtn.setVisibility(View.GONE);
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_ERROR:
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_NONE:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
        }
        hideProgressBar();
    }

    private void setUpSettingsBtn() {
        final ImageView button = findViewById(R.id.tv_settings_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(v -> showTvSettings());
    }

    private void setUpSearchBtn() {
        final ImageView button = findViewById(R.id.tv_search_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                v -> AppUtils.startActivityForResultSafe(
                        this,
                        TvSearchActivity.makeStartIntent(this),
                        TvSearchActivity.SEARCH_TV_ACTIVITY_REQUEST_CODE
                )
        );
    }

    private void setUpEqualizerBtn() {
        final ImageView button = findViewById(R.id.tv_eq_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                v -> {
                    // Show Equalizer Dialog
                    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    final DialogFragment dialog = BaseDialogFragment.newInstance(
                            EqualizerDialog.class.getName()
                    );
                    dialog.show(transaction, EqualizerDialog.DIALOG_TAG);
                }
        );
    }

    private void setUpAddBtn() {
        final ImageView button = findViewById(R.id.tv_add_btn);
        if (button == null) {
            return;
        }
        button.setOnClickListener(
                (view) -> {
                    // Show Add Station Dialog
                    final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    final DialogFragment dialog = BaseDialogFragment.newInstance(
                            AddStationDialog.class.getName()
                    );
                    dialog.show(transaction, AddStationDialog.DIALOG_TAG);
                }
        );

    }

    private void showTvSettings() {
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        final Fragment fragment = getSupportFragmentManager().findFragmentByTag(TvSettingsDialog.DIALOG_TAG);
        if (fragment != null) {
            transaction.remove(fragment);
        }
        // Show Settings Dialog
        final DialogFragment dialogFragment = BaseDialogFragment.newInstance(
                TvSettingsDialog.class.getName()
        );
        dialogFragment.show(transaction, TvSettingsDialog.DIALOG_TAG);
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
     * Sets the item on the provided index as active.
     *
     * @param position Position of the item in the list.
     */
    private void setActiveItem(final int position) {
        if (mListView == null) {
            return;
        }
        mAdapter.setActiveItemId(position);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private void handleMetadataChanged(@Nullable final MediaMetadataCompat metadata) {
        if (metadata == null) {
            AppLogger.e("Handle metadata changed, metadata is null");
            return;
        }
        if (mCurrentRadioStationView.getVisibility() != View.VISIBLE) {
            mCurrentRadioStationView.setVisibility(View.VISIBLE);
        }
        final Context context = this;

        final RadioStation radioStation = LatestRadioStationStorage.get(context);
        if (radioStation == null) {
            AppLogger.e("Handle metadata changed, rs is null");
            return;
        }

        final MediaDescriptionCompat description = metadata.getDescription();

        final TextView nameView = findViewById(R.id.tv_crs_name_view);
        if (nameView != null) {
            nameView.setText(description.getTitle());
        }
        final TextView descriptionView = findViewById(R.id.tv_crs_description_view);
        if (descriptionView != null) {
            descriptionView.setText(description.getDescription());
        }
        final ImageView imgView = findViewById(R.id.tv_crs_img_view);
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station);
        MediaItemsAdapter.updateImage(description, imgView);
        MediaItemsAdapter.updateBitrateView(
                radioStation.getMediaStream().getVariant(0).getBitrate(),
                findViewById(R.id.tv_crs_bitrate_view),
                true
        );
//        final CheckBox favoriteCheckView = findViewById(R.id.tv_crs_favorite_check_view);
//        if (favoriteCheckView != null) {
//            favoriteCheckView.setButtonDrawable(
//                    AppCompatResources.getDrawable(this, R.drawable.src_favorite)
//            );
//            favoriteCheckView.setChecked(false);
//            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
//                    MediaItemHelper.buildMediaDescriptionFromRadioStation(context, radioStation),
//                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
//            );
//            MediaItemHelper.updateFavoriteField(
//                    mediaItem,
//                    FavoritesStorage.isFavorite(radioStation, context)
//            );
//            MediaItemsAdapter.handleFavoriteAction(favoriteCheckView, description, mediaItem, context);
//        }
    }

    /**
     * Remove provided Media Id from the collection. Reconnect {@link MediaBrowserCompat}.
     *
     * @param mediaItemId Media Id.
     */
    private void unsubscribeFromItem(final String mediaItemId) {
        hideProgressBar();

        mMediaPresenter.unsubscribeFromItem(mediaItemId);
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
                    CLASS_NAME + " Children loaded:" + parentId + ", children:" + children.size()
            );

            hideProgressBar();

            mCurrentParentId = parentId;

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

        @Override
        public void onError(@NonNull final String id) {
            SafeToast.showAnyThread(
                    getApplicationContext(),
                    getString(R.string.error_loading_media)
            );
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
            TvMainActivity.this.updateListVisiblePositions(recyclerView);
            if (TvMainActivity.this.mListLastVisiblePosition == TvMainActivity.this.mAdapter.getItemCount() - 1) {
                TvMainActivity.this.onScrolledToEnd();
            }
        }
    }

    private final class MediaPresenterListenerImpl implements MediaPresenterListener {

        /**
         * Constructor.
         */
        private MediaPresenterListenerImpl() {
            super();
        }

        @Override
        public void showProgressBar() {
            TvMainActivity.this.showProgressBar();
        }

        @Override
        public void handleMetadataChanged(final MediaMetadataCompat metadata) {
            TvMainActivity.this.handleMetadataChanged(metadata);
        }

        @Override
        public void handlePlaybackStateChanged(final PlaybackStateCompat state) {
            TvMainActivity.this.handlePlaybackStateChanged(state);
        }
    }

    private final class TvMediaItemsAdapterListenerImpl implements TvMediaItemsAdapter.Listener {

        private TvMediaItemsAdapterListenerImpl() {
            super();
        }

        @Override
        public void onItemSelected(@NonNull final MediaBrowserCompat.MediaItem item, final int position) {
            TvMainActivity.this.setActiveItem(position);
            TvMainActivity.this.mMediaPresenter.handleItemClick(item, position);
        }
    }
}
