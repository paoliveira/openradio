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

package com.yuriy.openradio.gabor.shared.model.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;

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
    private static final String FILE_NAME = "OpenRadioPref";

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
     *
     */
    private static final String PREFS_KEY_IS_CUSTOM_USER_AGENT = "IS_CUSTOM_USER_AGENT";

    /**
     *
     */
    private static final String PREFS_KEY_CUSTOM_USER_AGENT = "CUSTOM_USER_AGENT";

    /**
     *
     */
    private static final String PREFS_KEY_MASTER_VOLUME = "MASTER_VOLUME";

    private static final String PREFS_KEY_MIN_BUFFER = "PREFS_KEY_MIN_BUFFER";
    private static final String PREFS_KEY_MAX_BUFFER = "PREFS_KEY_MAX_BUFFER";
    private static final String PREFS_KEY_BUFFER_FOR_PLAYBACK = "PREFS_KEY_BUFFER_FOR_PLAYBACK";
    private static final String PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK = "PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK";
    private static final String PREFS_KEY_BT_AUTO_PLAY = "PREFS_KEY_BT_AUTO_PLAY";

    private static final int MASTER_VOLUME_DEFAULT = 100;

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
     * @return True if it is allowed to addToLocals logs into a file. False - otherwise.
     */
    public static boolean areLogsEnabled(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_ARE_LOGS_ENABLED,
                false
        );
    }

    /**
     * Set True if it is allowed to addToLocals logs into a file. False - otherwise.
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
     * @return {@code true} if custom user agent is enabled, {@code false} otherwise.
     */
    public static boolean isCustomUserAgent(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_IS_CUSTOM_USER_AGENT,
                false
        );
    }

    /**
     * Sets {@code true} if custom user agent is enabled, {@code false} otherwise.
     *
     * @param value Boolean value.
     */
    public static void isCustomUserAgent(@NonNull final Context context,
                                          final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_IS_CUSTOM_USER_AGENT, value);
        editor.apply();
    }

    /**
     * @return Value of the custom user agent, or default one in case of errors.
     */
    public static String getCustomUserAgent(@NonNull final Context context) {
        String value = getSharedPreferences(context).getString(
                PREFS_KEY_CUSTOM_USER_AGENT,
                AppUtils.getDefaultUserAgent(context)
        );
        if (TextUtils.isEmpty(value)) {
            value = AppUtils.getDefaultUserAgent(context);
        }
        return value;
    }

    /**
     * Sets the value of custom user agent.
     *
     * @param value Custom user agent.
     */
    public static void setCustomUserAgent(@NonNull final Context context,
                                          @NonNull String value) {
        if (TextUtils.isEmpty(value)) {
            value = AppUtils.getDefaultUserAgent(context);
        }
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putString(PREFS_KEY_CUSTOM_USER_AGENT, value);
        editor.apply();
    }

    /**
     * @return Value of the master volume, or default one in case of errors.
     */
    public static int getMasterVolume(@NonNull final Context context) {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_MASTER_VOLUME,
                MASTER_VOLUME_DEFAULT
        );
    }

    /**
     * Sets the value of master volume.
     *
     * @param value Master volume.
     */
    public static void setMasterVolume(@NonNull final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFS_KEY_MASTER_VOLUME, value);
        editor.apply();
    }

    public static int getMinBuffer(@NonNull final Context context) {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_MIN_BUFFER, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
        );
    }

    public static void setMinBuffer(@NonNull final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFS_KEY_MIN_BUFFER, value);
        editor.apply();
    }

    public static int getMaxBuffer(@NonNull final Context context) {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_MAX_BUFFER, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
        );
    }

    public static void setMaxBuffer(@NonNull final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFS_KEY_MAX_BUFFER, value);
        editor.apply();
    }

    public static int getPlayBuffer(@NonNull final Context context) {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_BUFFER_FOR_PLAYBACK, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
        );
    }

    public static void setPlayBuffer(@NonNull final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFS_KEY_BUFFER_FOR_PLAYBACK, value);
        editor.apply();
    }

    public static int getPlayBufferRebuffer(@NonNull final Context context) {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        );
    }

    public static void setPlayBufferRebuffer(@NonNull final Context context, final int value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK, value);
        editor.apply();
    }

    public static boolean isBtAutoPlay(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(PREFS_KEY_BT_AUTO_PLAY, false);
    }

    public static void setBtAutoPlay(@NonNull final Context context, final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_BT_AUTO_PLAY, value);
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
