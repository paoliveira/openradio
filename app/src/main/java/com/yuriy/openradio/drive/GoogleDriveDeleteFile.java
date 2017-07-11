package com.yuriy.openradio.drive;

import com.yuriy.openradio.utils.AppLogger;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class GoogleDriveDeleteFile extends GoogleDriveAPIChain {

    GoogleDriveDeleteFile() {
        this(false);
    }

    GoogleDriveDeleteFile(final boolean isTerminator) {
        super(isTerminator);
    }

    @Override
    protected void handleRequest(final GoogleDriveRequest request, final GoogleDriveResult result) {
        final String name = request.getFileName();
        if (result.getFile() != null) {
            AppLogger.d("Delete file '" + name + "'");

            result.getFile().delete(request.getGoogleApiClient()).setResultCallback(
                    status -> {
                        if (status.isSuccess()) {
                            AppLogger.d("File '" + name + "' deleted, path execution farther");

                            handleNext(request, result);
                        } else {
                            // TODO:
                        }
                    }
            );
        } else {
            AppLogger.d("File '" + name + "' not exists, nothing to delete, path execution farther");

            handleNext(request, result);
        }
    }
}
