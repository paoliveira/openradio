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

package com.yuriy.openradio.drive;

import android.text.TextUtils;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class GoogleDriveQueryDrive extends GoogleDriveAPIChain {

    GoogleDriveQueryDrive() {
        this(false);
    }

    GoogleDriveQueryDrive(final boolean isTerminator) {
        super(isTerminator);
    }

    protected abstract DriveFolder getDriveFolder(final GoogleDriveRequest request, final GoogleDriveResult result);

    protected abstract String getName(final GoogleDriveRequest request);

    protected abstract void setResult(final GoogleDriveResult result, final DriveId driveId);

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        AppLogger.d("Query resource '" + getName(request) + "'");

        request.getListener().onStart();

        final Thread thread = new Thread(
                () -> {
                    requestSync(request.getGoogleApiClient());

                    final PendingResult<DriveApi.MetadataBufferResult> pendingResult = getDriveFolder(request, result)
                            .listChildren(request.getGoogleApiClient());
                    if (pendingResult != null) {
                        pendingResult.setResultCallback(bufferResult -> handleResult(bufferResult, request, result));
                    } else {
                        AppLogger.e("Can not query resource '" + getName(request) + "', pending result is null");

                        request.getListener().onError();
                    }
                }
        );
        thread.start();
    }

    private void handleResult(final DriveApi.MetadataBufferResult bufferResult,
                              final GoogleDriveRequest request,
                              final GoogleDriveResult result) {
        if (bufferResult == null) {
            handleNext(request, result);
            return;
        }

        final MetadataBuffer metadataBuffer = bufferResult.getMetadataBuffer();
        if (metadataBuffer == null) {
            handleNext(request, result);
            return;
        }

        final DriveId driveId = getDriveId(metadataBuffer, getName(request));
        if (driveId == null) {
            AppLogger.d("Resource '" + getName(request) + "' queried, does not exists, pass execution farther");
            handleNext(request, result);
        } else {
            AppLogger.d("Resource '" + getName(request) + "' queried, exists, getting DriveResource reference");

            setResult(result, driveId);

            handleNext(request, result);
        }
    }

    private DriveId getDriveId(final MetadataBuffer metadataBuffer, final String name) {
        AppLogger.d("Check resource '" + name + "'");
        for (final Metadata metadata : metadataBuffer) {
            AppLogger.d(
                    " - metadata, title:" + metadata.getTitle()
                            + ", trashed:" + metadata.isTrashed()
                            + ", trashable:" + metadata.isTrashable()
                            + ", explTrashed:" + metadata.isExplicitlyTrashed()
            );
            if (TextUtils.equals(name, metadata.getTitle())
                    && !metadata.isTrashed()
                    && !metadata.isExplicitlyTrashed()) {
                return metadata.getDriveId();
            }
        }
        return null;
    }
}
