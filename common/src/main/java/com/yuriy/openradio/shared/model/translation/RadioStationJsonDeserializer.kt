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

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.text.TextUtils
import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.JsonUtils.getBooleanValue
import com.yuriy.openradio.shared.utils.JsonUtils.getIntValue
import com.yuriy.openradio.shared.utils.JsonUtils.getStringValue
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONObject
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RadioStationJsonDeserializer : RadioStationDeserializer {

    override fun deserialize(context: Context, value: String): RadioStation? {
        if (value.isEmpty()) {
            return null
        }
        // Those values are used in store and are related to radio stations but not radio stations themselves.
        // It is legal but need better design.
        if (value.lowercase(Locale.ROOT) == "true") {
            return null
        }
        if (value.lowercase(Locale.ROOT) == "false") {
            return null
        }
        try {
            val jsonObject = JSONObject(value)
            val radioStation = RadioStation.makeDefaultInstance(
                    getStringValue(jsonObject, RadioStationJsonHelper.KEY_ID)
            )
            radioStation.name = getStringValue(jsonObject, RadioStationJsonHelper.KEY_NAME)
            var bitrateStr = getStringValue(jsonObject, RadioStationJsonHelper.KEY_BITRATE, "0")
            if (!TextUtils.isDigitsOnly(bitrateStr) || bitrateStr.isEmpty()) {
                bitrateStr = "0"
            }
            radioStation.mediaStream.setVariant(bitrateStr.toInt(),
                    getStringValue(jsonObject, RadioStationJsonHelper.KEY_STREAM_URL)
            )
            radioStation.country = getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY)
            radioStation.countryCode = getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY_CODE)
            radioStation.genre = getStringValue(jsonObject, RadioStationJsonHelper.KEY_GENRE)
            radioStation.homePage = getStringValue(jsonObject, RadioStationJsonHelper.KEY_HOME_PAGE)
            radioStation.setIsLocal(getBooleanValue(jsonObject, RadioStationJsonHelper.KEY_IS_LOCAL))
            radioStation.sortId = getIntValue(
                    jsonObject, RadioStationJsonHelper.KEY_SORT_ID, MediaSessionCompat.QueueItem.UNKNOWN_ID
            )
            return radioStation
        } catch (e: Throwable) {
            /* Ignore this exception */
            e("Error while de-marshall $value, exception:${Log.getStackTraceString(e)}")
        }
        return null
    }
}
