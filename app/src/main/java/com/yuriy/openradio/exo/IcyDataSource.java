package com.yuriy.openradio.exo;

import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.yuriy.openradio.utils.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * A {@link IcyDataSource} that uses ExoPlayer's {@link DefaultHttpDataSource}.
 */
public final class IcyDataSource extends DefaultHttpDataSource {

    private final IcyInputStreamListener mListener;

    public IcyDataSource(final String userAgent,
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
