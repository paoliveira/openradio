package com.yuriy.openradio.utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 1/25/16
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * Helper class to check whether Radio Station's stream URL is provide 200 OK response.
 */

public final class RadioStationChecker implements Runnable {

    /**
     * String tag to use in the logs.
     */
    private static final String CLASS_NAME = RadioStationChecker.class.getSimpleName();
    /**
     * Monitor object to use when perform
     * "add to the collection of the Radio Stations to be remove" operation.
     */
    private static final Object MONITOR = new Object();
    /**
     * Time out for the stream to decide whether there is response or not, ms.
     */
    public static final int TIME_OUT = 2000;
    /**
     * Radio Station that is uses to check.
     */
    private final String mUrl;
    /**
     * Latch object to use to determine completion.
     */
    private final CountDownLatch mCompleteLatch;
    /**
     * Collection of the urls with correct 200 OK response. This is collection
     * of the items to be used.
     */
    private final Set<String> mPassedUrls;

    private static final Set<String> BLACK_LIST = new HashSet<>();

    static {
        BLACK_LIST.add("susehost.com");
    }

    /**
     * Constructor.
     *
     * @param url           Url of the Radio Station to be checked.
     * @param completeLatch Latch object to use to determine completion.
     * @param passedUrls    Collection of the Urls with correct 200 OK response.
     */
    public RadioStationChecker(final String url,
                               final CountDownLatch completeLatch,
                               final Set<String> passedUrls) {
        super();
        mUrl = url;
        mCompleteLatch = completeLatch;
        mPassedUrls = passedUrls;
    }

    @Override
    public void run() {
        AppLogger.d(CLASS_NAME + " Check Stream Url:" + mUrl);

        for (final String url : BLACK_LIST) {
            if (mUrl.contains(url)) {
                AppLogger.w("Skipp black listed url");
                mCompleteLatch.countDown();
                return;
            }
        }

        HttpURLConnection urlConnection = null;
        // Use input stream in order to close stream explicitly, this is the expectation of one
        // of the HttpURLConnection implementation.
        InputStream inputStream = null;
        final double startTime = System.currentTimeMillis();
        try {
            final URL url = new URL(mUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(TIME_OUT);
            urlConnection.setConnectTimeout(TIME_OUT);
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setUseCaches(false);
            urlConnection.setDefaultUseCaches(false);
            urlConnection.connect();
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
            final int responseCode = urlConnection.getResponseCode();
            AppLogger.d(CLASS_NAME + " Stream response:" + responseCode
                    + " within " + (System.currentTimeMillis() - startTime) + " ms Url:" + mUrl);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                synchronized (MONITOR) {
                    mPassedUrls.add(mUrl);
                }
            }
        } catch (final Exception e) {
            AppLogger.e(
                    CLASS_NAME + " Stream Url " + mUrl + " check failed:" + e.getMessage()
            );
            //FabricUtils.logException(e);
        } finally {
            clear(urlConnection, inputStream);
        }
    }

    /**
     * Clear timer and disconnect connection.
     */
    private void clear(final HttpURLConnection urlConnection,
                       final InputStream inputStream) {
        mCompleteLatch.countDown();
        if (urlConnection != null) {
            urlConnection.disconnect();
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (final Exception e) {
                /* Ignore */
            }
        }
    }
}
