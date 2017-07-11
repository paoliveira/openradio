package com.yuriy.openradio.drive;

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
