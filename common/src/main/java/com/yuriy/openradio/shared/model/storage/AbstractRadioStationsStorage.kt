/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.support.v4.media.session.MediaSessionCompat
import com.yuriy.openradio.shared.model.translation.RadioStationJsonDeserializer
import com.yuriy.openradio.shared.model.translation.RadioStationJsonSerializer
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.isInvalid
import java.lang.ref.WeakReference
import java.util.TreeSet

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class AbstractRadioStationsStorage(contextRef: WeakReference<Context>, name: String) :
    AbstractStorage(contextRef, name) {

    /**
     * Add provided [RadioStation] to the storage.
     *
     * @param radioStation [RadioStation] to add to the storage.
     */
    @Synchronized
    fun add(radioStation: RadioStation, key: String = createKeyForRadioStation(radioStation)) {
        val serializer = RadioStationJsonSerializer()
        putStringValue(key, serializer.serialize(radioStation))
    }

    @Synchronized
    open fun get(key: String): RadioStation {
        val radioStation = getStringValue(key, AppUtils.EMPTY_STRING)
        val deserializer = RadioStationJsonDeserializer()
        return deserializer.deserialize(radioStation)
    }

    /**
     * Remove provided [RadioStation] from the storage by the provided Media Id.
     *
     * @param radioStation [RadioStation] to remove from the storage.
     */
    @Synchronized
    open fun remove(radioStation: RadioStation) {
        removeKey(createKeyForRadioStation(radioStation))
    }

    @Synchronized
    fun clear() {
        clearStorage()
    }

    /**
     * Retrieves all data stored and returns as a String where Radio Station represented as String mapped to its key.
     *
     * @return Stored data as String.
     */
    fun getAllAsString(): String {
        val map = getAllValues()
        val builder = StringBuilder()
        for (key in map.keys) {
            val value = map[key].toString()
            if (value.isEmpty()) {
                continue
            }
            builder.append(key).append(KEY_VALUE_DELIMITER).append(value).append(KEY_VALUE_PAIR_DELIMITER)
        }
        if (builder.length >= KEY_VALUE_PAIR_DELIMITER.length) {
            builder.delete(builder.length - KEY_VALUE_PAIR_DELIMITER.length, builder.length)
        }
        val result = builder.toString()
        AppLogger.d("getAllAsString:$result")
        return result
    }

    /**
     * Demarshall string representation of the Radio Stations into the Java list of items.
     *
     * @param marshalledRadioStations String representation of the Radio Stations,
     * obtained from the [.getAllAsString]
     * @return List of Radio Stations.
     */
    fun getAllFromString(marshalledRadioStations: String): Set<RadioStation> {
        val list = TreeSet<RadioStation>()
        if (marshalledRadioStations.isEmpty()) {
            return list
        }
        val deserializer = RadioStationJsonDeserializer()
        val radioStationsPairs = marshalledRadioStations.split(KEY_VALUE_PAIR_DELIMITER.toRegex()).toTypedArray()
        for (radioStationString in radioStationsPairs) {
            val keyValue = radioStationString.split(KEY_VALUE_DELIMITER.toRegex()).toTypedArray()
            if (keyValue.size != 2) {
                continue
            }
            if (keyValue[1].isEmpty()) {
                continue
            }
            val radioStation = deserializer.deserialize(keyValue[1])
            if (radioStation.isInvalid()) {
                AppLogger.e("Can not deserialize (getAllFromString) from '${keyValue[1]}'")
                continue
            }
            list.add(radioStation)
        }
        return list
    }

    /**
     * Return collection of the Radio Stations which are stored in the persistent storage.
     *
     * @return Collection of the Radio Stations.
     */
    fun getAll(): TreeSet<RadioStation> {
        // TODO: Return cache when possible
        val radioStations = TreeSet<RadioStation>()
        val map = getAllValues()
        val deserializer = RadioStationJsonDeserializer()
        var counter = 0
        var isListSorted: Boolean? = null
        for (key in map.keys) {
            // This is not Radio Station
            if (DeviceLocalsStorage.isKeyId(key)) {
                continue
            }
            val value = map[key].toString()
            val radioStation = deserializer.deserialize(value)
            if (radioStation.isInvalid()) {
                AppLogger.e("Can not deserialize (getAll) from '$value'")
                continue
            }

            // This is not valid Radio Station. It can be happen in case of there is assigned ID
            // but actual Radio Station is not created yet. Probably it is necessary to re-design
            // functionality to avoid such scenario.
            if (radioStation.isMediaStreamEmpty()) {
                continue
            }

            radioStations.add(radioStation)

            // This is solution for the new functionality - drag and drop in order to sort
            // Assume that if there is undefined sort id then user runs application with
            // new feature with Radio Stations already in Favorites.
            // Just assign another incremental value.
            if (isListSorted == null) {
                isListSorted = radioStation.sortId != MediaSessionCompat.QueueItem.UNKNOWN_ID
            }
            if (!isListSorted) {
                radioStation.sortId = counter++
                add(radioStation)
            }
        }
        return radioStations
    }

    fun addAll(set: Set<RadioStation>) {
        val serializer = RadioStationJsonSerializer()
        for (radioStation in set) {
            putStringValue(createKeyForRadioStation(radioStation), serializer.serialize(radioStation))
        }
    }

    /**
     * Creates a key for given Radio Station to use in storage.
     *
     * @param radioStation [RadioStation] to create key for.
     * @return Key associated with Radio Station.
     */
    fun createKeyForRadioStation(radioStation: RadioStation): String {
        return radioStation.id
    }

    companion object {

        private const val KEY_VALUE_DELIMITER = "<:>"
        private const val KEY_VALUE_PAIR_DELIMITER = "<<::>>"
    }
}
