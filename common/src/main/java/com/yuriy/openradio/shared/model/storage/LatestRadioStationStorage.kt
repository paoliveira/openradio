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
package com.yuriy.openradio.shared.model.storage

import android.content.Context
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object LatestRadioStationStorage : AbstractRadioStationsStorage() {

    /**
     * Name of the file for the Favorite Preferences.
     */
    private const val FILE_NAME = "LatestRadioStationPreferences"

    /**
     * Key to associate latest Radio Station with.
     */
    private const val KEY = "LatestRadioStationKey"

    /**
     * Cache object in order to prevent use of storage.
     */
    private var sRadioStation: RadioStation? = null

    /**
     * Save provided [RadioStation] to the Latest Radio Station preferences.
     *
     * @param radioStation [RadioStation] to add as Latest Radio Station.
     * @param context      Context of the callee.
     */
    @JvmStatic
    @Synchronized
    fun add(radioStation: RadioStation?, context: Context) {
        sRadioStation = RadioStation.makeCopyInstance(radioStation!!)
        add(KEY, radioStation, context, FILE_NAME)
    }

    /**
     * Return Latest Radio Station which is stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    @JvmStatic
    @Synchronized
    operator fun get(context: Context): RadioStation? {
        if (sRadioStation != null) {
            return sRadioStation
        }
        val list = getAll(context, FILE_NAME)
        // There is only one Radio Station in collection.
        if (list.isNotEmpty()) {
            sRadioStation = RadioStation.makeCopyInstance(list[0])
            return sRadioStation
        }
        return null
    }
}
