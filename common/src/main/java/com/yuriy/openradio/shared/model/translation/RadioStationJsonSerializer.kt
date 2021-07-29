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

import android.util.Log
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONObject

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [RadioStationJsonSerializer] is implementation of the [RadioStationSerializer]
 * interface that serialize [RadioStation] into JSON's String.
 */
class RadioStationJsonSerializer : RadioStationSerializer {

    override fun serialize(radioStation: RadioStation): String {
        val jsonObject = JSONObject()
        if (radioStation.isMediaStreamEmpty()) {
            return jsonObject.toString()
        }
        try {
            jsonObject.put(RadioStationJsonHelper.KEY_ID, radioStation.id)
            jsonObject.put(RadioStationJsonHelper.KEY_NAME, radioStation.name)
            jsonObject.put(RadioStationJsonHelper.KEY_BITRATE, radioStation.mediaStream.getVariant(0)!!.bitrate)
            jsonObject.put(RadioStationJsonHelper.KEY_COUNTRY, radioStation.country)
            jsonObject.put(RadioStationJsonHelper.KEY_COUNTRY_CODE, radioStation.countryCode)
            jsonObject.put(RadioStationJsonHelper.KEY_GENRE, radioStation.genre)
            jsonObject.put(RadioStationJsonHelper.KEY_STREAM_URL, radioStation.mediaStream.getVariant(0)!!.url)
            jsonObject.put(RadioStationJsonHelper.KEY_HOME_PAGE, radioStation.homePage)
            jsonObject.put(RadioStationJsonHelper.KEY_IS_LOCAL, radioStation.isLocal)
            jsonObject.put(RadioStationJsonHelper.KEY_SORT_ID, radioStation.sortId)
            jsonObject.put(RadioStationJsonHelper.KEY_IMAGE_URL, radioStation.imageUrl)
        } catch (e: Exception) {
            /* Ignore this exception */
            AppLogger.e("Error while marshall $radioStation, exception:${Log.getStackTraceString(e)}")
        }
        return jsonObject.toString()
    }
}
