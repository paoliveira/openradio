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

package com.yuriy.openradio.business.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Receiver for the local broadcast event when master volume of Exo Player changed.
 */
public final class MasterVolumeBroadcastReceiver {

    private final BroadcastReceiver mReceiver;

    /**
     * Default constructor.
     *
     * @param listener Listener for the master volume changed event.
     */
    public MasterVolumeBroadcastReceiver(final MasterVolumeBroadcastReceiverListener listener) {
        super();
        mReceiver = new BroadcastReceiverImpl(listener);
    }

    /**
     * Register event listener.
     *
     * @param context Context of the callee.
     */
    public final void register(final Context context) {
        final IntentFilter intentFilter = new IntentFilter(
                AppLocalBroadcast.getActionMasterVolumeChanged()
        );
        LocalBroadcastManager.getInstance(context).registerReceiver(mReceiver, intentFilter);
    }

    /**
     * Unregister event listener.
     *
     * @param context Context of the callee.
     */
    public final void unregister(final Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver);
    }

    /**
     * Internal listener for the broadcast event when master volume changed.
     */
    private static final class BroadcastReceiverImpl extends BroadcastReceiver {

        private final MasterVolumeBroadcastReceiverListener mListener;

        /**
         * Main constructor.
         *
         * @param listener Listener for the master volume changed event.
         */
        private BroadcastReceiverImpl(final MasterVolumeBroadcastReceiverListener listener) {
            super();
            mListener = listener;
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent == null) {
                return;
            }
            final String action = intent.getAction();
            if (!TextUtils.equals(action, AppLocalBroadcast.getActionMasterVolumeChanged())) {
                return;
            }
            if (mListener == null) {
                return;
            }
            mListener.onMasterVolumeChanged();
        }
    }
}
