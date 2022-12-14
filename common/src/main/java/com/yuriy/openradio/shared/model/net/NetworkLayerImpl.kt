/*
 * Copyright 2021-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.broadcast.AbstractReceiver
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.view.SafeToast

class NetworkLayerImpl(private val mConnectivityManager: ConnectivityManager) : NetworkLayer {

    /**
     * The BroadcastReceiver that tracks network connectivity changes.
     */
    private val mConnectivityReceiver: AbstractReceiver

    private var mListener: NetworkMonitorListener? = null

    private var mType = -1

    companion object {

        private const val CLASS_NAME = "NetworkMonitor"
    }

    init {
        mConnectivityReceiver = ConnectivityReceiver(
            object : ConnectivityReceiver.Listener {

                override fun onConnectivityChange() {
                    val networkInfo = mConnectivityManager.activeNetworkInfo
                    if (networkInfo == null) {
                        AppLogger.e("$CLASS_NAME network info is null")
                        return
                    }
                    mType = networkInfo.type
                    dumpQuality(networkInfo)
                    AppLogger.i("$CLASS_NAME network changed to $networkInfo")

                    mListener?.onConnectivityChange(networkInfo.isConnectedOrConnecting)
                }
            }
        )
    }

    override fun isMobileNetwork(): Boolean {
        return when (mType) {
            ConnectivityManager.TYPE_MOBILE,
            ConnectivityManager.TYPE_MOBILE_DUN,
            ConnectivityManager.TYPE_MOBILE_HIPRI,
            ConnectivityManager.TYPE_MOBILE_MMS,
            ConnectivityManager.TYPE_MOBILE_SUPL -> return true
            else -> false
        }
    }

    override fun startMonitor(context: Context, listener: NetworkMonitorListener) {
        // It might be need more then one listener. Currently only one is supported.
        mListener = listener
        mConnectivityReceiver.register(context)
    }

    override fun stopMonitor(context: Context) {
        mListener = null
        mConnectivityReceiver.unregister(context)
    }

    /**
     * Checks for the connection availability.
     *
     * @param context Context of the callee.
     * @return true if connection is available, false otherwise.
     */
    override fun checkConnectivityAndNotify(context: Context): Boolean {
        if (checkConnectivity()) {
            return true
        }
        SafeToast.showAnyThread(
            context,
            context.getString(R.string.no_network_connection_toast)
        )
        return false
    }

    /**
     * Checks for the connection availability.
     *
     * @return true if connection is available, false otherwise.
     */
    private fun checkConnectivity(): Boolean {
        val networkInfo = mConnectivityManager.activeNetworkInfo
        return !(networkInfo == null || !networkInfo.isConnectedOrConnecting)
    }

    private fun dumpQuality(info: NetworkInfo) {
        if (info.type == ConnectivityManager.TYPE_WIFI) {
            AppLogger.d("$CLASS_NAME NetQ:WiFi")
        } else if (info.type == ConnectivityManager.TYPE_MOBILE) {
            AppLogger.d("$CLASS_NAME NetQ:Mobile:${info.subtype}")
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
}
