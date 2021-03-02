/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.shared.utils

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/26/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * A helper class designed to assist with Analytics APIs.
 */
object AnalyticsUtils {

    private const val EVENT_UNSUPPORTED_PLAYLIST_V2 = "EVENT_UNSUPPORTED_PLAYLIST_V2"
    private const val KEY_URL = "KEY_URL"
    private const val KEY_URL_INVALID = "KEY_URL_INVALID"

    @JvmStatic
    fun init() {
        // TODO: FIX ME
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
    }

    /**
     * All logged exceptions will appear as “non-fatal” issues in the crash report dashboard.
     *
     * @param exception Exception.
     */
    @JvmStatic
    fun logException(exception: Exception) {
        AppLogger.e(Log.getStackTraceString(exception))
        Firebase.crashlytics.recordException(exception)
    }

    @JvmStatic
    fun logMessage(message: String) {
        AppLogger.d(message)
        Firebase.crashlytics.log(message)
    }

    @JvmStatic
    fun logUnsupportedPlaylist(playlistUrl: String) {
        val bundle = Bundle()
        bundle.putString(KEY_URL, playlistUrl)
        Firebase.analytics.logEvent(EVENT_UNSUPPORTED_PLAYLIST_V2, bundle)
    }

    @JvmStatic
    fun logUnsupportedInvalidPlaylist(playlistUrl: String) {
        val bundle = Bundle()
        bundle.putString(KEY_URL_INVALID, playlistUrl)
        Firebase.analytics.logEvent(EVENT_UNSUPPORTED_PLAYLIST_V2, bundle)
    }
}
