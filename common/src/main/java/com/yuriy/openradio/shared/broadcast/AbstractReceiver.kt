/*
 * Copyright 2018 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Base cass to handle broadcast receivers. For example, register and unregister safely.
 */
abstract class AbstractReceiver : BroadcastReceiver() {

    @Volatile
    private var mIsRegistered = false

    abstract fun makeIntentFilter(): IntentFilter

    override fun onReceive(context: Context, intent: Intent) {
        /* Handle in implementor */
    }

    fun register(context: Context) {
        if (!mIsRegistered) {
            val filter = makeIntentFilter()
            val intent = ContextCompat.registerReceiver(context,this, filter, ContextCompat.RECEIVER_EXPORTED)
            AppLogger.i("Register receiver $intent for $this by $filter")
        }
        mIsRegistered = true
    }

    open fun unregister(context: Context) {
        if (mIsRegistered) {
            context.unregisterReceiver(this)
            AppLogger.i("Unregister receiver $this")
        }
        mIsRegistered = false
    }
}
