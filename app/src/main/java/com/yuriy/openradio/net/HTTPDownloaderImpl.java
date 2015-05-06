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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

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
        HttpGet request = null;
        byte[] response = new byte[0];
        try {
            request = new HttpGet(uri.toString());
        } catch (IllegalArgumentException e) {
            Log.e(CLASS_NAME, "IllegalArgumentException error: " + e.getMessage());
        }

        if (request == null) {
            return response;
        }

        final HttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpResponse httpResponse = httpClient.execute(request);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            Log.d(CLASS_NAME, "Response code: " + responseCode);

            if (responseCode == 200) {
                final HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    try {
                        response = EntityUtils.toByteArray(entity);
                        return response;
                    } catch (IOException e) {
                        Log.e(CLASS_NAME, "EntityUtils error: " + e.getMessage());
                    }
                }
            }
        } catch (ClientProtocolException e) {
            Log.e(CLASS_NAME, "ClientProtocolException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(CLASS_NAME, "IOException: " + e.getMessage());
        } catch (SecurityException e) {
            Log.e(CLASS_NAME, "SecurityException error: " + e.getMessage());
        }
        return response;
    }
}
