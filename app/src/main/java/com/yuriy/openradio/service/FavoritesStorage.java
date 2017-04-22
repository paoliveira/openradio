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

package com.yuriy.openradio.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.yuriy.openradio.api.RadioStationVO;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class FavoritesStorage extends AbstractStorage {

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
     * Add provided {@link RadioStationVO} to the Favorites preferences.
     *
     * @param radioStation {@link RadioStationVO} to add to Favorites.
     * @param context      Context of the callee.
     */
    public static synchronized void addToFavorites(final RadioStationVO radioStation,
                                                   final Context context) {
        add(radioStation, context, FILE_NAME);
    }

    /**
     * Remove provided {@link RadioStationVO} from the Favorites preferences
     * by the provided media Id.
     *
     * @param mediaId Media Id of the {@link RadioStationVO}.
     * @param context Context of the callee.
     */
    public static synchronized void removeFromFavorites(final String mediaId,
                                                        final Context context) {
        remove(mediaId, context, FILE_NAME);
    }

    /**
     * Return collection of the Favorite Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Favorites Radio stations.
     */
    public static List<RadioStationVO> getAllFavorites(final Context context) {
        return getAll(context, FILE_NAME);
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
     * Check whether provided {@link RadioStationVO} is in Favorites preferences.
     *
     * @param radioStation {@link RadioStationVO} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isFavorite(final RadioStationVO radioStation, final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        return sharedPreferences.contains(String.valueOf(radioStation.getId()));
    }
}
