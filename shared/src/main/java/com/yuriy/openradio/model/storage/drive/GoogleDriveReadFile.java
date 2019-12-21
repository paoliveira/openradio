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

import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.yuriy.openradio.utils.AppLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

        final DriveFile driveFile = result.getFile();
        if (driveFile == null) {
            request.getListener().onError(
                    new GoogleDriveError("Error while get file '" + request.getFileName() + "'")
            );
            return;
        }

        driveFile
                .open(request.getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(
                        driveContentsResult -> request.getExecutorService().submit(
                                () -> {
                                    if (!driveContentsResult.getStatus().isSuccess()) {
                                        request.getListener().onError(
                                                new GoogleDriveError("Error while open file '" + request.getFileName() + "'")
                                        );
                                        return;
                                    }
                                    final DriveContents driveContents = driveContentsResult.getDriveContents();
                                    final BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(driveContents.getInputStream())
                                    );
                                    final StringBuilder builder = new StringBuilder();
                                    String line;
                                    try {
                                        while ((line = reader.readLine()) != null) {
                                            builder.append(line);
                                        }
                                        final String data = new String(Base64.decode(builder.toString(), Base64.DEFAULT));
                                        request.getListener().onDownloadComplete(
                                                data,
                                                request.getFileName()
                                        );
                                    } catch (final IOException e) {
                                        request.getListener().onError(
                                                new GoogleDriveError(
                                                        "Error while download file '" + request.getFileName() + "'"
                                                )
                                        );
                                    } finally {
                                        try {
                                            reader.close();
                                        } catch (IOException e) {
                                        /* Ignore */
                                        }
                                    }

                                    driveContents.discard(request.getGoogleApiClient());
                                }
                        )
                );
    }
}
