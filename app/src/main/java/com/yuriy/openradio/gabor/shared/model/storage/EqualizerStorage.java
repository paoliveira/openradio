/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import androidx.annotation.NonNull;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class EqualizerStorage extends AbstractStorage {

    /**
     * Name of the Preferences.
     */
    private static final String FILE_NAME = "EqualizerStorage";

    private static final String EQUALIZER_STATE = "EQUALIZER_STATE";

    /**
     * Default constructor.
     */
    private EqualizerStorage() {
        super();
    }

    public static boolean isEmpty(@NonNull final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getString(EQUALIZER_STATE, "").equals("");
    }

    public static void saveEqualizerState(@NonNull final Context context, final String state) {
        final SharedPreferences.Editor editor = getEditor(context, FILE_NAME);
        editor.putString(EQUALIZER_STATE, state);
        editor.apply();
    }

    public static String loadEqualizerState(@NonNull final Context context) {
        final SharedPreferences preferences = getSharedPreferences(context, FILE_NAME);
        return preferences.getString(EQUALIZER_STATE, "");
    }
}
