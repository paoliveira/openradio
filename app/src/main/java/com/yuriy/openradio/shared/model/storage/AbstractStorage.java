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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class AbstractStorage {

    /**
     * Default constructor.
     */
    AbstractStorage() {
        super();
    }

    /**
     * Return an instance of the Shared Preferences.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     * @return An instance of the Shared Preferences.
     */
    static SharedPreferences getSharedPreferences(final Context context,
                                                  final String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Return {@link SharedPreferences.Editor} associated with the
     * Shared Preferences.
     *
     * @param context Context of the callee.
     * @param name    Name of the file for the preferences.
     * @return {@link SharedPreferences.Editor}.
     */
    static SharedPreferences.Editor getEditor(final Context context, final String name) {
        return getSharedPreferences(context, name).edit();
    }
}
