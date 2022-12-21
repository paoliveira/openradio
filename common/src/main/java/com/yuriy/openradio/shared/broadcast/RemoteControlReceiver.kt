/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon
import com.yuriy.openradio.shared.dependencies.RemoteControlListenerDependency
import com.yuriy.openradio.shared.model.media.RemoteControlListener
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.IntentUtils

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/12/22
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This is a receiver for Remote Control events.
 *
 * adb shell am broadcast -a android.intent.action.MEDIA_BUTTON -n com.yuriy.openradio/.shared.broadcast.RemoteControlReceiver --ei android.intent.extra.KEY_EVENT_TEST 126
 */
class RemoteControlReceiver : BroadcastReceiver(), RemoteControlListenerDependency {

    private lateinit var mRemoteControlListener: RemoteControlListener

    override fun onReceive(context: Context, intent: Intent) {
        AppLogger.d("$CLASS_NAME [" + this.hashCode() + "]->onReceive(" + intent + ")")
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) {
            return
        }
        DependencyRegistryCommon.inject(this)
        val event = IntentUtils.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT, intent)
        var keyCode = event?.keyCode ?: Int.MIN_VALUE
        val intExtra = intent.getIntExtra(EXTRA_KEY_EVENT_TEST, Int.MIN_VALUE)
        if (keyCode == Int.MIN_VALUE && intExtra != Int.MIN_VALUE) {
            keyCode = intExtra
        }
        AnalyticsUtils.logMessage("$CLASS_NAME [" + this.hashCode() + "]->onReceive(" + keyCode + ")")
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                mRemoteControlListener.onMediaPlay()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                mRemoteControlListener.onMediaPlayPause()
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> {
                mRemoteControlListener.onMediaPauseStop()
            }
            else -> AppLogger.w("$CLASS_NAME Unhandled key code:$keyCode")
        }
    }

    override fun configureWith(listener: RemoteControlListener) {
        mRemoteControlListener = listener
    }

    companion object {
        private val CLASS_NAME = RemoteControlReceiver::class.java.simpleName
        private const val EXTRA_KEY_EVENT_TEST = "android.intent.extra.KEY_EVENT_TEST"
    }
}
