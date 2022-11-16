/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

import android.util.Log

object AppLogger {

    private const val LOG_TAG = "OPNRD"

    fun e(logMsg: String) {
        Log.e(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun e(logMsg: String, t: Throwable?) {
        Log.e(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg + "\n${Log.getStackTraceString(t)}")
    }

    fun w(logMsg: String) {
        Log.w(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun i(logMsg: String) {
        Log.i(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }

    fun d(logMsg: String) {
        Log.d(LOG_TAG, "[" + Thread.currentThread().name + "] " + logMsg)
    }
}
