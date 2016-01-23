/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.service.FavoritesStorage;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;
import com.yuriy.openradio.utils.Utils;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link MediaItemSearchFromApp} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations from the search collection.
 */
public class MediaItemSearchFromApp implements MediaItemCommand {

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load all categories into menu
                        loadSearchedStations(playbackStateListener, shareObject);
                    }
                }
        );
    }

    /**
     * Load Radio Stations from the Search query.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command
     */
    private void loadSearchedStations(final IUpdatePlaybackState playbackStateListener,
                                      @NonNull final MediaItemShareObject shareObject) {
        final List<RadioStationVO> list = shareObject.getServiceProvider().getStations(
                shareObject.getDownloader(),
                UrlBuilder.getSearchQuery(shareObject.getContext(),
                // Get search query from the holder util
                Utils.getSearchQuery())
        );

        shareObject.getMediaItems().clear();

        if (list.isEmpty()) {

            shareObject.getResult().sendResult(shareObject.getMediaItems());
            if (playbackStateListener != null) {
                playbackStateListener.updatePlaybackState(
                        shareObject.getContext().getString(R.string.no_search_results)
                );
            }

            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.copyCollection(shareObject.getRadioStations(), list);
        }

        for (final RadioStationVO radioStation : shareObject.getRadioStations()) {

            final MediaDescription mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    shareObject.getContext(),
                    radioStation
            );
            final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                    mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);

            if (FavoritesStorage.isFavorite(radioStation, shareObject.getContext())) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }

            shareObject.getMediaItems().add(mediaItem);
        }

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
