package com.yuriy.openradio.vo;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;
import androidx.leanback.widget.MultiActionsProvider;

public class MediaItemActionable extends MediaBrowserCompat.MediaItem implements MultiActionsProvider {

    private MultiAction[] mMediaRowActions;
    private boolean mFavorite = false;

    public MediaItemActionable(final @NonNull MediaDescriptionCompat description,
                               final int flags) {
        super(description, flags);
    }

    public void setMediaRowActions(final MultiAction[] mediaRowActions) {
        mMediaRowActions = mediaRowActions;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(final boolean favorite) {
        mFavorite = favorite;
    }

    @Override
    public MultiAction[] getActions() {
        return mMediaRowActions;
    }
}
