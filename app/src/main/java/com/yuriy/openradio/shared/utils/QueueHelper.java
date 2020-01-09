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

package com.yuriy.openradio.shared.utils;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.shared.vo.Category;
import com.yuriy.openradio.shared.vo.RadioStation;

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

    public static final int UNKNOWN_INDEX = -1;

    @SuppressWarnings("unused")
    private static final String CLASS_NAME = QueueHelper.class.getSimpleName();

    /**
     * @param radioStations
     * @return
     */
    public static List<MediaSessionCompat.QueueItem> getPlayingQueue(
            final List<RadioStation> radioStations) {
        final List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        MediaMetadataCompat track;
        for (final RadioStation radioStation : radioStations) {
            track = MediaItemHelper.metadataFromRadioStation(radioStation);
            if (track == null) {
                AppLogger.w(CLASS_NAME + " Get playing queue warning, Radio Station is null");
                continue;
            }
            queue.add(new MediaSessionCompat.QueueItem(track.getDescription(), radioStation.getId()));
        }
        return queue;
    }

    /**
     * Method return index of the {@link RadioStation} at the queue.
     *
     * @param queue   Playing queue.
     * @param mediaId Id of the Radio Station.
     * @return Index of the Radio Station in the queue.
     */
    public static int getRadioStationIndexOnQueue(final List<RadioStation> queue,
                                                  final String mediaId) {
        int index = 0;
        for (final RadioStation item : queue) {
            if (mediaId.equals(item.getIdAsString())) {
                return index;
            }
            index++;
        }
        return UNKNOWN_INDEX;
    }

    /**
     * @param index
     * @param queue
     * @return
     */
    public static boolean isIndexPlayable(final int index,
                                          final List<RadioStation> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }

    /**
     * @param id
     * @param radioStations
     * @return
     */
    public static RadioStation getRadioStationById(final String id,
                                                   final List<RadioStation> radioStations) {
        for (final RadioStation radioStation : radioStations) {
            if (radioStation == null) {
                continue;
            }
            if (TextUtils.equals(radioStation.getIdAsString(), id)) {
                return radioStation;
            }
        }
        return null;
    }

    /**
     * Safely add Radio Station to collection, skip execution if Radio Station is already
     * in collection.
     *
     * @param radioStation  Radio Station to add to collection.
     * @param radioStations Collection to add Radio Station into.
     */
    public static void addRadioStation(final RadioStation radioStation,
                                       final List<RadioStation> radioStations) {
        if (radioStation == null) {
            return;
        }
        if (radioStations == null) {
            return;
        }

        boolean contains = false;
        for (final RadioStation radioStationInt : radioStations) {
            if (radioStationInt == null) {
                continue;
            }
            if (radioStationInt.getId() == radioStation.getId()) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            radioStations.add(radioStation);
        }
    }

    /**
     * Clear destination and copy collection from source.
     *
     * @param destination Destination collection.
     * @param source      Source collection.
     */
    public static <T> void clearAndCopyCollection(@NonNull final List<T> destination,
                                                  @NonNull final List<T> source) {
        destination.clear();
        destination.addAll(source);
    }

    /**
     * @param radioStationVO
     * @param radioStations
     */
    public static void updateRadioStationMediaStream(final RadioStation radioStationVO,
                                                     final List<RadioStation> radioStations) {
        for (final RadioStation radioStation : radioStations) {
            if (radioStationVO.getId() == radioStation.getId()) {
                radioStation.setMediaStream(radioStationVO.getMediaStream());
                break;
            }
        }
    }

    /**
     * @param mediaId
     * @param radioStations
     * @return
     */
    public static RadioStation removeRadioStation(final String mediaId,
                                                  final List<RadioStation> radioStations) {
        for (final RadioStation radioStation : radioStations) {
            if (radioStation == null) {
                continue;
            }
            if (TextUtils.equals(radioStation.getIdAsString(), mediaId)) {
                radioStations.remove(radioStation);
                return radioStation;
            }
        }
        return null;
    }

    /**
     * @param genreId
     * @param categoriesList
     * @return
     */
    public static String getGenreNameById(final String genreId,
                                          final List<Category> categoriesList) {
        String genre = "";
        if (genreId == null) {
            return genre;
        }
        if (categoriesList == null) {
            return genre;
        }

        for (Category category : categoriesList) {
            if (String.valueOf(category.getId()).equals(genreId)) {
                genre = category.getName();
                break;
            }
        }

        return genre;
    }

    /**
     * Merge Radio Stations from listB to listA.
     *
     * @param listA
     * @param listB
     */
    public static void merge(final List<RadioStation> listA, final List<RadioStation> listB) {
        if (listA == null || listB == null) {
            return;
        }
        for (final RadioStation radioStation : listB) {
            if (listA.contains(radioStation)) {
                continue;
            }
            listA.add(radioStation);
        }
    }

    /**
     * @param list
     * @return
     */
    @NonNull
    public static String queueToString(@Nullable final List<RadioStation> list) {
        final StringBuilder builder = new StringBuilder("RadioStations:{");
        final String delimiter = ", ";
        if (list == null) {
            builder.append("null");
        } else {
            for (final RadioStation radioStation : list) {
                builder.append(radioStation.toString()).append(delimiter);
            }
            if (builder.length() > delimiter.length()) {
                builder.delete(builder.length() - delimiter.length(), builder.length());
            }
        }
        builder.append("}");
        return builder.toString();
    }
}
