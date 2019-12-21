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

import android.util.Base64;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.MetadataChangeSet;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.FabricUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

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

        // create new contents resource
        Drive.DriveApi.newDriveContents(request.getGoogleApiClient()).setResultCallback(
                driveContentsResult -> request.getExecutorService().submit(
                        () -> {
                            if (!driveContentsResult.getStatus().isSuccess()) {
                                request.getListener().onError(new GoogleDriveError(
                                        "File '" + request.getFileName() + "' is not saved"
                                ));
                                return;
                            }
                            final DriveContents driveContents = driveContentsResult.getDriveContents();
                            handleSaveFile(driveContents, request, result);
                        }
                )
        );
    }

    private void handleSaveFile(final DriveContents driveContents,
                                final GoogleDriveRequest request, final GoogleDriveResult result) {
        final OutputStream outputStream = driveContents.getOutputStream();
        final Writer writer = new OutputStreamWriter(outputStream);
        final String data = Base64.encodeToString(request.getData().getBytes(), Base64.DEFAULT);
        try {
            writer.write(data);
        } catch (final IOException e) {
            FabricUtils.logException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                /* Ignore */
            }
        }

        final String name = request.getFileName();
        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("text/plain")
                .build();

        // create a file on root folder
        result.getFolder()
                .createFile(request.getGoogleApiClient(), changeSet, driveContents)
                .setResultCallback(
                        driveFileResult -> request.getExecutorService().submit(
                                () -> {
                                    if (driveFileResult.getStatus().isSuccess()) {
                                        AppLogger.d("File '" + name + "' saved");
                                        request.getListener().onUploadComplete();
                                    } else {
                                        request.getListener().onError(
                                                new GoogleDriveError("File '" + name + "' is not saved")
                                        );
                                    }
                                }
                        )
                );
    }
}
