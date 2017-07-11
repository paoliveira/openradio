package com.yuriy.openradio.drive;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.yuriy.openradio.utils.AppLogger;

import java.util.concurrent.TimeUnit;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class GoogleDriveAPIChain {

    private GoogleDriveAPIChain mNext;

    private final boolean mIsTerminator;

    GoogleDriveAPIChain(final boolean isTerminator) {
        super();
        mIsTerminator = isTerminator;
    }

    abstract protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result);

    protected void requestSync(final GoogleApiClient googleApiClient) {
        Drive.DriveApi.requestSync(googleApiClient).await(3, TimeUnit.SECONDS);
    }

    public void setNext(final GoogleDriveAPIChain value) {
        mNext = value;
    }

    void handleNext(final GoogleDriveRequest request, final GoogleDriveResult result) {
        if (mIsTerminator) {
            AppLogger.d("No more requests to handle");
            return;
        }
        mNext.handleRequest(request, result);
    }
}
