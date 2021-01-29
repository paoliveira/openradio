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

import android.util.Base64
import com.yuriy.openradio.shared.utils.AppLogger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal class GoogleDriveSaveFile(isTerminator: Boolean) : GoogleDriveAPIChain(isTerminator) {

    override fun handleRequest(request: GoogleDriveRequest, result: GoogleDriveResult) {
        val name = request.fileName
        AppLogger.d("Save file '$name'")
        if (result.fileId != null) {
            // Create new file and save data to it.
            val data = Base64.encodeToString(request.data!!.toByteArray(), Base64.DEFAULT)
            request.googleApiClient.createFile(result.folderId!!, name, data)
                    .addOnSuccessListener { fileId: String ->
                        AppLogger.d("File '$fileId' created")
                        request.listener.onUploadComplete()
                    }
                    .addOnFailureListener {
                        request.listener.onError(
                                GoogleDriveError("File '$name' is not created")
                        )
                    }
        } else {
            AppLogger.d("File '$name' not exists, nothing to save, path execution farther")
            handleNext(request, result)
        }
    }
}
