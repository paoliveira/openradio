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

package com.yuriy.openradio.model.storage.drive;

import android.support.annotation.NonNull;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.MetadataChangeSet;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveCreateFolder extends GoogleDriveAPIChain {

    GoogleDriveCreateFolder() {
        this(false);
    }

    GoogleDriveCreateFolder(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(@NonNull final GoogleDriveRequest request,
                                 @NonNull final GoogleDriveResult result) {
        requestSync(request.getGoogleApiClient());

        final String name = request.getFolderName();

        if (result.getFolder() != null) {
            AppLogger.d("Folder " + name + " exists, path execution farther");
            handleNext(request, result);
        } else {
            final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(name).build();
            Drive.DriveApi
                    .getRootFolder(request.getGoogleApiClient())
                    .createFolder(request.getGoogleApiClient(), changeSet)
                    .setResultCallback(
                            driveFolderResult -> request.getExecutorService().submit(
                                    () -> {
                                        if (driveFolderResult.getStatus().isSuccess()) {
                                            AppLogger.d("Folder " + name + " created, pass execution farther");
                                            result.setFolder(driveFolderResult.getDriveFolder());
                                            handleNext(request, result);
                                        } else {
                                            request.getListener().onError(
                                                    new GoogleDriveError("Folder " + name + " is not created")
                                            );
                                        }
                                    }
                            )
                    );
        }
    }
}
