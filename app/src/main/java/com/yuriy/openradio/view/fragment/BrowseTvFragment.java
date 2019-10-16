package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.leanback.app.VerticalGridSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.model.media.MediaResourcesManager;
import com.yuriy.openradio.presenter.CardPresenter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.ImageFetcherFactory;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;
import com.yuriy.openradio.view.activity.SearchTvActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public final class BrowseTvFragment extends VerticalGridSupportFragment {

    private static final String CLASS_NAME = BrowseTvFragment.class.getSimpleName() + " ";
    private static final int NUM_COLUMNS = 3;
    private ArrayObjectAdapter mAdapter;
    /**
     * Listener for the Media Browser Subscription callback
     */
    private final MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback
            = new MediaBrowserSubscriptionCallback(this);
    /**
     * Manager object that acts as interface between Media Resources and current Activity.
     */
    private MediaResourcesManager mMediaResourcesManager;

    public BrowseTvFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }

        // Handles loading the  image in a background thread.
        final ImageFetcher imageFetcher = ImageFetcherFactory.getSmallImageFetcher(getActivity());

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the main TV Fragment sidebar labels.
        mAdapter = new ArrayObjectAdapter(new CardPresenter(getContext(), imageFetcher));
        setAdapter(mAdapter);

        mMediaResourcesManager = new MediaResourcesManager(
                getActivity(),
                new MediaResourceManagerListenerImpl(this)
        );
        mMediaResourcesManager.create(null);
        mMediaResourcesManager.connect();

        setupFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Disconnect Media Browser
        mMediaResourcesManager.disconnect();
    }

    private void setupFragment() {
        final VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        // After 500ms, start the animation to transition the cards into view.
        new Handler().postDelayed(this::startEntranceTransition, 500);

        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchTvActivity.class);
            startActivity(intent);
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        public ItemViewSelectedListener() {
            super();
        }

        @Override
        public void onItemSelected(final Presenter.ViewHolder itemViewHolder,
                                   final Object item,
                                   final RowPresenter.ViewHolder rowViewHolder,
                                   final Row row) {
            final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem)item;
            if (mediaItem == null) {
                return;
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        private ItemViewClickedListener() {
            super();
        }

        @Override
        public void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                                  final Object item,
                                  final RowPresenter.ViewHolder rowViewHolder,
                                  final Row row) {
            final MediaBrowserCompat.MediaItem mediaItem = (MediaBrowserCompat.MediaItem)item;
            if (mediaItem == null) {
                return;
            }
//                final Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
//                intent.putExtra(VideoDetailsActivity.VIDEO, video);
//
//                final Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
//                        getActivity(),
//                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
//                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
//                getActivity().startActivity(intent, bundle);
        }
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<BrowseTvFragment> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final BrowseTvFragment reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onChildrenLoaded(@NonNull final String parentId,
                                     @NonNull final List<MediaBrowserCompat.MediaItem> children) {
            AppLogger.i(
                    CLASS_NAME + "Children loaded:" + parentId + ", children:" + children.size()
            );

            final BrowseTvFragment fragment = mReference.get();
            if (fragment == null) {
                AppLogger.w(CLASS_NAME + "On children loaded -> fragment ref is null");
                return;
            }

            // No need to go on if indexed list ended with last item.
            if (MediaItemHelper.isEndOfList(children)) {
                return;
            }

            fragment.mAdapter.clear();
            fragment.mAdapter.addAll(0, children);
        }

        @Override
        public void onError(@NonNull final String id) {

            final BrowseTvFragment fragment = mReference.get();
            if (fragment == null) {
                return;
            }
            SafeToast.showAnyThread(
                    fragment.getContext(), fragment.getString(R.string.error_loading_media)
            );
        }
    }

    /**
     * Listener for the Media Resources related events.
     */
    private static final class MediaResourceManagerListenerImpl implements MediaResourceManagerListener {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<BrowseTvFragment> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaResourceManagerListenerImpl(final BrowseTvFragment reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(final List<MediaSessionCompat.QueueItem> queue) {
            final BrowseTvFragment activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onConnected reference to MainActivity is null");
                return;
            }

            activity.mMediaResourcesManager.subscribe(
                    activity.mMediaResourcesManager.getRoot(),
                    activity.mMedSubscriptionCallback
            );
        }

        @Override
        public void onPlaybackStateChanged(@NonNull final PlaybackStateCompat state) {
            AppLogger.d(CLASS_NAME + "PlaybackStateChanged:" + state);
            final BrowseTvFragment activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onPlaybackStateChanged reference to MainActivity is null");
                return;
            }
        }

        @Override
        public void onQueueChanged(final List<MediaSessionCompat.QueueItem> queue) {
            AppLogger.d(CLASS_NAME + "Queue changed to:" + queue);
        }

        @Override
        public void onMetadataChanged(final MediaMetadataCompat metadata,
                                      final List<MediaSessionCompat.QueueItem> queue) {
            final BrowseTvFragment activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onMetadataChanged reference to MainActivity is null");
                return;
            }
            if (metadata == null) {
                return;
            }
        }
    }
}
