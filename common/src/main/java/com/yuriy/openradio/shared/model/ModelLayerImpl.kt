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

package com.yuriy.openradio.shared.model

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.model.net.DownloaderLayer
import com.yuriy.openradio.shared.model.net.NetworkLayer
import com.yuriy.openradio.shared.model.parser.ParserLayer
import com.yuriy.openradio.shared.model.storage.cache.api.ApiCache
import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONException
import org.json.JSONObject
import java.util.TreeSet

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [ModelLayerImpl] is the main implementation of the [ModelLayer] interface.
 */
class ModelLayerImpl(
    private val mContext: Context,
    private val mDataParser: ParserLayer,
    private val mNetworkLayer: NetworkLayer,
    private val mDownloaderLayer: DownloaderLayer,
    private val mApiCachePersistent: ApiCache,
    private val mApiCacheInMemory: ApiCache
) : ModelLayer {

    override fun getCategories(uri: Uri): Set<Category> {
        val data = downloadData(uri)
        return mDataParser.getCategories(data)
    }

    override fun getCountries(): Set<Country> {
        val set = TreeSet<Country>()
        for (countryName in LocationService.COUNTRY_NAME_TO_CODE.keys) {
            val countryCode = LocationService.COUNTRY_NAME_TO_CODE[countryName] ?: continue
            set.add(Country(countryName, countryCode))
        }
        return set
    }

    override fun getStations(uri: Uri, mediaIdBuilder: MediaIdBuilder): Set<RadioStation> {
        val data = downloadData(uri)
        return mDataParser.getRadioStations(data, mediaIdBuilder, uri)
    }

    override fun addStation(uri: Uri, parameters: List<Pair<String, String>>): Boolean {
        // Post data to the server.
        val response = String(mDownloaderLayer.downloadDataFromUri(mContext, uri, parameters))
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
            AppLogger.e("Add station", e)
        }
        return value
    }

    /**
     * Download data as [String].
     *
     * @param uri Uri to download from.
     * @return [String]
     */
    private fun downloadData(uri: Uri): String {
        var response = AppUtils.EMPTY_STRING
        if (!mNetworkLayer.checkConnectivityAndNotify(mContext)) {
            return response
        }

        // Create key to associate response with.
        val responsesMapKey = uri.toString()

        // Fetch RAM memory first.
        response = mApiCacheInMemory[responsesMapKey]
        if (response != AppUtils.EMPTY_STRING && response != "[]") {
            return response
        }

        // Then look up data in the DB.
        response = mApiCachePersistent[responsesMapKey]
        if (response != AppUtils.EMPTY_STRING && response != "[]") {
            mApiCacheInMemory.remove(responsesMapKey)
            mApiCacheInMemory.put(responsesMapKey, response)
            return response
        }
        // Finally, go to internet.

        // Declare and initialize variable for response.
        response = String(mDownloaderLayer.downloadDataFromUri(mContext, uri))
        // Ignore empty response finally.
        if (response == AppUtils.EMPTY_STRING || response == "[]") {
            response = AppUtils.EMPTY_STRING
            AppLogger.w("$CLASS_NAME can not parse data, response is empty")
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
        private const val CLASS_NAME = "ASPI"
    }
}
