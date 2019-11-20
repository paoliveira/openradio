package com.yuriy.openradio.view.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
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
import com.yuriy.openradio.model.storage.FavoritesStorage;
import com.yuriy.openradio.presenter.MediaPresenter;
import com.yuriy.openradio.presenter.MediaPresenterListener;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.service.ServicePlayerTvAdapter;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.BitmapUtils;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.view.SafeToast;
import com.yuriy.openradio.view.activity.MainTvActivity;
import com.yuriy.openradio.vo.MediaItemActionable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainTvFragment extends PlaybackSupportFragment {

    private static final String CLASS_NAME = MainTvFragment.class.getSimpleName();
    private static final int PLAYLIST_ACTION_ID = 0;
    private static final int FAVORITE_ACTION_ID = 1;
    /**
     * Listener for the Media Browser Subscription callback
     */
    private final MediaBrowserCompat.SubscriptionCallback mMedSubscriptionCallback
            = new MediaBrowserSubscriptionCallback(this);

    private MediaPresenter mMediaPresenter;
    private ArrayObjectAdapter mRowsAdapter;
    private PlaybackBannerControlGlue<ServicePlayerTvAdapter> mGlue;

    public MainTvFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setControlsOverlayAutoHideEnabled(false);
        setBackgroundType(BG_NONE);
        mGlue = new PlaybackBannerControlGlue<>(
                getContext(),
                new int[]{0, 1},
                new ServicePlayerTvAdapter()
        );
        mGlue.setHost(new PlaybackSupportFragmentGlueHost(this));

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

    private void onItemClicked(final Presenter.ViewHolder itemViewHolder,
                               final Object item,
                               final RowPresenter.ViewHolder rowViewHolder,
                               final Object row) {
        if (row instanceof MediaItemActionable) {
            final MediaItemActionable mediaItem = (MediaItemActionable) row;
            if (item != null
                    && ((MultiActionsProvider.MultiAction) item).getId() == FAVORITE_ACTION_ID) {
                final MultiActionsProvider.MultiAction action =
                        (MultiActionsProvider.MultiAction) item;
                action.incrementIndex();

                final AbstractMediaItemPresenter.ViewHolder rasRowVh =
                        (AbstractMediaItemPresenter.ViewHolder) rowViewHolder;

                mediaItem.setFavorite(!mediaItem.isFavorite());
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

                rasRowVh.notifyActionChanged(action);
                rasRowVh.notifyDetailsChanged();
            } else {
                // TODO: Real position
                final int position = 0;
                mMediaPresenter.handleItemClick(mediaItem, position);
            }

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
                        R.color.song_row_favorite_color
                );
                viewHolder.getMediaItemNumberView().setTextColor(favoriteTextColor);
                viewHolder.getMediaItemNameView().setTextColor(favoriteTextColor);
                final MultiActionsProvider.MultiAction action = mediaItem.getActions()[0];
                action.incrementIndex();
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

        private RSPresenterSelector(final Context context) {
            super();
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
            return mediaItem.isFavorite() ? mFavoritePresenter : mRegularPresenter;
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
                    CLASS_NAME + " Children loaded:" + parentId + ", children:" + children.size()
            );

            final MainTvFragment fragment = mReference.get();
            if (fragment == null) {
                AppLogger.w(CLASS_NAME + " On children loaded -> fragment ref is null");
                return;
            }

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

            final List<MediaItemActionable> items = new ArrayList<>();
            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                final MediaItemActionable item = new MediaItemActionable(
                        mediaItem.getDescription(), mediaItem.getFlags()
                );

                final Drawable[] drawables = new Drawable[2];

                if (mediaItem.getDescription().getIconBitmap() != null) {
                    drawables[0] = new BitmapDrawable(
                            fragment.getResources(),
                            mediaItem.getDescription().getIconBitmap()
                    );
                    drawables[1] = drawables[0];
                } else {
                    if (mediaItem.isPlayable()) {
                        drawables[0] = fragment.getResources().getDrawable(
                                R.drawable.ic_favorite_border_white_24dp,
                                fragment.getActivity().getTheme()
                        );
                        drawables[1] = fragment.getResources().getDrawable(
                                R.drawable.ic_favorite_filled_24dp,
                                fragment.getActivity().getTheme()
                        );
                    }
                    drawables[0] = BitmapUtils.drawableFromUri(
                            fragment.getContext(), mediaItem.getDescription().getIconUri()
                    );
                    drawables[1] = drawables[0];
                }

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

            if (fragment.getActivity() instanceof MainTvActivity) {
                ((MainTvActivity) fragment.getActivity()).onDataLoaded();
            }
        }

        @Override
        public void onError(@NonNull final String id) {
            final MainTvFragment fragment = mReference.get();
            if (fragment == null) {
                return;
            }
            SafeToast.showAnyThread(
                    fragment.getContext(),
                    fragment.getString(R.string.error_loading_media)
            );
        }
    }
}
