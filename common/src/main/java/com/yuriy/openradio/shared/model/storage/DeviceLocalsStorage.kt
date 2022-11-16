/*
 * Copyright 2017, 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.vo.MediaStream.Companion.BITRATE_DEFAULT
import com.yuriy.openradio.shared.vo.MediaStream.Companion.makeDefaultInstance
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class DeviceLocalsStorage(
    context: Context,
    private val mFavoritesStorage: FavoritesStorage,
    private val mLatestRadioStationStorage: LatestRadioStationStorage,
) : AbstractRadioStationsStorage(context) {

    /**
     * Set value of the Radio Station Id.
     *
     * @param value Value of the Radio Station Id.
     */
    private fun setId(value: Int) {
        val editor = getEditor(FILE_NAME)
        editor.putInt(KEY_ID, value)
        editor.apply()
    }

    /**
     * Creates a value of Id for Radio Station.
     *
     * @return The value of the Radio Station Id.
     */
    fun getId(): String {
        val sharedPreferences = getSharedPreferences(FILE_NAME)
        var id = sharedPreferences.getInt(KEY_ID, Int.MAX_VALUE)
        // If value is Integer MAX, means that this is the first call, initialize it and addToLocals.
        if (id == Int.MAX_VALUE) {
            setId(ID_INIT_VALUE)
            return ID_INIT_VALUE.toString()
        }
        // Increment previous value, addToLocals it and return it.
        id += 1
        setId(id)
        return id.toString()
    }

    /**
     * Add provided [RadioStation] to the Local Radio Stations preferences.
     *
     * @param radioStation [RadioStation] to add to the Local Radio Stations.
     */
    @Synchronized
    fun add(radioStation: RadioStation) {
        add(radioStation, FILE_NAME)
    }

    @Synchronized
    fun addAll(list: List<RadioStation>) {
        return addAll(FILE_NAME, list)
    }

    /**
     * Remove provided [RadioStation] from the Local radio Stations preferences
     * by the provided media Id.
     *
     * @param radioStation [RadioStation] to remove from the Local Radio Stations.
     */
    @Synchronized
    fun remove(radioStation: RadioStation) {
        remove(radioStation, FILE_NAME)
    }

    /**
     * Update Radio Station with provided values.
     *
     * @param mediaId  Media Id of the [RadioStation].
     * @param name     Name of Radio Station.
     * @param url      URL of stream associated with Radio Station.
     * @param imageUrl URL of image associated with Radio Stream.
     * @param genre    Genre of Radio Station.
     * @param country  Country associated with Radio Station.
     * @param addToFav Whether or not Radio Station is in Favorite category.
     * @return `true` in case of success or `false` if Radio Station was not found.
     */
    @Synchronized
    fun update(
        mediaId: String?, name: String?, url: String?, imageUrl: String?,
        genre: String?, country: String?, addToFav: Boolean
    ): Boolean {
        var result = false
        val list = getAll(FILE_NAME)
        for (radioStation in list) {
            if (radioStation.id.endsWith(mediaId!!)) {
                remove(radioStation)
                mFavoritesStorage.remove(radioStation)
                radioStation.name = name!!
                val mediaStream = makeDefaultInstance()
                mediaStream.setVariant(BITRATE_DEFAULT, url!!)
                radioStation.mediaStream = mediaStream
                radioStation.imageUrl = imageUrl ?: AppUtils.EMPTY_STRING
                radioStation.genre = genre!!
                radioStation.country = country!!
                if (addToFav) {
                    mFavoritesStorage.add(radioStation)
                }
                add(radioStation)
                val current = mLatestRadioStationStorage.get()
                if (current.id.endsWith(mediaId)) {
                    mLatestRadioStationStorage.add(radioStation)
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
     * @return Radio Station or `null` if there was nothing found.
     */
    @Synchronized
    operator fun get(mediaId: String): RadioStation {
        val list = getAll(FILE_NAME)
        for (radioStation in list) {
            if (radioStation.id.endsWith(mediaId)) {
                return radioStation
            }
        }
        return RadioStation.INVALID_INSTANCE
    }

    /**
     * Return Local added Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @return Local added Radio Stations in a String representation.
     */
    fun getAllLocalAsString(): String {
        return getAllAsString(FILE_NAME)
    }

    /**
     * Return collection of the Local Radio Stations which are stored in the persistent storage.
     *
     * @return Collection of the Local Radio Stations.
     */
    fun getAllLocals(): ArrayList<RadioStation> {
        return getAll(FILE_NAME)
    }

    companion object {

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
         * Check whether provided value is equal to [.KEY_ID].
         *
         * @param value Value ot compare of.
         * @return `true` in case of value is [.KEY_ID], `false` otherwise.
         */
        fun isKeyId(value: String?): Boolean {
            return KEY_ID == value
        }
    }
}
