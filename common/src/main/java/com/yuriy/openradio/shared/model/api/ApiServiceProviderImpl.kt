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
package com.yuriy.openradio.shared.model.api

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.parser.DataParser
import com.yuriy.openradio.shared.model.parser.JsonDataParserImpl
import com.yuriy.openradio.shared.model.storage.cache.CacheType
import com.yuriy.openradio.shared.model.storage.cache.api.ApiCache
import com.yuriy.openradio.shared.model.storage.cache.api.InMemoryApiCache
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentAPIDbHelper
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentApiCache
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.NetUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.MediaStream
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [ApiServiceProviderImpl] is the implementation of the
 * [ApiServiceProvider] interface.
 *
 * @param context    Context of a callee.
 * @param dataParser Implementation of the [DataParser]
 */
class ApiServiceProviderImpl(context: Context, dataParser: DataParser) : ApiServiceProvider {
    /**
     * Implementation of the [DataParser] which allows to
     * parse raw response of the data into different formats.
     */
    private val mDataParser: DataParser?

    /**
     *
     */
    private val mContext: Context

    /**
     *
     */
    private val mApiCachePersistent: ApiCache
    private val mApiCacheInMemory: ApiCache
    override fun close() {
        if (mApiCachePersistent is PersistentApiCache) {
            mApiCachePersistent.close()
        }
        (mApiCacheInMemory as? InMemoryApiCache)?.clear()
    }

    override fun clear() {
        mApiCachePersistent.clear()
        mApiCacheInMemory.clear()
    }

