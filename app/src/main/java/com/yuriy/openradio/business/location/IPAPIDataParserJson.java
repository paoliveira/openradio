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

package com.yuriy.openradio.business.location;

import android.support.annotation.NonNull;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.Country;
import com.yuriy.openradio.vo.GeoLocation;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Implementation of the {@link GeoDataParser} designed to parse a response which is
 * representation of JSON.
 */
public final class IPAPIDataParserJson implements GeoDataParser {

    /**
     * Tag string to use in logging messages.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = IPAPIDataParserJson.class.getSimpleName();

    private static final String KEY_COUNTRY = "country";
    private static final String KEY_COUNTRY_CODE = "countryCode";

    public IPAPIDataParserJson() {
        super();
    }

    @Override
    @NonNull
    public GeoLocation getLocation(final byte[] data) {
        final GeoLocation location = new GeoLocation();
        final JSONObject object = getJsonObject(data);
        if (object == null) {
            return location;
        }
        try {
            if (object.has(KEY_COUNTRY_CODE) && object.has(KEY_COUNTRY)) {
                location.setCountry(
                        new Country(
                                object.getString(KEY_COUNTRY), object.getString(KEY_COUNTRY_CODE)
                        )
                );
            }
        } catch (final JSONException e) {
            AppLogger.w(CLASS_NAME + " Can not extract data from response:" + e);
        }
        return location;
    }
}
