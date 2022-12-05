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
import com.yuriy.openradio.shared.utils.AppUtils
import java.lang.ref.WeakReference

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class EqualizerStorage(contextRef: WeakReference<Context>) : AbstractStorage(contextRef, FILE_NAME) {

    fun isEmpty(): Boolean {
        return getStringValue(EQUALIZER_STATE, AppUtils.EMPTY_STRING) == AppUtils.EMPTY_STRING
    }

    fun saveEqualizerState(state: String) {
        putStringValue(EQUALIZER_STATE, state)
    }

    fun loadEqualizerState(): String {
        return getStringValue(EQUALIZER_STATE, AppUtils.EMPTY_STRING)
    }

    companion object {
        /**
         * Name of the Preferences.
         */
        private const val FILE_NAME = "EqualizerStorage"
        private const val EQUALIZER_STATE = "EQUALIZER_STATE"
    }
}
