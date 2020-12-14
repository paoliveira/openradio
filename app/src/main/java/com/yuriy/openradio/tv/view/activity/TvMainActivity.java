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

package com.yuriy.openradio.tv.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
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
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.yuriy.openradio.R;
import com.yuriy.openradio.mobile.view.list.MobileMediaItemsAdapter;
import com.yuriy.openradio.shared.broadcast.AppLocalReceiverCallback;
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.shared.permission.PermissionChecker;
import com.yuriy.openradio.shared.presenter.MediaPresenter;
import com.yuriy.openradio.shared.presenter.MediaPresenterListener;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.utils.UiUtils;
import com.yuriy.openradio.shared.view.BaseDialogFragment;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.view.dialog.AddStationDialog;
import com.yuriy.openradio.shared.view.dialog.EqualizerDialog;
import com.yuriy.openradio.shared.view.dialog.GoogleDriveDialog;
import com.yuriy.openradio.shared.view.dialog.LogsDialog;
import com.yuriy.openradio.shared.view.list.MediaItemsAdapter;
import com.yuriy.openradio.shared.vo.RadioStation;
import com.yuriy.openradio.tv.view.dialog.TvSettingsDialog;
import com.yuriy.openradio.tv.view.list.TvMediaItemsAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final TvMediaItemsAdapter.Listener mListener;
    private View mPlayBtn;
    private View mPauseBtn;
    /**
     * Guardian field to prevent UI operation after addToLocals instance passed.
     */
    private final AtomicBoolean mIsOnSaveInstancePassed;
    /**
     * Member field to keep reference to the Local broadcast receiver.
     */
    private final LocalBroadcastReceiverCallback mLocalBroadcastReceiverCb;

    public TvMainActivity() {
        super();
        mListener = new TvMediaItemsAdapterListenerImpl();
        mLocalBroadcastReceiverCb = new LocalBroadcastReceiverCallback();
        mIsOnSaveInstancePassed = new AtomicBoolean(false);
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

        mIsOnSaveInstancePassed.set(false);

        // Register local receivers.
        mMediaPresenter.registerReceivers(getApplicationContext(), mLocalBroadcastReceiverCb);

        final MediaBrowserCompat.SubscriptionCallback subscriptionCb = new MediaBrowserSubscriptionCallback();
        final MediaPresenterListener listener = new MediaPresenterListenerImpl();
        mMediaPresenter.init(
                this, savedInstanceState, findViewById(R.id.tv_list_view),
                findViewById(R.id.tv_current_radio_station_view), new TvMediaItemsAdapter(context), mListener,
                subscriptionCb, listener
        );

        mMediaPresenter.restoreState(savedInstanceState);

        if (AppUtils.hasLocation(context)) {
            if (PermissionChecker.isLocationGranted(context)) {
                mMediaPresenter.connect();
                LocationService.doEnqueueWork(getApplicationContext());
            } else {
                PermissionChecker.requestLocationPermission(
                        this, findViewById(R.id.tv_main_layout), 1234
                );
            }
        } else {
            mMediaPresenter.connect();
        }
    }

    @Override
    protected final void onResume() {
        mIsOnSaveInstancePassed.set(false);
        super.onResume();

        // Hide any progress bar
        hideProgressBar();

        mMediaPresenter.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPresenter != null) {
            // Unregister local receivers
            mMediaPresenter.unregisterReceivers(getApplicationContext());
            mMediaPresenter.clean();
            mMediaPresenter.destroy();
        }
        ContextCompat.startForegroundService(
                getApplicationContext(),
                OpenRadioService.makeStopServiceIntent(getApplicationContext())
        );
    }

    @Override
    protected final void onSaveInstanceState(@NonNull final Bundle outState) {
        AppLogger.d(CLASS_NAME + "OnSaveInstance:" + outState);
        // Track OnSaveInstanceState passed
        mIsOnSaveInstancePassed.set(true);
        mMediaPresenter.handleSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppLogger.d(
                CLASS_NAME + " permissions:" + Arrays.toString(permissions)
                        + ", results:" + Arrays.toString(grantResults)
        );

        for (int i = 0; i < permissions.length; ++i) {
            final String permission = permissions[i];
            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                LocationService.doEnqueueWork(getApplicationContext());
            }
        }
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
        UiUtils.clearDialogs(this, transaction);
        // Show Settings Dialog
        final DialogFragment dialogFragment = BaseDialogFragment.newInstance(
                TvSettingsDialog.class.getName()
        );
        dialogFragment.show(transaction, TvSettingsDialog.DIALOG_TAG);
    }

    /**
     * Handles event of Metadata updated.
     * Updates UI related to the currently playing Radio Station.
     *
     * @param metadata Metadata related to currently playing Radio Station.
     */
    private void handleMetadataChanged(@NonNull final MediaMetadataCompat metadata) {
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
            descriptionView.setText(
                    MediaItemHelper.getDisplayDescription(description, getString(R.string.media_description_default))
            );
        }
        final ImageView imgView = findViewById(R.id.tv_crs_img_view);
        // Show placeholder before load an image.
        imgView.setImageResource(R.drawable.ic_radio_station);
        MobileMediaItemsAdapter.updateImage(description, imgView);
        MobileMediaItemsAdapter.updateBitrateView(
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

    private void handleChildrenLoaded(@NonNull final String parentId,
                                      @NonNull final List<MediaBrowserCompat.MediaItem> children) {
        if (mIsOnSaveInstancePassed.get()) {
            AppLogger.w(CLASS_NAME + "Can not perform on children loaded after OnSaveInstanceState");
            return;
        }
        hideProgressBar();
        if (mMediaPresenter != null) {
            mMediaPresenter.handleChildrenLoaded(parentId, children);
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
            TvMainActivity.this.handleChildrenLoaded(parentId, children);
        }

        @Override
        public void onError(@NonNull final String id) {
            SafeToast.showAnyThread(
                    getApplicationContext(),
                    getString(R.string.error_loading_media)
            );
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

    private final class TvMediaItemsAdapterListenerImpl implements MediaItemsAdapter.Listener {

        private TvMediaItemsAdapterListenerImpl() {
            super();
        }

        @Override
        public void onItemSettings(MediaBrowserCompat.MediaItem item, int position) {
            //TODO:
        }

        @Override
        public void onItemSelected(@NonNull final MediaBrowserCompat.MediaItem item, final int position) {
            if (TvMainActivity.this.mMediaPresenter == null) {
                return;
            }
            TvMainActivity.this.mMediaPresenter.setActiveItem(position);
            TvMainActivity.this.mMediaPresenter.handleItemClick(item, position);
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
        }

        @Override
        public void onCurrentIndexOnQueueChanged(final int index, final String mediaId) {
            if (TvMainActivity.this.mMediaPresenter != null) {
                TvMainActivity.this.mMediaPresenter.handleCurrentIndexOnQueueChanged(mediaId);
            }
        }
    }
}