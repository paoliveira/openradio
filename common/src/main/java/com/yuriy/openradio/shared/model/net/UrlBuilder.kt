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
package com.yuriy.openradio.shared.model.net

import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.vo.RadioStationToAdd

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [UrlBuilder] is a helper class which performs
 * build URL of the different API calls.
 */
object UrlBuilder {

    /**
     * Id of the first page of the Radio Stations List.
     */
    const val FIRST_PAGE_INDEX = 0

    /**
     * Number of Radio Stations in each page.
     */
    const val ITEMS_PER_PAGE = 200
    const val BASE_URL_PREFIX = "https://do-look-up-dns-first"
    const val LOOK_UP_DNS = "all.api.radio-browser.info"

    val RESERVED_URLS = arrayOf(
        "https://de1.api.radio-browser.info",
        "https://fr1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    )
    private const val RECENT_POPULAR_PER_PAGE = 200

    /**
     * Base URL for the API requests.
     */
    private const val BASE_URL = "$BASE_URL_PREFIX/json/"

    /**
     * Get Uri for the All Categories list.
     *
     * @return [Uri]
     */
    val allCategoriesUrl: Uri
        get() = Uri.parse(BASE_URL + "tags?reverse=true&order=stationcount&hidebroken=true")

    /**
     * Get Uri for the list of the Radio Stations in concrete Category.
     *
     * @param categoryId Id of the Category.
     * @return [Uri]
     */
    fun getStationsInCategory(categoryId: String,
                              pageNumber: Int,
                              numberPerPage: Int): Uri {
        return Uri.parse(
            BASE_URL + "stations/bytag/" + encodeValue(categoryId) + "?hidebroken=true&order=clickcount"
                + "&offset=" + pageNumber
                + "&limit=" + numberPerPage
        )
    }

    /**
     * Get Uri for the list of the Radio Stations in country.
     *
     * @param countryCode Country Code.
     * @return [Uri]
     */
    fun getStationsByCountry(countryCode: String,
                             pageNumber: Int,
                             numberPerPage: Int): Uri {
        return Uri.parse(
            BASE_URL + "stations/bycountrycodeexact/" + countryCode
                + "?offset=" + pageNumber
                + "&limit=" + numberPerPage
        )
    }

    /**
     * Get Uri for the list of the popular Radio Stations.
     *
     * @return [Uri]
     */
    fun getPopularStations(numOfStations: Int = RECENT_POPULAR_PER_PAGE): Uri {
        return Uri.parse(BASE_URL + "stations/topclick/" + numOfStations)
    }

    /**
     * Get Uri for the list of the recently added Radio Stations.
     *
     * @return [Uri]
     */
    fun getRecentlyAddedStations(numOfStations: Int = RECENT_POPULAR_PER_PAGE): Uri {
        return Uri.parse(BASE_URL + "stations/lastchange/" + numOfStations)
    }

    /**
     * Get Uri for the search.
     *
     * @return [Uri].
     */
    fun getSearchUrl(query: String): Uri {
        return Uri.parse(
            BASE_URL + "stations/search?name=" + encodeValue(query)
                + "&offset=" + 0
                + "&limit=" + ITEMS_PER_PAGE
        )
    }

    fun addStation(rsToAdd: RadioStationToAdd): Pair<Uri, List<Pair<String, String>>> {
        val postParams = ArrayList<Pair<String, String>>()
        postParams.add(Pair("name", rsToAdd.name))
        postParams.add(Pair("url", rsToAdd.url))
        postParams.add(Pair("homepage", rsToAdd.homePage))
        postParams.add(Pair("favicon", rsToAdd.imageWebUrl))
        postParams.add(Pair("countrycode", LocationService.COUNTRY_NAME_TO_CODE[rsToAdd.country]))
        postParams.add(Pair("tags", rsToAdd.genre))
        return Pair(Uri.parse(BASE_URL + "add"), postParams)
    }

    /**
     * Method to encode a string value using UTF-8 encoding scheme.
     *
     * @param value
     * @return
     */
    private fun encodeValue(value: String): String {
        return value.replace(" ".toRegex(), "%20")
    }
}
