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

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage;
import com.yuriy.openradio.shared.model.storage.cache.CacheType;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 */
public abstract class MediaItemCommandImpl implements MediaItemCommand {

    private static final String CLASS_NAME = MediaItemCommandImpl.class.getSimpleName();

    /**
     *
     */
    MediaItemCommandImpl() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemCommandDependencies dependencies) {
        AppLogger.d(CLASS_NAME + " invoked");
        if (!dependencies.isSameCatalogue() || dependencies.isSavedInstance()) {
            AppLogger.d("Not the same catalogue, clear list");
            dependencies.getRadioStationsStorage().clear();
        }
    }

    public abstract boolean doLoadNoDataReceived();

    void handleDataLoaded(final IUpdatePlaybackState playbackStateListener,
                          @NonNull final MediaItemCommandDependencies dependencies,
                          final List<RadioStation> list) {
        AppLogger.d(CLASS_NAME + " Loaded " + list.size() + " items");
        if (list.isEmpty()) {

            if (doLoadNoDataReceived()) {
                final MediaMetadataCompat track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                        dependencies.getContext(),
                        MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES
                );
                final MediaDescriptionCompat mediaDescription = track.getDescription();
                final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
                dependencies.addMediaItem(mediaItem);
                dependencies.getResult().sendResult(dependencies.getMediaItems());
                dependencies.getResultListener().onResult();

                if (playbackStateListener != null) {
                    playbackStateListener.updatePlaybackState(dependencies.getContext().getString(R.string.no_data_message));
                }
            } else {
                dependencies.getResult().sendResult(MediaItemHelper.createListEndedResult());
                dependencies.getResultListener().onResult();
            }

            if (AppPreferencesManager.lastKnownRadioStationEnabled(dependencies.getContext())) {
                final RadioStation radioStation = LatestRadioStationStorage.get(dependencies.getContext());
                if (radioStation != null) {
                    dependencies.getRemotePlay().restoreActiveRadioStation(radioStation);
                }
            }
            return;
        }

        dependencies.getRadioStationsStorage().addAll(list);

        final List<RadioStation> radioStations = dependencies.getRadioStationsStorage().getAll();
        for (final RadioStation radioStation : radioStations) {

            final MediaDescriptionCompat mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    radioStation
            );

            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            if (FavoritesStorage.isFavorite(radioStation, dependencies.getContext())) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }

            dependencies.addMediaItem(mediaItem);
        }

        dependencies.getResult().sendResult(dependencies.getMediaItems());
        dependencies.getResultListener().onResult();

        if (AppPreferencesManager.lastKnownRadioStationEnabled(dependencies.getContext())) {
            final RadioStation radioStation = LatestRadioStationStorage.get(dependencies.getContext());
            if (radioStation != null) {
                dependencies.getRemotePlay().restoreActiveRadioStation(radioStation);
            }
        }
    }

    static CacheType getCacheType(final MediaItemCommandDependencies dependencies) {
        if (dependencies.isSavedInstance()) {
            return CacheType.IN_MEMORY;
        }
        return CacheType.PERSISTENT;
    }
}
