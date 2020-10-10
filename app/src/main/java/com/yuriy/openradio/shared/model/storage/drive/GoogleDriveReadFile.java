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

package com.yuriy.openradio.shared.model.storage.drive;

import android.util.Base64;

import com.google.api.services.drive.model.File;
import com.yuriy.openradio.shared.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveReadFile extends GoogleDriveAPIChain {

    GoogleDriveReadFile() {
        this(false);
    }

    GoogleDriveReadFile(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        AppLogger.d("Read file '" + request.getFileName() + "'");

        final File driveFile = result.getFile();
        if (driveFile == null) {
            request.getListener().onError(
                    new GoogleDriveError("Error while get file '" + request.getFileName() + "'")
            );
            return;
        }

        request.getGoogleApiClient().readFile(driveFile.getId())
                .addOnSuccessListener(
                        pair -> request.getListener().onDownloadComplete(
                                new String(Base64.decode(pair.second, Base64.DEFAULT)),
                                request.getFileName()
                        )
                )
                .addOnFailureListener(
                        e -> {
                            AppLogger.d("File read error:" + e);
                            request.getListener().onError(
                                    new GoogleDriveError("Error while open file '" + request.getFileName() + "'")
                            );
                        }
                );
    }
}
