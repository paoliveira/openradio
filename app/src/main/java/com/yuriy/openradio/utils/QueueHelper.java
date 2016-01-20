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

package com.yuriy.openradio.utils;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.support.annotation.NonNull;

import com.yuriy.openradio.api.CategoryVO;
import com.yuriy.openradio.api.RadioStationVO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with Android Studio.
 * Author: Chernyshov Yuriy - Mobile Development
 * Date: 16.12.14
 * Time: 11:44
 */
public final class QueueHelper {

    /**
     * Monitor object which manages access to the radio stations collection.
     */
    public static final Object RADIO_STATIONS_MANAGING_LOCK = new Object();

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = QueueHelper.class.getSimpleName();

    public static List<MediaSession.QueueItem> getPlayingQueue(
            final Context context,
            final List<RadioStationVO> radioStations) {
        final List<MediaSession.QueueItem> queue = new ArrayList<>();
        int count = 0;
        MediaSession.QueueItem item;
        MediaMetadata track;
        for (RadioStationVO radioStation : radioStations) {
            track = MediaItemHelper.buildMediaMetadataFromRadioStation(context, radioStation);
            item = new MediaSession.QueueItem(track.getDescription(), count++);
            queue.add(item);
        }
        return queue;
    }

    /**
     * Method return index of the {@link com.yuriy.openradio.api.RadioStationVO} at the queue.
     *
     * @param queue   Playing queue.
     * @param mediaId Id of the Radio Station.
     * @return Index of the Radio Station in the queue.
     */
    public static int getRadioStationIndexOnQueue(final Iterable<MediaSession.QueueItem> queue,
                                                  final long mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (mediaId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    /**
     * Method return index of the {@link com.yuriy.openradio.api.RadioStationVO} at the queue.
     *
     * @param queue   Playing queue.
     * @param mediaId Id of the Radio Station.
     * @return Index of the Radio Station in the queue.
     */
    public static int getRadioStationIndexOnQueue(final Iterable<MediaSession.QueueItem> queue,
                                                  final String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item: queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static boolean isIndexPlayable(final int index,
                                          final List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    public static RadioStationVO getRadioStationById(final String id,
                                                     final List<RadioStationVO> radioStations) {
        for (RadioStationVO radioStation : radioStations) {
            if (radioStation == null) {
                continue;
            }
            if (String.valueOf(radioStation.getId()).equals(id)) {
                return radioStation;
            }
        }

        return null;
    }

    /**
     * Copying collection from source to destination.
     *
     * @param destination Destination collection.
     * @param source      Source collection.
     */
    public static <T> void copyCollection(@NonNull final List<T> destination,
                                          @NonNull final List<T> source) {
        destination.clear();
        destination.addAll(source);
    }

    public static void updateRadioStation(final RadioStationVO radioStationVO,
                                          final List<RadioStationVO> radioStationVOs) {
        for (final RadioStationVO radioStation : radioStationVOs) {
            // TODO : replace with equals()
            if (radioStationVO.getId() == radioStation.getId()) {
                radioStation.setStreamURL(radioStationVO.getStreamURL());
                radioStation.setBitRate(radioStationVO.getBitRate());
                radioStation.setIsUpdated(true);
                break;
            }
        }
    }

    public static boolean removeRadioStation(final String mediaId,
                                             final List<RadioStationVO> radioStations) {
        for (final RadioStationVO radioStation : radioStations) {
            if (radioStation == null) {
                continue;
            }
            if (String.valueOf(radioStation.getId()).equals(mediaId)) {
                radioStations.remove(radioStation);
                return true;
            }
        }
        return false;
    }

    public static String getGenreNameById(final String genreId,
                                          final List<CategoryVO> categoriesList) {
        String genre = "";
        if (genreId == null) {
            return genre;
        }
        if (categoriesList == null) {
            return genre;
        }

        for (CategoryVO category : categoriesList) {
            if (String.valueOf(category.getId()).equals(genreId)) {
                genre = category.getName();
                break;
            }
        }

        return genre;
    }
}