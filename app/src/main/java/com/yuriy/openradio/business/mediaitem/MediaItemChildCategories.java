/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.business.mediaitem;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.service.FavoritesStorage;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemChildCategories} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of Child Category.
 */
public final class MediaItemChildCategories implements MediaItemCommand {

    /**
     * Index of the current page (refer to Dirble API for more info) of the Radio Stations List.
     */
    private int mPageIndex;

    /**
     * Default constructor.
     */
    public MediaItemChildCategories() {
        super();
        mPageIndex = UrlBuilder.FIRST_PAGE_INDEX;
    }

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load Radio Stations into menu
                    loadStationsInCategory(playbackStateListener, shareObject);
                });
    }

    /**
     * Load Radio Stations into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command.
     */
    private void loadStationsInCategory(final IUpdatePlaybackState playbackStateListener,
                                        @NonNull final MediaItemShareObject shareObject) {

        final String childMenuId
                = shareObject.getParentId().replace(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES, "");

        final List<RadioStationVO> list = shareObject.getServiceProvider().getStations(
                shareObject.getDownloader(),
                UrlBuilder.getStationsInCategory(
                        shareObject.getContext(),
                        childMenuId,
                        mPageIndex++,
                        UrlBuilder.ITEMS_PER_PAGE
                )
        );

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
                shareObject.getResult().sendResult(shareObject.getMediaItems());
            }

            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.copyCollection(shareObject.getRadioStations(), list);
        }

        final String genre = QueueHelper.getGenreNameById(
                shareObject.getParentId(), shareObject.getChildCategories()
        );

        for (final RadioStationVO radioStation : shareObject.getRadioStations()) {

            radioStation.setGenre(genre);

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
