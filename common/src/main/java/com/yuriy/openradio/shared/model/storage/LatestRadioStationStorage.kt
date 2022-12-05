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
import com.yuriy.openradio.shared.vo.isInvalid
import java.lang.ref.WeakReference

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class LatestRadioStationStorage(contextRef: WeakReference<Context>) :
    AbstractRadioStationsStorage(contextRef, FILE_NAME) {

    /**
     * Cache object in order to prevent use of storage.
     */
    private var mRadioStation = RadioStation.INVALID_INSTANCE

    /**
     * Save provided [RadioStation] to the Latest Radio Station preferences.
     *
     * @param radioStation [RadioStation] to add as Latest Radio Station.
     */
    @Synchronized
    fun addLatest(radioStation: RadioStation) {
        mRadioStation = RadioStation.makeCopyInstance(radioStation)
        add(radioStation, KEY)
    }

    /**
     * Return Latest Radio Station which is stored in the persistent storage.
     *
     * @return Collection of the Local Radio Stations.
     */
    @Synchronized
    fun get(): RadioStation {
        if (mRadioStation.isInvalid().not()) {
            return mRadioStation
        }
        val list = getAll()
        // There is only one Radio Station in collection.
        if (list.isNotEmpty()) {
            mRadioStation = RadioStation.makeCopyInstance(list[0])
            return mRadioStation
        }
        return RadioStation.INVALID_INSTANCE
    }

    companion object {
        /**
         * Name of the file for the Favorite Preferences.
         */
        private const val FILE_NAME = "LatestRadioStationPreferences"

        /**
         * Key to associate latest Radio Station with.
         */
        private const val KEY = "LatestRadioStationKey"
    }
}
