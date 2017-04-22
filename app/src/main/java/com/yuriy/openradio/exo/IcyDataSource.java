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

package com.yuriy.openradio.exo;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.yuriy.openradio.utils.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * A {@link IcyDataSource} that uses ExoPlayer's {@link DefaultHttpDataSource}.
 */
final class IcyDataSource extends DefaultHttpDataSource {

    /**
     * Listener for the ICY stream events.
     */
    private final IcyInputStreamListener mListener;

    /**
     * Main constructor.
     *
     * @param userAgent            The User-Agent string that should be used.
     * @param connectTimeoutMillis The connection timeout, in milliseconds.
     * @param readTimeoutMillis    The read timeout, in milliseconds.
     * @param listener             Listener for the ICY stream events.
     */
    IcyDataSource(final String userAgent,
                         final int connectTimeoutMillis,
                         final int readTimeoutMillis,
                         final IcyInputStreamListener listener) {
        super(
                userAgent, null, null,
                connectTimeoutMillis, readTimeoutMillis, true, null
        );
        mListener = listener;
        setRequestProperty("Icy-Metadata", "1");
    }

    /**
     * Gets the input stream from the connection.
     * Actually returns the IcyInputStream.
     */
    @Override
    protected InputStream getInputStream(final HttpURLConnection conn) throws IOException {
        final String smetaint = conn.getHeaderField("icy-metaint");
        InputStream ret = conn.getInputStream();
        if (smetaint != null) {
            int period = -1;
            try {
                period = Integer.parseInt(smetaint);
            } catch (final Exception e) {
                AppLogger.e("The icy-metaint '" + smetaint + "' cannot be parsed: '" + e);
            }
            if (period > 0) {
                ret = new IcyInputStream(ret, period, mListener, null);
            }
        } else {
            AppLogger.i("This stream does not provide dynamic meta info");
        }
        return ret;
    }
}
