/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.business;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link MediaItemStationsInCategory} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of concrete Category.
 */
public class MediaItemStationsInCategory implements MediaItemCommand {

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load Radio Station
                        loadStation(playbackStateListener, shareObject);
                    }
                }
        );
    }

    /**
     * Load Radio Station.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command
     */
    private void loadStation(final IUpdatePlaybackState playbackStateListener,
                             @NonNull final MediaItemShareObject shareObject) {
        final String radioStationId
                = shareObject.getParentId().replace(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, "");

        final RadioStationVO radioStation = shareObject.getServiceProvider().getStation(
                shareObject.getDownloader(),
                UrlBuilder.getStation(shareObject.getContext(), radioStationId));

        if (radioStation.getStreamURL() == null || radioStation.getStreamURL().isEmpty()) {
            if (playbackStateListener != null) {
                playbackStateListener.updatePlaybackState(
                        shareObject.getContext().getString(R.string.no_data_message)
                );
            }
            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.updateRadioStation(radioStation, shareObject.getRadioStations());
        }

        final MediaDescription mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                shareObject.getContext(),
                radioStation
        );
        final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);
        shareObject.getMediaItems().add(mediaItem);

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
