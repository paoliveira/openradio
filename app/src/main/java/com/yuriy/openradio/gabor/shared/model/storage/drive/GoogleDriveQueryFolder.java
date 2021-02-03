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

package com.yuriy.openradio.gabor.shared.model.storage.drive;

import com.google.android.gms.tasks.Task;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveQueryFolder extends GoogleDriveQueryDrive {

    GoogleDriveQueryFolder() {
        this(false);
    }

    GoogleDriveQueryFolder(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected Task<FileList> getQueryTask(final GoogleDriveRequest request) {
        return request.getGoogleApiClient().queryFolder(request.getFolderName());
    }

    @Override
    protected String getName(final GoogleDriveRequest request) {
        return request.getFolderName();
    }

    @Override
    protected void setResult(final GoogleDriveResult result, final File file) {
        result.setFolderId(file.getId());
    }
}