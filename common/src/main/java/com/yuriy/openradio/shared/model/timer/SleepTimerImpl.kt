/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.timer

import com.yuriy.openradio.shared.utils.AppLogger

class SleepTimerImpl private constructor(private val mListener: SleepTimerListener): SleepTimer {

    private val mTimerTask = CoroutineTimerTask("SleepTimerTask") {
        AppLogger.i("Sleep timer completed")
        mListener.onComplete()
    }

    override fun handle(enabled:Boolean, time: Long) {
        mTimerTask.cancel()
        AppLogger.i("Sleep timer cancelled")
        if (!enabled) {
            return
        }
        val delay = time - System.currentTimeMillis()
        if (delay < 0) {
            AppLogger.e("Sleep timer can ot be started with negative delay")
            return
        }
        mTimerTask.mDelay = delay
        mTimerTask.start()
        AppLogger.i("Sleep timer started for ${mTimerTask.mDelay} ms")
    }

    companion object {

        fun makeInstance(listener: SleepTimerListener): SleepTimer {
            return SleepTimerImpl(listener)
        }
    }
}