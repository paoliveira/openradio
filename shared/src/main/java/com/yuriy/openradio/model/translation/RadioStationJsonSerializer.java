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

package com.yuriy.openradio.model.translation;

import android.util.Log;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.RadioStation;

import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link RadioStationJsonSerializer} is implementation of the {@link RadioStationSerializer}
 * interface that serialize {@link RadioStation} into JSON's String.
 */
public final class RadioStationJsonSerializer implements RadioStationSerializer {

    private static final String CLASS_NAME = RadioStationJsonSerializer.class.getSimpleName();

    /**
     * Default constructor.
     */
    public RadioStationJsonSerializer() {
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
            jsonObject.put(RadioStationJsonHelper.KEY_ID, radioStation.getId());
            jsonObject.put(RadioStationJsonHelper.KEY_NAME, radioStation.getName());
            jsonObject.put(RadioStationJsonHelper.KEY_BITRATE, radioStation.getMediaStream().getVariant(0).getBitrate());
            jsonObject.put(RadioStationJsonHelper.KEY_COUNTRY, radioStation.getCountry());
            jsonObject.put(RadioStationJsonHelper.KEY_GENRE, radioStation.getGenre());
            jsonObject.put(RadioStationJsonHelper.KEY_IMG_URL, radioStation.getImageUrl());
            jsonObject.put(RadioStationJsonHelper.KEY_STREAM_URL, radioStation.getMediaStream().getVariant(0).getUrl());
            jsonObject.put(RadioStationJsonHelper.KEY_STATUS, radioStation.getStatus());
            jsonObject.put(RadioStationJsonHelper.KEY_THUMB_URL, radioStation.getThumbUrl());
            jsonObject.put(RadioStationJsonHelper.KEY_WEB_SITE, radioStation.getWebSite());
            jsonObject.put(RadioStationJsonHelper.KEY_IS_LOCAL, radioStation.isLocal());
            jsonObject.put(RadioStationJsonHelper.KEY_SORT_ID, radioStation.getSortId());
        } catch (final Exception e) {
            /* Ignore this exception */
            AppLogger.e("Error while marshall " + radioStation + ", exception:\n" + Log.getStackTraceString(e));
        }
        return jsonObject.toString();
    }
}
