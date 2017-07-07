package com.yuriy.openradio.drive;

import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class GoogleDriveCreateFolder extends GoogleDriveAPIChain {

    private final boolean mIsTerminator;

    public GoogleDriveCreateFolder() {
        this(false);
    }

    public GoogleDriveCreateFolder(final boolean isTerminator) {
        super();
        mIsTerminator = isTerminator;
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request) {
        AppLogger.d("Folder created, pass execution farther");

        handleNext(request);
    }

    @Override
    protected boolean isTerminator() {
        return mIsTerminator;
    }
}
