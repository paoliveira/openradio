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

package com.yuriy.openradio.shared.model.storage;

import android.content.Context;

import androidx.annotation.Nullable;

import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LatestRadioStationStorage extends AbstractRadioStationsStorage {

    /**
     * Private constructor
     */
    private LatestRadioStationStorage() {
        super();
    }

    /**
     * Name of the file for the Favorite Preferences.
     */
    private static final String FILE_NAME = "LatestRadioStationPreferences";

    /**
     * Key to associate latest Radio Station with.
     */
    private static final String KEY = "LatestRadioStationKey";

    /**
     * Cache object in order to prevent use of storage.
     */
    private static RadioStation sRadioStation;

    /**
     * Save provided {@link RadioStation} to the Latest Radio Station preferences.
     *
     * @param radioStation {@link RadioStation} to add as Latest Radio Station.
     * @param context      Context of the callee.
     */
    public static synchronized void add(final RadioStation radioStation, final Context context) {
        sRadioStation = RadioStation.makeCopyInstance(radioStation);
        add(KEY, radioStation, context, FILE_NAME);
    }

    /**
     * Return Latest Radio Station which is stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    @Nullable
    public static synchronized RadioStation get(final Context context) {
        if (sRadioStation != null) {
            return sRadioStation;
        }
        final List<RadioStation> list = getAll(context, FILE_NAME);
        // There is only one Radio Station in collection.
        if (!list.isEmpty()) {
            list.get(0).setLastKnown(true);
            sRadioStation = RadioStation.makeCopyInstance(list.get(0));
            return sRadioStation;
        }
        return null;
    }
}
