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

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.yuriy.openradio.shared.utils.AppLogger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    protected abstract Task<FileList> getQueryTask(final GoogleDriveRequest request);

    protected abstract String getName(final GoogleDriveRequest request);

    protected abstract void setResult(final GoogleDriveResult result, final File driveId);

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        AppLogger.d("Query resource '" + getName(request) + "'");

        request.getListener().onStart();

        final Task<FileList> task = getQueryTask(request);
        final CountDownLatch latch = new CountDownLatch(1);
        final FileList[] queryResult = {null};
        task.addOnSuccessListener(
                fileList -> {
                    queryResult[0] = fileList.clone();
                    latch.countDown();
                }
        );
        task.addOnFailureListener(
                e -> {
                    AppLogger.e("Can not query google drive folder:" + e);
                    // TODO: Handle error
                    latch.countDown();
                }
        );
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            AppLogger.e("Can not query google drive folder, await exception:" + e);
        }
        if (queryResult[0] == null) {
            request.getListener().onError(
                    new GoogleDriveError(
                            "Can not query resource '" + getName(request) + "', drive folder is null"
                    )
            );
            return;
        }
        handleResult(queryResult[0], request, result);
    }

    private void handleResult(final FileList fileList,
                              final GoogleDriveRequest request,
                              final GoogleDriveResult result) {
        if (fileList == null) {
            handleNext(request, result);
            return;
        }

        final List<File> list = fileList.getFiles();
        if (list == null) {
            handleNext(request, result);
            return;
        }

        final File driveId = getDriveId(list, getName(request));
        if (driveId == null) {
            AppLogger.d("Resource '" + getName(request) + "' queried, does not exists, pass execution farther");
        } else {
            AppLogger.d("Resource '" + getName(request) + "' queried, exists, getting DriveResource reference");
            setResult(result, driveId);
        }
        handleNext(request, result);
    }

    @Nullable
    private File getDriveId(final List<File> list, final String name) {
        AppLogger.d("Check resource '" + name + "', list of " + list.size());
        File fileReturn = null;
        for (final File file : list) {
            AppLogger.d(" - file:" + file);
            // All other fields are null, except name type and id.
            // Get the first record.
            if (TextUtils.equals(name, file.getName())) {
                fileReturn = file.clone();
                break;
            }
        }
        return fileReturn;
    }
}
