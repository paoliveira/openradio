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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This is a receiver for Bluetooth device connection events.
 */
class BTConnectionReceiver(private val mListener: Listener) : AbstractReceiver() {

    interface Listener {
        fun onSameDeviceConnected()
        fun onDisconnected()
    }

    private var mConnectedDevice = AppUtils.EMPTY_STRING

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.i("$CLASS_NAME receive:$intent")
        AppLogger.i("$CLASS_NAME    data:" + IntentUtils.intentBundleToString(intent))
        if (!intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
            return
        }
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_CONNECTED -> {
                AppLogger.i("$CLASS_NAME connected to ${device.address}")
                if (device.address == mConnectedDevice) {
                    AppLogger.i("$CLASS_NAME connected to the same device.")
                    mListener.onSameDeviceConnected()
                }
                mConnectedDevice = device.address
            }
            BluetoothAdapter.STATE_DISCONNECTED -> {
                AppLogger.i("$CLASS_NAME disconnected from ${device.address}")
                if (mConnectedDevice.isNotEmpty()) {
                    mListener.onDisconnected()
                }
            }
        }
    }

    override fun makeIntentFilter(): IntentFilter {
        return IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
    }

    companion object {
        private val CLASS_NAME = BTConnectionReceiver::class.java.simpleName
    }
}
