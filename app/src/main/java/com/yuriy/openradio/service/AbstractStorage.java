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
import com.yuriy.openradio.business.RadioStationDeserializer;
import com.yuriy.openradio.business.RadioStationJSONDeserializer;
import com.yuriy.openradio.business.RadioStationJSONSerializer;
import com.yuriy.openradio.business.RadioStationSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public abstract class AbstractStorage {

    /**
     * Add provided {@link RadioStationVO} to the storage.
     *
     * @param radioStation {@link RadioStationVO} to add to the storage.
     * @param context      Context of the callee.
     * @param name         Name of the file for the preferences.
     */
    protected static synchronized void add(final RadioStationVO radioStation,
                                           final Context context, final String name) {
        final RadioStationSerializer serializer = new RadioStationJSONSerializer();
        final SharedPreferences.Editor editor = getEditor(context, name);
        editor.putString(String.valueOf(radioStation.getId()), serializer.serialize(radioStation));
        editor.commit();
    }

    /**
     * Remove provided {@link RadioStationVO} from the storage by the provided Media Id.
     *
     * @param mediaId Media Id of the {@link RadioStationVO}.
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     */
    protected static synchronized void remove(final String mediaId, final Context context,
                                              final String name) {
        final SharedPreferences.Editor editor = getEditor(context, name);
        editor.remove(mediaId);
        editor.commit();
    }

    /**
     * Return collection of the Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     *
     * @return Collection of the Radio Stations.
     */
    protected static List<RadioStationVO> getAll(final Context context, final String name) {
        final List<RadioStationVO> radioStations = new ArrayList<>();
        final SharedPreferences sharedPreferences = getSharedPreferences(context, name);
        final Map<String, ?> map = sharedPreferences.getAll();
        final RadioStationDeserializer deserializer = new RadioStationJSONDeserializer();
        RadioStationVO radioStation;
        String value;
        for (String key : map.keySet()) {
            value = String.valueOf(map.get(key));
            if (value == null || value.isEmpty()) {
                continue;
            }
            radioStation = deserializer.deserialize(value);
            radioStations.add(radioStation);
        }
        return radioStations;
    }

    /**
     * Determines whether collection is empty or not.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     *
     * @return {@code true} in case of the are items in collection, {@code false} - otherwise.
     */
    protected static boolean isEmpty(final Context context, final String name) {
        final SharedPreferences sharedPreferences = getSharedPreferences(context, name);
        final Map<String, ?> map = sharedPreferences.getAll();
        return map.isEmpty();
    }

    /**
     * Return an instance of the Shared Preferences.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     *
     * @return An instance of the Shared Preferences.
     */
    protected static SharedPreferences getSharedPreferences(final Context context,
                                                            final String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Return {@link android.content.SharedPreferences.Editor} associated with the
     * Shared Preferences.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     *
     * @return {@link android.content.SharedPreferences.Editor}.
     */
    protected static SharedPreferences.Editor getEditor(final Context context, final String name) {
        return getSharedPreferences(context, name).edit();
    }
}
