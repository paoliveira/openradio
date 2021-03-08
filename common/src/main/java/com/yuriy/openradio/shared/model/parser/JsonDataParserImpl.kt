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

package com.yuriy.openradio.shared.model.parser

import android.content.Context
import android.util.Log
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.MediaStream
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This is implementation of [DataParser] that designed to works with JSON as input format.
 */
class JsonDataParserImpl(private val mContext: Context) : DataParser {

    companion object {
        private val CLASS_NAME = JsonDataParserImpl::class.java.simpleName

        private const val KEY_STATION_UUID = "stationuuid"
        private const val KEY_NAME = "name"
        private const val KEY_COUNTRY = "country"

        /**
         * 2 letters, uppercase.
         */
        private const val KEY_COUNTRY_CODE = "countrycode"
        private const val KEY_BIT_RATE = "bitrate"
        private const val KEY_STATUS = "status"
        private const val KEY_URL = "url"
        private const val KEY_URL_RESOLVED = "url_resolved"
        private const val KEY_FAV_ICON = "favicon"
        private const val KEY_STATIONS_COUNT = "stationcount"
        private const val KEY_HOME_PAGE = "homepage"

        private const val KEY_LAST_CHECK_OK_TIME = "lastcheckoktime"

        private const val KEY_LAST_CHECK_OK = "lastcheckok"
    }

    override fun getRadioStation(data: String): RadioStation? {
        val jsonObject: JSONObject = try {
            JSONObject(data)
        } catch (e: JSONException) {
            AnalyticsUtils.logException(e)
            return null
        }
        return getRadioStation(jsonObject)
    }

    override fun getRadioStations(data: String): List<RadioStation> {
        val array = try {
            JSONArray(data)
        } catch (e: Exception) {
            AppLogger.e(
                    "$CLASS_NAME can't convert data to JSON Array, data:$data, e:${Log.getStackTraceString(e)}"
            )
            return emptyList()
        }
        val result = mutableListOf<RadioStation>()
        var jsonObject: JSONObject
        for (i in 0 until array.length()) {
            try {
                jsonObject = array[i] as JSONObject
                val radioStation = getRadioStation(jsonObject) ?: continue
                if (radioStation.isMediaStreamEmpty()) {
                    continue
                }
                result.add(radioStation)
            } catch (e: JSONException) {
                AppLogger.e("$CLASS_NAME get stations exception:$e")
            }
        }
        return result
    }

    override fun getCategories(data: String): List<Category> {
        val array = try {
            JSONArray(data)
        } catch (e: Exception) {
            AppLogger.e(
                    "$CLASS_NAME can't convert data to JSON Array, data:$data, e:${Log.getStackTraceString(e)}"
            )
            return emptyList()
        }
        val result = mutableListOf<Category>()
        var item: JSONObject
        var category: Category
        for (i in 0 until array.length()) {
            try {
                item = array[i] as JSONObject
                category = Category.makeDefaultInstance()
                if (item.has(KEY_NAME)) {
                    category.id = item.getString(KEY_NAME)
                    category.title = item.getString(KEY_NAME).capitalize(Locale.ROOT)
                    if (item.has(KEY_STATIONS_COUNT)) {
                        category.stationsCount = item.getInt(KEY_STATIONS_COUNT)
                    }
                }
                result.add(category)
            } catch (e: Exception) {
                AnalyticsUtils.logException(e)
            }
        }
        return result
    }

    private fun getRadioStation(jsonObject: JSONObject): RadioStation? {
        if (!jsonObject.has(KEY_STATION_UUID)) {
            AppLogger.e("No UUID present in data:$jsonObject")
            return null
        }

        val radioStation = RadioStation.makeDefaultInstance(jsonObject.getString(KEY_STATION_UUID))
        if (jsonObject.has(KEY_STATUS)) {
            radioStation.status = jsonObject.getInt(KEY_STATUS)
        }
        if (jsonObject.has(KEY_NAME)) {
            radioStation.name = jsonObject.getString(KEY_NAME)
        }
        if (jsonObject.has(KEY_HOME_PAGE)) {
            radioStation.homePage = jsonObject.getString(KEY_HOME_PAGE)
        }
        if (jsonObject.has(KEY_COUNTRY)) {
            radioStation.country = jsonObject.getString(KEY_COUNTRY)
        }
        if (jsonObject.has(KEY_COUNTRY_CODE)) {
            radioStation.countryCode = jsonObject.getString(KEY_COUNTRY_CODE)
        }
        if (jsonObject.has(KEY_URL)) {
            var bitrate = 0
            if (jsonObject.has(KEY_BIT_RATE)) {
                bitrate = jsonObject.getInt(KEY_BIT_RATE)
            }
            val mediaStream = MediaStream.makeDefaultInstance()
            mediaStream.setVariant(bitrate, jsonObject.getString(KEY_URL))
            radioStation.mediaStream = mediaStream
        }
        var imgUrl = ""
        if (jsonObject.has(KEY_FAV_ICON)) {
            imgUrl = jsonObject.getString(KEY_FAV_ICON)
        }
        radioStation.setImgUrl(mContext, imgUrl)
        if (jsonObject.has(KEY_LAST_CHECK_OK)) {
            radioStation.lastCheckOk = jsonObject.getInt(KEY_LAST_CHECK_OK)
        }
        if (jsonObject.has(KEY_LAST_CHECK_OK_TIME)) {
            radioStation.lastCheckOkTime = jsonObject.getString(KEY_LAST_CHECK_OK_TIME)
        }
        if (jsonObject.has(KEY_URL_RESOLVED)) {
            radioStation.urlResolved = jsonObject.getString(KEY_URL_RESOLVED)
        }
        return radioStation
    }
}
