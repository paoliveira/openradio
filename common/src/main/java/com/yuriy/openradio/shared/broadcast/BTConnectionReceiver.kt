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
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.IntentUtils

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class designed in a way to listen for audio noisy events, such as disconnecting bluetooth device or unplug
 * headphones.
 */
class BTConnectionReceiver(private val mListener: Listener) : AbstractReceiver() {

    interface Listener {
        fun onSameDeviceConnected()
        fun onDisconnected()
    }

    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val mProfileListener = BluetoothProfileServiceListenerImpl()
    private var mConnectedDevice = AppUtils.EMPTY_STRING

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.i("$CLASS_NAME receive:$intent")
        AppLogger.i("$CLASS_NAME data:" + IntentUtils.intentBundleToString(intent))
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_CONNECTED -> {
                AppLogger.i("$CLASS_NAME connected")
                if (!intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
                    return
                }
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                handleConnectedDevice(device.address)
            }
            BluetoothAdapter.STATE_DISCONNECTED -> {
                AppLogger.i("$CLASS_NAME disconnected:$mConnectedDevice")
                if (mConnectedDevice.isNotEmpty()) {
                    mListener.onDisconnected()
                }
            }
        }
    }

    override fun unregister(context: Context) {
        mProfileListener.clear()
        super.unregister(context)
    }

    override fun makeIntentFilter(): IntentFilter {
        return IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
    }

    /**
     * Locate connected device.
     *
     * @param context Context of callee.
     */
    fun locateDevice(context: Context, intent: Intent?) {
        // Establish connection to the proxy.
        if (mBluetoothAdapter == null) {
            return
        }
        try {
            // Check whether proxy was connected.
            mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET)
        } catch (e: Exception) {
            // SecurityException: query intent receivers: Requires android.permission.INTERACT_ACROSS_USERS_FULL or
            // android.permission.INTERACT_ACROSS_USERS.
            // Linking to BluetoothAdapter.getProfileProxy
            val msg = "$CLASS_NAME can not locate device, ctx:$context, intent:$intent, " +
                    "data:${IntentUtils.intentBundleToString(intent)}, e:${Log.getStackTraceString(e)}"
            AnalyticsUtils.logMessage(msg)
        }
    }

    private fun handleConnectedDevice(connectedDevice: String?) {
        AppLogger.i("$CLASS_NAME handle connected device $connectedDevice")
        if (connectedDevice.isNullOrEmpty()) {
            return
        }
        if (connectedDevice == mConnectedDevice) {
            AppLogger.i("$CLASS_NAME connected to same BT device.")
            mListener.onSameDeviceConnected()
        }
        mConnectedDevice = connectedDevice
    }

    private inner class BluetoothProfileServiceListenerImpl : BluetoothProfile.ServiceListener {

        private var mBluetoothHeadset: BluetoothHeadset? = null
        private var mProfile = 0

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            AppLogger.i("$CLASS_NAME connected profile:$profile, proxy:$proxy")
            mBluetoothHeadset = proxy as BluetoothHeadset
            mProfile = profile
            AppLogger.i("$CLASS_NAME connected headset:$mBluetoothHeadset")

            val devices = mBluetoothHeadset?.getDevicesMatchingConnectionStates(mStates)
            devices?.forEach { device ->
                AppLogger.d("$CLASS_NAME ${device.name} ${device.bondState} ${device.address}")
            }

            val list = mBluetoothHeadset!!.connectedDevices
            if (list.isEmpty()) {
                AppLogger.d("$CLASS_NAME connected devices are empty")
                return
            }
            var connectedDevice: String? = null
            for (device in list) {
                AppLogger.i(
                        "$CLASS_NAME device name:${device.name}, MAC:${device.address}, " +
                                "state:${mBluetoothHeadset?.getConnectionState(device)}"
                )
                if (mBluetoothHeadset?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = device.address
                    break
                }
            }
            handleConnectedDevice(connectedDevice)
        }

        override fun onServiceDisconnected(profile: Int) {
            AppLogger.i("$CLASS_NAME disconnected headset:$profile")
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null
            }
        }

        fun clear() {
            if (mBluetoothAdapter != null && mBluetoothHeadset != null) {
                AppLogger.i("$CLASS_NAME clear")
                mBluetoothAdapter.closeProfileProxy(mProfile, mBluetoothHeadset)
            }
        }
    }

    companion object {
        private val CLASS_NAME = BTConnectionReceiver::class.java.simpleName
        private val mStates = intArrayOf(
            BluetoothProfile.STATE_DISCONNECTING,
            BluetoothProfile.STATE_DISCONNECTED,
            BluetoothProfile.STATE_CONNECTED,
            BluetoothProfile.STATE_CONNECTING
        )
    }
}
