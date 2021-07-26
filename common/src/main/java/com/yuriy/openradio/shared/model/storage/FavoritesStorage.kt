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
import com.yuriy.openradio.shared.dependencies.DependencyRegistry
import com.yuriy.openradio.shared.model.api.ApiServiceProviderImpl
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.model.storage.cache.CacheType
import com.yuriy.openradio.shared.model.storage.images.ImagesDatabase
import com.yuriy.openradio.shared.vo.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    private const val KEY_IS_NEW_SORT_FEATURE = "KEY_IS_NEW_SORT_FEATURE"

    /**
     * Cache key of the Favorite Radio Station in order to ease load from preferences.
     */
    private val sSet = mutableMapOf<String, Boolean>()

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
    fun add(radioStation: RadioStation, context: Context) {
        val key = createKeyForRadioStation(radioStation)
        sSet[key] = true
        if (radioStation.sortId == MediaSessionCompat.QueueItem.UNKNOWN_ID) {
            radioStation.sortId = sSet.size
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
    @JvmStatic
    @Synchronized
    fun remove(radioStation: RadioStation, context: Context) {
        val key = createKeyForRadioStation(radioStation)
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
        val data = getAll(context, FILE_NAME)
        for (radioStation in data) {
            checkImageUrl(context, radioStation.id)
        }
        return getAll(context, FILE_NAME)
    }

    @JvmStatic
    fun addAll(context: Context, list: List<RadioStation>) {
        return addAll(context, FILE_NAME, list)
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
        return isEmpty(context, FILE_NAME, setOf(KEY_IS_NEW_SORT_FEATURE))
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
        if (sSet[key] != null) {
            return sSet[key] ?: false
        }
        val list = getAll(context)
        for (station in list) {
            if (station.id == radioStation.id) {
                sSet[key] = true
                return true
            }
        }
        sSet[key] = false
        return false
    }

    fun getNewSortFeatureInited(context: Context): Boolean {
        return getSharedPreferences(context, FILE_NAME).getBoolean(KEY_IS_NEW_SORT_FEATURE, false)
    }

    fun setNewSortFeatureInited(context: Context, value: Boolean) {
        getEditor(context, FILE_NAME).putBoolean(KEY_IS_NEW_SORT_FEATURE, value).apply()
    }

    private fun checkImageUrl(context: Context, id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val db = ImagesDatabase.getInstance(context)
            val image = db.rsImageDao().getImage(id)
            if (image != null) {
                return@launch
            }
            val provider =
                ApiServiceProviderImpl(context, DependencyRegistry.getParser(), DependencyRegistry.getNetMonitor())
            provider.getStation(
                DependencyRegistry.getDownloader(), UrlBuilder.getStation(id), CacheType.NONE
            )
        }
    }
}
