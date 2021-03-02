/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.vo.Country

/**
 * [LocationStorage] is a class that provides access to data associated with  Location.
 */
object LocationStorage: AbstractStorage() {
    /**
     * Name of the Preferences.
     */
    private const val FILE_NAME = "LocationPref"
    private const val PREFS_KEY_LAST_COUNTRY_CODE = "PREFS_KEY_LAST_COUNTRY_CODE"

    fun getLastCountryCode(context: Context): String {
        return getSharedPreferences(context, FILE_NAME).getString(
                PREFS_KEY_LAST_COUNTRY_CODE, Country.COUNTRY_CODE_DEFAULT
        ) ?: Country.COUNTRY_CODE_DEFAULT
    }

    fun setLastCountryCode(context: Context, value: String) {
        val editor = getEditor(context, FILE_NAME)
        editor.putString(PREFS_KEY_LAST_COUNTRY_CODE, value)
        editor.apply()
    }
}
