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

import com.yuriy.openradio.api.RadioStationVO;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link RadioStationJSONSerializer} is implementation of the {@link RadioStationSerializer}
 * interface that serialize {@link RadioStationVO} into JSON's String.
 */
public final class RadioStationJSONSerializer implements RadioStationSerializer {

    /**
     * Default constructor.
     */
    public RadioStationJSONSerializer() {
        super();
    }

    @Override
    public final String serialize(final RadioStationVO radioStation) {
        final JSONObject jsonObject = new JSONObject();
        if (radioStation == null) {
            return jsonObject.toString();
        }
        try {
            jsonObject.put(RadioStationJSONHelper.KEY_ID, radioStation.getId());
            jsonObject.put(RadioStationJSONHelper.KEY_NAME, radioStation.getName());
            jsonObject.put(RadioStationJSONHelper.KEY_BITRATE, radioStation.getBitRate());
            jsonObject.put(RadioStationJSONHelper.KEY_COUNTRY, radioStation.getCountry());
            jsonObject.put(RadioStationJSONHelper.KEY_GENRE, radioStation.getGenre());
            jsonObject.put(RadioStationJSONHelper.KEY_IMG_URL, radioStation.getImageUrl());
            jsonObject.put(RadioStationJSONHelper.KEY_STREAM_URL, radioStation.getStreamURL());
            jsonObject.put(RadioStationJSONHelper.KEY_STATUS, radioStation.getStatus());
            jsonObject.put(RadioStationJSONHelper.KEY_THUMB_URL, radioStation.getThumbUrl());
            jsonObject.put(RadioStationJSONHelper.KEY_WEB_SITE, radioStation.getWebSite());
            jsonObject.put(RadioStationJSONHelper.KEY_IS_LOCAL, radioStation.isLocal());
            jsonObject.put(RadioStationJSONHelper.KEY_SORT_ID, radioStation.getSortId());
        } catch (final JSONException e) {
            /* Ignore this exception */
        }
        return jsonObject.toString();
    }
}
