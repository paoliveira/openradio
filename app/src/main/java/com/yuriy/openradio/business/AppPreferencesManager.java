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

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link AppPreferencesManager} is a class that provides access and manage of the
 * Application's Shared Preferences.
 */
public class AppPreferencesManager {

    /**
     * Name of the Preferences.
     */
    private static final String PREFS_NAME = "OpenRadioPref";

    /**
     * Key for the "Is Location dialog enabled shown" dialog.
     */
    private static final String PREFS_KEY_IS_LOCATION_DIALOG_SHOWN = "IS_LOCATION_DIALOG_SHOWN";

    /**
     * Application's Context.
     */
    private static Context sContext;

    /**
     * Set Application's context.
     *
     * @param context Application's context.
     */
    public static void setContext(Context context) {
        sContext = context;
    }

    /**
     * @return True if "Enable Location service" dialog has been shown. False - otherwise.
     */
    public static boolean isLocationDialogShown() {
        return getSharedPreferences().getBoolean(
                PREFS_KEY_IS_LOCATION_DIALOG_SHOWN,
                false
        );
    }

    /**
     * Set True if "Enable Location service" dialog has been shown. False - otherwise.
     *
     * @param value Boolean value.
     */
    public static void setIsLocationDialogShown(final boolean value) {
        final SharedPreferences.Editor editor = getEditor();
        editor.putBoolean(PREFS_KEY_IS_LOCATION_DIALOG_SHOWN, value);
        editor.apply();
    }

    /**
     * @return {@link android.content.SharedPreferences.Editor}
     */
    private static SharedPreferences.Editor getEditor() {
        return getSharedPreferences().edit();
    }

    /**
     * @return {@link android.content.SharedPreferences} of the Application
     */
    private static SharedPreferences getSharedPreferences() {
        return sContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
