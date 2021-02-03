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

package com.yuriy.openradio.gabor.shared.broadcast;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.IntentUtils;

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
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothProfileServiceListenerImpl mProfileListener;
    private String mConnectedDevice;
    private final Listener mListener;

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

    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppLogger.i(CLASS_NAME + " receive:" + intent);
        if (intent == null) {
            return;
        }
        AppLogger.i(CLASS_NAME + " data:" + IntentUtils.intentBundleToString(intent));
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR);
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTED:
                AppLogger.i(CLASS_NAME + " connected");
                locateDevice(context);
                break;
            case BluetoothAdapter.STATE_DISCONNECTED:
                AppLogger.i(CLASS_NAME + " disconnected:" + mConnectedDevice);
                if (!TextUtils.isEmpty(mConnectedDevice)) {
                    mListener.onDisconnected();
                }
                break;
        }
    }

    @Override
    public void unregister(@NonNull Context context) {
        if (mProfileListener != null) {
            mProfileListener.clear();
        }
        super.unregister(context);
    }

    /**
     * Locate connected device.
     *
     * @param context Context of callee.
     */
    public void locateDevice(final Context context) {
        // Establish connection to the proxy.
        if (mBluetoothAdapter == null) {
            return;
        }
        // Check whether proxy was connected.
        mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.HEADSET);
    }

    /**
     *
     */
    private class BluetoothProfileServiceListenerImpl implements BluetoothProfile.ServiceListener {

        private BluetoothHeadset mBluetoothHeadset;
        private int mProfile;

        /**
         * Default constructor.
         */
        private BluetoothProfileServiceListenerImpl() {
            super();
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            AppLogger.i(CLASS_NAME + " connected profile:" + profile);
            mBluetoothHeadset = (BluetoothHeadset) proxy;
            mProfile = profile;
            AppLogger.i(CLASS_NAME + " connected headset:" + mBluetoothHeadset);
            final List<BluetoothDevice> list = mBluetoothHeadset.getConnectedDevices();
            if (list.isEmpty()) {
                AppLogger.d(CLASS_NAME + " connected devices are empty");
                return;
            }
            String connectedDevice = null;
            for (final BluetoothDevice device : list) {
                AppLogger.i(
                        CLASS_NAME + " device name:" + device.getName()
                                + ", MAC:" + device.getAddress()
                                + ", state:" + mBluetoothHeadset.getConnectionState(device)
                );
                if (mBluetoothHeadset.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                    connectedDevice = device.getAddress();
                    break;
                }
            }

            if (TextUtils.equals(connectedDevice, BTConnectionReceiver.this.mConnectedDevice)) {
                AppLogger.i(CLASS_NAME + " connected to same BT device.");
                BTConnectionReceiver.this.mListener.onSameDeviceConnected();
            }
            BTConnectionReceiver.this.mConnectedDevice = connectedDevice;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            AppLogger.i(CLASS_NAME + " disconnected headset:" + profile);
            if (profile == BluetoothProfile.HEADSET) {
                mBluetoothHeadset = null;
            }
        }

        private void clear() {
            if (mBluetoothAdapter != null && mBluetoothHeadset != null) {
                AppLogger.i(CLASS_NAME + " clear");
                mBluetoothAdapter.closeProfileProxy(mProfile, mBluetoothHeadset);
            }
        }
    }
}
