package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.VerticalGridPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.presenter.CardPresenter;
import com.yuriy.openradio.presenter.MediaPresenter;
import com.yuriy.openradio.presenter.MediaPresenterListener;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;

import java.lang.ref.WeakReference;
import java.util.List;

public final class RadioStationsTvFragment extends GridTvFragment {

    private static final String CLASS_NAME = RadioStationsTvFragment.class.getSimpleName();
    private static final int COLUMNS = 4;
    private ArrayObjectAdapter mAdapter;
    /**
     * Listener for the Media Browser Subscription callback
     */
    private final MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback
            = new MediaBrowserSubscriptionCallback(this);

    private MediaPresenter mMediaPresenter;

    public RadioStationsTvFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupAdapter();

        mMediaPresenter = new MediaPresenter();
        mMediaPresenter.init(
                getActivity(),
                savedInstanceState,
                mMedSubscriptionCallback,
                new MediaPresenterListener() {

                    @Override
                    public void showProgressBar() {

                    }

                    @Override
                    public void handleMetadataChanged(final MediaMetadataCompat metadata) {

                    }

                    @Override
                    public void handlePlaybackStateChanged(final PlaybackStateCompat state) {

                    }
                }
        );

        mMediaPresenter.restoreState(savedInstanceState);

        getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());

        mMediaPresenter.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaPresenter.destroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        mMediaPresenter.saveState(outState);
    }

    private void setupAdapter() {
        final VerticalGridPresenter presenter = new VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_SMALL);
        presenter.setNumberOfColumns(COLUMNS);
        setGridPresenter(presenter);

        final CardPresenter cardPresenter = new CardPresenter(getActivity(), null);
        mAdapter = new ArrayObjectAdapter(cardPresenter);
        setAdapter(mAdapter);

        setOnItemViewClickedListener(
                (itemViewHolder, item, rowViewHolder, row) -> {
                    final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
                    if (mediaItem == null) {
                        return;
                    }
                    //TODO: Get real position id
                    int position = 0;
                    mMediaPresenter.handleItemClick(mediaItem, position);
                    if (mMediaPresenter.getNumItemsInStack() > 1) {
                        showBackButton();
                    }
                }
        );

        setOnBackClickListener(
                v -> {
                    mMediaPresenter.handleBackPressed(getActivity());
                    if (mMediaPresenter.getNumItemsInStack() <= 1) {
                        hideBackButton();
                    }
                }
        );
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<RadioStationsTvFragment> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final RadioStationsTvFragment reference) {
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

            final RadioStationsTvFragment fragment = mReference.get();
            if (fragment == null) {
                AppLogger.w(CLASS_NAME + " On children loaded -> fragment ref is null");
                return;
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            fragment.mAdapter.clear();
            fragment.mAdapter.addAll(0, children);
            fragment.onDataLoaded();
        }

        @Override
        public void onError(@NonNull final String id) {

            final RadioStationsTvFragment fragment = mReference.get();
            if (fragment == null) {
                return;
            }
            SafeToast.showAnyThread(
                    fragment.getContext(), fragment.getString(R.string.error_loading_media)
            );
        }
    }
}
