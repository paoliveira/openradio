package com.yuriy.openradio.business.mediaitem;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.business.storage.FavoritesStorage;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;
import com.yuriy.openradio.vo.RadioStation;

import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public abstract class IndexableMediaItemCommand implements MediaItemCommand {

    private static final String LOG_TAG = IndexableMediaItemCommand.class.getSimpleName();

    /**
     * Index of the current page (refer to Dirble API for more info) of the Radio Stations List.
     */
    private int mPageIndex;

    IndexableMediaItemCommand() {
        super();
        mPageIndex = UrlBuilder.FIRST_PAGE_INDEX;
    }

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {
        AppLogger.d(LOG_TAG + " invoked");
        if (!shareObject.isSameCatalogue()) {
            AppLogger.d("Not the same catalogue, clear list");
            mPageIndex = UrlBuilder.FIRST_PAGE_INDEX;
            shareObject.getRadioStations().clear();
        }
    }

    int incrementAndGetPageIndex() {
        return mPageIndex++;
    }

    void handleDataLoaded(final IUpdatePlaybackState playbackStateListener,
                          @NonNull final MediaItemShareObject shareObject,
                          final List<RadioStation> list) {
        AppLogger.d(LOG_TAG + " Loaded " + list.size() + " items, index " + mPageIndex);
        if (list.isEmpty()) {

            if (mPageIndex == UrlBuilder.FIRST_PAGE_INDEX + 1) {
                final MediaMetadataCompat track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                        shareObject.getContext(),
                        MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES + shareObject.getCurrentCategory()
                );
                final MediaDescriptionCompat mediaDescription = track.getDescription();
                final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
                shareObject.getMediaItems().add(mediaItem);
                shareObject.getResult().sendResult(shareObject.getMediaItems());

                if (playbackStateListener != null) {
                    playbackStateListener.updatePlaybackState(
                            shareObject.getContext().getString(R.string.no_data_message)
                    );
                }
            } else {
                shareObject.getResult().sendResult(
                        MediaItemHelper.createListEndedResult()
                );
            }

            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            shareObject.getRadioStations().addAll(list);
        }

        for (final RadioStation radioStation : shareObject.getRadioStations()) {

            final MediaDescriptionCompat mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    shareObject.getContext(),
                    radioStation
            );

            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            if (FavoritesStorage.isFavorite(radioStation, shareObject.getContext())) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }

            shareObject.getMediaItems().add(mediaItem);
        }

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
