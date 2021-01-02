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
package com.yuriy.openradio.shared.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast.getActionValidateOfRSFailed
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast.getActionValidateOfRSFailedReason
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast.getActionValidateOfRSSuccess
import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast.getActionValidateOfRSSuccessMessage

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Receiver for the local broadcast event when Radio Station that supposed to be added is validated.
 *
 * @param listener Listener for the master volume changed event.
 */
class RSAddValidatedReceiver(listener: RSAddValidatedReceiverListener) {
    private val mReceiver: BroadcastReceiver

    /**
     * Register event listener.
     *
     * @param context Context of the callee.
     */
    fun register(context: Context?) {
        val intentFilter = IntentFilter(
                getActionValidateOfRSFailed()
        )
        intentFilter.addAction(getActionValidateOfRSSuccess())
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
     *
     * @param listener Listener for the master volume changed event.
     */
    private class BroadcastReceiverImpl(listener: RSAddValidatedReceiverListener) : BroadcastReceiver() {

        private val mListener: RSAddValidatedReceiverListener?

        override fun onReceive(context: Context, intent: Intent) {
            if (mListener == null) {
                return
            }
            val action = intent.action
            if (TextUtils.equals(action, getActionValidateOfRSSuccess())) {
                mListener.onSuccess(getActionValidateOfRSSuccessMessage(intent))
            }
            if (TextUtils.equals(action, getActionValidateOfRSFailed())) {
                mListener.onFailure(getActionValidateOfRSFailedReason(intent))
            }
        }

        init {
            mListener = listener
        }
    }

    init {
        mReceiver = BroadcastReceiverImpl(listener)
    }
}