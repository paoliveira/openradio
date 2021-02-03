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

package com.yuriy.openradio.gabor.shared.model.storage.drive;

import android.util.Base64;

import com.yuriy.openradio.gabor.shared.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveSaveFile extends GoogleDriveAPIChain {

    GoogleDriveSaveFile() {
        this(false);
    }

    GoogleDriveSaveFile(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        AppLogger.d("Save file '" + request.getFileName() + "'");

        // Create new file and save data to it.
        final String data = Base64.encodeToString(request.getData().getBytes(), Base64.DEFAULT);
        request.getGoogleApiClient().createFile(result.getFolderId(), request.getFileName(), data)
                .addOnSuccessListener(
                        fileId -> {
                            AppLogger.d("File '" + fileId + "' created");
                            request.getListener().onUploadComplete();
                        }
                )
                .addOnFailureListener(
                        e -> request.getListener().onError(
                                new GoogleDriveError("File '" + request.getFileName() + "' is not created")
                        )
                );
    }
}