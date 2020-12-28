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
package com.yuriy.openradio.shared.model.api

import android.content.Context
import android.net.Uri
import android.text.TextUtils
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
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppLogger.w
import com.yuriy.openradio.shared.utils.AppUtils.capitalize
import com.yuriy.openradio.shared.utils.NetUtils.getPostParametersQuery
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.MediaStream
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.RadioStation.Companion.makeDefaultInstance
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
 */
class ApiServiceProviderImpl(context: Context, dataParser: DataParser?) : ApiServiceProvider {
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
            w(CLASS_NAME + "Can not parse data, parser is null")
            return allCategories
        }
        val array = downloadJsonArray(downloader, uri, cacheType)
        var `object`: JSONObject
        var category: Category
        for (i in 0 until array!!.length()) {
            try {
                `object` = array[i] as JSONObject
                category = Category.makeDefaultInstance()

                // TODO: Use data parser to parse JSON to value object
                if (`object`.has(JsonDataParserImpl.KEY_NAME)) {
                    category.id = `object`.getString(JsonDataParserImpl.KEY_NAME)
                    category.title = capitalize(`object`.getString(JsonDataParserImpl.KEY_NAME))
                    if (`object`.has(JsonDataParserImpl.KEY_STATIONS_COUNT)) {
                        category.stationsCount = `object`.getInt(JsonDataParserImpl.KEY_STATIONS_COUNT)
                    }
                }
                allCategories.add(category)
            } catch (e: JSONException) {
                logException(e)
            }
        }
        return allCategories
    }

    override fun getCountries(downloader: Downloader, uri: Uri, cacheType: CacheType): List<Country> {
        val allCountries: MutableList<Country> = ArrayList()
        if (mDataParser == null) {
            w(CLASS_NAME + "Can not parse data, parser is null")
            return allCountries
        }
        for (countryName in LocationService.COUNTRY_NAME_TO_CODE.keys) {
            val countryCode = LocationService.COUNTRY_NAME_TO_CODE[countryName]
            if (countryCode == null) {
                e("Country code not found for $countryName")
                continue
            }
            allCountries.add(Country(countryName, countryCode))
        }
        return allCountries
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
            w(CLASS_NAME + "Can not parse data, parser is null")
            return radioStations
        }
        val array = downloadJsonArray(downloader, uri, parameters, cacheType)
        var `object`: JSONObject
        for (i in 0 until array!!.length()) {
            try {
                `object` = array[i] as JSONObject
                val radioStation = getRadioStation(mContext, `object`)
                // TODO: Move this check point into Radio Station
                if (radioStation.isMediaStreamEmpty()) {
                    continue
                }
                radioStations.add(radioStation)
            } catch (e: JSONException) {
                logException(e)
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
        i("Add station response:$response")
        if (TextUtils.isEmpty(response)) {
            return false
        }
        var value = false
        try {
            // {"ok":false,"message":"AddStationError 'url is empty'","uuid":""}
            // {"ok":true,"message":"added station successfully","uuid":"3516ff35-14b9-4845-8624-4e6b0a7a3ab9"}
            val jsonObject = JSONObject(response)
            if (jsonObject.has("ok")) {
                val str = jsonObject.getString("ok")
                if (!TextUtils.isEmpty(str)) {
                    value = str.equals("true", ignoreCase = true)
                }
            }
        } catch (e: JSONException) {
            logException(e)
        }
        return value
    }

    override fun getStation(downloader: Downloader, uri: Uri, cacheType: CacheType): RadioStation? {
        // Download response from the server.
        val response = String(downloader.downloadDataFromUri(mContext, uri))
        i(CLASS_NAME + "Response:" + response)

        // Ignore empty response.
        if (response.isEmpty()) {
            e(CLASS_NAME + "Can not parse data, response is empty")
            return null
        }
        val `object`: JSONObject
        `object` = try {
            JSONObject(response)
        } catch (e: JSONException) {
            logException(e)
            return null
        }
        try {
            return getRadioStation(mContext, `object`)
        } catch (e: JSONException) {
            logException(e)
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
                                  cacheType: CacheType): JSONArray? {
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
                                  cacheType: CacheType?): JSONArray? {
        var array: JSONArray? = JSONArray()
        if (!ConnectivityReceiver.checkConnectivityAndNotify(mContext)) {
            return array
        }

        // Create key to associate response with.
        var responsesMapKey: String? = uri.toString()
        try {
            responsesMapKey += getPostParametersQuery(parameters)
        } catch (e: UnsupportedEncodingException) {
            logException(e)
            responsesMapKey = null
        }

        // Fetch RAM memory first.
        array = mApiCacheInMemory[responsesMapKey]
        if (array != null) {
            return array
        }

        // Then look up data in the DB.
        array = mApiCachePersistent[responsesMapKey]
        if (array != null) {
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
            w(CLASS_NAME + "Can not parse data, response is empty")
            return array
        }
        var isSuccess = false
        try {
            array = JSONArray(response)
            isSuccess = true
        } catch (e: JSONException) {
            logException(e)
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
     * @param object JSON object that holds informational parameters.
     * @return RadioStation or null.
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun getRadioStation(context: Context, `object`: JSONObject): RadioStation {
        val radioStation = makeDefaultInstance(
                context, `object`.getString(JsonDataParserImpl.KEY_STATION_UUID)
        )
        if (`object`.has(JsonDataParserImpl.KEY_STATUS)) {
            radioStation.status = `object`.getInt(JsonDataParserImpl.KEY_STATUS)
        }
        if (`object`.has(JsonDataParserImpl.KEY_NAME)) {
            radioStation.name = `object`.getString(JsonDataParserImpl.KEY_NAME)
        }
        if (`object`.has(JsonDataParserImpl.KEY_HOME_PAGE)) {
            radioStation.homePage = `object`.getString(JsonDataParserImpl.KEY_HOME_PAGE)
        }
        if (`object`.has(JsonDataParserImpl.KEY_COUNTRY)) {
            radioStation.country = `object`.getString(JsonDataParserImpl.KEY_COUNTRY)
        }
        if (`object`.has(JsonDataParserImpl.KEY_COUNTRY_CODE)) {
            radioStation.countryCode = `object`.getString(JsonDataParserImpl.KEY_COUNTRY_CODE)
        }
        if (`object`.has(JsonDataParserImpl.KEY_URL)) {
            var bitrate = 0
            if (`object`.has(JsonDataParserImpl.KEY_BIT_RATE)) {
                bitrate = `object`.getInt(JsonDataParserImpl.KEY_BIT_RATE)
            }
            val mediaStream = MediaStream.makeDefaultInstance()
            mediaStream.setVariant(bitrate, `object`.getString(JsonDataParserImpl.KEY_URL))
            radioStation.mediaStream = mediaStream
        }
        if (`object`.has(JsonDataParserImpl.KEY_IMAGE)) {
            // TODO : Encapsulate Image in the same way as Stream.
            val imageObject = `object`.getJSONObject(JsonDataParserImpl.KEY_IMAGE)
            if (imageObject.has(JsonDataParserImpl.KEY_URL)) {
                radioStation.imageUrl = imageObject.getString(JsonDataParserImpl.KEY_URL)
            }
            if (imageObject.has(JsonDataParserImpl.KEY_THUMB)) {
                val imageThumbObject = imageObject.getJSONObject(
                        JsonDataParserImpl.KEY_THUMB
                )
                if (imageThumbObject.has(JsonDataParserImpl.KEY_URL)) {
                    radioStation.thumbUrl = imageThumbObject.getString(JsonDataParserImpl.KEY_URL)
                }
            }
        }
        if (`object`.has(JsonDataParserImpl.KEY_FAV_ICON)) {
            radioStation.imageUrl = `object`.getString(JsonDataParserImpl.KEY_FAV_ICON)
            radioStation.thumbUrl = `object`.getString(JsonDataParserImpl.KEY_FAV_ICON)
        }
        return radioStation
    }

    companion object {
        /**
         * Tag string to use in logging messages.
         */
        private val CLASS_NAME = ApiServiceProviderImpl::class.java.simpleName + " "
    }

    /**
     * Constructor.
     *
     * @param context    Context of a callee.
     * @param dataParser Implementation of the [DataParser]
     */
    init {
        mApiCachePersistent = PersistentApiCache(context, PersistentAPIDbHelper.DATABASE_NAME)
        mApiCacheInMemory = InMemoryApiCache()
        mContext = context
        mDataParser = dataParser
    }
}
