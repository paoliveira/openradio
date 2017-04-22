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

package com.yuriy.openradio.net;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.net.Uri;
import android.support.v4.util.Pair;

import java.util.List;

/**
 * {@link com.yuriy.openradio.net.Downloader} is an interface provides method which allows to
 * perform download operations. Different implementations will allows to perform downloading via
 * different protocols: HTTP, FTP, etc ...
 */
public interface Downloader {

    /**
     * Method to download data from provided {@link android.net.Uri}.
     *
     * @param uri Provided {@link android.net.Uri}.
     * @return Downloaded data.
     */
    byte[] downloadDataFromUri(final Uri uri);

    /**
     * Method to download data from provided {@link android.net.Uri}.
     *
     * @param uri        Provided {@link android.net.Uri}.
     * @param parameters List of parameters to attach to connection.
     * @return Downloaded data.
     */
    byte[] downloadDataFromUri(final Uri uri, List<Pair<String, String>> parameters);
}
