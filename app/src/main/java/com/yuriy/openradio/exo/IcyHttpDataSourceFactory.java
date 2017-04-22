/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.support.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

/**
 * Data source factory designed in a way to provide {@link IcyDataSource} when required.
 */
final class IcyHttpDataSourceFactory implements DataSource.Factory {

    /**
     * Timeout value used for the http connection.
     */
    private final int mTimeOut;
    /**
     * User agent used in {@link IcyDataSource}.
     */
    @NonNull
    private final String mUserAgent;
    /**
     * Listener for the ICY stream events.
     */
    @NonNull
    private final IcyInputStreamListener mIcyInputStreamListener;

    /**
     * Main constructor.
     *
     * @param userAgent              The User-Agent string that should be used.
     * @param icyInputStreamListener Listener for the ICY stream events.
     * @param timeOut                The connection timeout, in milliseconds.
     */
    IcyHttpDataSourceFactory(@NonNull final String userAgent,
                                    @NonNull final IcyInputStreamListener icyInputStreamListener,
                                    final int timeOut) {
        super();
        mUserAgent = userAgent;
        mIcyInputStreamListener = icyInputStreamListener;
        mTimeOut = timeOut;
    }

    @Override
    public DefaultHttpDataSource createDataSource() {
        return new IcyDataSource(
                mUserAgent, mTimeOut, mTimeOut, mIcyInputStreamListener
        );
    }
}