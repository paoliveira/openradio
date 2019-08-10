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

package com.yuriy.openradio.model.parser;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.Country;
import com.yuriy.openradio.vo.GeoLocation;

import org.json.JSONArray;
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
public final class GoogleGeoDataParserJson implements GeoDataParser {

    /**
     * Tag string to use in logging messages.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = GoogleGeoDataParserJson.class.getSimpleName();

    private static final String KEY_RESULTS = "results";
    private static final String KEY_ADDRESS_COMPONENTS = "address_components";
    private static final String KEY_TYPES = "types";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_SHORT_NAME = "short_name";
    private static final String KEY_LONG_NAME = "long_name";

    public GoogleGeoDataParserJson() {
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
            final JSONArray results = object.getJSONArray(KEY_RESULTS);
            if (results.length() == 0) {
                return location;
            }
            final JSONObject element = results.getJSONObject(0);
            final JSONArray addressComponents = element.getJSONArray(KEY_ADDRESS_COMPONENTS);
            if (addressComponents.length() == 0) {
                return location;
            }
            for (int i = 0; i < addressComponents.length(); i++) {
                final JSONObject address = addressComponents.getJSONObject(i);
                if (!address.has(KEY_TYPES)) {
                    continue;
                }
                final JSONArray types = address.getJSONArray(KEY_TYPES);
                if (!isAddressTypeCountry(types)) {
                    continue;
                }
                final String countryName = address.getString(KEY_LONG_NAME);
                final String countryCode = address.getString(KEY_SHORT_NAME);
                if (!TextUtils.isEmpty(countryName) && !TextUtils.isEmpty(countryCode)) {
                    location.setCountry(new Country(countryName, countryCode));
                }
            }
        } catch (final JSONException e) {
            AppLogger.w(CLASS_NAME + " Can not extract data from response:" + e);
        }
        return location;
    }

    /**
     * Checks whether provided JSON array is belong to type of "country".
     *
     * @param addressTypes JSON array represented an input.
     * @return {@code true} in case of success, {@code false} otherwise.
     * @throws JSONException
     */
    private boolean isAddressTypeCountry(final JSONArray addressTypes) throws JSONException {
        if (addressTypes == null) {
            return false;
        }
        for (int i = 0; i < addressTypes.length(); i++) {
            if (TextUtils.equals(KEY_COUNTRY, addressTypes.getString(i))) {
                return true;
            }
        }
        return false;
    }
}
