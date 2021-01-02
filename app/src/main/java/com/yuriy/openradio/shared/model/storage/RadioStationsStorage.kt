/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

class RadioStationsStorage {
    /**
     * Collection of the Radio Stations.
     */
    private val mRadioStations: MutableList<RadioStation> = Collections.synchronizedList(ArrayList())

    fun sort(comparator: Comparator<RadioStation>) {
        synchronized(mRadioStations) { Collections.sort(mRadioStations, comparator) }
    }

    fun addAll(list: List<RadioStation>) {
        synchronized(mRadioStations) { mRadioStations.addAll(list) }
    }

    fun add(value: RadioStation) {
        synchronized(mRadioStations) { mRadioStations.add(value) }
    }

    fun clear() {
        synchronized(mRadioStations) { mRadioStations.clear() }
    }

    val isEmpty: Boolean
        get() {
            var result: Boolean
            synchronized(mRadioStations) { result = mRadioStations.isEmpty() }
            return result
        }

    fun size(): Int {
        var result: Int
        synchronized(mRadioStations) { result = mRadioStations.size }
        return result
    }

    /**
     * Method return index of the [RadioStation] in the collection.
     *
     * @param mediaId Id of the Radio Station.
     * @return Index of the Radio Station in the collection.
     */
    fun getIndex(mediaId: String?): Int {
        if (TextUtils.isEmpty(mediaId)) {
            return MediaSessionCompat.QueueItem.UNKNOWN_ID
        }
        var index = 0
        synchronized(mRadioStations) {
            for (item in mRadioStations) {
                if (mediaId == item.id) {
                    return index
                }
                index++
            }
        }
        return MediaSessionCompat.QueueItem.UNKNOWN_ID
    }

    /**
     * @param id
     * @return
     */
    fun getById(id: String?): RadioStation? {
        var result: RadioStation? = null
        if (id.isNullOrEmpty()) {
            return result
        }
        synchronized(mRadioStations) {
            for (item in mRadioStations) {
                if (TextUtils.equals(item.id, id)) {
                    result = item
                    break
                }
            }
        }
        return result
    }

    /**
     * @param mediaId
     * @return
     */
    fun remove(mediaId: String?): RadioStation? {
        var result: RadioStation? = null
        if (TextUtils.isEmpty(mediaId)) {
            return result
        }
        synchronized(mRadioStations) {
            for (radioStation in mRadioStations) {
                if (TextUtils.equals(radioStation.id, mediaId)) {
                    mRadioStations.remove(radioStation)
                    result = radioStation
                    break
                }
            }
        }
        return result
    }

    fun getAt(index: Int): RadioStation? {
        if (index < 0) {
            return null
        }
        if (index >= size()) {
            return null
        }
        var result: RadioStation
        synchronized(mRadioStations) { result = mRadioStations[index] }
        return result
    }

    /**
     * @param index
     * @return
     */
    fun isIndexPlayable(index: Int): Boolean {
        return index >= 0 && index < size()
    }

    /**
     * Clear destination and copy collection from source.
     *
     * @param source Source collection.
     */
    fun clearAndCopy(source: List<RadioStation>) {
        clear()
        addAll(source)
    }

    val all: List<RadioStation>
        get() {
            synchronized(mRadioStations) { return ArrayList(mRadioStations) }
        }

    companion object {
        /**
         * Merge Radio Stations from listB to listA.
         *
         * @param listA
         * @param listB
         */
        @JvmStatic
        fun merge(listA: MutableList<RadioStation>?, listB: List<RadioStation>?) {
            if (listA == null || listB == null) {
                return
            }
            for (radioStation in listB) {
                if (listA.contains(radioStation)) {
                    continue
                }
                listA.add(radioStation)
            }
        }
    }
}
