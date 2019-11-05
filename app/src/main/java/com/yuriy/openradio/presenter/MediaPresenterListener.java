package com.yuriy.openradio.presenter;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public interface MediaPresenterListener {

    void showProgressBar();

    void handleMetadataChanged(final MediaMetadataCompat metadata);

    void handlePlaybackStateChanged(final PlaybackStateCompat state);
}
