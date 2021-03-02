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
package com.yuriy.openradio.shared.model.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.android.exoplayer2.DefaultLoadControl

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [AppPreferencesManager] is a class that provides access and manage of the
 * Application's Shared Preferences.
 */
object AppPreferencesManager {
    /**
     * Name of the Preferences.
     */
    private const val FILE_NAME = "OpenRadioPref"

    /**
     *
     */
    private const val PREFS_KEY_ARE_LOGS_ENABLED = "IS_LOGS_ENABLED"

    /**
     *
     */
    private const val PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED = "LAST_KNOWN_RADIO_STATION_ENABLED"

    /**
     *
     */
    private const val PREFS_KEY_IS_CUSTOM_USER_AGENT = "IS_CUSTOM_USER_AGENT"

    /**
     *
     */
    private const val PREFS_KEY_CUSTOM_USER_AGENT = "CUSTOM_USER_AGENT"

    /**
     *
     */
    private const val PREFS_KEY_MASTER_VOLUME = "MASTER_VOLUME"
    private const val PREFS_KEY_MIN_BUFFER = "PREFS_KEY_MIN_BUFFER"
    private const val PREFS_KEY_MAX_BUFFER = "PREFS_KEY_MAX_BUFFER"
    private const val PREFS_KEY_BUFFER_FOR_PLAYBACK = "PREFS_KEY_BUFFER_FOR_PLAYBACK"
    private const val PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK = "PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK"
    private const val PREFS_KEY_BT_AUTO_PLAY = "PREFS_KEY_BT_AUTO_PLAY"
    private const val MASTER_VOLUME_DEFAULT = 100

    /**
     * @return True if it is allowed to addToLocals logs into a file. False - otherwise.
     */
    @JvmStatic
    fun areLogsEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFS_KEY_ARE_LOGS_ENABLED,false)
    }

    /**
     * Set True if it is allowed to addToLocals logs into a file. False - otherwise.
     *
     * @param value Boolean value.
     */
    fun setLogsEnabled(context: Context, value: Boolean) {
        val editor = getEditor(context)
        editor.putBoolean(PREFS_KEY_ARE_LOGS_ENABLED, value)
        editor.apply()
    }

    /**
     * @return True if it is allowed to autoplay last known Radio Station on app start. False - otherwise.
     */
    fun lastKnownRadioStationEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED, true)
    }

    /**
     * Set True if it is allowed to autoplay last known Radio Station on app start. False - otherwise.
     *
     * @param value Boolean value.
     */
    fun lastKnownRadioStationEnabled(context: Context, value: Boolean) {
        val editor = getEditor(context)
        editor.putBoolean(PREFS_KEY_LAST_KNOWN_RADIO_STATION_ENABLED, value)
        editor.apply()
    }

    /**
     * @return `true` if custom user agent is enabled, `false` otherwise.
     */
    fun isCustomUserAgent(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFS_KEY_IS_CUSTOM_USER_AGENT, false)
    }

    /**
     * Sets `true` if custom user agent is enabled, `false` otherwise.
     *
     * @param value Boolean value.
     */
    fun isCustomUserAgent(context: Context, value: Boolean) {
        val editor = getEditor(context)
        editor.putBoolean(PREFS_KEY_IS_CUSTOM_USER_AGENT, value)
        editor.apply()
    }

    /**
     * @return Value of the custom user agent, or default one in case of errors.
     */
    fun getCustomUserAgent(context: Context, defaultValue: String): String {
        var value = getSharedPreferences(context).getString(PREFS_KEY_CUSTOM_USER_AGENT, defaultValue)
        if (value.isNullOrEmpty()) {
            value = defaultValue
        }
        return value
    }

    /**
     * Sets the value of custom user agent.
     *
     * @param value Custom user agent. Non null and not empty value.
     */
    fun setCustomUserAgent(context: Context, value: String) {
        val editor = getEditor(context)
        editor.putString(PREFS_KEY_CUSTOM_USER_AGENT, value)
        editor.apply()
    }

    /**
     * @return Value of the master volume, or default one in case of errors.
     */
    fun getMasterVolume(context: Context): Int {
        return getSharedPreferences(context).getInt(PREFS_KEY_MASTER_VOLUME, MASTER_VOLUME_DEFAULT)
    }

    /**
     * Sets the value of master volume.
     *
     * @param value Master volume.
     */
    fun setMasterVolume(context: Context, value: Int) {
        val editor = getEditor(context)
        editor.putInt(PREFS_KEY_MASTER_VOLUME, value)
        editor.apply()
    }

    @JvmStatic
    fun getMinBuffer(context: Context): Int {
        return getSharedPreferences(context).getInt(PREFS_KEY_MIN_BUFFER, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS)
    }

    @JvmStatic
    fun setMinBuffer(context: Context, value: Int) {
        val editor = getEditor(context)
        editor.putInt(PREFS_KEY_MIN_BUFFER, value)
        editor.apply()
    }

    @JvmStatic
    fun getMaxBuffer(context: Context): Int {
        return getSharedPreferences(context).getInt(PREFS_KEY_MAX_BUFFER, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS)
    }

    @JvmStatic
    fun setMaxBuffer(context: Context, value: Int) {
        val editor = getEditor(context)
        editor.putInt(PREFS_KEY_MAX_BUFFER, value)
        editor.apply()
    }

    @JvmStatic
    fun getPlayBuffer(context: Context): Int {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_BUFFER_FOR_PLAYBACK, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
        )
    }

    @JvmStatic
    fun setPlayBuffer(context: Context, value: Int) {
        val editor = getEditor(context)
        editor.putInt(PREFS_KEY_BUFFER_FOR_PLAYBACK, value)
        editor.apply()
    }

    @JvmStatic
    fun getPlayBufferRebuffer(context: Context): Int {
        return getSharedPreferences(context).getInt(
                PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
        )
    }

    @JvmStatic
    fun setPlayBufferRebuffer(context: Context, value: Int) {
        val editor = getEditor(context)
        editor.putInt(PREFS_KEY_BUFFER_FOR_REBUFFER_PLAYBACK, value)
        editor.apply()
    }

    fun isBtAutoPlay(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PREFS_KEY_BT_AUTO_PLAY, false)
    }

    fun setBtAutoPlay(context: Context, value: Boolean) {
        val editor = getEditor(context)
        editor.putBoolean(PREFS_KEY_BT_AUTO_PLAY, value)
        editor.apply()
    }

    /**
     * @return [SharedPreferences.Editor]
     */
    private fun getEditor(context: Context): SharedPreferences.Editor {
        return getSharedPreferences(context).edit()
    }

    /**
     * @return [SharedPreferences] of the Application
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }
}
