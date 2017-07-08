package com.yuriy.openradio.drive;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.yuriy.openradio.utils.AppLogger;

import java.util.concurrent.TimeUnit;

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

        final Thread thread = new Thread(
                () -> {
                    Drive.DriveApi.requestSync(request.getGoogleApiClient()).await(3, TimeUnit.SECONDS);

                    final PendingResult<DriveApi.MetadataBufferResult> result = Drive.DriveApi
                            .getRootFolder(request.getGoogleApiClient())
                            .listChildren(request.getGoogleApiClient());
                    if (result != null) {
                        result.setResultCallback(result1 -> handleResult(result1, request));
                    }
                }
        );
        thread.start();
    }

    @Override
    protected boolean isTerminator() {
        return mIsTerminator;
    }

    private void handleResult(final DriveApi.MetadataBufferResult result, final GoogleDriveRequest request) {
        AppLogger.d("OnResult:" + result);
        if (result == null) {
            handleNext(request);
            return;
        }

        final MetadataBuffer metadataBuffer = result.getMetadataBuffer();
        if (metadataBuffer == null) {
            handleNext(request);
            return;
        }

        for (final Metadata metadata : metadataBuffer) {
            AppLogger.d(
                    " - metadata, title:" + metadata.getTitle()
                            + ", trashed:" + metadata.isTrashed()
                            + ", trashable:" + metadata.isTrashable()
                            + ", explTrashed:" + metadata.isExplicitlyTrashed()
            );
        }

        AppLogger.d("Folder queried, pass execution farther");
        handleNext(request);
    }
}
