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

package com.yuriy.openradio.shared.utils

import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Utility class to help on queue related tasks.
 * Media IDs used on browseable items of [android.media.browse.MediaBrowser].
 */
object MediaIdHelper {

    const val MEDIA_ID_ROOT = "__ROOT__"
    const val MEDIA_ID_ALL_CATEGORIES = "__ALL_CATEGORIES__"
    const val MEDIA_ID_COUNTRY_STATIONS = "__COUNTRY_STATIONS__"
    const val MEDIA_ID_COUNTRIES_LIST = "__COUNTRIES_LIST__"
    const val MEDIA_ID_FAVORITES_LIST = "__FAVORITES_LIST__"
    const val MEDIA_ID_LOCAL_RADIO_STATIONS_LIST = "__MEDIA_ID_LOCAL_RADIO_STATIONS_LIST__"
    const val MEDIA_ID_CHILD_CATEGORIES = "__CHILD_CATEGORIES__"
    private const val MEDIA_ID_RADIO_STATIONS_IN_CATEGORY = "__RADIO_STATIONS_IN_CATEGORY__"
    const val MEDIA_ID_SEARCH_FROM_APP = "__SEARCH_FROM_APP__"
    const val MEDIA_ID_POPULAR_STATIONS = "__POPULAR_STATIONS__"
    private const val MEDIA_ID_ALL_STATIONS = "__ALL_STATIONS__"
    private const val MEDIA_ID_RECENT_PLAYED_SONGS = "__RECENT_PLAYED_SONGS__"
    const val MEDIA_ID_RECENT_ADDED_STATIONS = "__RECENT_ADDED_STATIONS__"
    const val MEDIA_ID_LIST_ENDED = "MEDIA_ID_LIST_ENDED"

    private val IDS = arrayOf(
            MEDIA_ID_ALL_CATEGORIES,
            MEDIA_ID_COUNTRIES_LIST,
            MEDIA_ID_COUNTRY_STATIONS,
            MEDIA_ID_FAVORITES_LIST,
            MEDIA_ID_LOCAL_RADIO_STATIONS_LIST,
            MEDIA_ID_CHILD_CATEGORIES,
            MEDIA_ID_RADIO_STATIONS_IN_CATEGORY,
            MEDIA_ID_ROOT,
            MEDIA_ID_SEARCH_FROM_APP,
            MEDIA_ID_ALL_STATIONS,
            MEDIA_ID_POPULAR_STATIONS,
            MEDIA_ID_RECENT_PLAYED_SONGS,
            MEDIA_ID_RECENT_ADDED_STATIONS
    )

    /**
     * Gets Id that is use to extract correct command implementation of the MediaItemCommand.
     *
     * @param value String pattern that represents loaded menu item.
     *
     * @return Extracted Id.
     */
    @JvmStatic
    fun getId(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        for (id in IDS) {
            if (value.startsWith(id) || value == id) {
                val country = getCountryCode(value)
                return if (country != null && country.isNotEmpty()) {
                    MEDIA_ID_COUNTRY_STATIONS
                } else id
            }
        }
        return null
    }

    /**
     * Checks whether provided category Id is belongs to the "Stations in the Country" Id.
     * If `true` - extract Country Code and return it, in case of `false` - return null.
     *
     * @param value Category Id.
     *
     * @return The value of the Country Code, `null` - otherwise.
     */
    @JvmStatic
    fun getCountryCode(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        if (!value.startsWith(MEDIA_ID_COUNTRIES_LIST) || value == MEDIA_ID_COUNTRIES_LIST) {
            return null
        }
        val result = value.substring(value.length - 2)
        return if (value.isNotEmpty() && result.length == 2) {
            result.uppercase(Locale.ROOT)
        } else null
    }

    /**
     * Returns whether or not current Catalog is sortable, for instance Favorites or
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return `true` in case of Catalog is sortable, `false` otherwise.
     */
    fun isMediaIdSortable(categoryMediaId: String): Boolean {
        return MEDIA_ID_FAVORITES_LIST == categoryMediaId || MEDIA_ID_LOCAL_RADIO_STATIONS_LIST == categoryMediaId
    }

    /**
     * Returns whether or not current Catalog is refreshable once end of list reached, for instance
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return `true` in case of Catalog is refreshable, `false` otherwise.
     */
    @JvmStatic
    fun isMediaIdRefreshable(categoryMediaId: String): Boolean {
        return (categoryMediaId.isNotEmpty()
                && (MEDIA_ID_ALL_STATIONS == categoryMediaId
                || MEDIA_ID_COUNTRY_STATIONS == categoryMediaId
                || MEDIA_ID_RADIO_STATIONS_IN_CATEGORY == categoryMediaId
                || categoryMediaId.contains(MEDIA_ID_COUNTRIES_LIST)
                || categoryMediaId.contains(MEDIA_ID_CHILD_CATEGORIES)))
    }
}
