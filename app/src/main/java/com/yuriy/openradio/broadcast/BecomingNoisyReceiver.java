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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/12/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class designed in a way to listen for audio noisy events, such as disconnecting bluetooth device or unplug
 * headphones.
 */
public final class BecomingNoisyReceiver extends AbstractReceiver {

    /**
     *
     */
    public interface BecomingNoisyReceiverListener {

        /**
         *
         */
        void onAudioBecomingNoisy();
    }

    private static final String CLASS_NAME = BecomingNoisyReceiver.class.getSimpleName();
    private final BecomingNoisyReceiverListener mListener;

    /**
     * Main constructor.
     *
     * @param listener Listener.
     */
    public BecomingNoisyReceiver(final BecomingNoisyReceiverListener listener) {
        super(new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        mListener = listener;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppLogger.i(CLASS_NAME + " receive:" + intent.getAction());
        if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            mListener.onAudioBecomingNoisy();
        }
    }
}
