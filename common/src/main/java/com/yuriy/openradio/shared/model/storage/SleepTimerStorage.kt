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
import java.util.*

object SleepTimerStorage : AbstractStorage() {

    /**
     * Name of the Preferences file.
     */
    private const val FILE_NAME = "SleepTimerStorage"
    private const val ENABLED = "ENABLED"
    private const val TIME = "TIME"

    @JvmStatic
    fun saveEnabled(context: Context, isEnabled: Boolean) {
        val editor = getEditor(context, FILE_NAME)
        editor.putBoolean(ENABLED, isEnabled)
        editor.apply()
    }

    @JvmStatic
    fun loadEnabled(context: Context): Boolean {
        val preferences = getSharedPreferences(context, FILE_NAME)
        return preferences.getBoolean(ENABLED, false)
    }

    @JvmStatic
    fun saveDate(context: Context, date: Date) {
        val editor = getEditor(context, FILE_NAME)
        editor.putLong(TIME, date.time)
        editor.apply()
    }

    @JvmStatic
    fun loadDate(context: Context): Date {
        val preferences = getSharedPreferences(context, FILE_NAME)
        return Date(preferences.getLong(TIME, System.currentTimeMillis()))
    }
}