    override fun getCategories(downloader: Downloader, uri: Uri, cacheType: CacheType): List<Category> {
        val allCategories: MutableList<Category> = ArrayList()
        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null")
            return allCategories
        }
        val array = downloadJsonArray(downloader, uri, cacheType)
        var item: JSONObject
        var category: Category
        for (i in 0 until array.length()) {
            try {
                item = array[i] as JSONObject
                category = Category.makeDefaultInstance()

                // TODO: Use data parser to parse JSON to value object
                if (item.has(JsonDataParserImpl.KEY_NAME)) {
                    category.id = item.getString(JsonDataParserImpl.KEY_NAME)
                    category.title = AppUtils.capitalize(item.getString(JsonDataParserImpl.KEY_NAME))
                    if (item.has(JsonDataParserImpl.KEY_STATIONS_COUNT)) {
                        category.stationsCount = item.getInt(JsonDataParserImpl.KEY_STATIONS_COUNT)
                    }
                }
                allCategories.add(category)
            } catch (e: Exception) {
                AppLogger.e("$e")
            }
        }
        return allCategories
    }

    override fun getCountries(downloader: Downloader, uri: Uri, cacheType: CacheType): List<Country> {
        val list: MutableList<Country> = ArrayList()
        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null")
            return list
        }
        for (countryName in LocationService.COUNTRY_NAME_TO_CODE.keys) {
            val countryCode = LocationService.COUNTRY_NAME_TO_CODE[countryName]
            if (countryCode == null) {
                AppLogger.e("Country code not found for $countryName")
                continue
            }
            list.add(Country(countryName, countryCode))
        }
        list.sortWith { c1: Country, c2: Country -> c1.name.compareTo(c2.name) }
        return list
    }

    override fun getStations(downloader: Downloader, uri: Uri, cacheType: CacheType): List<RadioStation> {
        return getStations(downloader, uri, ArrayList(), cacheType)
    }

    override fun getStations(downloader: Downloader,
                             uri: Uri,
                             parameters: List<Pair<String, String>>,
                             cacheType: CacheType): List<RadioStation> {
        val radioStations: MutableList<RadioStation> = ArrayList()
        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + "Can not parse data, parser is null")
            return radioStations
        }
        val array = downloadJsonArray(downloader, uri, parameters, cacheType)
        var jsonObject: JSONObject
        for (i in 0 until array.length()) {
            try {
                jsonObject = array[i] as JSONObject
                val radioStation = getRadioStation(mContext, jsonObject)
                // TODO: Move this check point into Radio Station
                if (radioStation.isMediaStreamEmpty()) {
                    continue
                }
                radioStations.add(radioStation)
            } catch (e: JSONException) {
                AppLogger.e("$CLASS_NAME get stations exception:$e")
            }
        }
        return radioStations
    }

    override fun addStation(downloader: Downloader,
                            uri: Uri,
                            parameters: List<Pair<String, String>>,
                            cacheType: CacheType): Boolean {
        // Post data to the server.
        val response = String(downloader.downloadDataFromUri(mContext, uri, parameters))
        AppLogger.i("Add station response:$response")
        if (response.isEmpty()) {
            return false
        }
        var value = false
        try {
            // {"ok":false,"message":"AddStationError 'url is empty'","uuid":""}
            // {"ok":true,"message":"added station successfully","uuid":"3516ff35-14b9-4845-8624-4e6b0a7a3ab9"}
            val jsonObject = JSONObject(response)
            if (jsonObject.has("ok")) {
                val str = jsonObject.getString("ok")
                if (str.isNotEmpty()) {
                    value = str.equals("true", ignoreCase = true)
                }
            }
        } catch (e: JSONException) {
            AppLogger.e("$e")
        }
        return value
    }

    override fun getStation(downloader: Downloader, uri: Uri, cacheType: CacheType): RadioStation? {
        // Download response from the server.
        val response = String(downloader.downloadDataFromUri(mContext, uri))
        AppLogger.i(CLASS_NAME + "Response:" + response)

        // Ignore empty response.
        if (response.isEmpty()) {
            AppLogger.e(CLASS_NAME + "Can not parse data, response is empty")
            return null
        }
        val jsonObject: JSONObject = try {
            JSONObject(response)
        } catch (e: JSONException) {
            AppLogger.e("$e")
            return null
        }
        try {
            return getRadioStation(mContext, jsonObject)
        } catch (e: JSONException) {
            AppLogger.e("$e")
        }
        return null
    }

    /**
     * Download data as [JSONArray].
     *
     * @param downloader Implementation of the [Downloader].
     * @param uri        Uri to download from.
     * @return [JSONArray]
     */
    private fun downloadJsonArray(downloader: Downloader,
                                  uri: Uri,
                                  cacheType: CacheType): JSONArray {
        return downloadJsonArray(downloader, uri, ArrayList(), cacheType)
    }

    /**
     * Download data as [JSONArray].
     *
     * @param downloader Implementation of the [Downloader].
     * @param uri        Uri to download from.
     * @param parameters List of parameters to attach to connection.
     * @return [JSONArray]
     */
    // TODO: Refactor this method to download raw response. Then Use parser to get data.
    private fun downloadJsonArray(downloader: Downloader,
                                  uri: Uri,
                                  parameters: List<Pair<String, String>>,
                                  cacheType: CacheType?): JSONArray {
        var array = JSONArray()
        if (!ConnectivityReceiver.checkConnectivityAndNotify(mContext)) {
            return array
        }

        // Create key to associate response with.
        var responsesMapKey: String = uri.toString()
        try {
            responsesMapKey += NetUtils.getPostParametersQuery(parameters)
        } catch (e: UnsupportedEncodingException) {
            AppLogger.e("$e")
            responsesMapKey = ""
        }

        // Fetch RAM memory first.
        array = mApiCacheInMemory[responsesMapKey]
        if (array.length() != 0) {
            return array
        }

        // Then look up data in the DB.
        array = mApiCachePersistent[responsesMapKey]
        if (array.length() != 0) {
            mApiCacheInMemory.remove(responsesMapKey)
            mApiCacheInMemory.put(responsesMapKey, array)
            return array
        }
        // Finally, go to internet.

        // Declare and initialize variable for response.
        val response = String(downloader.downloadDataFromUri(mContext, uri, parameters))
        // Ignore empty response finally.
        if (response.isEmpty()) {
            array = JSONArray()
            AppLogger.w(CLASS_NAME + "Can not parse data, response is empty")
            return array
        }
        var isSuccess = false
        try {
            array = JSONArray(response)
            isSuccess = true
        } catch (e: JSONException) {
            AppLogger.e("$e")
        }
        if (isSuccess) {
            // Remove previous record.
            mApiCachePersistent.remove(responsesMapKey)
            mApiCacheInMemory.remove(responsesMapKey)
            // Finally, cache new response.
            mApiCachePersistent.put(responsesMapKey, array)
            mApiCacheInMemory.put(responsesMapKey, array)
        }
        return array
    }

    /**
     * Updates [RadioStation] with the values extracted from the JSOn Object.
     *
     * @param `object` JSON object that holds informational parameters.
     * @return RadioStation or null.
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun getRadioStation(context: Context, jsonObject: JSONObject): RadioStation {
        val radioStation = RadioStation.makeDefaultInstance(
                context, jsonObject.getString(JsonDataParserImpl.KEY_STATION_UUID)
        )
        if (jsonObject.has(JsonDataParserImpl.KEY_STATUS)) {
            radioStation.status = jsonObject.getInt(JsonDataParserImpl.KEY_STATUS)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_NAME)) {
            radioStation.name = jsonObject.getString(JsonDataParserImpl.KEY_NAME)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_HOME_PAGE)) {
            radioStation.homePage = jsonObject.getString(JsonDataParserImpl.KEY_HOME_PAGE)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_COUNTRY)) {
            radioStation.country = jsonObject.getString(JsonDataParserImpl.KEY_COUNTRY)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_COUNTRY_CODE)) {
            radioStation.countryCode = jsonObject.getString(JsonDataParserImpl.KEY_COUNTRY_CODE)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_URL)) {
            var bitrate = 0
            if (jsonObject.has(JsonDataParserImpl.KEY_BIT_RATE)) {
                bitrate = jsonObject.getInt(JsonDataParserImpl.KEY_BIT_RATE)
            }
            val mediaStream = MediaStream.makeDefaultInstance()
            mediaStream.setVariant(bitrate, jsonObject.getString(JsonDataParserImpl.KEY_URL))
            radioStation.mediaStream = mediaStream
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_FAV_ICON)) {
            radioStation.imageUrl = jsonObject.getString(JsonDataParserImpl.KEY_FAV_ICON)
            radioStation.thumbUrl = jsonObject.getString(JsonDataParserImpl.KEY_FAV_ICON)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_LAST_CHECK_OK)) {
            radioStation.lastCheckOk = jsonObject.getInt(JsonDataParserImpl.KEY_LAST_CHECK_OK)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_LAST_CHECK_OK_TIME)) {
            radioStation.lastCheckOkTime = jsonObject.getString(JsonDataParserImpl.KEY_LAST_CHECK_OK_TIME)
        }
        if (jsonObject.has(JsonDataParserImpl.KEY_URL_RESOLVED)) {
            radioStation.urlResolved = jsonObject.getString(JsonDataParserImpl.KEY_URL_RESOLVED)
        }
        return radioStation
    }

    companion object {
        /**
         * Tag string to use in logging messages.
         */
        private val CLASS_NAME = ApiServiceProviderImpl::class.java.simpleName + " "
    }

    init {
        mApiCachePersistent = PersistentApiCache(context, PersistentAPIDbHelper.DATABASE_NAME)
        mApiCacheInMemory = InMemoryApiCache()
        mContext = context
        mDataParser = dataParser
    }
}
