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

package com.yuriy.openradio.shared.model.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.vo.Country;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link LocationPreferencesManager} is a class that provides access to data associated with  Location.
 */
public final class LocationPreferencesManager {

    /**
     * Name of the Preferences.
     */
    private static final String FILE_NAME = "LocationPref";

    /**
     * Key for the "Is Active" status.
     */
    private static final String PREFS_KEY_LAST_COUNTRY_CODE = "PREFS_KEY_LAST_COUNTRY_CODE";

    /**
     * Default constructor.
     */
    private LocationPreferencesManager() {
        super();
    }

    /**
     * @return
     */
    public static String getLastCountryCode(@NonNull final Context context) {
        return getSharedPreferences(context).getString(
                PREFS_KEY_LAST_COUNTRY_CODE,
                Country.COUNTRY_CODE_DEFAULT
        );
    }

    /**
     *
     *
     * @param value
     */
    public static void setLastCountryCode(@NonNull final Context context, final String value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putString(PREFS_KEY_LAST_COUNTRY_CODE, value);
        editor.apply();
    }

    /**
     * @return {@link SharedPreferences.Editor}
     */
    private static SharedPreferences.Editor getEditor(@NonNull final Context context) {
        return getSharedPreferences(context).edit();
    }

    /**
     * @return {@link SharedPreferences} of the Application
     */
    private static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
