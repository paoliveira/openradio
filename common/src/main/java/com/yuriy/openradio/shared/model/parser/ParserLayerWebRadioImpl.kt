/*
 * Copyright 2023 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.net.Uri
import com.yuriy.openradio.shared.model.net.UrlLayerWebRadioImpl
import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.JsonUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.MediaStream
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.setVariant
import org.json.JSONObject
import java.util.TreeMap
import java.util.TreeSet

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This is implementation of [ParserLayer] that designed to works with JSON as input format.
 */
class ParserLayerWebRadioImpl : ParserLayer {

    companion object {
        private const val TAG = "PLWRI"
        private const val KEY_GENRE = "Genre"
        private const val KEY_NAME = "Name"
        private const val KEY_URL = "StreamUri"
        private const val KEY_CODEC = "Codec"
        private const val KEY_BIT_RATE = "Bitrate"
    }

    override fun getRadioStation(data: String, mediaIdBuilder: MediaIdBuilder, uri: Uri): RadioStation {
        val list = getRadioStations(data, mediaIdBuilder, uri)
        return if (list.isNotEmpty()) list.first() else RadioStation.INVALID_INSTANCE
    }

    override fun getRadioStations(data: String, mediaIdBuilder: MediaIdBuilder, uri: Uri): Set<RadioStation> {
        val jsonData = try {
            JSONObject(data)
        } catch (e: Exception) {
            AppLogger.e("$TAG to JSON Array, data:$data", e)
            return emptySet()
        }
        val categoryId = getCategoryId(uri)
        val result = TreeSet<RadioStation>()
        for (i in jsonData.keys()) {
            val jsonObject = try {
                jsonData[i] as JSONObject
            } catch (e: Exception) {
                AppLogger.e("$TAG get stations, data:$data", e)
                continue
            }
            if (jsonObject.has(KEY_GENRE)) {
                val genres = try {
                    jsonObject.getJSONArray(KEY_GENRE)
                } catch (e: Exception) {
                    AppLogger.e("$TAG get genre, data:$data", e)
                    continue
                }
                for (j in 0 until genres.length()) {
                    val genre = genres.getString(j)
                    if (genre != categoryId) {
                        continue
                    }
                    val radioStation = getRadioStation(jsonObject, i)
                    if (radioStation.isMediaStreamEmpty()) {
                        continue
                    }
                    result.add(radioStation)
                }
            }
        }
        return result
    }

    override fun getCategories(data: String): Set<Category> {
        val jsonData = try {
            JSONObject(data)
        } catch (e: Exception) {
            AppLogger.e("$TAG to JSON Array, data:$data", e)
            return emptySet()
        }
        val result = TreeSet<Category>()
        val tmp = TreeMap<String, Int>()
        for (i in jsonData.keys()) {
            val jsonObject = try {
                jsonData[i] as JSONObject
            } catch (e: Exception) {
                AppLogger.e("$TAG get categories, data:$data", e)
                continue
            }
            if (jsonObject.has(KEY_GENRE)) {
                val genres = try {
                    jsonObject.getJSONArray(KEY_GENRE)
                } catch (e: Exception) {
                    AppLogger.e("$TAG get genre, data:$data", e)
                    continue
                }
                for (j in 0 until genres.length()) {
                    val genre = genres.getString(j)
                    if (tmp.containsKey(genre)) {
                        tmp[genre] = tmp[genre]!! + 1
                    } else {
                        tmp[genre] = 1
                    }
                }
            }
        }
        for (entry in tmp.entries) {
            result.add(Category(entry.key, entry.key, entry.value))
        }
        return result
    }

    private fun getRadioStation(jsonObject: JSONObject, uuid: String): RadioStation {
        val radioStation = RadioStation.makeDefaultInstance(uuid)
        radioStation.name = JsonUtils.getStringValue(jsonObject, KEY_NAME)
        //radioStation.homePage = JsonUtils.getStringValue(jsonObject, KEY_HOME_PAGE)
        //radioStation.country = JsonUtils.getStringValue(jsonObject, KEY_COUNTRY)
        //radioStation.countryCode = JsonUtils.getStringValue(jsonObject, KEY_COUNTRY_CODE)
        //radioStation.imageUrl = JsonUtils.getStringValue(jsonObject, KEY_FAV_ICON)
        //radioStation.lastCheckOkTime = JsonUtils.getStringValue(jsonObject, KEY_LAST_CHECK_OK_TIME)
        //radioStation.urlResolved = JsonUtils.getStringValue(jsonObject, KEY_URL_RESOLVED)
        radioStation.codec = JsonUtils.getStringValue(jsonObject, KEY_CODEC)
        //radioStation.lastCheckOk = JsonUtils.getIntValue(jsonObject, KEY_LAST_CHECK_OK)

        var bitrate = MediaStream.BIT_RATE_DEFAULT
        if (jsonObject.has(KEY_BIT_RATE)) {
            bitrate = jsonObject.getInt(KEY_BIT_RATE)
        }
        radioStation.setVariant(bitrate, jsonObject.getString(KEY_URL))

        return radioStation
    }

    private fun getCategoryId(uri: Uri): String {
        val pair = uri.toString().split(UrlLayerWebRadioImpl.KEY_CATEGORY_ID)
        if (pair.size != 2) {
            return AppUtils.EMPTY_STRING
        }
        return pair[1]
    }
}
