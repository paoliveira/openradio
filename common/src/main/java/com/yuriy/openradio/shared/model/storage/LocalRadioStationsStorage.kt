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
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.vo.MediaStream.Companion.makeDefaultInstance
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object LocalRadioStationsStorage : AbstractRadioStationsStorage() {
    /**
     * Name of the file for the Favorite Preferences.
     */
    private const val FILE_NAME = "LocalRadioStationsPreferences"

    /**
     * Key for Radio Station Id.
     */
    private const val KEY_ID = "KEY_ID"

    /**
     * Init value of the custom Radio Station Id.
     */
    private const val ID_INIT_VALUE = Int.MAX_VALUE - 1000000

    /**
     * Set value of the Radio Station Id.
     *
     * @param context Applications context.
     * @param value   Value of the Radio Station Id.
     */
    private fun setId(context: Context, value: Int) {
        val editor = getEditor(context, FILE_NAME)
        editor.putInt(KEY_ID, value)
        editor.apply()
    }

    /**
     * Creates a value of Id for Radio Station.
     *
     * @param context Applications context.
     * @return The value of the Radio Station Id.
     */
    fun getId(context: Context): String {
        val sharedPreferences = getSharedPreferences(context, FILE_NAME)
        var id = sharedPreferences.getInt(KEY_ID, Int.MAX_VALUE)
        // If value is Integer MAX, means that this is the first call, initialize it and addToLocals.
        if (id == Int.MAX_VALUE) {
            setId(context, ID_INIT_VALUE)
            return ID_INIT_VALUE.toString()
        }
        // Increment previous value, addToLocals it and return it.
        id += 1
        setId(context, id)
        return id.toString()
    }

    /**
     * Check whether provided value is equal to [.KEY_ID].
     *
     * @param value Value ot compare of.
     * @return `true` in case of value is [.KEY_ID], `false` otherwise.
     */
    fun isKeyId(value: String?): Boolean {
        return KEY_ID == value
    }

    /**
     * Add provided [RadioStation] to the Local Radio Stations preferences.
     *
     * @param radioStation [RadioStation] to add to the Local Radio Stations.
     * @param context      Context of the callee.
     */
    @JvmStatic
    @Synchronized
    fun add(radioStation: RadioStation?, context: Context) {
        add(radioStation!!, context, FILE_NAME)
    }

    @JvmStatic
    fun addAll(context: Context, list: List<RadioStation>) {
        return addAll(context, FILE_NAME, list)
    }

    /**
     * Remove provided [RadioStation] from the Local radio Stations preferences
     * by the provided media Id.
     *
     * @param radioStation [RadioStation] to remove from the Local Radio Stations.
     * @param context Context of the callee.
     */
    @Synchronized
    fun remove(radioStation: RadioStation?, context: Context) {
        remove(radioStation!!, context, FILE_NAME)
    }

    /**
     * Update Radio Station with provided values.
     *
     * @param mediaId  Media Id of the [RadioStation].
     * @param context  Context of the callee.
     * @param name     Name of Radio Station.
     * @param url      URL of stream associated with Radio Station.
     * @param imageUrl URL of image associated with Radio Stream.
     * @param genre    Genre of Radio Station.
     * @param country  Country associated with Radio Station.
     * @param addToFav Whether or not Radio Station is in Favorite category.
     * @return `true` in case of success or `false` if Radio Station was not found.
     */
    @Synchronized
    fun update(mediaId: String?, context: Context,
               name: String?, url: String?, imageUrl: String?,
               genre: String?, country: String?, addToFav: Boolean): Boolean {
        var result = false
        val list = getAll(context, FILE_NAME)
        for (radioStation in list) {
            if (radioStation.id.endsWith(mediaId!!)) {
                remove(radioStation, context)
                FavoritesStorage.remove(radioStation, context)
                radioStation.name = name!!
                val mediaStream = makeDefaultInstance()
                mediaStream.setVariant(128, url!!)
                radioStation.mediaStream = mediaStream
                radioStation.setImgUrl(context, imageUrl)
                radioStation.genre = genre!!
                radioStation.country = country!!
                if (addToFav) {
                    FavoritesStorage.add(radioStation, context)
                }
                add(radioStation, context)
                val current = LatestRadioStationStorage[context]
                if (current != null && current.id.endsWith(mediaId)) {
                    LatestRadioStationStorage.add(radioStation, context)
                }
                d("Radio station updated to:$radioStation")
                result = true
                break
            }
        }
        return result
    }

    /**
     * Return Radio Station object associated with media id.
     *
     * @param mediaId Media Id of the [RadioStation].
     * @param context Context of the callee.
     * @return Radio Station or `null` if there was nothing found.
     */
    @Synchronized
    operator fun get(mediaId: String?, context: Context): RadioStation? {
        val list = getAll(context, FILE_NAME)
        for (radioStation in list) {
            if (radioStation.id.endsWith(mediaId!!)) {
                return radioStation
            }
        }
        return null
    }

    /**
     * Return Local added Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Local added Radio Stations in a String representation.
     */
    @JvmStatic
    fun getAllLocalAsString(context: Context): String {
        return getAllAsString(context, FILE_NAME)
    }

    /**
     * {@inheritDoc}
     */
    @JvmStatic
    fun getAllLocalsFromString(context: Context,
                               marshalledRadioStations: String): List<RadioStation> {
        return getAllFromString(context, marshalledRadioStations)
    }

    /**
     * Return collection of the Local Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Local Radio Stations.
     */
    @JvmStatic
    fun getAllLocals(context: Context): MutableList<RadioStation> {
        val list = getAll(context, FILE_NAME)
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (radioStation in list) {
            if (radioStation.id.isEmpty()) {
                list.remove(radioStation)
                break
            }
        }
        return list
    }

    /**
     * Determines whether Local Radio Stations collection is empty or not.
     *
     * @param context Context of the callee.
     * @return `true` in case of the are Local Radio Stations in collection,
     * `false` - otherwise.
     */
    fun isLocalsEmpty(context: Context): Boolean {
        val list = getAll(context, FILE_NAME)
        // Loop for the key that holds KEY for the next Local Radio Station
        // and remove it from collection.
        for (radioStation in list) {
            if (radioStation.id.isEmpty()) {
                list.remove(radioStation)
                break
            }
        }
        return list.isEmpty()
    }
}
