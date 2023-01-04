package com.yuriy.openradio.shared.model.net

import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.NetUtils
import com.yuriy.openradio.shared.vo.RadioStationToAdd
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException
import java.util.Random

class UrlLayerRadioBrowserImpl: UrlLayer {

    private val mRandom = Random()
    private var mUrlsSet = Array(0) { AppUtils.EMPTY_STRING }

    /**
     * Do DNS look up in order to get available url for service connection.
     * Addresses, if found, are cached and next time is used from the cache.
     *
     * @param uri        Initial (with dummy and predefined prefix) url to perform look up on.
     * @param parameters Parameters associated with request.
     * @return URL object to do connection with.
     */
    override fun getConnectionUrl(
        uri: Uri,
        parameters: List<Pair<String, String>>
    ): URL? {
        // If there is no predefined prefix - return original URL.
        val uriStr = uri.toString()
        if (!uriStr.startsWith(BASE_URL_PREFIX)) {
            return getUrl(uriStr, parameters)
        }

        // Return cached URL if available.
        synchronized(mRandom) {
            if (mUrlsSet.isNotEmpty()) {
                val i = mRandom.nextInt(mUrlsSet.size)
                return getUrlModified(uriStr, mUrlsSet[i], parameters)
            }
        }

        // Perform look up and cache results.
        try {
            val list = InetAddress.getAllByName(LOOK_UP_DNS)
            synchronized(mRandom) {
                mUrlsSet = Array(list.size) { AppUtils.EMPTY_STRING }
                var i = 0
                for (item in list) {
                    mUrlsSet[i++] = "https://" + item.canonicalHostName
                    AppLogger.i("$TAG look up host:" + mUrlsSet[i - 1])
                }
            }
        } catch (exception: UnknownHostException) {
            AppLogger.e(
                "$TAG do lookup ${NetUtils.createExceptionMessage(uri, parameters)}",
                exception
            )
        }

        // Do random selection from available addresses.
        var url: URL? = null
        synchronized(mRandom) {
            if (mUrlsSet.isNotEmpty()) {
                val i = mRandom.nextInt(mUrlsSet.size)
                url = getUrlModified(uriStr, mUrlsSet[i], parameters)
            }
        }

        // Uri to URL parse might fail.
        if (url != null) {
            return url
        }

        // Use predefined addresses, these are needs to be verified time after time in order to be up to date.
        val i = mRandom.nextInt(RESERVED_URLS.size)
        return getUrlModified(uriStr, RESERVED_URLS[i], parameters)
    }

    override fun getAllCategoriesUrl(): Uri {
        return Uri.parse(BASE_URL + "tags?reverse=true&hidebroken=true")
    }

    override fun getStationsInCategory(categoryId: String, pageNumber: Int): Uri {
        return Uri.parse(
            BASE_URL + "stations/bytag/" + encodeValue(categoryId) + "?hidebroken=true&order=name"
                    + "&offset=" + (pageNumber * (ITEMS_PER_PAGE + 1))
                    + "&limit=" + ITEMS_PER_PAGE
        )
    }

    override fun getStationsByCountry(countryCode: String, pageNumber: Int): Uri {
        return Uri.parse(
            BASE_URL + "stations/bycountrycodeexact/" + countryCode
                    + "?offset=" + (pageNumber * (ITEMS_PER_PAGE + 1))
                    + "&limit=" + ITEMS_PER_PAGE
        )
    }

    override fun getPopularStations(): Uri {
        return Uri.parse(BASE_URL + "stations/topclick/" + ITEMS_PER_PAGE)
    }

    override fun getRecentlyAddedStations(): Uri {
        return Uri.parse(BASE_URL + "stations/lastchange/" + ITEMS_PER_PAGE)
    }

    override fun getSearchUrl(query: String): Uri {
        return Uri.parse(
            BASE_URL + "stations/search?name=" + encodeValue(query)
                    + "&offset=" + 0
                    + "&limit=" + ITEMS_PER_PAGE
        )
    }

    override fun getAddStationUrl(rsToAdd: RadioStationToAdd): Pair<Uri, List<Pair<String, String>>> {
        val postParams = ArrayList<Pair<String, String>>()
        postParams.add(Pair("name", rsToAdd.name))
        postParams.add(Pair("url", rsToAdd.url))
        postParams.add(Pair("homepage", rsToAdd.homePage))
        postParams.add(Pair("favicon", rsToAdd.imageWebUrl))
        postParams.add(Pair("countrycode", LocationService.COUNTRY_NAME_TO_CODE[rsToAdd.country]))
        postParams.add(Pair("tags", rsToAdd.genre))
        return Pair(Uri.parse(BASE_URL + "add"), postParams)
    }

    private fun getUrlModified(
        uriOrigin: String,
        uri: String?, parameters: List<Pair<String, String>>
    ): URL? {
        val uriModified = uriOrigin.replaceFirst(BASE_URL_PREFIX.toRegex(), uri!!)
        return getUrl(uriModified, parameters)
    }

    private fun getUrl(uri: String, parameters: List<Pair<String, String>>): URL? {
        return try {
            URL(uri)
        } catch (exception: MalformedURLException) {
            AppLogger.e(
                "$TAG getUrl ${
                    NetUtils.createExceptionMessage(
                        uri,
                        parameters
                    )
                }", exception
            )
            null
        }
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

    companion object {

        private const val TAG = "ULRBI"
        /**
         * Number of Radio Stations in each page.
         */
        private const val ITEMS_PER_PAGE = 20
        private const val LOOK_UP_DNS = "all.api.radio-browser.info"
        private const val BASE_URL_PREFIX = "https://do-look-up-dns-first"
        /**
         * Base URL for the API requests.
         */
        private const val BASE_URL = "${BASE_URL_PREFIX}/json/"
        private val RESERVED_URLS = arrayOf(
            "https://de1.api.radio-browser.info",
            "https://fr1.api.radio-browser.info",
            "https://nl1.api.radio-browser.info"
        )
    }
}
