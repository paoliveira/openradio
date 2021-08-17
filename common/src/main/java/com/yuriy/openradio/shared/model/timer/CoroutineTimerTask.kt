package com.yuriy.openradio.shared.model.timer

import com.yuriy.openradio.shared.utils.AppLogger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.*

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
