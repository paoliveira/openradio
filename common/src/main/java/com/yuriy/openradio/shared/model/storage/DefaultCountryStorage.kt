/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
 * Storage to keep value of selected country to display in the main view.
 */
object DefaultCountryStorage : AbstractStorage() {
    /**
     * Name of the Preferences.
     */
    private const val FILE_NAME = "DefaultCountryStorage"
    private const val DEFAULT_COUNTRY = "DEFAULT_COUNTRY"

    @JvmStatic
    fun setDefaultCountryCode(context: Context, state: String) {
        val editor = getEditor(context, FILE_NAME)
        editor.putString(DEFAULT_COUNTRY, state)
        editor.apply()
    }

    @JvmStatic
    fun getDefaultCountryCode(context: Context): String {
        val preferences = getSharedPreferences(context, FILE_NAME)
        return preferences.getString(DEFAULT_COUNTRY, Country.COUNTRY_CODE_DEFAULT)
                ?: Country.COUNTRY_CODE_DEFAULT
    }
}
