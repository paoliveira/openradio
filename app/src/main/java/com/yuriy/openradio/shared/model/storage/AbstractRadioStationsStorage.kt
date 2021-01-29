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
import android.support.v4.media.session.MediaSessionCompat
import com.yuriy.openradio.shared.model.translation.RadioStationDeserializer
import com.yuriy.openradio.shared.model.translation.RadioStationJsonDeserializer
import com.yuriy.openradio.shared.model.translation.RadioStationJsonSerializer
import com.yuriy.openradio.shared.model.translation.RadioStationSerializer
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class AbstractRadioStationsStorage : AbstractStorage() {

    companion object {

        private const val KEY_VALUE_DELIMITER = "<:>"
        private const val KEY_VALUE_PAIR_DELIMITER = "<<::>>"

        /**
         * Add provided [RadioStation] to the storage.
         *
         * @param radioStation [RadioStation] to add to the storage.
         * @param context      Context of the callee.
         * @param name         Name of the file for the preferences.
         */
        @JvmStatic
        @Synchronized
        protected fun add(radioStation: RadioStation, context: Context, name: String) {
            addInternal(createKeyForRadioStation(radioStation), radioStation, context, name)
        }

        /**
         * Add provided [RadioStation] to the storage.
         *
         * @param key          Key for the Radio Station.
         * @param radioStation [RadioStation] to add to the storage.
         * @param context      Context of the callee.
         * @param name         Name of the file for the preferences.
         */
        @JvmStatic
        @Synchronized
        protected fun add(key: String,
                          radioStation: RadioStation,
                          context: Context,
                          name: String) {
            val all = getAll(context, name)
            var maxSortId = MediaSessionCompat.QueueItem.UNKNOWN_ID
            for (radioStationLocal in all) {
                if (radioStationLocal.sortId > maxSortId) {
                    maxSortId = radioStationLocal.sortId
                }
            }
            if (radioStation.sortId == MediaSessionCompat.QueueItem.UNKNOWN_ID) {
                radioStation.sortId = maxSortId + 1
            }
            addInternal(key, radioStation, context, name)
        }

        /**
         * Remove provided [RadioStation] from the storage by the provided Media Id.
         *
         * @param radioStation [RadioStation] to remove from the storage.
         * @param context      Context of the callee.
         * @param name         Name of the file for the preferences.
         */
        @JvmStatic
        @Synchronized
        protected fun remove(radioStation: RadioStation, context: Context, name: String) {
            val editor = getEditor(context, name)
            editor.remove(createKeyForRadioStation(radioStation))
            editor.apply()
            i("Radio Station $radioStation removed")
        }

        /**
         * Retrieves all data stored and returns as a String where Radio Station represented as String mapped to its key.
         *
         * @param context Context of the callee.
         * @param name    Name of the file for the preferences.
         * @return Stored data as String.
         */
        @JvmStatic
        fun getAllAsString(context: Context?, name: String): String {
            val sharedPreferences = getSharedPreferences(context!!, name)
            val map = sharedPreferences.all
            var value: String
            val builder = StringBuilder()
            for (key in map.keys) {
                value = map[key].toString()
                if (value.isEmpty()) {
                    continue
                }
                builder.append(key).append(KEY_VALUE_DELIMITER).append(value).append(KEY_VALUE_PAIR_DELIMITER)
            }
            if (builder.length >= KEY_VALUE_PAIR_DELIMITER.length) {
                builder.delete(builder.length - KEY_VALUE_PAIR_DELIMITER.length, builder.length)
            }
            val result = builder.toString()
            d("$name, getAllAsString:$result")
            return result
        }

        /**
         * Demarshall string representation of the Radio Stations into the Java list of items.
         *
         * @param marshalledRadioStations String representation of the Radio Stations,
         * obtained from the [.getAllAsString]
         * @return List of Radio Stations.
         */
        @JvmStatic
        fun getAllFromString(context: Context, marshalledRadioStations: String): List<RadioStation> {
            val list: MutableList<RadioStation> = ArrayList()
            if (marshalledRadioStations.isEmpty()) {
                return list
            }
            val deserializer: RadioStationDeserializer = RadioStationJsonDeserializer()
            val radioStationsPairs = marshalledRadioStations.split(KEY_VALUE_PAIR_DELIMITER.toRegex()).toTypedArray()
            var radioStationKeyValue: Array<String>
            var radioStation: RadioStation?
            for (radioStationString in radioStationsPairs) {
                radioStationKeyValue = radioStationString.split(KEY_VALUE_DELIMITER.toRegex()).toTypedArray()
                if (radioStationKeyValue.size != 2) {
                    continue
                }
                if (radioStationKeyValue[1].isEmpty()) {
                    continue
                }
                radioStation = deserializer.deserialize(context, radioStationKeyValue[1])
                if (radioStation == null) {
                    e("Can not deserialize (getAllFromString) from '" + radioStationKeyValue[1] + "'")
                    continue
                }
                list.add(radioStation)
            }
            return list
        }

        /**
         * Return collection of the Radio Stations which are stored in the persistent storage.
         *
         * @param context Context of the callee.
         * @param name    Name of the file for the preferences.
         * @return Collection of the Radio Stations.
         */
        @JvmStatic
        fun getAll(context: Context, name: String): MutableList<RadioStation> {
            // TODO: Return cache when possible
            val radioStations: MutableList<RadioStation> = ArrayList()
            val sharedPreferences = getSharedPreferences(context, name)
            val map = sharedPreferences.all
            val deserializer: RadioStationDeserializer = RadioStationJsonDeserializer()
            var radioStation: RadioStation?
            var value: String
            var counter = 0
            var isListSorted: Boolean? = null
            for (key in map.keys) {
                // This is not Radio Station
                if (LocalRadioStationsStorage.isKeyId(key)) {
                    continue
                }
                value = map[key].toString()
                if (value.isEmpty()) {
                    continue
                }
                radioStation = deserializer.deserialize(context, value)
                if (radioStation == null) {
                    e("Can not deserialize (getAll) from '$value'")
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
                    addInternal(createKeyForRadioStation(radioStation), radioStation, context, name)
                }
            }
            return radioStations
        }

        /**
         * Determines whether collection is empty or not.
         *
         * @param context Context of the callee.
         * @param name    Name of the file for the preferences.
         * @return `true` in case of the are items in collection, `false` - otherwise.
         */
        @JvmStatic
        protected fun isEmpty(context: Context, name: String): Boolean {
            val sharedPreferences = getSharedPreferences(context, name)
            val map = sharedPreferences.all
            return map.isEmpty()
        }

        /**
         * Add provided [RadioStation] to the storage.
         *
         * @param key          Key for the Radio Station.
         * @param radioStation [RadioStation] to add to the storage.
         * @param context      Context of the callee.
         * @param name         Name of the file for the preferences.
         */
        @Synchronized
        private fun addInternal(key: String, radioStation: RadioStation, context: Context, name: String) {
            val serializer: RadioStationSerializer = RadioStationJsonSerializer()
            val editor = getEditor(context, name)
            editor.putString(key, serializer.serialize(radioStation))
            editor.apply()
            i("Radio Station added $radioStation")
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
    }
}