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

import android.net.Uri;
import android.support.v4.util.Pair;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.CrashlyticsUtils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
public final class HTTPDownloaderImpl implements Downloader {

    /**
     * Tag to use in logging message.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = HTTPDownloaderImpl.class.getSimpleName();

    /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Represents the end-of-file (or stream).
     */
    public static final int EOF = -1;

    @Override
    public byte[] downloadDataFromUri(final Uri uri) {
        return downloadDataFromUri(uri, new ArrayList<>());
    }

    @Override
    public byte[] downloadDataFromUri(final Uri uri, final List<Pair<String, String>> parameters) {
        AppLogger.i(CLASS_NAME + " Request URL:" + uri);
        byte[] response = new byte[0];

        // TODO : Set everything in more compact way

        URL url = null;
        try {
            url = new URL(uri.toString());
        } catch (final MalformedURLException e) {
            AppLogger.e(CLASS_NAME + " Url exception: " + e.getMessage());
            CrashlyticsUtils.logException(e);
        }

        if (url == null) {
            return response;
        }

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (final IOException e) {
            AppLogger.e(CLASS_NAME + " Http Url connection exception: " + e.getMessage());
            CrashlyticsUtils.logException(e);
        }

        if (urlConnection == null) {
            return response;
        }

        // If there are http request parameters:
        if (!parameters.isEmpty()) {
            boolean result = false;
            // POST method is required for parameters.
            try {
                urlConnection.setRequestMethod("POST");
                result = true;
            } catch (final ProtocolException e) {
                AppLogger.e("Can not set POST method:" + e.getMessage());
                CrashlyticsUtils.logException(e);
            }

            // If POST is supported:
            if (result) {
                OutputStream outputStream = null;
                BufferedWriter writer = null;

                try {
                    outputStream = urlConnection.getOutputStream();
                    writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    writer.write(getPostParametersQuery(parameters));
                    writer.flush();
                } catch (final IOException e) {
                    AppLogger.e(CLASS_NAME + " Can not add http parameters:" + e.getMessage());
                    CrashlyticsUtils.logException(e);
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    } catch (final IOException e) {
                    /* Ignore */
                    }
                }
            }
        }

        int responseCode = 0;
        try {
            responseCode = urlConnection.getResponseCode();
        } catch (final IOException e) {
            AppLogger.e(CLASS_NAME + " Http Url connection get response code exception: " + e.getMessage());
            CrashlyticsUtils.logException(e);
        }

        AppLogger.d("Response code:" + responseCode);
        if (responseCode < 200 || responseCode > 299) {
            urlConnection.disconnect();
            return response;
        }

        try {
            final InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            response = toByteArray(inputStream);
        } catch (final IOException e) {
            AppLogger.e(CLASS_NAME + " Http Url connection getInputStream exception: " + e.getMessage());
            CrashlyticsUtils.logException(e);
        } finally {
            urlConnection.disconnect();
        }

        return response;
    }

    /**
     * Gets the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     *
     * @param input the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    private static byte[] toByteArray(final InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    /**
     * Copies bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    private static int copy(final InputStream input, final OutputStream output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    /**
     * Copies bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.3
     */
    private static long copyLarge(final InputStream input, final OutputStream output)
            throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Copies bytes from an <code>InputStream</code> to an <code>OutputStream</code>
     * using an internal buffer of the given size.
     * <p>
     * This method buffers the input internally, so there is no need to use
     * a <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param bufferSize the bufferSize used to copy from the input to the output
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.5
     */
    public static long copy(final InputStream input, final OutputStream output,
                            final int bufferSize)
            throws IOException {
        return copyLarge(input, output, new byte[bufferSize]);
    }

    /**
     * Copies bytes from a large (over 2GB) <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method uses the provided buffer, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     *
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    public static long copyLarge(final InputStream input, final OutputStream output,
                                 final byte[] buffer)
            throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Creates and returns a query of htpp connection parameters.
     *
     * @param params List of the parameters (keys and values).
     * @return String representation of query.
     * @throws UnsupportedEncodingException
     */
    public static String getPostParametersQuery(final List<Pair<String, String>> params)
            throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final Pair<String, String> pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(pair.first, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.second, "UTF-8"));
        }

        return result.toString();
    }
}
