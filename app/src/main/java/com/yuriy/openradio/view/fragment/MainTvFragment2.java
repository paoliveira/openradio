package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.leanback.app.PlaybackFragment;
import androidx.leanback.app.PlaybackFragmentGlueHost;
import androidx.leanback.media.PlaybackBannerControlGlue;
import androidx.leanback.widget.AbstractMediaItemPresenter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.RowPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.presenter.MediaPresenter;
import com.yuriy.openradio.presenter.MediaPresenterListener;
import com.yuriy.openradio.service.ServicePlayerTvAdapter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;

import java.lang.ref.WeakReference;
import java.util.List;

public class MainTvFragment2 extends PlaybackFragment {

    private static final String CLASS_NAME = MainTvFragment2.class.getSimpleName();
    /**
     * Listener for the Media Browser Subscription callback
     */
    private final MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback
            = new MediaBrowserSubscriptionCallback(this);

    private MediaPresenter mMediaPresenter;
    private ArrayObjectAdapter mRowsAdapter;
    private PlaybackBannerControlGlue<ServicePlayerTvAdapter> mGlue;

    public MainTvFragment2() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setControlsOverlayAutoHideEnabled(false);
        mGlue = new PlaybackBannerControlGlue<>(
                getActivity(),
                new int[]{0, 1},
                new ServicePlayerTvAdapter()
        );
        mGlue.setHost(new PlaybackFragmentGlueHost(this));

//        mMediaPlayerGlue.setArt(getResources().getDrawable(R.drawable.ic_launcher));
        String uriPath = "android.resource://com.example.android.leanback/raw/video";
//        mMediaPlayerGlue.getPlayerAdapter().setDataSource(Uri.parse(uriPath));
//        mMediaPlayerGlue.playWhenPrepared();

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

        setUpAdapter();
        setOnItemViewClickedListener(this::onItemClicked);

        mMediaPresenter.restoreState(savedInstanceState);
        mMediaPresenter.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        showControlsOverlay(true);
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

    @Override
    public void setControlsOverlayAutoHideEnabled(boolean enabled) {
        super.setControlsOverlayAutoHideEnabled(false);
    }

    private void setUpAdapter() {
        final ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenterSelector(
                MediaBrowserCompat.MediaItem.class,
                new SongPresenterSelector()
                        .setSongPresenterRegular(
                                new SongPresenter(getActivity(), R.style.radio_station_regular)
                        )
                        .setSongPresenterFavorite(
                                new SongPresenter(getActivity(), R.style.radio_station_selected)
                        )
        );
        mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
        setAdapter(mRowsAdapter);
    }

    private void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                               final Object item,
                               final RowPresenter.ViewHolder rowViewHolder,
                               final Object row) {
        if (row instanceof MediaBrowserCompat.MediaItem) {
            final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) row;
            // TODO: Real position
            final int position = 0;
            mMediaPresenter.handleItemClick(mediaItem, position);
        }
    }

    private static class SongPresenter extends AbstractMediaItemPresenter {

        private SongPresenter(final Context context, final int themeResId) {
            super(themeResId);
            setHasMediaRowSeparator(true);
        }

        @Override
        protected void onBindMediaDetails(ViewHolder viewHolder, Object item) {
            int favoriteTextColor = viewHolder.view.getContext().getResources().getColor(
                    R.color.song_row_favorite_color
            );
            MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem) item;
            viewHolder.getMediaItemNumberView().setText(mediaItem.getMediaId());

            String songTitle = mediaItem.getDescription().getTitle() + " / " + mediaItem.getDescription().getSubtitle();
            viewHolder.getMediaItemNameView().setText(songTitle);

            viewHolder.getMediaItemDurationView().setText("");

//            if (mediaItem.isFavorite()) {
//                viewHolder.getMediaItemNumberView().setTextColor(favoriteTextColor);
//                viewHolder.getMediaItemNameView().setTextColor(favoriteTextColor);
//                viewHolder.getMediaItemDurationView().setTextColor(favoriteTextColor);
//            } else {
            Context context = viewHolder.getMediaItemNumberView().getContext();
            viewHolder.getMediaItemNumberView().setTextAppearance(context,
                    R.style.TextAppearance_Leanback_PlaybackMediaItemNumber);
            viewHolder.getMediaItemNameView().setTextAppearance(context,
                    R.style.TextAppearance_Leanback_PlaybackMediaItemName);
            viewHolder.getMediaItemDurationView().setTextAppearance(context,
                    R.style.TextAppearance_Leanback_PlaybackMediaItemDuration);
//            }
        }
    }

    private static class SongPresenterSelector extends PresenterSelector {

        private Presenter mRegularPresenter;
        private Presenter mFavoritePresenter;

        private SongPresenterSelector() {
            super();
        }

        /**
         * Adds a presenter to be used for the given class.
         */
        private SongPresenterSelector setSongPresenterRegular(final Presenter presenter) {
            mRegularPresenter = presenter;
            return this;
        }

        /**
         * Adds a presenter to be used for the given class.
         */
        private SongPresenterSelector setSongPresenterFavorite(final Presenter presenter) {
            mFavoritePresenter = presenter;
            return this;
        }

        @Override
        public Presenter[] getPresenters() {
            return new Presenter[]{mRegularPresenter, mFavoritePresenter};
        }

        @Override
        public Presenter getPresenter(final Object item) {
//            return ((MediaBrowserCompat.MediaItem) item).isFavorite() ? mFavoritePresenter : mRegularPresenter;
            return mRegularPresenter;
        }
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MainTvFragment2> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final MainTvFragment2 reference) {
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

            final MainTvFragment2 fragment = mReference.get();
            if (fragment == null) {
                AppLogger.w(CLASS_NAME + " On children loaded -> fragment ref is null");
                return;
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            Object item;
            for (int i = 0; i < fragment.mRowsAdapter.size(); ++i) {
                item = fragment.mRowsAdapter.get(i);
                if (item instanceof MediaBrowserCompat.MediaItem) {
                    fragment.mRowsAdapter.remove(item);
                    i--;
                }
            }
            fragment.mRowsAdapter.addAll(fragment.mRowsAdapter.size(), children);
        }

        @Override
        public void onError(@NonNull final String id) {
            final MainTvFragment2 fragment = mReference.get();
            if (fragment == null) {
                return;
            }
            SafeToast.showAnyThread(
                    fragment.getActivity().getApplicationContext(),
                    fragment.getString(R.string.error_loading_media)
            );
        }
    }
}
