package com.yuriy.openradio.shared.model.net

import android.content.Context

interface NetworkLayer {

    fun startMonitor(context: Context, listener: NetworkMonitorListener)

    fun stopMonitor(context: Context)

    fun checkConnectivityAndNotify(context: Context): Boolean

    fun isMobileNetwork(): Boolean
}
