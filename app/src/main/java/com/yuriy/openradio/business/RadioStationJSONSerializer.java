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

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.MediaStream;
import com.yuriy.openradio.vo.RadioStation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link RadioStationJSONSerializer} is implementation of the {@link RadioStationSerializer}
 * interface that serialize {@link RadioStation} into JSON's String.
 */
public final class RadioStationJSONSerializer implements RadioStationSerializer {

    private static final String CLASS_NAME = RadioStationJSONSerializer.class.getSimpleName();

    /**
     * Default constructor.
     */
    public RadioStationJSONSerializer() {
        super();
    }

    @Override
    public final String serialize(final RadioStation radioStation) {
        final JSONObject jsonObject = new JSONObject();
        if (radioStation == null) {
            return jsonObject.toString();
        }
        if (radioStation.isMediaStreamEmpty()) {
            return jsonObject.toString();
        }
        try {
            jsonObject.put(RadioStationJSONHelper.KEY_ID, radioStation.getId());
            jsonObject.put(RadioStationJSONHelper.KEY_NAME, radioStation.getName());
            jsonObject.put(RadioStationJSONHelper.KEY_BITRATE, radioStation.getMediaStream().getVariant(0).getBitrate());
            jsonObject.put(RadioStationJSONHelper.KEY_COUNTRY, radioStation.getCountry());
            jsonObject.put(RadioStationJSONHelper.KEY_GENRE, radioStation.getGenre());
            jsonObject.put(RadioStationJSONHelper.KEY_IMG_URL, radioStation.getImageUrl());
            jsonObject.put(RadioStationJSONHelper.KEY_STREAM_URL, radioStation.getMediaStream().getVariant(0).getUrl());
            jsonObject.put(RadioStationJSONHelper.KEY_STATUS, radioStation.getStatus());
            jsonObject.put(RadioStationJSONHelper.KEY_THUMB_URL, radioStation.getThumbUrl());
            jsonObject.put(RadioStationJSONHelper.KEY_WEB_SITE, radioStation.getWebSite());
            jsonObject.put(RadioStationJSONHelper.KEY_IS_LOCAL, radioStation.isLocal());
            jsonObject.put(RadioStationJSONHelper.KEY_SORT_ID, radioStation.getSortId());
        } catch (final Exception e) {
            /* Ignore this exception */
            AppLogger.e("Error while marshall " + radioStation + ", exception:\n" + Log.getStackTraceString(e));
        }
        return jsonObject.toString();
    }
}
