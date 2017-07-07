package com.yuriy.openradio.drive;

import android.support.annotation.NonNull;

import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveQueryFolder extends GoogleDriveAPIChain {

    private final boolean mIsTerminator;

    public GoogleDriveQueryFolder() {
        this(false);
    }

    public GoogleDriveQueryFolder(final boolean isTerminator) {
        super();
        mIsTerminator = isTerminator;
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request) {
        AppLogger.d("Folder queried, pass execution farther");

        //final DriveId folderId = DriveId.decodeFromString(request.getFolderName());
        final DriveFolder folder = Drive.DriveApi.getAppFolder(request.getGoogleApiClient());
        folder.getMetadata(request.getGoogleApiClient()).setResultCallback(
                new ResultCallbacks<DriveResource.MetadataResult>() {
                    @Override
                    public void onSuccess(@NonNull DriveResource.MetadataResult metadataResult) {
                        AppLogger.d("On Success:" + metadataResult);
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        AppLogger.e("On Error:" + status);
                    }
                }
        );


        handleNext(request);
    }

    @Override
    protected boolean isTerminator() {
        return mIsTerminator;
    }
}
