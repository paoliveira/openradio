package com.yuriy.openradio.drive;

import android.support.annotation.NonNull;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.MetadataChangeSet;
import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveCreateFolder extends GoogleDriveAPIChain {

    GoogleDriveCreateFolder() {
        this(false);
    }

    GoogleDriveCreateFolder(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(@NonNull final GoogleDriveRequest request,
                                 @NonNull final GoogleDriveResult result) {
        final Thread thread = new Thread(
                () -> {
                    requestSync(request.getGoogleApiClient());

                    final String name = request.getFolderName();

                    if (result.getFolder() != null) {
                        AppLogger.d("Folder " + name + " exists, path execution farther");

                        handleNext(request, result);
                    } else {
                        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(name).build();
                        Drive.DriveApi.getRootFolder(request.getGoogleApiClient()).createFolder(
                                request.getGoogleApiClient(), changeSet).setResultCallback(
                                driveFolderResult -> {
                                    if (driveFolderResult.getStatus().isSuccess()) {
                                        AppLogger.d("Folder " + name + " created, pass execution farther");
                                        result.setFolder(driveFolderResult.getDriveFolder());
                                        handleNext(request, result);
                                    } else {
                                        AppLogger.e("Folder " + name + " is not created");

                                        // TODO:
                                    }
                                }
                        );
                    }
                }
        );
        thread.start();
    }
}
