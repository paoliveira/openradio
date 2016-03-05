package com.yuriy.openradio.business;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 3/5/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String CLASS_NAME = RemoteControlReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        final int keyCode = event.getKeyCode();
        Log.d(CLASS_NAME, "Key event:" + event);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:

                break;
            default:
                Log.w(CLASS_NAME, "Unhandled key code:" + keyCode);
                break;
        }
    }
}
