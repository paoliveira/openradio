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

package com.yuriy.openradio.gabor.shared.model.net;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import java.util.List;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 03/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */

public final class DownloaderException extends Exception {

    public DownloaderException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @param uri
     * @param parameters
     * @return
     */
    public static String createExceptionMessage(@NonNull final Uri uri,
                                                @NonNull final List<Pair<String, String>> parameters) {
        return createExceptionMessage(uri.toString(), parameters);
    }

    /**
     * @param uriStr
     * @param parameters
     * @return
     */
    public static String createExceptionMessage(@NonNull final String uriStr,
                                                @NonNull final List<Pair<String, String>> parameters) {
        final StringBuilder builder = new StringBuilder(uriStr);
        for (final Pair<String, String> pair : parameters) {
            builder.append(" ");
            builder.append(pair.toString());
        }
        return builder.toString();
    }
}
