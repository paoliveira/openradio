/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal class GoogleDriveDeleteFile(isTerminator: Boolean = false) : GoogleDriveAPIChain(isTerminator) {

    override fun handleRequest(request: GoogleDriveRequest, result: GoogleDriveResult) {
        val name = request.fileName
        AppLogger.d("Delete file '$name'")
        if (result.fileId != null) {
            request.googleApiClient.deleteFile(result.fileId!!)
                    .addOnSuccessListener {
                        val msg = "File '$name' deleted, path execution farther [$request/$result]"
                        AppLogger.d(msg)
                        AnalyticsUtils.logGDriveFileDeleted(msg)
                        handleNext(request, result)
                    }
                    .addOnFailureListener {
                        request.listener.onError(
                                GoogleDriveError("File '$name' is not deleted")
                        )
                    }
        } else {
            AppLogger.d("File '$name' not exists, nothing to delete, path execution farther")
            handleNext(request, result)
        }
    }
}
