package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.model.media.MediaResourcesManager;
import com.yuriy.openradio.presenter.CardPresenter;
import com.yuriy.openradio.presenter.GridItemPresenter;
import com.yuriy.openradio.presenter.IconHeaderItemPresenter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.ImageFetcher;
import com.yuriy.openradio.utils.ImageFetcherFactory;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;
import com.yuriy.openradio.view.activity.BrowseTvActivity;
import com.yuriy.openradio.view.activity.SearchTvActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public final class MainTvFragment extends BrowseSupportFragment {

    private static final String CLASS_NAME = MainTvFragment.class.getSimpleName() + " ";
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
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

    public MainTvFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This Adapter is used to render the MainFragment sidebar labels.
        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);

        // Every time we have to re-get the category loader, we must re-create the sidebar.
        mCategoryRowAdapter.clear();

        // Create header for this category.
        final HeaderItem header = new HeaderItem("Browse");
        // Handles loading the image in a background thread.
        final ImageFetcher imageFetcher = ImageFetcherFactory.getSmallImageFetcher(getActivity());

        mAdapter = new ArrayObjectAdapter(new CardPresenter(getContext(), imageFetcher));

        final ListRow row = new ListRow(header, mAdapter);
        mCategoryRowAdapter.add(row);


        // Create a row for this special case with more samples.
        HeaderItem gridHeader1 = new HeaderItem("Add");
        GridItemPresenter gridPresenter1 = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter2 = new ArrayObjectAdapter(gridPresenter1);
        gridRowAdapter2.add("Add Station");
        ListRow row3 = new ListRow(gridHeader1, gridRowAdapter2);
        mCategoryRowAdapter.add(row3);


        // Create a row for this special case with more samples.
        HeaderItem gridHeader = new HeaderItem("Settings");
        GridItemPresenter gridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add("General");
        gridRowAdapter.add("Streaming");
        gridRowAdapter.add("Google Drive");
        gridRowAdapter.add("Logs");
        gridRowAdapter.add("About");
        ListRow row2 = new ListRow(gridHeader, gridRowAdapter);
        mCategoryRowAdapter.add(row2);

        mMediaResourcesManager = new MediaResourcesManager(
                getActivity(),
                new MediaResourceManagerListenerImpl(this)
        );
        mMediaResourcesManager.create(null);
        mMediaResourcesManager.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mBackgroundTask);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Prepare the manager that maintains the same background image between activities.
//        prepareBackgroundManager();

        setupUiElements();
        setupEventListeners();
        prepareEntranceTransition();
        startEntranceTransition();
    }

    private void setupUiElements() {
        setBadgeDrawable(getActivity().getResources().getDrawable(R.drawable.ic_launcher, null));
        setTitle("TITLE"); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.primary_light_color));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.blue_light_color));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchTvActivity.class);
            startActivity(intent);
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
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
            if (item instanceof String) {
                if (((String) item).contains("Browse")) {
                    Intent intent = new Intent(getActivity(), BrowseTvActivity.class);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity()).toBundle();
                    startActivity(intent, bundle);
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        private ItemViewSelectedListener() {
            super();
        }

        @Override
        public void onItemSelected(final Presenter.ViewHolder itemViewHolder,
                                   final Object item,
                                   final RowPresenter.ViewHolder rowViewHolder,
                                   final Row row) {
            AppLogger.d(CLASS_NAME + "");
        }
    }

    private static final class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {

        /**
         * Weak reference to the outer activity.
         */
        private final WeakReference<MainTvFragment> mReference;

        /**
         * Constructor.
         *
         * @param reference Reference to the Activity.
         */
        private MediaBrowserSubscriptionCallback(final MainTvFragment reference) {
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

            final MainTvFragment fragment = mReference.get();
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

            final MainTvFragment fragment = mReference.get();
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
        private final WeakReference<MainTvFragment> mReference;

        /**
         * Constructor
         *
         * @param reference Reference to the Activity.
         */
        private MediaResourceManagerListenerImpl(final MainTvFragment reference) {
            super();
            mReference = new WeakReference<>(reference);
        }

        @Override
        public void onConnected(final List<MediaSessionCompat.QueueItem> queue) {
            final MainTvFragment activity = mReference.get();
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
            final MainTvFragment activity = mReference.get();
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
            final MainTvFragment activity = mReference.get();
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
