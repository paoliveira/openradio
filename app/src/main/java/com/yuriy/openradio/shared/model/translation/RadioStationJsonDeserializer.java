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

package com.yuriy.openradio.shared.model.translation;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.JsonUtils;
import com.yuriy.openradio.shared.vo.RadioStation;

import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RadioStationJsonDeserializer implements RadioStationDeserializer {

    /**
     * Default constructor.
     */
    public RadioStationJsonDeserializer() {
        super();
    }

    @Override
    @Nullable
    public final RadioStation deserialize(final Context context, final String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            final JSONObject jsonObject = new JSONObject(value);
            final RadioStation radioStation = RadioStation.makeDefaultInstance(
                    context, JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_ID)
            );
            radioStation.setName(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_NAME));

            String bitrateStr = JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_BITRATE, "0");
            if (!TextUtils.isDigitsOnly(bitrateStr) || TextUtils.isEmpty(bitrateStr)) {
                bitrateStr = "0";
            }
            radioStation.getMediaStream().setVariant(
                    Integer.parseInt(bitrateStr),
                    JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_STREAM_URL)
            );
            radioStation.setCountry(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY));
            radioStation.setCountryCode(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_COUNTRY_CODE));
            radioStation.setGenre(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_GENRE));
            radioStation.setImageUrl(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_IMG_URL));
            radioStation.setStatus(JsonUtils.getIntValue(jsonObject, RadioStationJsonHelper.KEY_STATUS));
            radioStation.setThumbUrl(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_THUMB_URL));
            radioStation.setHomePage(JsonUtils.getStringValue(jsonObject, RadioStationJsonHelper.KEY_HOME_PAGE));
            radioStation.setIsLocal(JsonUtils.getBooleanValue(jsonObject, RadioStationJsonHelper.KEY_IS_LOCAL));
            radioStation.setSortId(JsonUtils.getIntValue(jsonObject, RadioStationJsonHelper.KEY_SORT_ID, -1));

            return radioStation;
        } catch (final Throwable e) {
            /* Ignore this exception */
            AppLogger.e("Error while de-marshall " + value + ", exception:\n" + Log.getStackTraceString(e));
        }
        return null;
    }
}
