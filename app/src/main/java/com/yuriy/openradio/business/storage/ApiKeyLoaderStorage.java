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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 */
public final class ApiKeyLoaderStorage extends AbstractRadioStationsStorage {

    /**
     * Name of the file for the Preferences.
     */
    private static final String FILE_NAME = "ApiKeyLoaderPreferences";

    /**
     *
     */
    private static final String KEY_LAST_INDEX = "LastIndex";

    /**
     * Private constructor
     */
    private ApiKeyLoaderStorage() {
        super();
    }

    /**
     *
     * @param id
     * @param context Context of the callee.
     */
    public static void setLastIndex(final int id, final Context context) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putInt(KEY_LAST_INDEX, id);
        editor.apply();
    }

    /**
     *
     * @param context Context of the callee.
     * @return
     */
    public static int getLastIndex(final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getInt(KEY_LAST_INDEX, 0);
    }
}
