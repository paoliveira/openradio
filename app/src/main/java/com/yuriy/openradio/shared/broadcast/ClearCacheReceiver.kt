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
package com.yuriy.openradio.shared.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Receiver for the local broadcast event when clear internal API cache is required.
 */
class ClearCacheReceiver(listener: ClearCacheReceiverListener) {
    private val mReceiver: BroadcastReceiver

    /**
     * Register event listener.
     *
     * @param context Context of the callee.
     */
    fun register(context: Context?) {
        val intentFilter = IntentFilter(
                AppLocalBroadcast.getActionClearCache()
        )
        LocalBroadcastManager.getInstance(context!!).registerReceiver(mReceiver, intentFilter)
    }

    /**
     * Unregister event listener.
     *
     * @param context Context of the callee.
     */
    fun unregister(context: Context?) {
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(mReceiver)
    }

    /**
     * Internal listener for the broadcast event when master volume changed.
     */
    private class BroadcastReceiverImpl(listener: ClearCacheReceiverListener) : BroadcastReceiver() {
        private val mListener: ClearCacheReceiverListener?
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (!TextUtils.equals(action, AppLocalBroadcast.getActionClearCache())) {
                return
            }
            if (mListener == null) {
                return
            }
            mListener.onClearCache()
        }

        /**
         * Main constructor.
         *
         * @param listener Listener for the master volume changed event.
         */
        init {
            mListener = listener
        }
    }

    /**
     * Default constructor.
     *
     * @param listener Listener for the clear cache event.
     */
    init {
        mReceiver = BroadcastReceiverImpl(listener)
    }
}