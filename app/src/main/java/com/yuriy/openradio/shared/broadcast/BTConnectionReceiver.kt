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

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.IntentUtils.intentBundleToString

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

    private val mBluetoothAdapter: BluetoothAdapter?
    private val mProfileListener: BluetoothProfileServiceListenerImpl?
    private var mConnectedDevice: String? = null
    override fun onReceive(context: Context, intent: Intent) {
        i("$CLASS_NAME receive:$intent")
        i(CLASS_NAME + "  data:" + intentBundleToString(intent))
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_CONNECTED -> {
                i("$CLASS_NAME connected")
                locateDevice(context)
            }
            BluetoothAdapter.STATE_DISCONNECTED -> {
                i("$CLASS_NAME disconnected:$mConnectedDevice")
                if (!TextUtils.isEmpty(mConnectedDevice)) {
                    mListener.onDisconnected()
                }
            }
        }
    }

    override fun unregister(context: Context) {
        mProfileListener?.clear()
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
    fun locateDevice(context: Context?) {
        // Establish connection to the proxy.
        if (mBluetoothAdapter == null) {
            return
        }
        // Check whether proxy was connected.
        mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET)
    }

    /**
     *
     */
    private inner class BluetoothProfileServiceListenerImpl : BluetoothProfile.ServiceListener {
        private var mBluetoothHeadset: BluetoothHeadset? = null
        private var mProfile = 0
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            i("$CLASS_NAME connected profile:$profile")
            mBluetoothHeadset = proxy as BluetoothHeadset
            mProfile = profile
            i("$CLASS_NAME connected headset:$mBluetoothHeadset")
            val list = mBluetoothHeadset!!.connectedDevices
            if (list.isEmpty()) {
                d("$CLASS_NAME connected devices are empty")
                return
            }
            var connectedDevice: String? = null
            for (device in list) {
                i(
                        CLASS_NAME + " device name:" + device.name
                                + ", MAC:" + device.address
                                + ", state:" + mBluetoothHeadset!!.getConnectionState(device)
                )
                if (mBluetoothHeadset!!.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = device.address
                    break
                }
            }
            if (TextUtils.equals(connectedDevice, mConnectedDevice)) {
                i("$CLASS_NAME connected to same BT device.")
                mListener.onSameDeviceConnected()
            }
            mConnectedDevice = connectedDevice
        }

        override fun onServiceDisconnected(profile: Int) {
            i("$CLASS_NAME disconnected headset:$profile")
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null
            }
        }

        fun clear() {
            if (mBluetoothAdapter != null && mBluetoothHeadset != null) {
                i("$CLASS_NAME clear")
                mBluetoothAdapter.closeProfileProxy(mProfile, mBluetoothHeadset)
            }
        }
    }

    companion object {
        private val CLASS_NAME = BTConnectionReceiver::class.java.simpleName
    }

    /**
     * Main constructor.
     *
     */
    init {
        mProfileListener = BluetoothProfileServiceListenerImpl()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }
}