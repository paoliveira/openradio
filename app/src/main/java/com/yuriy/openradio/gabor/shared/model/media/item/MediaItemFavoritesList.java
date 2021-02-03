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

package com.yuriy.openradio.gabor.shared.model.media.item;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.gabor.shared.model.storage.FavoritesStorage;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.MediaItemHelper;
import com.yuriy.openradio.gabor.shared.vo.RadioStation;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemFavoritesList} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations from Favorites list.
 */
public final class MediaItemFavoritesList implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemFavoritesList.class.getSimpleName();

    /**
     * Default constructor.
     */
    public MediaItemFavoritesList() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemCommandDependencies dependencies) {
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.getResult().detach();

        final Context context = dependencies.getContext();

        final List<RadioStation> list = FavoritesStorage.getAll(context);
        dependencies.getRadioStationsStorage().clearAndCopy(list);

        for (final RadioStation radioStation : list) {

            final MediaDescriptionCompat mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    context, radioStation
            );
            final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

            if (FavoritesStorage.isFavorite(radioStation, context)) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }
            MediaItemHelper.updateSortIdField(mediaItem, radioStation.getSortId());

            dependencies.addMediaItem(mediaItem);
        }
        dependencies.getResult().sendResult(dependencies.getMediaItems());
        dependencies.getResultListener().onResult();
    }
}
