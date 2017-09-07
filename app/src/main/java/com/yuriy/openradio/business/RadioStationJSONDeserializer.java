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

package com.yuriy.openradio.business;

import android.util.Log;

import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.utils.AppLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RadioStationJSONDeserializer implements RadioStationDeserializer {

    /**
     * Default constructor.
     */
    public RadioStationJSONDeserializer() {
        super();
    }

    @Override
    public final RadioStationVO deserialize(final String value) {
        final RadioStationVO radioStation = RadioStationVO.makeDefaultInstance();
        if (value == null || value.isEmpty()) {
            return radioStation;
        }
        try {
            final JSONObject jsonObject = new JSONObject(value);
            radioStation.setId(getIntValue(jsonObject, RadioStationJSONHelper.KEY_ID));
            radioStation.setName(getStringValue(jsonObject, RadioStationJSONHelper.KEY_NAME));
            radioStation.setBitRate(getStringValue(jsonObject, RadioStationJSONHelper.KEY_BITRATE));
            radioStation.setCountry(getStringValue(jsonObject, RadioStationJSONHelper.KEY_COUNTRY));
            radioStation.setGenre(getStringValue(jsonObject, RadioStationJSONHelper.KEY_GENRE));
            radioStation.setImageUrl(getStringValue(jsonObject, RadioStationJSONHelper.KEY_IMG_URL));
            radioStation.setStreamURL(getStringValue(jsonObject, RadioStationJSONHelper.KEY_STREAM_URL));
            radioStation.setStatus(getIntValue(jsonObject, RadioStationJSONHelper.KEY_STATUS));
            radioStation.setThumbUrl(getStringValue(jsonObject, RadioStationJSONHelper.KEY_THUMB_URL));
            radioStation.setWebSite(getStringValue(jsonObject, RadioStationJSONHelper.KEY_WEB_SITE));
            radioStation.setIsLocal(getBooleanValue(jsonObject, RadioStationJSONHelper.KEY_IS_LOCAL));
            radioStation.setSortId(getIntValue(jsonObject, RadioStationJSONHelper.KEY_SORT_ID, -1));
        } catch (final Throwable e) {
            /* Ignore this exception */
            AppLogger.e("Error while demarshall RadioStation:\n" + Log.getStackTraceString(e));
        }
        return radioStation;
    }

    private String getStringValue(final JSONObject jsonObject, final String key)
            throws JSONException {
        if (jsonObject == null) {
            return "";
        }
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return "";
    }

    private int getIntValue(final JSONObject jsonObject, final String key) throws JSONException {
        return getIntValue(jsonObject, key, 0);
    }

    private int getIntValue(final JSONObject jsonObject,
                            final String key, final int defaultValue) throws JSONException {
        if (jsonObject == null) {
            return defaultValue;
        }
        if (jsonObject.has(key)) {
            return jsonObject.getInt(key);
        }
        return defaultValue;
    }

    private boolean getBooleanValue(final JSONObject jsonObject, final String key) throws JSONException {
        return jsonObject != null && jsonObject.has(key) && jsonObject.getBoolean(key);
    }
}
