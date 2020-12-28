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
package com.yuriy.openradio.shared.model.net

import android.net.Uri
import androidx.core.util.Pair

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class DownloaderException(message: String?, cause: Throwable?) : Exception(message, cause) {
    companion object {
        /**
         * @param uri
         * @param parameters
         * @return
         */
        fun createExceptionMessage(uri: Uri,
                                   parameters: List<Pair<String, String>>): String {
            return createExceptionMessage(uri.toString(), parameters)
        }

        /**
         * @param uriStr
         * @param parameters
         * @return
         */
        @JvmStatic
        fun createExceptionMessage(uriStr: String,
                                   parameters: List<Pair<String, String>>): String {
            val builder = StringBuilder(uriStr)
            for (pair in parameters) {
                builder.append(" ")
                builder.append(pair.toString())
            }
            return builder.toString()
        }
    }
}