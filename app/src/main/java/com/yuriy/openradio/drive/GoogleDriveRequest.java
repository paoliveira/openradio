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

package com.yuriy.openradio.drive;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/07/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class GoogleDriveRequest {

    public interface Listener {

        void onStart();

        void onComplete();

        void onError();
    }

    private final GoogleApiClient mGoogleApiClient;

    private final String mFolderName;

    private final String mFileName;

    private final String mData;

    private final Listener mListener;

    public GoogleDriveRequest(final GoogleApiClient googleApiClient, final String folderName, final String fileName,
                              final String data, final Listener listener) {
        super();
        mGoogleApiClient = googleApiClient;
        mFolderName = folderName;
        mFileName = fileName;
        mData = data;
        mListener = listener;
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

    public Listener getListener() {
        return mListener;
    }
}
