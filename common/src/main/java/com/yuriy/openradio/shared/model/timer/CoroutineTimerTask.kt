/*
 * Copyright 2021-2022 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CoroutineTimerTask internal constructor(private val mName: String, action: suspend () -> Unit) {

    var mDelay: Long = 0
    private var mRepeat: Long? = null
    private val mCoroutineScope = GlobalScope
    private val mKeepRunning = AtomicBoolean(true)
    private var mJob: Job? = null

    private val tryAction = suspend {
        try {
            action()
        } catch (e: Throwable) {
            AppLogger.e("Can't do action on sleep timer", e)
        }
    }

    fun start() {
        mKeepRunning.set(true)
        mJob = mCoroutineScope.launch(CoroutineName(mName)) {
            delay(mDelay)
            if (mRepeat != null) {
                while (mKeepRunning.get()) {
                    tryAction()
                    delay(mRepeat!!)
                }
            } else {
                if (mKeepRunning.get()) {
                    tryAction()
                    this@CoroutineTimerTask.cancel()
                }
            }
        }
    }

    /**
     * Immediately stops the timer task, even if the job is currently running,
     * by cancelling the underlying Job.
     */
    fun cancel() {
        mKeepRunning.set(false)
        mJob?.cancel("cancel() called")
    }
}
