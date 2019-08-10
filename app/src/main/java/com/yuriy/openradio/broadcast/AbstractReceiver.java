package com.yuriy.openradio.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Base cass to handle broadcast receivers. For example, register and unregister safely.
 */
public abstract class AbstractReceiver extends BroadcastReceiver {

    private volatile boolean mIsRegistered;
    private final IntentFilter mIntentFilter;

    /**
     *
     * @param intentFilter
     */
    public AbstractReceiver(@NonNull final IntentFilter intentFilter) {
        super();
        mIsRegistered = false;
        mIntentFilter = intentFilter;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        /* Handle in implementor */
    }

    public void register(@NonNull final Context context) {
        if (!mIsRegistered) {
            context.registerReceiver(this, mIntentFilter);
        }
        mIsRegistered = true;
    }

    public void unregister(@NonNull final Context context) {
        if (mIsRegistered) {
            context.unregisterReceiver(this);
        }
        mIsRegistered = false;
    }
}
