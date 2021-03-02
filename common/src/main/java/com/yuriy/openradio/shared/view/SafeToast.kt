/*
 * Copyright 2016 - 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.view

import android.content.Context
import android.widget.Toast
import com.yuriy.openradio.shared.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Yurii Chernyshov
 * @version 1.0
 * @since 2016-01-29
 *
 * Helper class to provide ability to display [Toast] from any Thread.
 */
object SafeToast {

    /**
     * Show [Toast]. This method can be invoked from any Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the [Toast].
     */
    @JvmStatic
    fun showAnyThread(context: Context?, text: CharSequence) {
        CoroutineScope(Dispatchers.Main).launch { showToastUIThread(context, text) }
    }

    /**
     * Show [Toast] in the UI Thread.
     *
     * @param context Context of the Application.
     * @param text    Message to display in the [Toast].
     */
    private fun showToastUIThread(context: Context?, text: CharSequence) {
        if (context == null) {
            AppLogger.w("Can not display Toast, Context is null")
            return
        }
        if (text.isEmpty()) {
            AppLogger.w("Can not display Toast, Text is null or empty")
            return
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        AppLogger.i("Toast:$text")
    }
}
