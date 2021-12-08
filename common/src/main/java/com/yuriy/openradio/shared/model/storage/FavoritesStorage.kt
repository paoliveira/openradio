/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONObject

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class FavoritesStorage : AbstractRadioStationsStorage() {

    /**
     * Cache key of the Favorite Radio Station in order to ease load from preferences.
     */
    private val mSet = mutableMapOf<String, Boolean>()

    /**
     * {@inheritDoc}
     */
    fun getAllFavoritesFromString(context: Context, marshalledRadioStations: String): List<RadioStation> {
        return getAllFromString(context, marshalledRadioStations)
    }

    /**
     * Add provided [RadioStation] to the Favorites preferences, or update it if already there.
     *
     * @param radioStation [RadioStation] to add to Favorites.
     * @param context      Context of the callee.
     */
    @Synchronized
    fun add(radioStation: RadioStation, context: Context) {
        val key = createKeyForRadioStation(radioStation)
        mSet[key] = true
        if (radioStation.sortId == MediaSessionCompat.QueueItem.UNKNOWN_ID) {
            radioStation.sortId = mSet.size
        }
        add(radioStation, context, FILE_NAME)
    }

    /**
     * Remove provided [RadioStation] from the Favorites preferences
     * by the provided media Id.
     *
     * @param radioStation [RadioStation] to remove from Favorites.
     * @param context Context of the callee.
     */
    @Synchronized
    fun remove(radioStation: RadioStation, context: Context) {
        val key = createKeyForRadioStation(radioStation)
        mSet.remove(key)
        remove(radioStation, context, FILE_NAME)
    }

    /**
     * Return collection of the Favorite Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Favorites Radio stations.
     */
    fun getAll(context: Context): MutableList<RadioStation> {
        return getAll(context, FILE_NAME)
    }

    fun getAllAsJson(context: Context): Map<String, JSONObject> {
        return getAllAsJson(context, FILE_NAME)
    }

    fun addAll(context: Context, list: List<RadioStation>) {
        return addAll(context, FILE_NAME, list)
    }

    /**
     * Return Favorite Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Favorite Radio Stations in a String representation.
     */
    fun getAllFavoritesAsString(context: Context): String {
        return getAllAsString(context, FILE_NAME)
    }

    /**
     * Determines whether Favorites collection is empty or not.
     *
     * @param context Context of the callee.
     * @return True in case of the are Favorites in collection, False - otherwise.
     */
    fun isFavoritesEmpty(context: Context): Boolean {
        return isEmpty(context, FILE_NAME, setOf(KEY_IS_NEW_SORT_FEATURE))
    }

    /**
     * Check whether provided [RadioStation] is in Favorites preferences.
     *
     * @param radioStation [RadioStation] to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    fun isFavorite(radioStation: RadioStation, context: Context): Boolean {
        val key = createKeyForRadioStation(radioStation)
        if (mSet[key] != null) {
            return mSet[key] ?: false
        }
        val list = getAll(context)
        for (station in list) {
            if (station.id == radioStation.id) {
                mSet[key] = true
                return true
            }
        }
        mSet[key] = false
        return false
    }

    fun getNewSortFeatureInited(context: Context): Boolean {
        return getSharedPreferences(context, FILE_NAME).getBoolean(KEY_IS_NEW_SORT_FEATURE, false)
    }

    fun setNewSortFeatureInited(context: Context, value: Boolean) {
        getEditor(context, FILE_NAME).putBoolean(KEY_IS_NEW_SORT_FEATURE, value).apply()
    }

    companion object {
        /**
         * Name of the file for the Favorite Preferences.
         */
        private const val FILE_NAME = "FavoritesPreferences"

        private const val KEY_IS_NEW_SORT_FEATURE = "KEY_IS_NEW_SORT_FEATURE"
    }
}
