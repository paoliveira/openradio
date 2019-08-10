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

package com.yuriy.openradio.model.media.item;

import androidx.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIdHelper;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;
import com.yuriy.openradio.vo.RadioStation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemStation} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio station by its Id.
 */
public final class MediaItemStation implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemStation.class.getSimpleName();

    /**
     * Default constructor.
     */
    public MediaItemStation() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject) {
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load Radio Station
                    loadStation(playbackStateListener, shareObject);
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
                = shareObject.getParentId().replace(MediaIdHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, "");

        final RadioStation radioStation = shareObject.getServiceProvider().getStation(
                shareObject.getDownloader(),
                UrlBuilder.getStation(radioStationId));

        if (radioStation.isMediaStreamEmpty()) {
            if (playbackStateListener != null) {
                playbackStateListener.updatePlaybackState(
                        shareObject.getContext().getString(R.string.no_data_message)
                );
            }
            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.updateRadioStationMediaStream(radioStation, shareObject.getRadioStations());
        }

        final MediaDescriptionCompat mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                shareObject.getContext(),
                radioStation
        );
        final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
        shareObject.getMediaItems().add(mediaItem);

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
