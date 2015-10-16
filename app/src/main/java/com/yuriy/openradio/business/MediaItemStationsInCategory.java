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

import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.UrlBuilder;
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
 */
public class MediaItemStationsInCategory implements MediaItemCommand {

    @Override
    public void create(final String countryCode,
                       final Downloader downloader, final APIServiceProvider serviceProvider,
                       @NonNull final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                       final List<MediaBrowser.MediaItem> mediaItems,
                       final IUpdatePlaybackState playbackStateListener,
                       @NonNull final String parentId,
                       @NonNull final List<RadioStationVO> radioStations,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        result.detach();

        final String radioStationId
                = parentId.replace(MediaIDHelper.MEDIA_ID_RADIO_STATIONS_IN_CATEGORY, "");

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load Radio Station
                        loadStation(
                                shareObject.getContext(),
                                serviceProvider,
                                downloader,
                                radioStationId,
                                mediaItems,
                                result,
                                playbackStateListener,
                                radioStations
                        );
                    }
                }
        );
    }

    /**
     * Load Radio Station.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param radioStationId  Id of the Radio Station.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadStation(final Context context,
                             final APIServiceProvider serviceProvider,
                             final Downloader downloader,
                             final String radioStationId,
                             final List<MediaBrowser.MediaItem> mediaItems,
                             final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                             final IUpdatePlaybackState playbackStateListener,
                             @NonNull final List<RadioStationVO> radioStations) {
        final RadioStationVO radioStation
                = serviceProvider.getStation(downloader,
                UrlBuilder.getStation(context, radioStationId));

        if (radioStation.getStreamURL() == null || radioStation.getStreamURL().isEmpty()) {
            if (playbackStateListener != null) {
                playbackStateListener.updatePlaybackState(context.getString(R.string.no_data_message));
            }
            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.updateRadioStation(radioStation, radioStations);
        }

        final MediaDescription mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                context,
                radioStation
        );
        final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);
        mediaItems.add(mediaItem);

        result.sendResult(mediaItems);
    }
}
