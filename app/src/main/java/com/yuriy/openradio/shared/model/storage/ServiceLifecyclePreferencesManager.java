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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link ServiceLifecyclePreferencesManager} is a class that provides access and manage of the
 * Open Radio service Shared Preferences.
 */
public final class ServiceLifecyclePreferencesManager {

    /**
     * Name of the Preferences.
     */
    private static final String PREFS_NAME = "OpenRadioServicePref";

    /**
     * Key for the "Is Active" status.
     */
    private static final String PREFS_KEY_IS_ACTIVE = "PREFS_KEY_IS_ACTIVE";

    /**
     * Default constructor.
     */
    private ServiceLifecyclePreferencesManager() {
        super();
    }

    /**
     * @return True if Open Radio Service is active. False - otherwise.
     */
    public static boolean isServiceActive(@NonNull final Context context) {
        return getSharedPreferences(context).getBoolean(
                PREFS_KEY_IS_ACTIVE,
                false
        );
    }

    /**
     * Set True if Open Radio Service is active. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void isServiceActive(@NonNull final Context context, final boolean value) {
        final SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(PREFS_KEY_IS_ACTIVE, value);
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
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
