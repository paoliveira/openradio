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

package com.yuriy.openradio.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.RadioStationJSONSerializer;
import com.yuriy.openradio.business.RadioStationSerializer;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class FavoritesStorage {

    /**
     * Private constructor
     */
    private FavoritesStorage() { }

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
        final RadioStationSerializer serializer = new RadioStationJSONSerializer();
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putString(String.valueOf(radioStation.getId()), serializer.serialize(radioStation));
        editor.commit();
    }

    /**
     * Remove provided {@link RadioStationVO} from the Favorites preferences.
     *
     * @param radioStation {@link RadioStationVO} to remove from the Favorites.
     * @param context      Context of the callee.
     */
    public static synchronized void removeFromFavorites(final RadioStationVO radioStation,
                                                        final Context context) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.remove(String.valueOf(radioStation.getId()));
        editor.commit();
    }

    /**
     * Check whether provided {@link RadioStationVO} is in Favorites preferences.
     *
     * @param radioStation {@link RadioStationVO} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isFavorite(final RadioStationVO radioStation, final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context);
        return sharedPreferences.contains(String.valueOf(radioStation.getId()));
    }

    /**
     * Return an instance of the Shared Preferences.
     *
     * @param context Context of the callee.
     * @return An instance of the Shared Preferences.
     */
    private static SharedPreferences getSharedPreferences(final Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Return {@link android.content.SharedPreferences.Editor} associated with the
     * Shared Preferences.
     *
     * @param context Context of the callee.
     * @return {@link android.content.SharedPreferences.Editor}.
     */
    private static SharedPreferences.Editor getEditor(final Context context) {
        return getSharedPreferences(context).edit();
    }
}
