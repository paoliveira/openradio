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
import android.support.v4.media.MediaBrowserCompat
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object FavoritesStorage : AbstractRadioStationsStorage() {
    /**
     * Name of the file for the Favorite Preferences.
     */
    private const val FILE_NAME = "FavoritesPreferences"

    /**
     * Cache key of the Favorite Radio Station in order to ease load from preferences.
     */
    private val sSet: MutableSet<String?> = HashSet()

    /**
     * {@inheritDoc}
     */
    @JvmStatic
    fun getAllFavoritesFromString(context: Context, marshalledRadioStations: String): List<RadioStation> {
        return getAllFromString(context, marshalledRadioStations)
    }

    /**
     * Add provided [RadioStation] to the Favorites preferences.
     *
     * @param radioStation [RadioStation] to add to Favorites.
     * @param context      Context of the callee.
     */
    @JvmStatic
    @Synchronized
    fun add(radioStation: RadioStation?, context: Context) {
        val key = createKeyForRadioStation(radioStation!!)
        sSet.add(key)
        add(radioStation, context, FILE_NAME)
    }

    /**
     * Remove provided [RadioStation] from the Favorites preferences
     * by the provided media Id.
     *
     * @param radioStation [RadioStation] to remove from Favorites.
     * @param context Context of the callee.
     */
    @JvmStatic
    @Synchronized
    fun remove(radioStation: RadioStation?, context: Context) {
        val key = createKeyForRadioStation(radioStation!!)
        sSet.remove(key)
        remove(radioStation, context, FILE_NAME)
    }

    /**
     * Return collection of the Favorite Radio Stations which are stored in the persistent storage.
     *
     * @param context Context of the callee.
     * @return Collection of the Favorites Radio stations.
     */
    @JvmStatic
    fun getAll(context: Context): MutableList<RadioStation> {
        return getAll(context, FILE_NAME)
    }

    /**
     * Return Favorite Radio Stations which are stored in the persistent storage represented in a single String.
     *
     * @param context Context of the callee.
     * @return Favorite Radio Stations in a String representation.
     */
    @JvmStatic
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
        return isEmpty(context, FILE_NAME)
    }

    /**
     * Check whether provided [RadioStation] is in Favorites preferences.
     *
     * @param radioStation [RadioStation] to check in the Favorites.
     * @param context      Context of the callee.
     * @return True in case of success, False - otherwise.
     */
    @JvmStatic
    fun isFavorite(radioStation: RadioStation, context: Context): Boolean {
        val key = createKeyForRadioStation(radioStation)
        if (sSet.contains(key)) {
            return true
        }
        val list = getAll(context)
        for (station in list) {
            if (station.id == radioStation.id) {
                sSet.add(key)
                return true
            }
        }
        return false
    }

    fun isFavorite(mediaItem: MediaBrowserCompat.MediaItem, context: Context): Boolean {
        val key = createKeyForRadioStation(mediaItem)
        if (sSet.contains(key)) {
            return true
        }
        val list = getAll(context)
        for (station in list) {
            if (station.id == mediaItem.mediaId) {
                sSet.add(key)
                return true
            }
        }
        return false
    }
}
