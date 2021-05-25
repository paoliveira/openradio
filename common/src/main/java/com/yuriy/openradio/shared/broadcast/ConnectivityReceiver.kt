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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Receiver for the connectivity changed event.
 */
class ConnectivityReceiver(private val mListener: Listener) : AbstractReceiver() {

    /**
     * Listener to provide callback about connectivity events.
     */
    interface Listener {

        /**
         * Call when connectivity changed.
         */
        fun onConnectivityChange(context: Context, intent: Intent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        mListener.onConnectivityChange(context, intent)
    }

    override fun makeIntentFilter(): IntentFilter {
        return IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    }
}
