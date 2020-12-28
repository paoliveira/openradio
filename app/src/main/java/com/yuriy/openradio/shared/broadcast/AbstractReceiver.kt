package com.yuriy.openradio.shared.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Base cass to handle broadcast receivers. For example, register and unregister safely.
 */
abstract class AbstractReceiver : BroadcastReceiver() {
    @Volatile
    private var mIsRegistered = false
    abstract fun makeIntentFilter(): IntentFilter?
    override fun onReceive(context: Context, intent: Intent) {
        /* Handle in implementor */
    }

    fun register(context: Context) {
        if (!mIsRegistered) {
            context.registerReceiver(this, makeIntentFilter())
        }
        mIsRegistered = true
    }

    open fun unregister(context: Context) {
        if (mIsRegistered) {
            context.unregisterReceiver(this)
        }
        mIsRegistered = false
    }
}