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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.yuriy.openradio.R;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.view.SafeToast;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class is designed in a way to handle actions associated with connectivity, such as listening to events from
 * system, provide check on available network, etc ...
 */
public final class ConnectivityReceiver extends AbstractReceiver {

    /**
     * Listener to provide callback about connectivity events.
     */
    public interface ConnectivityChangeListener {

        /**
         * Call when connectivity changed.
         *
         * @param isConnected Whther or not connectivity available.
         */
        void onConnectivityChange(final boolean isConnected);
    }

    private static final String CLASS_NAME = ConnectivityReceiver.class.getSimpleName();
    private final ConnectivityChangeListener mListener;

    /**
     * Main constructor.
     *
     * @param listener Listener for the connectivity events.
     */
    public ConnectivityReceiver(@NonNull final ConnectivityChangeListener listener) {
        super(new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mListener = listener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager manager = getConnectivityManager(context);
        if (manager == null) {
            mListener.onConnectivityChange(false);
            return;
        }
        final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            AppLogger.e(CLASS_NAME + " network info is null");
            mListener.onConnectivityChange(false);
            return;
        }
        AppLogger.i(CLASS_NAME + " network connected:" + networkInfo.isConnected());
        mListener.onConnectivityChange(networkInfo.isConnected());
    }

    /**
     * Checks for the connection availability.
     *
     * @param context Context of the callee.
     * @return {@code true} if connection is available, {@code false} otherwise.
     */
    public static boolean checkConnectivity(@NonNull final Context context) {
        final ConnectivityManager manager = getConnectivityManager(context);
        if (manager == null) {
            return false;
        }
        final NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return !(networkInfo == null || !networkInfo.isConnectedOrConnecting());
    }

    /**
     * Checks for the connection availability.
     *
     * @param context Context of the callee.
     * @return {@code true} if connection is available, {@code false} otherwise.
     */
    public static boolean checkConnectivityAndNotify(@NonNull final Context context) {
        if (checkConnectivity(context)) {
            return true;
        }
        SafeToast.showAnyThread(
                context,
                context.getString(R.string.no_network_connection_toast)
        );
        return false;
    }

    /**
     * Create instance of the system's Connectivity Service.
     *
     * @param context Context of the callee.
     * @return {@link ConnectivityManager} or {@code null}.
     */
    @Nullable
    private static ConnectivityManager getConnectivityManager(@NonNull final Context context) {
        final ConnectivityManager manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            AppLogger.e("Connectivity Manager is null");
        }
        return manager;
    }
}
