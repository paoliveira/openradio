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
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.model.parser.DataParser
import com.yuriy.openradio.shared.model.storage.cache.CacheType
import com.yuriy.openradio.shared.model.storage.cache.api.InMemoryApiCache
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentAPIDbHelper
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentApiCache
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.NetUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.RadioStation
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
 * [ApiServiceProviderImpl] is the implementation of the
 * [ApiServiceProvider] interface.
 *
 * @param context Context of a callee.
 */
class ApiServiceProviderImpl(
    context: Context,
    private val mDataParser: DataParser,
    private val mNetworkMonitor: NetworkMonitor
) : ApiServiceProvider {

    private val mContext = context
    private val mApiCachePersistent = PersistentApiCache(context, PersistentAPIDbHelper.DATABASE_NAME)
    private val mApiCacheInMemory = InMemoryApiCache()

    override fun close() {
        mApiCachePersistent.close()
        (mApiCacheInMemory as? InMemoryApiCache)?.clear()
    }

    override fun clear() {
        mApiCachePersistent.clear()
        mApiCacheInMemory.clear()
    }

    override fun getCategories(downloader: Downloader, uri: Uri, cacheType: CacheType): List<Category> {
        val data = downloadData(downloader, uri, cacheType)
        return mDataParser.getCategories(data)
    }

    override fun getCountries(downloader: Downloader, uri: Uri, cacheType: CacheType): List<Country> {
        val list = ArrayList<Country>()
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

    override fun getStations(
        downloader: Downloader, uri: Uri,
        parameters: List<Pair<String, String>>,
        cacheType: CacheType
    ): List<RadioStation> {
        val data = downloadData(downloader, uri, parameters, cacheType)
        return mDataParser.getRadioStations(data)
    }

    override fun addStation(
        downloader: Downloader, uri: Uri,
        parameters: List<Pair<String, String>>,
        cacheType: CacheType
    ): Boolean {
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
        return mDataParser.getRadioStation(response)
    }

    /**
     * Download data as [String].
     *
     * @param downloader Implementation of the [Downloader].
     * @param uri        Uri to download from.
     * @return [String]
     */
    private fun downloadData(downloader: Downloader, uri: Uri, cacheType: CacheType): String {
        return downloadData(downloader, uri, ArrayList(), cacheType)
    }

    /**
     * Download data as [String].
     *
     * @param downloader Implementation of the [Downloader].
     * @param uri        Uri to download from.
     * @param parameters List of parameters to attach to connection.
     * @return [String]
     */
    private fun downloadData(
        downloader: Downloader, uri: Uri,
        parameters: List<Pair<String, String>>,
        cacheType: CacheType?
    ): String {
        var response = AppUtils.EMPTY_STRING
        if (!mNetworkMonitor.checkConnectivityAndNotify(mContext)) {
            return response
        }

        // Create key to associate response with.
        var responsesMapKey = uri.toString()
        try {
            responsesMapKey += NetUtils.getPostParametersQuery(parameters)
        } catch (e: UnsupportedEncodingException) {
            AppLogger.e("$e")
            responsesMapKey = AppUtils.EMPTY_STRING
        }

        // Fetch RAM memory first.
        response = mApiCacheInMemory[responsesMapKey]
        if (response.isNotEmpty()) {
            return response
        }

        // Then look up data in the DB.
        response = mApiCachePersistent[responsesMapKey]
        if (response.isNotEmpty()) {
            mApiCacheInMemory.remove(responsesMapKey)
            mApiCacheInMemory.put(responsesMapKey, response)
            return response
        }
        // Finally, go to internet.

        // Declare and initialize variable for response.
        response = String(downloader.downloadDataFromUri(mContext, uri, parameters))
        // Ignore empty response finally.
        if (response.isEmpty()) {
            response = AppUtils.EMPTY_STRING
            AppLogger.w(CLASS_NAME + "Can not parse data, response is empty")
            return response
        }
        // Remove previous record.
        mApiCachePersistent.remove(responsesMapKey)
        mApiCacheInMemory.remove(responsesMapKey)
        // Finally, cache new response.
        mApiCachePersistent.put(responsesMapKey, response)
        mApiCacheInMemory.put(responsesMapKey, response)
        return response
    }

    companion object {
        /**
         * Tag string to use in logging messages.
         */
        private val CLASS_NAME = ApiServiceProviderImpl::class.java.simpleName + " "
    }
}
