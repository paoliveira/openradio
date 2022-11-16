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
import com.google.android.exoplayer2.IllegalSeekPositionException
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

    private const val EVENT_ILLEGAL_SEEK_POSITION = "EVENT_ILLEGAL_SEEK_POSITION"
    private const val EVENT_UNSUPPORTED_PLAYLIST_V2 = "EVENT_UNSUPPORTED_PLAYLIST_V2"
    private const val EVENT_METADATA = "EVENT_METADATA"
    private const val EVENT_CANT_DECODE_BITES = "EVENT_CANT_DECODE_BITES_V2"
    private const val EVENT_GDRIVE_FILE_DELETED = "EVENT_GDRIVE_FILE_DELETED"
    private const val KEY_METADATA = "KEY_METADATA"
    private const val KEY_URL = "KEY_URL"
    private const val KEY_BYTES_SIZE = "KEY_BYTES_SIZE"
    private const val KEY_URL_INVALID = "KEY_URL_INVALID"
    private const val KEY_MSG = "KEY_MSG"
    private const val KEY_POS = "KEY_POS"
    private const val KEY_IDX = "KEY_IDX"
    private const val KEY_ITEMS_COUNT = "KEY_ITEMS_COUNT"
    private const val KEY_OBJ = "KEY_OBJ"

    fun logMessage(message: String) {
        AppLogger.d(message)
        Firebase.crashlytics.log(message)
    }

    fun logIllegalSeekPosition(itemsCount: Int, exception: IllegalSeekPositionException) {
        val pos = exception.positionMs
        val idx = exception.windowIndex
        val timeLine = exception.timeline
        val bundle = Bundle()
        bundle.putLong(KEY_POS, pos)
        bundle.putInt(KEY_IDX, idx)
        bundle.putInt(KEY_ITEMS_COUNT, itemsCount)
        bundle.putString(KEY_OBJ, timeLine.toString())
        Firebase.analytics.logEvent(EVENT_ILLEGAL_SEEK_POSITION, bundle)
    }

    fun logUnsupportedPlaylist(value: String) {
        val bundle = Bundle()
        bundle.putString(KEY_URL, value)
        Firebase.analytics.logEvent(EVENT_UNSUPPORTED_PLAYLIST_V2, bundle)
    }

    fun logUnsupportedInvalidPlaylist(value: String) {
        val bundle = Bundle()
        bundle.putString(KEY_URL_INVALID, value)
        Firebase.analytics.logEvent(EVENT_UNSUPPORTED_PLAYLIST_V2, bundle)
    }

    fun logMetadata(value: String) {
        val bundle = Bundle()
        bundle.putString(KEY_METADATA, value)
        Firebase.analytics.logEvent(EVENT_METADATA, bundle)
    }

    fun logBitmapDecode(value: String, bytesSize: Int) {
        val bundle = Bundle()
        bundle.putString(KEY_URL, value)
        bundle.putInt(KEY_BYTES_SIZE, bytesSize)
        Firebase.analytics.logEvent(EVENT_CANT_DECODE_BITES, bundle)
    }

    fun logGDriveFileDeleted(value: String) {
        val bundle = Bundle()
        bundle.putString(KEY_MSG, value)
        Firebase.analytics.logEvent(EVENT_GDRIVE_FILE_DELETED, bundle)
    }
}
