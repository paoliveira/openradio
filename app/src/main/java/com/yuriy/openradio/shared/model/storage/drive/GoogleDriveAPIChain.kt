/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.model.storage.drive

import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppUtils.isUiThread
import java.util.concurrent.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class GoogleDriveAPIChain(private val mIsTerminator: Boolean, executorService: ExecutorService) {

    private var mNext: GoogleDriveAPIChain? = null
    val mExecutorService: ExecutorService = executorService

    abstract fun handleRequest(request: GoogleDriveRequest, result: GoogleDriveResult)

    fun setNext(value: GoogleDriveAPIChain) {
        mNext = value
    }

    fun handleNext(request: GoogleDriveRequest, result: GoogleDriveResult) {
        if (mIsTerminator) {
            d("No more requests to handle")
            return
        }
        // Callbacks from Google framework comes in UI thread.
        if (isUiThread()) {
            if (mExecutorService.isShutdown) {
                e("Executor is terminated, can't handle requests")
                return
            }
            mExecutorService.submit { mNext!!.handleRequest(request, result) }
        } else {
            mNext!!.handleRequest(request, result)
        }
    }
}
