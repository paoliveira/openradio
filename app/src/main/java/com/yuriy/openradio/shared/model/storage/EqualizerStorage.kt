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
package com.yuriy.openradio.shared.model.storage

import android.content.Context

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object EqualizerStorage : AbstractStorage() {
    /**
     * Name of the Preferences.
     */
    private const val FILE_NAME = "EqualizerStorage"
    private const val EQUALIZER_STATE = "EQUALIZER_STATE"
    @JvmStatic
    fun isEmpty(context: Context): Boolean {
        val preferences = getSharedPreferences(context, FILE_NAME)
        return preferences.getString(EQUALIZER_STATE, "") == ""
    }

    @JvmStatic
    fun saveEqualizerState(context: Context, state: String?) {
        val editor = getEditor(context, FILE_NAME)
        editor.putString(EQUALIZER_STATE, state)
        editor.apply()
    }

    @JvmStatic
    fun loadEqualizerState(context: Context): String {
        val preferences = getSharedPreferences(context, FILE_NAME)
        return preferences.getString(EQUALIZER_STATE, "") ?: ""
    }
}
