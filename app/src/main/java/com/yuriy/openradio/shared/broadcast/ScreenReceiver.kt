/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.broadcast

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import com.yuriy.openradio.shared.utils.AppLogger.i

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class ScreenReceiver : AbstractReceiver() {

    override fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        return intentFilter
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (TextUtils.equals(action, Intent.ACTION_SCREEN_OFF)) {
            i("Screen OFF")
        } else if (TextUtils.equals(action, Intent.ACTION_SCREEN_ON)) {
            i("Screen ON")
        }
    }
}