/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.yuriy.openradio.shared.model.storage.ServiceLifecyclePreferencesManager
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 3/5/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class RemoteControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ServiceLifecyclePreferencesManager.isServiceActive(context)) {
            return
        }
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) {
            return
        }
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        val keyCode = event?.keyCode ?: Int.MIN_VALUE
        AnalyticsUtils.logMessage("$CLASS_NAME [" + this.hashCode() + "]->onReceive(" + keyCode + "):startForegroundService")
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                ContextCompat.startForegroundService(context, OpenRadioService.makePlayLastPlayedItemIntent(context))
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                ContextCompat.startForegroundService(context, OpenRadioService.makeToggleLastPlayedItemIntent(context))
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                ContextCompat.startForegroundService(context, OpenRadioService.makeStopLastPlayedItemIntent(context))
            }
            else -> AppLogger.w("$CLASS_NAME Unhandled key code:$keyCode")
        }
    }

    companion object {
        private val CLASS_NAME = RemoteControlReceiver::class.java.simpleName
    }
}