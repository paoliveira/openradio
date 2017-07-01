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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link AppPreferencesManager} is a class that provides access and manage of the
 * Application's Shared Preferences.
 */
public final class AppPreferencesManager {

    /**
     * Name of the Preferences.
     */
    private static final String PREFS_NAME = "OpenRadioPref";

    /**
     * Key for the "Is Location dialog enabled shown" dialog.
     */
    private static final String PREFS_KEY_IS_LOCATION_DIALOG_SHOWN = "IS_LOCATION_DIALOG_SHOWN";

    /**
     *
     */
    private static final String PREFS_KEY_ARE_LOGS_ENABLED = "IS_LOGS_ENABLED";

    /**
     *
     */
    private static final String PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED = "LAST_KNOWN_RADIO_STATION_ENABLED";

    /**
     *
     */
    private static final String PREFS_KEY_IS_SORT_DIALOG_SHOWN = "IS_SORT_DIALOG_SHOWN";

    /**
     * Default constructor.
     */
    private AppPreferencesManager() {
        super();
    }

    /**
     * @return True if "Enable Location service" dialog has been shown. False - otherwise.
     */
    public static boolean isLocationDialogShown(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_IS_LOCATION_DIALOG_SHOWN,
                false
        );
    }

    /**
     * Set True if "Enable Location service" dialog has been shown. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void setLocationDialogShown(@NonNull final Context context,
                                              final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_IS_LOCATION_DIALOG_SHOWN, value);
        editor.apply();
    }

    /**
     * @return True if it is allowed to save logs into a file. False - otherwise.
     */
    public static boolean areLogsEnabled(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_ARE_LOGS_ENABLED,
                false
        );
    }

    /**
     * Set True if it is allowed to save logs into a file. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void setLogsEnabled(@NonNull final Context context, final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_ARE_LOGS_ENABLED, value);
        editor.apply();
    }

    /**
     * @return True if it is allowed to autoplay last known Radio Station on app start. False - otherwise.
     */
    public static boolean lastKnownRadioStationEnabled(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED,
                true
        );
    }

    /**
     * Set True if it is allowed to autoplay last known Radio Station on app start. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void lastKnownRadioStationEnabled(@NonNull final Context context, final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED, value);
        editor.apply();
    }

    /**
     * @return True if dialog about Sort feature was shown. False - otherwise.
     */
    public static boolean isSortDialogShown(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_IS_SORT_DIALOG_SHOWN,
                false
        );
    }

    /**
     * Set True if dialog about Sort feature was shown. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void setSortDialogShown(@NonNull final Context context,
                                          final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_IS_SORT_DIALOG_SHOWN, value);
        editor.apply();
    }

    /**
     * @return {@link android.content.SharedPreferences.Editor}
     */
    private static SharedPreferences.Editor getEditor(@NonNull final Context context) {
        return getSharedPreferences(context).edit();
    }

    /**
     * @return {@link android.content.SharedPreferences} of the Application
     */
    private static SharedPreferences getSharedPreferences(@NonNull final Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
