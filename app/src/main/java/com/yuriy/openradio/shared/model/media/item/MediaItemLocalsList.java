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

package com.yuriy.openradio.shared.model.media.item;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.utils.MediaItemsComparator;
import com.yuriy.openradio.shared.utils.QueueHelper;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.Collections;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemLocalsList} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations from Locals list.
 */
public final class MediaItemLocalsList implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemLocalsList.class.getSimpleName();

    /**
     * Default constructor.
     */
    public MediaItemLocalsList() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject) {
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        final Context context = shareObject.getContext();

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.clearAndCopyCollection(
                    shareObject.getRadioStations(),
                    LocalRadioStationsStorage.getAllLocals(context)
            );
        }

        for (final RadioStation radioStation : shareObject.getRadioStations()) {

            final MediaDescriptionCompat mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    radioStation
            );
            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            if (LocalRadioStationsStorage.isLocalRadioStation(radioStation, context)) {
                MediaItemHelper.updateLocalRadioStationField(mediaItem, true);
            }
            if (FavoritesStorage.isFavorite(radioStation, context)) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }
            MediaItemHelper.updateSortIdField(mediaItem, radioStation.getSortId());

            shareObject.getMediaItems().add(mediaItem);
        }
        Collections.sort(shareObject.getMediaItems(), new MediaItemsComparator());
        shareObject.getResult().sendResult(shareObject.getMediaItems());
        shareObject.getResultListener().onResult();

        if (AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            final RadioStation radioStation = LatestRadioStationStorage.get(shareObject.getContext());
            if (radioStation != null) {
                shareObject.getRemotePlay().restoreActiveRadioStation(radioStation);
            }
        }
    }
}
