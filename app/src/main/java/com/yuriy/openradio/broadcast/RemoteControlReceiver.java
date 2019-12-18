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

package com.yuriy.openradio.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import androidx.core.content.ContextCompat;

import com.yuriy.openradio.model.storage.ServiceLifecyclePreferencesManager;
import com.yuriy.openradio.service.OpenRadioService;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 3/5/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class RemoteControlReceiver extends BroadcastReceiver {

    private static final String CLASS_NAME = RemoteControlReceiver.class.getSimpleName();

    public RemoteControlReceiver() {
        super();
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (context == null) {
            return;
        }
        if (!ServiceLifecyclePreferencesManager.isServiceActive(context)) {
            return;
        }
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }
        final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        final int keyCode = event != null ? event.getKeyCode() : Integer.MIN_VALUE;
        AppLogger.d(CLASS_NAME + " KeyCode:" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                ContextCompat.startForegroundService(context, OpenRadioService.makePlayLastPlayedItemIntent(context));
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                ContextCompat.startForegroundService(context, OpenRadioService.makeToggleLastPlayedItemIntent(context));
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                ContextCompat.startForegroundService(context, OpenRadioService.makeStopLastPlayedItemIntent(context));
                break;
            default:
                AppLogger.w(CLASS_NAME + " Unhandled key code:" + keyCode);
                break;
        }
    }
}
