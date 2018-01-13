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

package com.yuriy.openradio.business.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.yuriy.openradio.vo.Country;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class designed in a way to provide storage of the data related to Geo requests, such as
 * country, coordinates, etc ...
 */
public final class GeoAPIStorage extends AbstractRadioStationsStorage {

    /**
     * Default time of the last Geo API usage.
     */
    public static final int LAST_USE_TIME_DEFAULT = 0;

    /**
     * Name of the file for the Preferences.
     */
    private static final String FILE_NAME = "GeoAPIPreferences";

    /**
     * Key to associate time of the last Geo API usage.
     */
    private static final String KEY_LAST_USE_TIME = "LastGeoAPIUseKey";

    /**
     * Key to associate country code of the last Geo API usage.
     */
    private static final String KEY_LAST_KNOWN_COUNTRY_CODE = "LastKnownCountryCode";

    /**
     * Key to associate country name of the last Geo API usage.
     */
    private static final String KEY_LAST_KNOWN_COUNTRY_NAME = "LastKnownCountryName";

    /**
     * Private constructor
     */
    private GeoAPIStorage() {
        super();
    }

    /**
     * Set the last time of the Geo API usage, milliseconds.
     *
     * @param time    Last time of the Geo API usage.
     * @param context Context of the callee.
     */
    public static void setLastUseTime(final long time, final Context context) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putLong(KEY_LAST_USE_TIME, time);
        editor.commit();
    }

    /**
     * Returns the lsat time of the Geo API usage, milliseconds.
     *
     * @param context Context of the callee.
     * @return The lsat time of the Geo API usage.
     */
    public static long getLastUseTime(final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getLong(KEY_LAST_USE_TIME, LAST_USE_TIME_DEFAULT);
    }

    /**
     *
     * @param countryCode
     * @param context Context of the callee.
     */
    public static void setLastKnownCountryCode(final String countryCode, final Context context) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putString(KEY_LAST_KNOWN_COUNTRY_CODE, countryCode);
        editor.commit();
    }

    /**
     *
     * @param context Context of the callee.
     * @return
     */
    public static String getLastKnownCountryCode(final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getString(
                KEY_LAST_KNOWN_COUNTRY_CODE,
                Country.COUNTRY_CODE_DEFAULT
        );
    }

    /**
     *
     * @param countryName
     * @param context Context of the callee.
     */
    public static void setLastKnownCountryName(final String countryName, final Context context) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putString(KEY_LAST_KNOWN_COUNTRY_NAME, countryName);
        editor.commit();
    }

    /**
     *
     * @param context Context of the callee.
     * @return
     */
    public static String getLastKnownCountryName(final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getString(
                KEY_LAST_KNOWN_COUNTRY_NAME,
                Country.COUNTRY_NAME_DEFAULT
        );
    }
}
