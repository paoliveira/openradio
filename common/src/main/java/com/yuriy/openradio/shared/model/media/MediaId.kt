/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.media

import com.yuriy.openradio.shared.utils.AppUtils
import java.util.Locale

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Class to manage Media IDs used on browsable items of [android.media.browse.MediaBrowser].
 */

object MediaId {

    const val MEDIA_ID_ROOT = "__ROOT__"
    const val MEDIA_ID_ROOT_CAR = "__ROOT_CAR__"
    const val MEDIA_ID_BROWSE_CAR = "__BROWSE_CAR__"
    const val MEDIA_ID_ALL_CATEGORIES = "__ALL_CATEGORIES__"
    const val MEDIA_ID_COUNTRY_STATIONS = "__COUNTRY_STATIONS__"
    const val MEDIA_ID_COUNTRIES_LIST = "__COUNTRIES_LIST__"
    const val MEDIA_ID_FAVORITES_LIST = "__FAVORITES_LIST__"
    const val MEDIA_ID_LOCAL_RADIO_STATIONS_LIST = "__MEDIA_ID_LOCAL_RADIO_STATIONS_LIST__"
    const val MEDIA_ID_CHILD_CATEGORIES = "__CHILD_CATEGORIES__"
    const val MEDIA_ID_SEARCH_FROM_APP = "__SEARCH_FROM_APP__"
    const val MEDIA_ID_SEARCH_FROM_SERVICE = "__SEARCH_FROM_SERVICE__"
    const val MEDIA_ID_POPULAR_STATIONS = "__POPULAR_STATIONS__"
    const val MEDIA_ID_RECENT_STATIONS = "__RECENT_STATIONS__"
    const val MEDIA_ID_LIST_ENDED = "MEDIA_ID_LIST_ENDED"

    private const val MEDIA_ID_SEARCH_PREFIX = "search:"

    private val IDS = arrayOf(
        MEDIA_ID_BROWSE_CAR,
        MEDIA_ID_ALL_CATEGORIES,
        MEDIA_ID_COUNTRIES_LIST,
        MEDIA_ID_COUNTRY_STATIONS,
        MEDIA_ID_FAVORITES_LIST,
        MEDIA_ID_LOCAL_RADIO_STATIONS_LIST,
        MEDIA_ID_CHILD_CATEGORIES,
        MEDIA_ID_ROOT,
        MEDIA_ID_ROOT_CAR,
        MEDIA_ID_SEARCH_FROM_APP,
        MEDIA_ID_SEARCH_FROM_SERVICE,
        MEDIA_ID_POPULAR_STATIONS,
        MEDIA_ID_RECENT_STATIONS,
        MEDIA_ID_LIST_ENDED
    )

    fun normalizeFromSearchId(value: String): String {
        return value.replace(MEDIA_ID_SEARCH_PREFIX, "")
    }

    fun makeSearchId(value: String): String {
        return "$MEDIA_ID_SEARCH_PREFIX$value"
    }

    /**
     * Gets Id that is use to extract correct command implementation of the MediaItemCommand.
     *
     * @param value String pattern that represents loaded menu item.
     * @param isCar Whether or not the Open Radio runs on a car.
     *
     * @return Category Id.
     */
    fun getId(value: String, defaultCountryCode: String, isCar: Boolean = false): String {
        if (value.isEmpty()) {
            return AppUtils.EMPTY_STRING
        }
        if (value == MEDIA_ID_ROOT && isCar) {
            return MEDIA_ID_ROOT_CAR
        }
        for (id in IDS) {
            if (value == id) {
                return id
            }
            if (value.startsWith(id)) {
                val country = getCountryCode(value, defaultCountryCode)
                return if (country.isNotEmpty()) {
                    MEDIA_ID_COUNTRY_STATIONS
                } else id
            }
        }
        return AppUtils.EMPTY_STRING
    }

    /**
     * Checks whether provided category Id is belongs to the "Stations in the Country" Id.
     * If `true` - extract Country Code and return it, in case of `false` - return null.
     *
     * @param value Category Id.
     *
     * @return The value of the Country Code.
     */
    fun getCountryCode(value: String?, defaultCountryCode: String): String {
        if (value.isNullOrEmpty()) {
            // Otherwise, use whatever is stored in preferences.
            return defaultCountryCode
        }
        if (!value.startsWith(MEDIA_ID_COUNTRIES_LIST) || value == MEDIA_ID_COUNTRIES_LIST) {
            // Otherwise, use whatever is stored in preferences.
            return defaultCountryCode
        }
        val result = value.substring(value.length - 2)
        return if (value.isNotEmpty() && result.length == 2) {
            result.uppercase(Locale.ROOT)
        } else {
            // Otherwise, use whatever is stored in preferences.
            return defaultCountryCode
        }
    }

    fun isFromSearch(value: String): Boolean {
        return value.startsWith(MEDIA_ID_SEARCH_PREFIX)
    }

    /**
     * Returns whether or not current Catalog is sortable, for instance Favorites or
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return `true` in case of Catalog is sortable, `false` otherwise.
     */
    fun isSortable(categoryMediaId: String): Boolean {
        return MEDIA_ID_FAVORITES_LIST == categoryMediaId || MEDIA_ID_LOCAL_RADIO_STATIONS_LIST == categoryMediaId
    }

    /**
     * Returns whether or not current Catalog is refreshable once end of list reached, for instance
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return `true` in case of Catalog is refreshable, `false` otherwise.
     */
    fun isRefreshable(categoryMediaId: String): Boolean {
        return (categoryMediaId.isNotEmpty()
                && (MEDIA_ID_COUNTRY_STATIONS == categoryMediaId
                || categoryMediaId.contains(MEDIA_ID_COUNTRIES_LIST)
                || categoryMediaId.contains(MEDIA_ID_CHILD_CATEGORIES)))
    }
}
