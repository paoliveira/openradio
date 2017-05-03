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
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LocalRadioStationsStorage extends AbstractStorage {

    /**
     * Private constructor
     */
    private LocalRadioStationsStorage() {
        super();
    }

    /**
     * Name of the file for the Favorite Preferences.
     */
    private static final String FILE_NAME = "LocalRadioStationsPreferences";

    /**
     * Key for the a of the Radio Station Id.
     */
    private static final String KEY_ID = "KEY_ID";

    /**
     * Init value of the custom Radio Station Id.
     */
    private static final int ID_INIT_VALUE = Integer.MAX_VALUE - 1000;

    /**
     * Set value of the Radio Station Id.
     *
     * @param context Applications context.
     * @param value   Value of the Radio Station Id.
     */
    private static void setId(final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putInt(KEY_ID, value);
        editor.commit();
    }

    /**
     * Gets value of the Radio Station Id.
     *
     * @param context Applications context.
     *
     * @return The value of the Radio Station Id.
     */
    public static int getId(final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        int id = sharedPreferences.getInt(KEY_ID, Integer.MAX_VALUE);
        // If value is Integer MAX, means that this is the first call, initialize it and save.
        if (id == Integer.MAX_VALUE) {
            setId(context, ID_INIT_VALUE);
            return ID_INIT_VALUE;
        }
        // Increment previous value, save it and return it.
        id = id + 1;
        setId(context, id);
        return id;
    }

    /**
     * Add provided {@link RadioStationVO} to the Local Radio Stations preferences.
     *
     * @param radioStation {@link RadioStationVO} to add to the Local Radio Stations.
     * @param context      Context of the callee.
     */
    public static synchronized void addToLocal(final RadioStationVO radioStation,
                                               final Context context) {
        add(radioStation, context, FILE_NAME);
    }

    /**
     * Remove provided {@link RadioStationVO} from the Local radio Stations preferences
     * by the provided media Id.
     *
     * @param mediaId Media Id of the {@link RadioStationVO}.
     * @param context Context of the callee.
     */
    public static synchronized void removeFromLocal(final String mediaId, final Context context) {
        remove(mediaId, context, FILE_NAME);
    }

    /**
     * Return collection of the Local Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    public static List<RadioStationVO> getAllLocal(final Context context) {
        final List<RadioStationVO> list = getAll(context, FILE_NAME);
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (final RadioStationVO radioStation : list) {
            if (radioStation.getId() == 0) {
                list.remove(radioStation);
                break;
            }
        }
        return list;
    }

    /**
     * Determines whether Local Radio Stations collection is empty or not.
     *
     * @param context Context of the callee.
     * @return {@code true} in case of the are Local Radio Stations in collection,
     *         {@code false} - otherwise.
     */
    public static boolean isLocalsEmpty(final Context context) {
        final List<RadioStationVO> list = getAll(context, FILE_NAME);
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (final RadioStationVO radioStation : list) {
            if (radioStation.getId() == 0) {
                list.remove(radioStation);
                break;
            }
        }
        return list.isEmpty();
    }

    /**
     * Check whether provided {@link RadioStationVO} is in Local Stations preferences.
     *
     * @param radioStation {@link RadioStationVO} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isLocalRadioStation(final RadioStationVO radioStation, final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        return sharedPreferences.contains(String.valueOf(radioStation.getId()));
    }
}
