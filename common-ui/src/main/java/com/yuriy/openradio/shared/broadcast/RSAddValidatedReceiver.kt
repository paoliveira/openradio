/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

/**
 * Receiver for the local broadcast event when Radio Station that supposed to be added is validated.
 *
 * @param mListener Listener for the master volume changed event.
 */
class RSAddValidatedReceiver(private val mListener: RSAddValidatedReceiverListener): LocalAbstractReceiver() {

    override fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter(
                AppLocalBroadcast.getActionValidateOfRSFailed()
        )
        intentFilter.addAction(AppLocalBroadcast.getActionValidateOfRSSuccess())
        return intentFilter
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == AppLocalBroadcast.getActionValidateOfRSSuccess()) {
            mListener.onSuccess(AppLocalBroadcast.getActionValidateOfRSSuccessMessage(intent))
        }
        if (action == AppLocalBroadcast.getActionValidateOfRSFailed()) {
            mListener.onFailure(AppLocalBroadcast.getActionValidateOfRSFailedReason(intent))
        }
    }
}