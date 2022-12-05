/*
 * Copyright 2017, 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.content.SharedPreferences
import com.yuriy.openradio.shared.utils.AppLogger
import java.lang.ref.WeakReference

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * @param mContextRef Weak reference to the application context.
 * @param mName Name of the file for the preferences.
 */
abstract class AbstractStorage(private val mContextRef: WeakReference<Context>, private val mName: String) {

    fun getIntValue(key: String, defaultValue: Int): Int {
        return getSharedPreferences()?.getInt(key, defaultValue) ?: defaultValue
    }

    fun getLongValue(key: String, defaultValue: Long): Long {
        return getSharedPreferences()?.getLong(key, defaultValue) ?: defaultValue
    }

    fun getStringValue(key: String, defaultValue: String): String {
        return getSharedPreferences()?.getString(key, defaultValue) ?: defaultValue
    }

    fun getBooleanValue(key: String, defaultValue: Boolean): Boolean {
        return getSharedPreferences()?.getBoolean(key, defaultValue) ?: defaultValue
    }

    fun getAllValues(): MutableMap<String, *> {
        return getSharedPreferences()?.all ?: HashMap<String, String>()
    }

    fun putStringValue(key: String, value: String) {
        val editor = getEditor()
        editor?.putString(key, value)
        editor?.apply()
        AppLogger.i("Added '$key'-'$value'")
    }

    fun putIntValue(key: String, value: Int) {
        val editor = getEditor()
        editor?.putInt(key, value)
        editor?.apply()
        AppLogger.i("Added '$key'-'$value'")
    }

    fun putBooleanValue(key: String, value: Boolean) {
        val editor = getEditor()
        editor?.putBoolean(key, value)
        editor?.apply()
        AppLogger.i("Added '$key'-'$value'")
    }

    fun putLongValue(key: String, value: Long) {
        val editor = getEditor()
        editor?.putLong(key, value)
        editor?.apply()
        AppLogger.i("Added '$key'-'$value'")
    }

    fun removeKey(key: String) {
        val editor = getEditor()
        editor?.remove(key)
        editor?.apply()
        AppLogger.i("Removed '$key'")
    }

    fun clearStorage() {
        val editor = getEditor()
        editor?.clear()
        editor?.apply()
        AppLogger.i("Storage cleared")
    }

    /**
     * Return an instance of the Shared Preferences or null.
     *
     * @return An instance of the Shared Preferences or null if context was GCed.
     */
    private fun getSharedPreferences(): SharedPreferences? {
        val context = mContextRef.get()
        if (context == null) {
            AppLogger.e("Abs storage has null ctx!")
        }
        return context?.getSharedPreferences(mName, Context.MODE_PRIVATE)
    }

    /**
     * Return [SharedPreferences.Editor] associated with the
     * Shared Preferences or null.
     *
     * @return [SharedPreferences.Editor] or null if context was GCed.
     */
    private fun getEditor(): SharedPreferences.Editor? {
        return getSharedPreferences()?.edit()
    }
}
