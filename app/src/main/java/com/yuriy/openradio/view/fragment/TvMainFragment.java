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

package com.yuriy.openradio.view.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.PlaybackSupportFragment;
import androidx.leanback.app.PlaybackSupportFragmentGlueHost;
import androidx.leanback.media.PlaybackBannerControlGlue;
import androidx.leanback.widget.AbstractMediaItemPresenter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.MultiActionsProvider;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.RowPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.service.TvServicePlayerAdapter;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.permission.PermissionChecker;
import com.yuriy.openradio.shared.permission.PermissionListener;
import com.yuriy.openradio.shared.permission.PermissionStatusListener;
import com.yuriy.openradio.shared.presenter.MediaPresenter;
import com.yuriy.openradio.shared.presenter.MediaPresenterListener;
import com.yuriy.openradio.shared.service.LocationService;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.BitmapUtils;
import com.yuriy.openradio.shared.utils.ImageFetcherFactory;
import com.yuriy.openradio.shared.utils.ImageWorker;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.utils.WrappedDrawable;
import com.yuriy.openradio.shared.view.SafeToast;
import com.yuriy.openradio.shared.view.activity.PermissionsDialogActivity;
import com.yuriy.openradio.shared.vo.Country;
import com.yuriy.openradio.view.activity.TvMainActivity;
import com.yuriy.openradio.vo.MediaItemActionable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TvMainFragment extends PlaybackSupportFragment {

    private static final String CLASS_NAME = TvMainFragment.class.getSimpleName();
    private static final int PLAYLIST_ACTION_ID = 0;
    private static final int FAVORITE_ACTION_ID = 1;
    /**
     * Listener for the Media Browser Subscription callback
     */
    private MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback;

    private MediaPresenter mMediaPresenter;
    private ArrayObjectAdapter mRowsAdapter;
    private PlaybackBannerControlGlue<TvServicePlayerAdapter> mGlue;
    private ImageView mDummyView;
    /**
     * Handles loading the  image in a background thread.
     */
    private ImageWorker mImageWorker;
    private String mCurrentMediaId;
    /**
     * ID of the parent of current item (whether it is directory or Radio Station).
     */
    private String mCurrentParentId = "";
    private int mCurrentSelectedPosition;

    /**
     * Stores an instance of {@link LocationHandler} that inherits from
     * Handler and uses its handleMessage() hook method to process
     * Messages sent to it from the {@link LocationService}.
     */
    private LocationHandler mLocationHandler = null;
    /**
     * Listener of the Permissions status changes.
     */
    private PermissionStatusListener mPermissionStatusListener;

    public TvMainFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setControlsOverlayAutoHideEnabled(false);
        setBackgroundType(BG_NONE);
        final Context context = getContext();
        mGlue = new PlaybackBannerControlGlue<>(
                context,
                new int[]{0, 1},
                new TvServicePlayerAdapter(context)
        );
        mGlue.setHost(new PlaybackSupportFragmentGlueHost(this));

        mMedSubscriptionCallback = new MediaBrowserSubscriptionCallback(this);
        mDummyView = new ImageView(context);
        // Handles loading the  image in a background thread
        mImageWorker = ImageFetcherFactory.getTvPlayerImageFetcher(getActivity());

        mMediaPresenter = new MediaPresenter();
        mMediaPresenter.init(
                getActivity(),
                savedInstanceState,
                mMedSubscriptionCallback,
                new MediaPresenterListener() {

                    @Override
                    public void showProgressBar() {
                        TvMainFragment.this.showProgressBar();
                    }

                    @Override
                    public void handleMetadataChanged(final MediaMetadataCompat metadata) {
                        TvMainFragment.this.handleMetadataChanged(metadata);
                    }

                    @Override
                    public void handlePlaybackStateChanged(final PlaybackStateCompat state) {
                        TvMainFragment.this.handlePlaybackStateChanged(state);
                    }
                }
        );

        setUpAdapter();
        setOnItemViewClickedListener(this::onItemClicked);
        setOnItemViewSelectedListener(this::onItemSelected);

        mMediaPresenter.restoreState(savedInstanceState);

        if (getActivity() != null && PermissionsDialogActivity.isLocationDenied(getActivity().getIntent())) {
            mMediaPresenter.connect();
            return;
        }

        mPermissionStatusListener = new PermissionListener(getContext());
        // Add listener for the permissions status
        PermissionChecker.addPermissionStatusListener(mPermissionStatusListener);
        final boolean isLocationPermissionGranted = PermissionChecker.isGranted(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        if (isLocationPermissionGranted) {
            // Initialize the Location Handler.
            mLocationHandler = new LocationHandler(this);
            LocationService.doEnqueueWork(context, mLocationHandler);
        } else {
            mMediaPresenter.connect();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        showControlsOverlay(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mLocationHandler != null) {
            mLocationHandler.removeCallbacksAndMessages(null);
            mLocationHandler.clear();
            mLocationHandler = null;
        }

        PermissionChecker.removePermissionStatusListener(mPermissionStatusListener);

        mMediaPresenter.destroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        mMediaPresenter.saveState(outState);
    }

    @Override
    public void setControlsOverlayAutoHideEnabled(boolean enabled) {
        super.setControlsOverlayAutoHideEnabled(false);
    }

    public void handleBackButton() {
        if (mMediaPresenter == null) {
            return;
        }
        mMediaPresenter.handleBackPressed(getActivity());
    }

    public int getNumItemsInStack() {
        if (mMediaPresenter == null) {
            return 0;
        }
        return mMediaPresenter.getNumItemsInStack();
    }

    public void onSearchDialogClick() {
        mMediaPresenter.unsubscribeFromItem(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
        mMediaPresenter.addMediaItemToStack(MediaIdHelper.MEDIA_ID_SEARCH_FROM_APP);
    }

    /**
     * Show progress bar.
     */
    private void showProgressBar() {
        if (!(getActivity() instanceof TvMainActivity)) {
            return;
        }
        ((TvMainActivity) getActivity()).showProgressBar();
    }

    /**
     * Hide progress bar.
     */
    private void hideProgressBar() {
        if (!(getActivity() instanceof TvMainActivity)) {
            return;
        }
        ((TvMainActivity) getActivity()).hideProgressBar();
    }

    private void handlePlaybackStateChanged(final PlaybackStateCompat state) {
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            if (!mGlue.isPlaying()) {
                mGlue.play();
            }
        }
        final long bufferedDuration = ((state.getBufferedPosition() - state.getPosition()));
        mGlue.getControlsRow().setCurrentPosition(bufferedDuration);
    }

    private void handleMetadataChanged(@Nullable final MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        final MediaDescriptionCompat description = metadata.getDescription();

        if (TextUtils.equals(mCurrentMediaId, description.getMediaId())) {
            // New Radio Station, reset fields
            mGlue.setArt(null);
        }
        mCurrentMediaId = description.getMediaId();

        mGlue.setTitle(description.getTitle());
        mGlue.setSubtitle(description.getSubtitle());

        mImageWorker.loadImage(
                description.getIconUri(),
                drawable -> mGlue.setArt(drawable),
                mDummyView
        );
    }

    private void setUpAdapter() {
        final ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenterSelector(
                MediaItemActionable.class,
                new RSPresenterSelector(getContext())
                        .setSongPresenterRegular(
                                new RSPresenter(getContext(), R.style.radio_station_regular)
                        )
                        .setSongPresenterFavorite(
                                new RSPresenter(getContext(), R.style.radio_station_selected)
                        )
        );
        mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mRowsAdapter);
    }

    private void onItemSelected(final Presenter.ViewHolder itemViewHolder,
                                final Object item,
                                final RowPresenter.ViewHolder rowViewHolder,
                                final Object row) {
        AppLogger.d(CLASS_NAME + " ItemSelected:" + row);
        if (row instanceof MediaItemActionable) {
            final MediaItemActionable actionable = (MediaItemActionable) row;
            mCurrentSelectedPosition = actionable.getListIndex();
            mMediaPresenter.handleItemSelect(actionable, mCurrentSelectedPosition);
            // Minus two - one is for playback row and one is for zero based list
            if (actionable.getListIndex() == mRowsAdapter.size() - 2) {
                onScrolledToEnd();
            }
        }
    }

    private void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                               final Object item,
                               final RowPresenter.ViewHolder rowViewHolder,
                               final Object row) {
        if (row instanceof MediaItemActionable) {
            final MediaItemActionable actionable = (MediaItemActionable) row;
            if (actionable.isBrowsable()) {
                showProgressBar();
            }
            handleActionableClicked(
                    actionable,
                    (AbstractMediaItemPresenter.ViewHolder) rowViewHolder,
                    (MultiActionsProvider.MultiAction) item
            );
        }
    }

    private void handleActionableClicked(final MediaItemActionable mediaItem,
                                         final AbstractMediaItemPresenter.ViewHolder rowViewHolder,
                                         final MultiActionsProvider.MultiAction item) {
        if (item != null && item.getId() == FAVORITE_ACTION_ID) {

            item.incrementIndex();

            mediaItem.setFavorite(!mediaItem.isFavorite());

            rowViewHolder.notifyDetailsChanged();
            rowViewHolder.notifyActionChanged(item);

            final boolean isChecked = mediaItem.isFavorite();
            MediaItemHelper.updateFavoriteField(mediaItem, isChecked);

            // Make Intent to update Favorite RadioStation object associated with
            // the Media Description
            final Intent intent = OpenRadioService.makeUpdateIsFavoriteIntent(
                    getContext(),
                    mediaItem.getDescription(),
                    isChecked
            );
            // Send Intent to the OpenRadioService.
            ContextCompat.startForegroundService(getContext(), intent);
        } else {
            mMediaPresenter.handleItemClick(mediaItem, mediaItem.getListIndex());
        }
    }

    private void onScrolledToEnd() {
        if (MediaIdHelper.isMediaIdRefreshable(mCurrentParentId)) {
            mMediaPresenter.unsubscribeFromItem(mCurrentParentId);
            mMediaPresenter.addMediaItemToStack(mCurrentParentId);
        } else {
            AppLogger.w(CLASS_NAME + "Category " + mCurrentParentId + " is not refreshable");
        }
    }

    private static class RSPresenter extends AbstractMediaItemPresenter {

        private RSPresenter(final Context context, final int themeResId) {
            super(themeResId);
            setHasMediaRowSeparator(true);
        }

        @Override
        protected void onBindMediaDetails(final ViewHolder viewHolder, final Object item) {
            final MediaItemActionable mediaItem = (MediaItemActionable) item;
            final String rsTitle =
                    TextUtils.isEmpty(mediaItem.getDescription().getSubtitle())
                            ? "" + mediaItem.getDescription().getTitle()
                            : mediaItem.getDescription().getTitle() + " / " + mediaItem.getDescription().getSubtitle();
            viewHolder.getMediaItemNameView().setText(rsTitle);

            if (mediaItem.isFavorite()) {
                int favoriteTextColor = viewHolder.view.getContext().getResources().getColor(
                        R.color.favorite_color
                );
                viewHolder.getMediaItemNumberView().setTextColor(favoriteTextColor);
                viewHolder.getMediaItemNameView().setTextColor(favoriteTextColor);
                final MultiActionsProvider.MultiAction action = mediaItem.getActions()[0];
                if (action.getIndex() == 0) {
                    action.incrementIndex();
                }
                viewHolder.notifyActionChanged(action);
            } else {
                final Context context = viewHolder.getMediaItemNumberView().getContext();
                viewHolder.getMediaItemNumberView().setTextAppearance(context,
                        R.style.TextAppearance_Leanback_PlaybackMediaItemNumber);
                viewHolder.getMediaItemNameView().setTextAppearance(context,
                        R.style.TextAppearance_Leanback_PlaybackMediaItemName);
            }
        }
    }

    private static class RSPresenterSelector extends PresenterSelector {

        private Presenter mRegularPresenter;
        private Presenter mFavoritePresenter;
        private final Context mContext;

        private RSPresenterSelector(final Context context) {
            super();
            mContext = context;
        }

        /**
         * Adds a presenter to be used for the given class.
         */
        private RSPresenterSelector setSongPresenterRegular(final Presenter presenter) {
            mRegularPresenter = presenter;
            return this;
        }

        /**
         * Adds a presenter to be used for the given class.
         */
        private RSPresenterSelector setSongPresenterFavorite(final Presenter presenter) {
            mFavoritePresenter = presenter;
            return this;
        }

        @Override
        public Presenter[] getPresenters() {
            return new Presenter[]{mRegularPresenter, mFavoritePresenter};
        }

        @Override
        public Presenter getPresenter(final Object item) {
            final MediaItemActionable mediaItem = (MediaItemActionable) item;

            // There is only one action currently.
            final MultiActionsProvider.MultiAction action = mediaItem.getActions()[0];
            final float density = mContext.getResources().getDisplayMetrics().density;
            if (mediaItem.getDescription().getIconBitmap() != null) {
                final Drawable drawable = new BitmapDrawable(
                        mContext.getResources(),
                        BitmapUtils.scaleDown(
                                mediaItem.getDescription().getIconBitmap(), 24 * density, true
                        )
                );
                action.getDrawables()[0] = drawable;
                action.getDrawables()[1] = drawable;
            } else {
                if (mediaItem.isPlayable()) {
                    action.getDrawables()[0] = mContext.getResources().getDrawable(
                            R.drawable.tv_ic_favorites_off_24,
                            mContext.getTheme()
                    );
                    action.getDrawables()[1] = mContext.getResources().getDrawable(
                            R.drawable.tv_ic_favorites_on_24,
                            mContext.getTheme()
                    );
                } else {
                    final WrappedDrawable drawable = new WrappedDrawable(
                            BitmapUtils.drawableFromUri(
                                    mContext, mediaItem.getDescription().getIconUri()
                            )
                    );
                    drawable.setBounds(0, 0, (int) (24 * density), (int) (24 * density));
                    action.getDrawables()[0] = drawable;
                    action.getDrawables()[1] = drawable;
                }
            }

            return mediaItem.isFavorite() ? mFavoritePresenter : mRegularPresenter;
        }
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<TvMainFragment> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final TvMainFragment reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onChildrenLoaded(@NonNull final String parentId,
                                     @NonNull final List<MediaBrowserCompat.MediaItem> children) {
            AppLogger.i(
                    CLASS_NAME + " Children loaded:" + parentId + ", children:" + children.size()
            );

            final TvMainFragment fragment = mReference.get();
            if (fragment == null) {
                AppLogger.w(CLASS_NAME + " On children loaded -> fragment ref is null");
                return;
            }

            fragment.hideProgressBar();

            fragment.mCurrentParentId = parentId;

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            {
                Object item;
                for (int i = 0; i < fragment.mRowsAdapter.size(); ++i) {
                    item = fragment.mRowsAdapter.get(i);
                    if (item instanceof MediaItemActionable) {
                        fragment.mRowsAdapter.remove(item);
                        i--;
                    }
                }
            }

            int counter = 0;
            final List<MediaItemActionable> items = new ArrayList<>();
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                final MediaItemActionable item = new MediaItemActionable(
                        mediaItem.getDescription(), mediaItem.getFlags(), counter++
                );

                final Drawable[] drawables = new Drawable[2];
                final MultiActionsProvider.MultiAction action = new
                        MultiActionsProvider.MultiAction(FAVORITE_ACTION_ID);
                action.setDrawables(drawables);

                final MultiActionsProvider.MultiAction[] mediaRowActions = new
                        MultiActionsProvider.MultiAction[]{action};

                // TODO: Set action - folder or stream
                item.setMediaRowActions(mediaRowActions);
                // TODO: Optimize this line
                item.setFavorite(FavoritesStorage.isFavorite(mediaItem, fragment.getContext()));

                items.add(item);
            }

            fragment.mRowsAdapter.addAll(fragment.mRowsAdapter.size(), items);

            if (fragment.getActivity() instanceof TvMainActivity) {
                ((TvMainActivity) fragment.getActivity()).onDataLoaded();
            }

            fragment.restoreSelectedPosition();
        }

        @Override
        public void onError(@NonNull final String id) {
            final TvMainFragment fragment = mReference.get();
            if (fragment == null) {
                return;
            }
            SafeToast.showAnyThread(
                    fragment.getContext(),
                    fragment.getString(R.string.error_loading_media)
            );
        }
    }

    private void restoreSelectedPosition() {
        // Restore positions for the Catalogue list.
        final int[] positions = mMediaPresenter.getPositions(mCurrentParentId);
        int selectedPosition = positions[0];
        // TODO: Make default value 1 for TV and improve this method (see mobile version)
        if (selectedPosition < 1) {
            selectedPosition = 1;
        }
        AppLogger.d(CLASS_NAME + " set selected:" + selectedPosition);
        setSelectedPosition(selectedPosition);
    }

    /**
     * A nested class that inherits from Handler and uses its
     * handleMessage() hook method to process Messages sent to
     * it from the Location Service.
     */
    private static final class LocationHandler extends Handler {
        /**
         * Allows {@link TvMainFragment} to be garbage collected properly.
         */
        private WeakReference<TvMainFragment> mActivity;

        /**
         * Tag string to use in logging message.
         */
        private final String CLASS_NAME;

        /**
         * Class constructor constructs mActivity as weak reference to
         * the {@link TvMainFragment}.
         *
         * @param activity The corresponding activity.
         */
        private LocationHandler(final TvMainFragment activity) {
            super();
            CLASS_NAME = activity.getClass().getSimpleName()
                    + " " + LocationHandler.class.getSimpleName()
                    + " " + activity.hashCode()
                    + " " + hashCode() + " ";
            mActivity = new WeakReference<>(activity);
        }

        private void clear() {
            if (mActivity != null) {
                mActivity.clear();
            }
            mActivity = null;
        }

        /**
         * This hook method is dispatched in response to receiving the
         * Location back from the {@link LocationService}.
         */
        @Override
        public void handleMessage(final Message message) {
            // Bail out if the {@link MainActivity} is gone.
            if (mActivity == null) {
                AppLogger.e(CLASS_NAME + "enclosing weak reference null");
                return;
            }
            if (mActivity.get() == null) {
                AppLogger.e(CLASS_NAME + "enclosing activity null");
                return;
            }

            // Try to extract the location from the message.
            String countryCode = LocationService.getCountryCode(message);
            // See if the get Location worked or not.
            if (countryCode == null) {
                countryCode = Country.COUNTRY_CODE_DEFAULT;
            }
            if (mActivity.get().mMediaPresenter != null) {
                mActivity.get().mMediaPresenter.connect();
            }
        }
    }
}
