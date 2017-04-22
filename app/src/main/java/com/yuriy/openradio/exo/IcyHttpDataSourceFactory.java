package com.yuriy.openradio.exo;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

/**
 *
 */
public final class IcyHttpDataSourceFactory implements DataSource.Factory {

    private final int mTimeOut;
    private final String mUserAgent;
    private IcyInputStreamListener mIcyInputStreamListener;

    public IcyHttpDataSourceFactory(final String userAgent,
                                    final IcyInputStreamListener icyInputStreamListener,
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