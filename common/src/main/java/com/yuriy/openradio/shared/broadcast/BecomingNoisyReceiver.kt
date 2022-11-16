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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.yuriy.openradio.shared.utils.AppLogger.i

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class designed in a way to listen for audio noisy events, such as disconnecting bluetooth device or unplug
 * headphones.
 */
class BecomingNoisyReceiver(private val mListener: Listener) : AbstractReceiver() {
    /**
     *
     */
    interface Listener {
        /**
         *
         */
        fun onAudioBecomingNoisy()
    }

    override fun onReceive(context: Context, intent: Intent) {
        i(CLASS_NAME + " receive:" + intent.action)
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
            mListener.onAudioBecomingNoisy()
        }
    }

    override fun makeIntentFilter(): IntentFilter {
        return IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    }

    companion object {
        private val CLASS_NAME = BecomingNoisyReceiver::class.java.simpleName
    }
}