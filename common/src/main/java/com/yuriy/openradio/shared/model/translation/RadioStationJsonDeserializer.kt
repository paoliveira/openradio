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

package com.yuriy.openradio.shared.model.translation

import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.JsonUtils
import com.yuriy.openradio.shared.vo.RadioStation
import com.yuriy.openradio.shared.vo.setVariant
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RadioStationJsonDeserializer : RadioStationDeserializer {

    override fun deserialize(value: String): RadioStation {
        if (value.isEmpty()) {
            return RadioStation.INVALID_INSTANCE
        }
        // Those values are used in store and are related to radio stations but not radio stations themselves.
        // It is legal but need better design.
        if (value.lowercase(Locale.ROOT) == "true") {
            return RadioStation.INVALID_INSTANCE
        }
        if (value.lowercase(Locale.ROOT) == "false") {
            return RadioStation.INVALID_INSTANCE
        }
        val jsonObject: JSONObject = try {
            JSONObject(value)
        } catch (e: JSONException) {
            AppLogger.e("Error while de-marshall $value", e)
            return RadioStation.INVALID_INSTANCE
        }
        val radioStation = RadioStation.makeDefaultInstance(
            JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_ID)
        )
        radioStation.name = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_NAME)
        var bitrateStr = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_BITRATE, "0")
        if (!TextUtils.isDigitsOnly(bitrateStr) || bitrateStr.isEmpty()) {
            bitrateStr = "0"
        }
        radioStation.setVariant(
            bitrateStr.toInt(),
            JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_STREAM_URL)
        )
        radioStation.country = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY)
        radioStation.countryCode = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY_CODE)
        radioStation.genre = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_GENRE)
        radioStation.homePage = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_HOME_PAGE)
        radioStation.codec = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_CODEC)
        radioStation.isLocal = JsonUtils.getBooleanValue(jsonObject, RadioStationJsonHelper.KEY_IS_LOCAL)
        radioStation.sortId = JsonUtils.getIntValue(
            jsonObject, RadioStationJsonHelper.KEY_SORT_ID, MediaSessionCompat.QueueItem.UNKNOWN_ID
        )
        radioStation.imageUrl = JsonUtils.getStringValue(
            jsonObject, RadioStationJsonHelper.KEY_IMG_URL, AppUtils.EMPTY_STRING
        )
        return radioStation
    }
}
