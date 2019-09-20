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

package com.yuriy.openradio.model.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import com.yuriy.openradio.vo.RadioStation;

import java.util.List;

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
     * {@inheritDoc}
     */
    @NonNull
    public static List<RadioStation> getAllFavoritesFromString(final String marshalledRadioStations) {
        return getAllFromString(marshalledRadioStations);
    }

    /**
     * Add provided {@link RadioStation} to the Favorites preferences.
     *
     * @param radioStation {@link RadioStation} to add to Favorites.
     * @param context      Context of the callee.
     */
    public static synchronized void add(final RadioStation radioStation, final Context context) {
        add(radioStation, context, FILE_NAME);
    }

    /**
     * Remove provided {@link RadioStation} from the Favorites preferences
     * by the provided media Id.
     *
     * @param mediaId Media Id of the {@link RadioStation}.
     * @param context Context of the callee.
     */
    public static synchronized void remove(final String mediaId, final Context context) {
        remove(mediaId, context, FILE_NAME);
    }

    /**
     * Return collection of the Favorite Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Favorites Radio stations.
     */
    public static List<RadioStation> getAll(final Context context) {
        return getAll(context, FILE_NAME);
    }

    /**
     * Return Favorite Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Favorite Radio Stations in a String representation.
     */
    public static String getAllFavoritesAsString(final Context context) {
        return getAllAsString(context, FILE_NAME);
    }

    /**
     * Determines whether Favorites collection is empty or not.
     *
     * @param context Context of the callee.
     * @return True in case of the are Favorites in collection, False - otherwise.
     */
    public static boolean isFavoritesEmpty(final Context context) {
        return isEmpty(context, FILE_NAME);
    }

    /**
     * Check whether provided {@link RadioStation} is in Favorites preferences.
     *
     * @param radioStation {@link RadioStation} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isFavorite(final RadioStation radioStation, final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        if (sharedPreferences.contains(radioStation.getIdAsString())) {
            return true;
        }
        final List<RadioStation> list = getAll(context);
        for (final RadioStation station : list) {
            if (station == null) {
                continue;
            }
            if (station.getId() == radioStation.getId()) {
                return true;
            }
        }
        return false;
    }
}
