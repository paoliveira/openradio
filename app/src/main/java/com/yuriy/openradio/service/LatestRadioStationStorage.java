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
import android.support.annotation.Nullable;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.business.AppPreferencesManager;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class LatestRadioStationStorage extends AbstractStorage {

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
     * Save provided {@link RadioStationVO} to the Latest Radio Station preferences.
     *
     * @param radioStation {@link RadioStationVO} to add as Latest Radio Station.
     * @param context      Context of the callee.
     */
    public static synchronized void addToLocals(final RadioStationVO radioStation, final Context context) {
        add(KEY, radioStation, context, FILE_NAME);
    }

    /**
     * Return Latest Radio Station which is stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    @Nullable
    public static synchronized RadioStationVO load(final Context context) {
        // If this feature disabled by Settings - return null, in this case all consecutive UI views will not be
        // exposed.
        if (!AppPreferencesManager.lastKnownRadioStationEnabled(context)) {
            return null;
        }

        final List<RadioStationVO> list = getAll(context, FILE_NAME);
        // There is only one Radio Station in collection.
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
