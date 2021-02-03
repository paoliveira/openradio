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

package com.yuriy.openradio.gabor.shared.model.storage;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.gabor.shared.vo.RadioStation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class FavoritesStorage extends AbstractRadioStationsStorage {

    /**
     * Private constructor
     */
    private FavoritesStorage() {
        super();
    }

    /**
     * Name of the file for the Favorite Preferences.
     */
    private static final String FILE_NAME = "FavoritesPreferences";

    /**
     * Cache key of the Favorite Radio Station in order to ease load from preferences.
     */
    private static final Set<String> sSet = new HashSet<>();

    /**
     * {@inheritDoc}
     */
    @NonNull
    public static List<RadioStation> getAllFavoritesFromString(final Context context,
                                                               final String marshalledRadioStations) {
        return AbstractRadioStationsStorage.getAllFromString(context, marshalledRadioStations);
    }

    /**
     * Add provided {@link RadioStation} to the Favorites preferences.
     *
     * @param radioStation {@link RadioStation} to add to Favorites.
     * @param context      Context of the callee.
     */
    public static synchronized void add(final RadioStation radioStation, final Context context) {
        final String key = AbstractRadioStationsStorage.createKeyForRadioStation(radioStation);
        sSet.add(key);
        AbstractRadioStationsStorage.add(radioStation, context, FILE_NAME);
    }

    /**
     * Remove provided {@link RadioStation} from the Favorites preferences
     * by the provided media Id.
     *
     * @param radioStation {@link RadioStation} to remove from Favorites.
     * @param context Context of the callee.
     */
    public static synchronized void remove(final RadioStation radioStation, final Context context) {
        final String key = AbstractRadioStationsStorage.createKeyForRadioStation(radioStation);
        sSet.remove(key);
        AbstractRadioStationsStorage.remove(radioStation, context, FILE_NAME);
    }

    /**
     * Return collection of the Favorite Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Favorites Radio stations.
     */
    public static List<RadioStation> getAll(final Context context) {
        return AbstractRadioStationsStorage.getAll(context, FILE_NAME);
    }

    /**
     * Return Favorite Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Favorite Radio Stations in a String representation.
     */
    public static String getAllFavoritesAsString(final Context context) {
        return AbstractRadioStationsStorage.getAllAsString(context, FILE_NAME);
    }

    /**
     * Determines whether Favorites collection is empty or not.
     *
     * @param context Context of the callee.
     * @return True in case of the are Favorites in collection, False - otherwise.
     */
    public static boolean isFavoritesEmpty(final Context context) {
        return AbstractRadioStationsStorage.isEmpty(context, FILE_NAME);
    }

    /**
     * Check whether provided {@link RadioStation} is in Favorites preferences.
     *
     * @param radioStation {@link RadioStation} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isFavorite(final RadioStation radioStation, final Context context) {
        final String key = AbstractRadioStationsStorage.createKeyForRadioStation(radioStation);
        if (sSet.contains(key)) {
            return true;
        }
        final List<RadioStation> list = getAll(context);
        for (final RadioStation station : list) {
            if (station == null) {
                continue;
            }
            if (station.getId().equals(radioStation.getId())) {
                sSet.add(key);
                return true;
            }
        }
        return false;
    }

    public static boolean isFavorite(final MediaBrowserCompat.MediaItem mediaItem, final Context context) {
        final String key = AbstractRadioStationsStorage.createKeyForRadioStation(mediaItem);
        if (sSet.contains(key)) {
            return true;
        }
        final List<RadioStation> list = getAll(context);
        for (final RadioStation station : list) {
            if (station == null) {
                continue;
            }
            if (station.getId().equals(mediaItem.getMediaId())) {
                sSet.add(key);
                return true;
            }
        }
        return false;
    }
}
