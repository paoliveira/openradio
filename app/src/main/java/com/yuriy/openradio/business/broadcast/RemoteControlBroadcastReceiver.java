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
import android.view.KeyEvent;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 3/5/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RemoteControlBroadcastReceiver extends BroadcastReceiver {

    private static final String CLASS_NAME = RemoteControlBroadcastReceiver.class.getSimpleName();

    public RemoteControlBroadcastReceiver() {
        super();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        final int keyCode = event != null ? event.getKeyCode() : Integer.MIN_VALUE;
        FabricUtils.logCustomEvent(
                FabricUtils.EVENT_NAME_REMOTE_CONTROL_RECEIVER_RECEIVED, "KeyCode", (keyCode)
        );
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:

                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:

                break;
            default:
                AppLogger.w(CLASS_NAME + " Unhandled key code:" + keyCode);
                break;
        }
    }
}
