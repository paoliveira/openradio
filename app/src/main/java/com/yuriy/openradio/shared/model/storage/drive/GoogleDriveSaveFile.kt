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
package com.yuriy.openradio.shared.model.storage.drive

import android.util.Base64
import com.yuriy.openradio.shared.utils.AppLogger.d
import java.util.concurrent.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal class GoogleDriveSaveFile(isTerminator: Boolean, executorService: ExecutorService) :
        GoogleDriveAPIChain(isTerminator, executorService) {

    override fun handleRequest(request: GoogleDriveRequest, result: GoogleDriveResult) {
        d("Save file '" + request.fileName + "'")

        // Create new file and save data to it.
        val data = Base64.encodeToString(request.data!!.toByteArray(), Base64.DEFAULT)
        request.googleApiClient.createFile(mExecutorService, result.folderId, request.fileName, data)
                .addOnSuccessListener { fileId: String ->
                    d("File '$fileId' created")
                    request.listener.onUploadComplete()
                }
                .addOnFailureListener { e: Exception? ->
                    request.listener.onError(
                            GoogleDriveError("File '" + request.fileName + "' is not created")
                    )
                }
    }
}
