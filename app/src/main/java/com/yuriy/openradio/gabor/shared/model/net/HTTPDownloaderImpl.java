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
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.yuriy.openradio.gabor.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.NetUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link HTTPDownloaderImpl} allows to download data from the
 * resource over HTTP protocol.
 */
public final class HTTPDownloaderImpl implements Downloader {

    /**
     * Tag to use in logging message.
     */
    private static final String CLASS_NAME = HTTPDownloaderImpl.class.getSimpleName();

    /**
     * The default buffer size ({@value}) to use for
     * {@link #copyLarge(InputStream, OutputStream)}
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    /**
     * Represents the end-of-file (or stream).
     */
    private static final int EOF = -1;

    private String[] mUrlsSet;

    private final Random mRandom;

    public HTTPDownloaderImpl() {
        super();
        mRandom = new Random();
    }

    @Override
    public byte[] downloadDataFromUri(final Uri uri) {
        return downloadDataFromUri(uri, new ArrayList<>());
    }

    @Override
    public byte[] downloadDataFromUri(final Uri uri,
                                      @NonNull final List<Pair<String, String>> parameters) {
        byte[] response = new byte[0];

        final URL url = getConnectionUrl(uri, parameters);

        if (url == null) {
            return response;
        }

        AppLogger.i(CLASS_NAME + " Request URL:" + url.toString());

        final HttpURLConnection connection = NetUtils.getHttpURLConnection(
                url,
                parameters.isEmpty() ? "GET" : "POST",
                parameters
        );
        if (connection == null) {
            return response;
        }

        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (final IOException exception) {
            AnalyticsUtils.logException(
                    new DownloaderException(
                            DownloaderException.createExceptionMessage(uri, parameters),
                            exception
                    )
            );
        }

        AppLogger.d("Response code:" + responseCode);
        if (responseCode < 200 || responseCode > 299) {
            NetUtils.closeHttpURLConnection(connection);
            AnalyticsUtils.logException(
                    new DownloaderException(
                            DownloaderException.createExceptionMessage(uri, parameters),
                            new Exception("Response code is " + responseCode)
                    )
            );
            return response;
        }

        try {
            final InputStream inputStream = new BufferedInputStream(connection.getInputStream());
            response = toByteArray(inputStream);
        } catch (final IOException exception) {
            AnalyticsUtils.logException(
                    new DownloaderException(
                            DownloaderException.createExceptionMessage(uri, parameters),
                            exception
                    )
            );
        } finally {
            NetUtils.closeHttpURLConnection(connection);
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
     * @param input  the <code>InputStream</code> to read from
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
     * @param input  the <code>InputStream</code> to read from
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
     * @param input      the <code>InputStream</code> to read from
     * @param output     the <code>OutputStream</code> to write to
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
     * @param input  the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    private static long copyLarge(final InputStream input, final OutputStream output,
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
     * Do DNS look up in order to get available url for service connection.
     * Addresses, if found, are cached and next time is used from the cache.
     *
     * @param uri        Initial (with dummy and predefined prefix) url to perform look up on.
     * @param parameters Parameters associated with request.
     * @return URL object to do connection with.
     */
    @Nullable
    private URL getConnectionUrl(@NonNull final Uri uri,
                                 @NonNull final List<Pair<String, String>> parameters) {
        // If there is no predefined prefix - return original URL.
        String uriStr = uri.toString();
        if (!(uriStr.startsWith(UrlBuilder.BASE_URL_PREFIX))) {
            return getUrl(uriStr, parameters);
        }

        // Return cached URL if available.
        synchronized (mRandom) {
            if (mUrlsSet != null && mUrlsSet.length != 0) {
                final int i = mRandom.nextInt(mUrlsSet.length);
                return getUrlModified(uriStr, mUrlsSet[i], parameters);
            }
        }

        // Perform look up and cache results.
        try {
            final InetAddress[] list = InetAddress.getAllByName(UrlBuilder.LOOK_UP_DNS);
            synchronized (mRandom) {
                mUrlsSet = new String[list.length];
                int i = 0;
                for (final InetAddress item : list) {
                    mUrlsSet[i++] = "https://" + item.getCanonicalHostName();
                    AppLogger.i(CLASS_NAME + " look up host:" + mUrlsSet[i - 1]);
                }
            }
        } catch (final UnknownHostException exception) {
            AnalyticsUtils.logException(
                    new DownloaderException(DownloaderException.createExceptionMessage(uri, parameters), exception)
            );
        }

        // Do random selection from available addresses.
        URL url = null;
        synchronized (mRandom) {
            if (mUrlsSet != null && mUrlsSet.length != 0) {
                final int i = mRandom.nextInt(mUrlsSet.length);
                url = getUrlModified(uriStr, mUrlsSet[i], parameters);
            }
        }

        // Uri to URL parse might fail.
        if (url != null) {
            return url;
        }

        // Use predefined addresses, these are needs to be verified time after time in order to be up to date.
        final int i = mRandom.nextInt(UrlBuilder.RESERVED_URLS.length);
        return getUrlModified(uriStr, UrlBuilder.RESERVED_URLS[i], parameters);
    }

    @Nullable
    private URL getUrlModified(final String uriOrigin,
                               final String uri, @NonNull final List<Pair<String, String>> parameters) {
        final String uriModified = uriOrigin.replaceFirst(UrlBuilder.BASE_URL_PREFIX, uri);
        return getUrl(uriModified, parameters);
    }

    @Nullable
    private URL getUrl(final String uri, @NonNull final List<Pair<String, String>> parameters) {
        URL url;
        try {
            url = new URL(uri);
        } catch (final MalformedURLException exception) {
            AnalyticsUtils.logException(
                    new DownloaderException(DownloaderException.createExceptionMessage(uri, parameters), exception)
            );
            url = null;
        }
        return url;
    }
}
