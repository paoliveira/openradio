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
import com.yuriy.openradio.business.storage.FavoritesStorage;
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
    public abstract void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject);

    public abstract boolean doLoadNoDataReceived();

    void handleDataLoaded(final IUpdatePlaybackState playbackStateListener,
                          @NonNull final MediaItemShareObject shareObject,
                          final List<RadioStation> list) {
        AppLogger.d(CLASS_NAME + " Loaded " + list.size() + " items");
        if (!shareObject.isRestoreInstance() && list.isEmpty()) {

            if (doLoadNoDataReceived()) {
                final MediaMetadataCompat track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                        shareObject.getContext(),
                        MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES + shareObject.getCurrentCategory()
                );
                final MediaDescriptionCompat mediaDescription = track.getDescription();
                final MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
                shareObject.getMediaItems().add(mediaItem);
                shareObject.getResult().sendResult(shareObject.getMediaItems());

                if (playbackStateListener != null) {
                    playbackStateListener.updatePlaybackState(shareObject.getContext().getString(R.string.no_data_message));
                }
            } else {
                shareObject.getResult().sendResult(MediaItemHelper.createListEndedResult());
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
