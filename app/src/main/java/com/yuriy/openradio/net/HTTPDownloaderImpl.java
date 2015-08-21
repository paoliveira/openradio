/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.net.Uri;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link com.yuriy.openradio.net.HTTPDownloaderImpl} allows to download data from the
 * resource over HTTP protocol.
 */
public class HTTPDownloaderImpl implements Downloader {

    /**
     * Tag to use in logging message.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = HTTPDownloaderImpl.class.getSimpleName();

    @Override
    public byte[] downloadDataFromUri(final Uri uri) {
        Log.i(CLASS_NAME, "Request URL:" + uri);
        byte[] response = new byte[0];

        // TODO : Set everything in more compact way

        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (final MalformedURLException e) {
            Log.e(CLASS_NAME, "Url exception: " + e.getMessage());
        }

        if (url == null) {
            return response;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            Log.e(CLASS_NAME, "Http Url connection exception: " + e.getMessage());
        }

        if (urlConnection == null) {
            return response;
        }

        try {
            final InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            response = IOUtils.toByteArray(inputStream);
        } catch (final IOException e) {
            Log.e(CLASS_NAME, "Http Url connection getInputStream exception: " + e.getMessage());
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }
}
