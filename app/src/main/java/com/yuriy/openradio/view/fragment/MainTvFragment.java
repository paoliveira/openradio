package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.yuriy.openradio.R;
import com.yuriy.openradio.model.media.MediaResourceManagerListener;
import com.yuriy.openradio.model.media.MediaResourcesManager;
import com.yuriy.openradio.presenter.IconHeaderItemPresenter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;

import java.lang.ref.WeakReference;
import java.util.List;

public final class MainTvFragment extends BrowseSupportFragment {

    private static final String CLASS_NAME = MainTvFragment.class.getSimpleName() + " ";
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private ArrayObjectAdapter mCategoryRowAdapter;
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
    public void onAttach(@NonNull final Context context) {
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUiElements();

        prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the main TV Fragment sidebar labels.
        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);

        mMediaResourcesManager = new MediaResourcesManager(
                getActivity(),
                new MediaResourceManagerListenerImpl(this)
        );
        mMediaResourcesManager.create(null);
        //mMediaResourcesManager.subscribe(mMediaResourcesManager.getRoot(), mMedSubscriptionCallback);
        mMediaResourcesManager.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Disconnect Media Browser
        mMediaResourcesManager.disconnect();
    }

    private void setupUiElements() {
        setBadgeDrawable(
                getActivity().getResources().getDrawable(R.drawable.ic_launcher, null)
        );
        setTitle("Title is here"); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.accent_color));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.blue_light_color));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background_tv, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(this)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private final class UpdateBackgroundTask implements Runnable {

        private UpdateBackgroundTask() {
            super();
        }

        @Override
        public void run() {
            if (MainTvFragment.this.mBackgroundURI == null) {
                return;
            }
            MainTvFragment.this.updateBackground(MainTvFragment.this.mBackgroundURI.toString());
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
            AppLogger.d(CLASS_NAME + "onConnected");
            final MainTvFragment activity = mReference.get();
            if (activity == null) {
                AppLogger.w(CLASS_NAME + "onConnected reference to MainActivity is null");
                return;
            }
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
