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
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.Downloader;
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
 */
public class MediaItemChildCategories implements MediaItemCommand {

    @Override
    public void create(final String countryCode,
                       final Downloader downloader, final APIServiceProvider serviceProvider,
                       @NonNull final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                       final List<MediaBrowser.MediaItem> mediaItems,
                       final IUpdatePlaybackState playbackStateListener,
                       @NonNull final String parentId, final List<RadioStationVO> radioStations,
                       @NonNull final MediaItemShareObject shareObject) {

        // Use result.detach to allow calling result.sendResult from another thread:
        result.detach();

        final String childMenuId
                = parentId.replace(MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES, "");

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load Radio Stations into menu
                        loadStationsInCategory(
                                shareObject.getContext(),
                                serviceProvider,
                                downloader,
                                childMenuId,
                                mediaItems,
                                result,
                                playbackStateListener,
                                radioStations,
                                shareObject
                        );
                    }
                });
    }

    /**
     * Load Radio Stations into Menu.
     *
     * @param context         Context of the callee.
     * @param serviceProvider {@link APIServiceProvider}.
     * @param downloader      {@link Downloader}.
     * @param categoryId      Id of the Category.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     * @param playbackStateListener
     * @param radioStations
     */
    private void loadStationsInCategory(final Context context,
                                        final APIServiceProvider serviceProvider,
                                        final Downloader downloader,
                                        final String categoryId,
                                        final List<MediaBrowser.MediaItem> mediaItems,
                                        final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                                        final IUpdatePlaybackState playbackStateListener,
                                        final List<RadioStationVO> radioStations,
                                        @NonNull final MediaItemShareObject shareObject) {
        final List<RadioStationVO> list = serviceProvider.getStations(downloader,
                UrlBuilder.getStationsInCategory(context, categoryId));

        MediaMetadata track;

        if (list.isEmpty()) {

            track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                    context,
                    MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES + shareObject.getCurrentCategory()
            );
            final MediaDescription mediaDescription = track.getDescription();
            final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                    mediaDescription, MediaBrowser.MediaItem.FLAG_BROWSABLE);
            mediaItems.add(mediaItem);
            result.sendResult(mediaItems);

            if (playbackStateListener != null) {
                playbackStateListener.updatePlaybackState(context.getString(R.string.no_data_message));
            }

            return;
        }

        synchronized (QueueHelper.RADIO_STATIONS_MANAGING_LOCK) {
            QueueHelper.copyCollection(radioStations, list);
        }

        final String genre = QueueHelper.getGenreNameById(
                categoryId, shareObject.getChildCategories()
        );

        for (RadioStationVO radioStation : radioStations) {

            radioStation.setGenre(genre);

            final MediaDescription mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                    context,
                    radioStation
            );
            final MediaBrowser.MediaItem mediaItem = new MediaBrowser.MediaItem(
                    mediaDescription, MediaBrowser.MediaItem.FLAG_PLAYABLE);

            if (FavoritesStorage.isFavorite(radioStation, context)) {
                MediaItemHelper.updateFavoriteField(mediaItem, true);
            }

            mediaItems.add(mediaItem);
        }

        result.sendResult(mediaItems);
    }
}
