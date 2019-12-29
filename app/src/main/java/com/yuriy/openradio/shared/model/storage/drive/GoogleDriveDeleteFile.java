/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.shared.model.storage.drive;

import com.yuriy.openradio.shared.utils.AppLogger;

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

            result.getFile()
                    .trash(request.getGoogleApiClient())
                    .setResultCallback(
                            status -> request.getExecutorService().submit(
                                    () -> {
                                        if (status.isSuccess()) {
                                            AppLogger.d("File '" + name + "' deleted, path execution farther");

                                            handleNext(request, result);
                                        } else {
                                            request.getListener().onError(
                                                    new GoogleDriveError(
                                                            "File '" + name + "' is not deleted"
                                                    )
                                            );
                                        }
                                    }
                            )
                    );
        } else {
            AppLogger.d("File '" + name + "' not exists, nothing to delete, path execution farther");

            handleNext(request, result);
        }
    }
}
