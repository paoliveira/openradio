/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import com.yuriy.openradio.shared.vo.RadioStation
import java.util.Collections
import java.util.TreeSet

class RadioStationsStorage {

    interface Listener {

        fun onClear()

        fun onAdd(item: RadioStation, position: Int)

        fun onAddAll(set: Set<RadioStation>)

        fun onUpdate(item: RadioStation)
    }

    /**
     * Collection of the Radio Stations.
     */
    private val mRadioStations = Collections.synchronizedSet<RadioStation>(TreeSet())

    val all: Set<RadioStation>
        get() {
            return TreeSet(mRadioStations)
        }

    fun addAll(set: Set<RadioStation>) {
        mRadioStations.addAll(set)
    }

    fun clear() {
        mRadioStations.clear()
    }

    fun size(): Int {
        return mRadioStations.size
    }

    /**
     * @param id
     * @return
     */
    fun getById(id: String): RadioStation {
        var result = RadioStation.INVALID_INSTANCE
        synchronized(mRadioStations) {
            for (item in mRadioStations) {
                if (item.id == id) {
                    result = item
                    break
                }
            }
        }
        return result
    }

    /**
     * @param mediaId
     */
    fun removeById(mediaId: String) {
        synchronized(mRadioStations) {
            for (radioStation in all) {
                if (radioStation.id == mediaId) {
                    mRadioStations.remove(radioStation)
                    break
                }
            }
        }
    }

    fun getAt(index: Int): RadioStation {
        if (index < 0) {
            return RadioStation.INVALID_INSTANCE
        }
        if (index >= size()) {
            return RadioStation.INVALID_INSTANCE
        }
        return mRadioStations.elementAt(index)
    }

    /**
     * Clear destination and copy collection from source.
     *
     * @param source Source collection.
     */
    fun clearAndCopy(source: Set<RadioStation>) {
        clear()
        addAll(source)
    }
}
