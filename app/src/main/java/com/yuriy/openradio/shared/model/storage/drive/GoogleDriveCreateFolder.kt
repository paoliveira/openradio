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

import com.yuriy.openradio.shared.utils.AppLogger.d

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal class GoogleDriveCreateFolder(isTerminator: Boolean = false) : GoogleDriveAPIChain(isTerminator) {

    override fun handleRequest(request: GoogleDriveRequest,
                               result: GoogleDriveResult) {
        val name = request.folderName
        if (result.folderId != null) {
            d("Folder $name exists, path execution farther")
            handleNext(request, result)
        } else {
            request.googleApiClient.createFolder(request.folderName)
                    .addOnSuccessListener { folderId: String? ->
                        d("Folder $name created, pass execution farther")
                        result.folderId = folderId
                        handleNext(request, result)
                    }
                    .addOnFailureListener {
                        request.listener.onError(
                                GoogleDriveError("Folder $name is not created")
                        )
                    }
        }
    }
}
