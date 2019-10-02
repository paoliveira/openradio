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
import android.text.TextUtils;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.RadioStation;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LocalRadioStationsStorage extends AbstractRadioStationsStorage {

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
     * Key for Radio Station Id.
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
        editor.apply();
    }

    /**
     * Gets value of the Radio Station Id.
     *
     * @param context Applications context.
     * @return The value of the Radio Station Id.
     */
    public static int getId(final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        int id = sharedPreferences.getInt(KEY_ID, Integer.MAX_VALUE);
        // If value is Integer MAX, means that this is the first call, initialize it and addToLocals.
        if (id == Integer.MAX_VALUE) {
            setId(context, ID_INIT_VALUE);
            return ID_INIT_VALUE;
        }
        // Increment previous value, addToLocals it and return it.
        id = id + 1;
        setId(context, id);
        return id;
    }

    /**
     * Check whether provided value is equal to {@link #KEY_ID}.
     *
     * @param value Value ot compare of.
     * @return {@code true} in case of value is {@link #KEY_ID}, {@code false} otherwise.
     */
    static boolean isKeyId(final String value) {
        return TextUtils.equals(KEY_ID, value);
    }

    /**
     * Add provided {@link RadioStation} to the Local Radio Stations preferences.
     *
     * @param radioStation {@link RadioStation} to add to the Local Radio Stations.
     * @param context      Context of the callee.
     */
    public static synchronized void add(final RadioStation radioStation, final Context context) {
        add(radioStation, context, FILE_NAME);
    }

    /**
     * Remove provided {@link RadioStation} from the Local radio Stations preferences
     * by the provided media Id.
     *
     * @param radioStation {@link RadioStation} to remove from the Local Radio Stations.
     * @param context Context of the callee.
     */
    public static synchronized void removeFromLocal(final RadioStation radioStation, final Context context) {
        remove(radioStation, context, FILE_NAME);
    }

    /**
     * Update Radio Station with provided values.
     *
     * @param mediaId  Media Id of the {@link RadioStation}.
     * @param context  Context of the callee.
     * @param name     Name of Radio Station.
     * @param url      URL of stream associated with Radio Station.
     * @param imageUrl URL of image associated with Radio Stream.
     * @param genre    Genre of Radio Station.
     * @param country  Country associated with Radio Station.
     * @param addToFav Whether or not Radio Station is in Favorite category.
     * @return {@code true} in case of success or {@code false} if Radio Station was not found.
     */
    public static synchronized boolean update(final String mediaId, final Context context,
                                              final String name, final String url, final String imageUrl,
                                              final String genre, final String country, final boolean addToFav) {
        boolean result = false;
        final List<RadioStation> list = getAll(context, FILE_NAME);
        for (final RadioStation radioStation : list) {
            if (radioStation.getIdAsString().endsWith(mediaId)) {
                radioStation.setName(name);
                radioStation.getMediaStream().setVariant(0, url);
                //TODO: Should we remove previous image from storage?
                radioStation.setImageUrl(imageUrl);
                radioStation.setGenre(genre);
                radioStation.setCountry(country);

                if (addToFav) {
                    FavoritesStorage.add(radioStation, context);
                } else {
                    FavoritesStorage.remove(radioStation, context);
                }

                add(radioStation, context);
                AppLogger.d("Radio station updated to:" + radioStation.toString());

                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Return Radio Station object associated with media id.
     *
     * @param mediaId Media Id of the {@link RadioStation}.
     * @param context Context of the callee.
     * @return Radio Station or {@code null} if there was nothing found.
     */
    public static synchronized RadioStation get(final String mediaId, final Context context) {
        final List<RadioStation> list = getAll(context, FILE_NAME);
        for (final RadioStation radioStation : list) {
            if (radioStation.getIdAsString().endsWith(mediaId)) {
                return radioStation;
            }
        }
        return null;
    }

    /**
     * Return Local added Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Local added Radio Stations in a String representation.
     */
    public static String getAllLocalAsString(final Context context) {
        return getAllAsString(context, FILE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    public static List<RadioStation> getAllLocalsFromString(final String marshalledRadioStations) {
        return getAllFromString(marshalledRadioStations);
    }

    /**
     * Return collection of the Local Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    public static List<RadioStation> getAllLocals(final Context context) {
        final List<RadioStation> list = getAll(context, FILE_NAME);
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (final RadioStation radioStation : list) {
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
     * {@code false} - otherwise.
     */
    public static boolean isLocalsEmpty(final Context context) {
        final List<RadioStation> list = getAll(context, FILE_NAME);
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (final RadioStation radioStation : list) {
            if (radioStation.getId() == 0) {
                list.remove(radioStation);
                break;
            }
        }
        return list.isEmpty();
    }

    /**
     * Check whether provided {@link RadioStation} is in Local Stations preferences.
     *
     * @param radioStation {@link RadioStation} to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    public static boolean isLocalRadioStation(final RadioStation radioStation, final Context context) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, FILE_NAME);
        return sharedPreferences.contains(radioStation.getIdAsString());
    }
}
