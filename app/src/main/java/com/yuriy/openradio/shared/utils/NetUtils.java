/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.yuriy.openradio.shared.model.net.DownloaderException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import okhttp3.internal.Util;

public final class NetUtils {

    private static final String USER_AGENT_PARAMETER_KEY = "User-Agent";
    private static final String USER_AGENT_PARAMETER_VALUE = "OpenRadioApp";

    private NetUtils() {
        super();
    }

    @Nullable
    public static HttpURLConnection getHttpURLConnection(final String urlString, final String requestMethod) {
        try {
            return getHttpURLConnection(new URL(urlString), requestMethod, null);
        } catch (final MalformedURLException exception) {
            AnalyticsUtils.logException(
                    new RuntimeException("Can not get http connection from " + urlString, exception)
            );
            return null;
        }
    }

    public static HttpURLConnection getHttpURLConnection(final URL url,
                                                         final String requestMethod,
                                                         @Nullable final List<Pair<String, String>> parameters) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(AppUtils.TIME_OUT);
            connection.setConnectTimeout(AppUtils.TIME_OUT);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty(USER_AGENT_PARAMETER_KEY, USER_AGENT_PARAMETER_VALUE);

            // If there are http request parameters:
            if (parameters != null && !parameters.isEmpty()) {
                connection.setRequestProperty("enctype", "application/x-www-form-urlencoded");
                try (final OutputStream outputStream = connection.getOutputStream();
                     final BufferedWriter writer
                             = new BufferedWriter(new OutputStreamWriter(outputStream, Util.UTF_8))) {
                    writer.write(getPostParametersQuery(parameters));
                    writer.flush();
                } catch (final IOException exception) {
                    AnalyticsUtils.logException(
                            new DownloaderException(
                                    DownloaderException.createExceptionMessage(url.toString(), parameters), exception
                            )
                    );
                }
            }

            connection.connect();
        } catch (final IOException exception) {
            AnalyticsUtils.logException(
                    new RuntimeException("Can not get http connection from " + url, exception)
            );
        }
        return connection;
    }

    public static void closeHttpURLConnection(final HttpURLConnection connection) {
        if (connection == null) {
            return;
        }
        connection.disconnect();
    }

    public static boolean checkResource(final String url) {
        final HttpURLConnection connection = getHttpURLConnection(url, "GET");
        if (connection == null) {
            return false;
        }
        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (final IOException exception) {
            closeHttpURLConnection(connection);
            return false;
        }
        if (responseCode < 200 || responseCode > 299) {
            closeHttpURLConnection(connection);
            return false;
        }
        closeHttpURLConnection(connection);
        return true;
    }

    /**
     * Creates and returns a query of htpp connection parameters.
     *
     * @param params List of the parameters (keys and values).
     * @return String representation of query.
     * @throws UnsupportedEncodingException
     */
    public static String getPostParametersQuery(@NonNull final List<Pair<String, String>> params)
            throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final Pair<String, String> pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(pair.first, AppUtils.UTF8));
            result.append("=");
            result.append(URLEncoder.encode(pair.second, AppUtils.UTF8));
        }
        AppLogger.i("POST query:" + result.toString());
        return result.toString();
    }
}
