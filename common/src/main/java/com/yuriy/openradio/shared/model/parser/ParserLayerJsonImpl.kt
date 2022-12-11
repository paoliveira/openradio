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

import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.JsonUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.setVariant
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
 * This is implementation of [ParserLayer] that designed to works with JSON as input format.
 */
class ParserLayerJsonImpl : ParserLayer {

    companion object {
        private const val CLASS_NAME = "JsonDataParserImpl"
        private const val KEY_STATION_UUID = "stationuuid"
        private const val KEY_NAME = "name"
        private const val KEY_COUNTRY = "country"
        /**
         * 2 letters, uppercase.
         */
        private const val KEY_COUNTRY_CODE = "countrycode"
        private const val KEY_BIT_RATE = "bitrate"
        private const val KEY_URL = "url"
        private const val KEY_URL_RESOLVED = "url_resolved"
        private const val KEY_FAV_ICON = "favicon"
        private const val KEY_STATIONS_COUNT = "stationcount"
        private const val KEY_HOME_PAGE = "homepage"
        private const val KEY_LAST_CHECK_OK_TIME = "lastcheckoktime"
        private const val KEY_LAST_CHECK_OK = "lastcheckok"
        private const val KEY_CODEC = "codec"
    }

    override fun getRadioStation(data: String, mediaIdBuilder: MediaIdBuilder): RadioStation {
        val list = getRadioStations(data, mediaIdBuilder)
        return if (list.isNotEmpty()) list[0] else RadioStation.INVALID_INSTANCE
    }

    override fun getRadioStations(data: String, mediaIdBuilder: MediaIdBuilder): List<RadioStation> {
        val array = try {
            JSONArray(data)
        } catch (e: Exception) {
            AppLogger.e("$CLASS_NAME can't convert data to JSON Array, data:$data", e)
            return emptyList()
        }
        val result = ArrayList<RadioStation>()
        for (i in 0 until array.length()) {
            val jsonObject: JSONObject = try {
                array[i] as JSONObject
            } catch (e: JSONException) {
                AppLogger.e("$CLASS_NAME get stations", e)
                continue
            }
            val radioStation = getRadioStation(jsonObject, mediaIdBuilder)
            if (radioStation.isMediaStreamEmpty()) {
                continue
            }
            result.add(radioStation)
        }
        return result
    }

    override fun getCategories(data: String): List<Category> {
        val array = try {
            JSONArray(data)
        } catch (e: JSONException) {
            AppLogger.e("$CLASS_NAME can't convert data to JSON Array, data:$data", e)
            return emptyList()
        }
        val result = ArrayList<Category>()
        for (i in 0 until array.length()) {
            val jsonObject: JSONObject =  try {
                array[i] as JSONObject
            } catch (e: JSONException) {
                AppLogger.e("$CLASS_NAME getCategories", e)
                continue
            }
            val category = Category.makeDefaultInstance()
            if (jsonObject.has(KEY_NAME)) {
                category.id = jsonObject.getString(KEY_NAME)
                category.title = jsonObject.getString(KEY_NAME)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                if (jsonObject.has(KEY_STATIONS_COUNT)) {
                    category.stationsCount = jsonObject.getInt(KEY_STATIONS_COUNT)
                }
            }
            result.add(category)
        }
        return result
    }

    private fun getRadioStation(jsonObject: JSONObject, mediaIdBuilder: MediaIdBuilder): RadioStation {
        val uuid = mediaIdBuilder.build(JsonUtils.getStringValue(jsonObject, KEY_STATION_UUID))
        if (uuid == AppUtils.EMPTY_STRING) {
            AppLogger.e("No UUID present in data:$jsonObject")
            return RadioStation.INVALID_INSTANCE
        }
        val radioStation = RadioStation.makeDefaultInstance(uuid)
        radioStation.name = JsonUtils.getStringValue(jsonObject, KEY_NAME)
        radioStation.homePage = JsonUtils.getStringValue(jsonObject, KEY_HOME_PAGE)
        radioStation.country = JsonUtils.getStringValue(jsonObject, KEY_COUNTRY)
        radioStation.countryCode = JsonUtils.getStringValue(jsonObject, KEY_COUNTRY_CODE)
        radioStation.imageUrl = JsonUtils.getStringValue(jsonObject, KEY_FAV_ICON)
        radioStation.lastCheckOkTime = JsonUtils.getStringValue(jsonObject, KEY_LAST_CHECK_OK_TIME)
        radioStation.urlResolved = JsonUtils.getStringValue(jsonObject, KEY_URL_RESOLVED)
        radioStation.codec = JsonUtils.getStringValue(jsonObject, KEY_CODEC)
        radioStation.lastCheckOk = JsonUtils.getIntValue(jsonObject, KEY_LAST_CHECK_OK)
        if (jsonObject.has(KEY_URL)) {
            var bitrate = 0
            if (jsonObject.has(KEY_BIT_RATE)) {
                bitrate = jsonObject.getInt(KEY_BIT_RATE)
            }
            radioStation.setVariant(bitrate, jsonObject.getString(KEY_URL))
        }
        return radioStation
    }
}
