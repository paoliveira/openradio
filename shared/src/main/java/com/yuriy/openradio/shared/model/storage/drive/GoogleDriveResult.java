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

import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class GoogleDriveResult {

    private DriveFolder mFolder;

    private DriveFile mFile;

    public GoogleDriveResult() {
        super();
    }

    public DriveFile getFile() {
        return mFile;
    }

    public void setFile(final DriveFile value) {
        mFile = value;
    }

    public DriveFolder getFolder() {
        return mFolder;
    }

    public void setFolder(final DriveFolder value) {
        mFolder = value;
    }
}
