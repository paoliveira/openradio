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

package com.yuriy.openradio.shared.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class AppLocalReceiver private constructor() : BroadcastReceiver() {
    /**
     * Callback listener of the various events.
     */
    private var mCallback: AppLocalReceiverCallback? = null

    /**
     * Register listener for the Local broadcast receiver actions. This listener pass events to
     * Activity class.
     *
     * @param callback Implementation of the [AppLocalReceiverCallback]
     */
    fun registerListener(callback: AppLocalReceiverCallback?) {
        mCallback = callback
    }

    /**
     * Unregister listener for the Local broadcast receiver actions.
     */
    fun unregisterListener() {
        mCallback = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.i("$CLASS_NAME On receive:$intent")
        val action = intent.action
        if (action == null || action.isEmpty()) {
            return
        }
        if (action == AppLocalBroadcast.getActionLocationChanged()) {
            if (mCallback != null) {
                mCallback!!.onLocationChanged()
            }
        }
        if (action == AppLocalBroadcast.getActionSleepTimer()) {
            if (mCallback != null) {
                mCallback!!.onSleepTimer()
            }
        }
        if (action == AppLocalBroadcast.getActionSortIdChanged()) {
            if (mCallback != null) {
                mCallback!!.onSortIdChanged(AppLocalBroadcast.getSortId(intent))
            }
        }
        if (action == AppLocalBroadcast.getActionCurrentIndexOnQueueChanged()) {
            if (mCallback != null) {
                mCallback!!.onCurrentIndexOnQueueChanged(
                        AppLocalBroadcast.getCurrentIndexOnQueue(intent),
                        AppLocalBroadcast.getCurrentMediaIdOnQueue(intent)
                )
            }
        }
    }

    companion object {
        /**
         * Tag name to use in the logging.
         */
        private val CLASS_NAME = AppLocalReceiver::class.java.simpleName

        /**
         * Factory method to create default instance.
         *
         * @return Instance of the [AppLocalReceiver]
         */
        val instance: AppLocalReceiver
            get() = AppLocalReceiver()
    }
}