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
import com.yuriy.openradio.shared.utils.AppUtils

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object NetworkSettingsStorage : AbstractStorage() {

    /**
     * Name of the Preferences.
     */
    private const val FILE_NAME = "NetworkSettingsStorage"
    private const val IS_USE_MOBILE = "IS_USE_MOBILE"

    @JvmStatic
    fun setUseMobile(context: Context, value: Boolean) {
        val editor = getEditor(context, FILE_NAME)
        editor.putBoolean(IS_USE_MOBILE, value)
        editor.apply()
    }

    @JvmStatic
    fun getUseMobile(context: Context): Boolean {
        return getSharedPreferences(context, FILE_NAME).getBoolean(IS_USE_MOBILE, false)
    }
}
