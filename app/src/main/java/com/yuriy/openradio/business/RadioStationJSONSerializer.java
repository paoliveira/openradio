/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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
 */

/**
 * {@link RadioStationJSONSerializer} is implementation of the {@link RadioStationSerializer}
 * interface that serialize {@link RadioStationVO} into JSON's String.
 */
public final class RadioStationJSONSerializer implements RadioStationSerializer {

    /**
     * JSON's keys
     */

    public static final String KEY_ID = "Id";

    private static final String KEY_STATUS = "Status";

    private static final String KEY_NAME = "Name";

    private static final String KEY_STREAM_URL = "StreamUrl";

    private static final String KEY_WEB_SITE = "Website";

    private static final String KEY_COUNTRY = "Country";

    private static final String KEY_BITRATE = "Bitrate";

    private static final String KEY_GENRE = "Genre";

    private static final String KEY_IMG_URL = "ImgUrl";

    private static final String KEY_THUMB_URL = "ThumbUrl";

    @Override
    public final String serialize(final RadioStationVO radioStation) {
        final JSONObject jsonObject = new JSONObject();
        if (radioStation == null) {
            return jsonObject.toString();
        }
        try {
            jsonObject.put(KEY_ID, radioStation.getId());
            jsonObject.put(KEY_NAME, radioStation.getName());
            jsonObject.put(KEY_BITRATE, radioStation.getBitRate());
            jsonObject.put(KEY_COUNTRY, radioStation.getCountry());
            jsonObject.put(KEY_GENRE, radioStation.getGenre());
            jsonObject.put(KEY_IMG_URL, radioStation.getImageUrl());
            jsonObject.put(KEY_STREAM_URL, radioStation.getStreamURL());
            jsonObject.put(KEY_STATUS, radioStation.getStatus());
            jsonObject.put(KEY_THUMB_URL, radioStation.getThumbUrl());
            jsonObject.put(KEY_WEB_SITE, radioStation.getWebSite());
        } catch (final JSONException e) {
            /* Ignore this exception */
        }
        return jsonObject.toString();
    }
}
