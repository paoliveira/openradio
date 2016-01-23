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

import android.content.Context;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.support.annotation.NonNull;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.service.LocalRadioStationsStorage;
import com.yuriy.openradio.utils.MediaItemHelper;
import com.yuriy.openradio.utils.QueueHelper;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link MediaItemLocalsList} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations from Locals list.
 */
public class MediaItemLocalsList implements MediaItemCommand {

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        final Context context = shareObject.getContext();

        final List<RadioStationVO> list = LocalRadioStationsStorage.getAllLocal(context);

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.copyCollection(shareObject.getRadioStations(), list);
        }

        for (final RadioStationVO radioStation : shareObject.getRadioStations()) {

            final MediaDescription mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    context,
                    radioStation
            );
            final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                    mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);

            if (LocalRadioStationsStorage.isLocalRadioStation(radioStation, context)) {
                MediaItemHelper.updateLocalRadioStationField(mediaItem, true);
            }

            shareObject.getMediaItems().add(mediaItem);
        }
        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
