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

package com.yuriy.openradio.broadcast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.yuriy.openradio.utils.AppLogger;

import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class designed in a way to listen for audio noisy events, such as disconnecting bluetooth device or unplug
 * headphones.
 */
public final class BTConnectionReceiver extends AbstractReceiver {

    public interface Listener {

        void onSameDeviceConnected();
        void onDisconnected();
    }

    private static final String CLASS_NAME = BTConnectionReceiver.class.getSimpleName();

    @Nullable
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothProfile.ServiceListener mProfileListener;
    private String mConnectedDevice;
    private Listener mListener;

    /**
     * Main constructor.
     *
     */
    public BTConnectionReceiver(final Listener listener) {
        super(new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED));
        mListener = listener;
        mProfileListener = new BluetoothProfileServiceListenerImpl();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     *
     * @param context
     */
    public void locateDevice(final Context context) {
        // Establish connection to the proxy.
        if (mBluetoothAdapter == null) {
            return;
        }
        mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppLogger.i(CLASS_NAME + " receive:" + intent);
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED:
                AppLogger.i(CLASS_NAME + " connected");
                locateDevice(context);
                break;
            case BluetoothAdapter.STATE_DISCONNECTED:
                AppLogger.i(CLASS_NAME + " disconnected");
                mListener.onDisconnected();
                break;
        }
    }

    @Override
    public void unregister(@NonNull final Context context) {
        super.unregister(context);
    }

    /**
     *
     */
    private class BluetoothProfileServiceListenerImpl implements BluetoothProfile.ServiceListener {

        /**
         *
         */
        private BluetoothHeadset mBluetoothHeadset;

        /**
         *
         */
        private BluetoothProfileServiceListenerImpl() {
            super();
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            AppLogger.i(CLASS_NAME + " connected:" + profile);
            if (profile != BluetoothProfile.HEADSET) {
                return;
            }
            mBluetoothHeadset = (BluetoothHeadset) proxy;
            AppLogger.i(CLASS_NAME + " connected headset:" + mBluetoothHeadset);
            final List<BluetoothDevice> list = mBluetoothHeadset.getConnectedDevices();
            if (list.isEmpty()) {
                return;
            }
            String connectedDevice = null;
            for (final BluetoothDevice device : list) {
                AppLogger.i(
                        CLASS_NAME + "  device name:" + device.getName()
                                + ", MAC:" + device.getAddress()
                                + ", state:" + mBluetoothHeadset.getConnectionState(device)
                );
                if (mBluetoothHeadset.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = device.getAddress();
                    break;
                }
            }

            if (TextUtils.equals(connectedDevice, BTConnectionReceiver.this.mConnectedDevice)) {
                AppLogger.i("Connected to same BT device.");
                BTConnectionReceiver.this.mListener.onSameDeviceConnected();
            }
            BTConnectionReceiver.this.mConnectedDevice = connectedDevice;
            if (BTConnectionReceiver.this.mBluetoothAdapter != null) {
                BTConnectionReceiver.this.mBluetoothAdapter.closeProfileProxy(profile, mBluetoothHeadset);
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            AppLogger.i(CLASS_NAME + " disconnected headset:" + profile);
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }
    }
}
