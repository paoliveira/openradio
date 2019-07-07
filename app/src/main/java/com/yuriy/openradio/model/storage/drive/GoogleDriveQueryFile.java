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

import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveQueryFile extends GoogleDriveQueryDrive {

    GoogleDriveQueryFile() {
        this(false);
    }

    GoogleDriveQueryFile(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected DriveFolder getDriveFolder(final GoogleDriveRequest request, final GoogleDriveResult result) {
        return result.getFolder();
    }

    @Override
    protected String getName(final GoogleDriveRequest request) {
        return request.getFileName();
    }

    @Override
    protected void setResult(final GoogleDriveResult result, final DriveId driveId) {
        result.setFile(driveId.asDriveFile());
    }
}
