package com.yuriy.openradio.drive;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class GoogleDriveRequest {

    private final GoogleApiClient mGoogleApiClient;

    private final String mFolderName;

    private final String mFileName;

    private final String mData;

    public GoogleDriveRequest(final GoogleApiClient googleApiClient, final String folderName, final String fileName,
                              final String data) {
        super();
        mGoogleApiClient = googleApiClient;
        mFolderName = folderName;
        mFileName = fileName;
        mData = data;
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    public String getFolderName() {
        return mFolderName;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getData() {
        return mData;
    }
}
