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

package com.yuriy.openradio.shared.model.storage.drive;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;

import java.util.concurrent.ExecutorService;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class GoogleDriveAPIChain {

    private GoogleDriveAPIChain mNext;
    private final boolean mIsTerminator;
    final ExecutorService mExecutorService;

    GoogleDriveAPIChain(final boolean isTerminator, final ExecutorService executorService) {
        super();
        mIsTerminator = isTerminator;
        mExecutorService = executorService;
    }

    abstract protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result);

    public void setNext(final GoogleDriveAPIChain value) {
        mNext = value;
    }

    void handleNext(final GoogleDriveRequest request, final GoogleDriveResult result) {
        if (mIsTerminator) {
            AppLogger.d("No more requests to handle");
            return;
        }
        // Callbacks from Google framework comes in UI thread.
        if (AppUtils.isUiThread()) {
            if (mExecutorService.isShutdown()) {
                AppLogger.e("Executor is terminated, can't handle requests");
                return;
            }
            mExecutorService.submit(
                    () -> mNext.handleRequest(request, result)
            );
        } else {
            mNext.handleRequest(request, result);
        }
    }
}
