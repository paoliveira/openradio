package com.yuriy.openradio.drive;

import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public abstract class GoogleDriveAPIChain {

    private GoogleDriveAPIChain mNext;

    public GoogleDriveAPIChain() {
        super();
    }

    abstract protected void handleRequest(final GoogleDriveRequest request);

    abstract protected boolean isTerminator();

    public void setNext(final GoogleDriveAPIChain value) {
        mNext = value;
    }

    void handleNext(final GoogleDriveRequest request) {
        if (isTerminator()) {
            AppLogger.d("No more requests to handle");
            return;
        }
        mNext.handleRequest(request);
    }
}
