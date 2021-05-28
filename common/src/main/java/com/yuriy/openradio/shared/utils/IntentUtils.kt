/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
object IntentUtils {
    const val REQUEST_CODE_FILE_SELECTED = 101

    /**
     * Make intent to navigate to provided url.
     *
     * @param url Url to navigate to.
     * @return [Intent].
     */
    fun makeUrlBrowsableIntent(url: String?): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    /**
     * Dump content of [Intent]'s [Bundle] into [String].
     *
     * @param intent [Intent] to process.
     * @return [String] representation of [Bundle].
     */
    @JvmStatic
    fun intentBundleToString(intent: Intent?): String {
        return if (intent == null) {
            "Intent[null]"
        } else bundleToString(intent.extras)
    }

    /**
     * Dump content of [Bundle] into [String].
     *
     * @param bundle [Bundle] to process.
     * @return [String] representation of [Bundle].
     */
    @JvmStatic
    fun bundleToString(bundle: Bundle?): String {
        if (bundle == null) {
            return "Bundle[null]"
        }
        if (bundle.size() == 0) {
            return "Bundle[]"
        }
        val builder = StringBuilder("Bundle[")
        try {
            for (key in bundle.keySet()) {
                builder.append(key).append(":").append(if (bundle[key] != null) bundle[key] else "NULL")
                builder.append("|")
            }
            builder.delete(builder.length - 1, builder.length)
        } catch (e: Exception) {
            AppLogger.e("Intent's bundle to string exception:$e")
        }
        builder.append("]")
        return builder.toString()
    }
}
