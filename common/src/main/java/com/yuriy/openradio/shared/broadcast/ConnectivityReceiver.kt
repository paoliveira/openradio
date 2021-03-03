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
package com.yuriy.openradio.shared.broadcast

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.view.SafeToast

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * This class is designed in a way to handle actions associated with connectivity, such as listening to events from
 * system, provide check on available network, etc ...
 */
class ConnectivityReceiver (private val mListener: Listener) : AbstractReceiver() {
    /**
     * Listener to provide callback about connectivity events.
     */
    interface Listener {
        /**
         * Call when connectivity changed.
         *
         * @param isConnected Whether or not connectivity available.
         */
        fun onConnectivityChange(isConnected: Boolean)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val manager = getConnectivityManager(context)
        val networkInfo = manager.activeNetworkInfo
        if (networkInfo == null) {
            AppLogger.e("$CLASS_NAME network info is null")
            mListener.onConnectivityChange(false)
            return
        }
        getQuality(networkInfo)
        AppLogger.i(CLASS_NAME + " network connected:" + networkInfo.isConnected)
        mListener.onConnectivityChange(networkInfo.isConnected)
    }

    override fun makeIntentFilter(): IntentFilter {
        return IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    }

    private fun getQuality(info: NetworkInfo) {
        if (info.type == ConnectivityManager.TYPE_WIFI) {
            AppLogger.d("$CLASS_NAME NetQ:WiFi")
        } else if (info.type == ConnectivityManager.TYPE_MOBILE) {
            AppLogger.d(CLASS_NAME + " NetQ:Mobile:" + info.subtype)
            when (info.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS -> {
                    // Bandwidth between 100 kbps and below
                }
                TelephonyManager.NETWORK_TYPE_EDGE -> {
                    // Bandwidth between 50-100 kbps
                }
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> {
                    // Bandwidth between 400-1000 kbps
                }
                TelephonyManager.NETWORK_TYPE_EVDO_A -> {
                    // Bandwidth between 600-1400 kbps
                }
            }

            // Other list of various subtypes you can check for and their bandwidth limits
            // TelephonyManager.NETWORK_TYPE_1xRTT       ~ 50-100 kbps
            // TelephonyManager.NETWORK_TYPE_CDMA        ~ 14-64 kbps
            // TelephonyManager.NETWORK_TYPE_HSDPA       ~ 2-14 Mbps
            // TelephonyManager.NETWORK_TYPE_HSPA        ~ 700-1700 kbps
            // TelephonyManager.NETWORK_TYPE_HSUPA       ~ 1-23 Mbps
            // TelephonyManager.NETWORK_TYPE_UMTS        ~ 400-7000 kbps
            // TelephonyManager.NETWORK_TYPE_UNKNOWN     ~ Unknown
        }
    }

    companion object {
        private val CLASS_NAME = ConnectivityReceiver::class.java.simpleName

        /**
         * Checks for the connection availability.
         *
         * @param context Context of the callee.
         * @return `true` if connection is available, `false` otherwise.
         */
        @SuppressLint("MissingPermission")
        fun checkConnectivity(context: Context): Boolean {
            val manager = getConnectivityManager(context)
            val networkInfo = manager.activeNetworkInfo
            return !(networkInfo == null || !networkInfo.isConnectedOrConnecting)
        }

        /**
         * Checks for the connection availability.
         *
         * @param context Context of the callee.
         * @return `true` if connection is available, `false` otherwise.
         */
        fun checkConnectivityAndNotify(context: Context): Boolean {
            if (checkConnectivity(context)) {
                return true
            }
            SafeToast.showAnyThread(
                    context,
                    context.getString(R.string.no_network_connection_toast)
            )
            return false
        }

        /**
         * Create instance of the system's Connectivity Service.
         *
         * @param context Context of the callee.
         * @return [ConnectivityManager] or `null`.
         */
        private fun getConnectivityManager(context: Context): ConnectivityManager {
            return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
    }
}
