package com.yuriy.openradio.vo;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;
import androidx.leanback.widget.MultiActionsProvider;

import java.util.Arrays;

public class MediaItemActionable extends MediaBrowserCompat.MediaItem implements MultiActionsProvider {

    private MultiAction[] mMediaRowActions;
    private final int mListIndex;

    public MediaItemActionable(final @NonNull MediaDescriptionCompat description,
                               final int flags, final int listIndex) {
        super(description, flags);
        mListIndex = listIndex;
    }

    public int getListIndex() {
        return mListIndex;
    }

    public void setMediaRowActions(final MultiAction[] mediaRowActions) {
        mMediaRowActions = mediaRowActions;
    }

    @Override
    public MultiAction[] getActions() {
        return mMediaRowActions;
    }

    @Override
    public String toString() {
        return "MediaItemActionable{" +
                "mMediaRowActions=" + Arrays.toString(mMediaRowActions) +
                ", mListIndex=" + mListIndex +
                ", super=" + super.toString() +
                '}';
    }
}
