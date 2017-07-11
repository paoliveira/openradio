package com.yuriy.openradio.drive;

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
