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
import androidx.leanback.widget.VerticalGridPresenter;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.model.media.MediaResourcesManager;
import com.yuriy.openradio.presenter.CardPresenter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;
import com.yuriy.openradio.view.activity.SearchTvActivity;

import java.lang.ref.WeakReference;
import java.util.List;

public final class MainTvFragment extends VerticalGridSupportFragment {

    private static final String CLASS_NAME = MainTvFragment.class.getSimpleName() + " ";
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

    public MainTvFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.app_name));

        if (savedInstanceState == null) {
            prepareEntranceTransition();
        }

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the main TV Fragment sidebar labels.
        mAdapter = new ArrayObjectAdapter(new CardPresenter());
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
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        // After 500ms, start the animation to transition the cards into view.
        new Handler().postDelayed(this::startEntranceTransition, 500);

        setOnSearchClickedListener(view -> {
            Intent intent = new Intent(getActivity(), SearchTvActivity.class);
            startActivity(intent);
        });

//        setOnItemViewClickedListener(new ItemViewClickedListener());
//        setOnItemViewSelectedListener(new ItemViewSelectedListener());
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

            fragment.mAdapter.add(new Object());
            fragment.mAdapter.add(new Object());
            fragment.mAdapter.add(new Object());
            fragment.mAdapter.add(new Object());
            fragment.mAdapter.add(new Object());
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
