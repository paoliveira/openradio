package com.yuriy.openradio.utils;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 1/25/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Helper class to check whether Radio Station's stream URL is provide 200 OK response.
 */
public final class RadioStationChecker extends Thread {

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
     * Time out for the stream to decide whether there is response or not.
     */
    private static final int CHECK_TIME = 2000;
    /**
     * Waiting max time for the thread for the initialization.
     */
    private static final int INIT_WAIT_TIME = 2000;
    /**
     * Radio Station that is uses to check.
     */
    private final String mUrl;
    /**
     * Timer to keep track on the check time. When specified time runs out, time is used
     * to terminate check procedure.
     */
    private final Timer mTimer = new Timer();
    /**
     * Latch object to use to determine completion.
     */
    private final CountDownLatch mCompleteLatch;
    /**
     * Latch object to use to determine init.
     */
    private final CountDownLatch mInitLatch;
    /**
     * Collection of the urls with correct 200 OK response. This is collection
     * of the items to be used.
     */
    private final Set<String> mPassedUrls;

    /**
     * Constructor.
     *
     * @param url           Url of the Radio Station to be checked.
     * @param initLatch     Latch object to use to determine init.
     * @param completeLatch Latch object to use to determine completion.
     * @param passedUrls    Collection of the Urls with correct 200 OK response.
     */
    public RadioStationChecker(final String url,
                               final CountDownLatch initLatch,
                               final CountDownLatch completeLatch,
                               final Set<String> passedUrls) {
        super();
        mUrl = url;
        mInitLatch = initLatch;
        mCompleteLatch = completeLatch;
        mPassedUrls = passedUrls;

        setName(CLASS_NAME + "-Thread");
    }

    @Override
    public void run() {
        super.run();
        AppLogger.d(CLASS_NAME + " Check Stream Url:" + mUrl);
        try {
            mInitLatch.await(INIT_WAIT_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
                /* Ignore */
        }

        HttpURLConnection urlConnection = null;
        try {
            final double startTime = System.currentTimeMillis();
            final URL url = new URL(mUrl);
            urlConnection = (HttpURLConnection) url.openConnection();

            mTimer.schedule(new TimerTaskListener(this, urlConnection), CHECK_TIME);

            urlConnection.connect();
            final int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                AppLogger.d(CLASS_NAME + " Stream Url OK:" + urlConnection.getResponseMessage()
                        + " within " + (System.currentTimeMillis() - startTime) + " ms");
                synchronized (MONITOR) {
                    mPassedUrls.add(mUrl);
                }
            }
        } catch (final Exception e) {
            AppLogger.e(CLASS_NAME + " Stream Url check failed:" + e.getMessage());
            CrashlyticsUtils.logException(e);
        } finally {
            clear(urlConnection);
        }
    }

    /**
     * Clear timer and disconnect connection.
     */
    private synchronized void clear(final HttpURLConnection urlConnection) {
        if (urlConnection == null) {
            return;
        }
        mTimer.cancel();
        mTimer.purge();
        urlConnection.disconnect();
        mCompleteLatch.countDown();
    }

    /**
     * Helper class to keep track on the check time.
     * When the time runs out - terminate check procedure.
     */
    private static final class TimerTaskListener extends TimerTask {

        /**
         * Weak reference to the checker class.
         */
        private final WeakReference<RadioStationChecker> mReference;

        private HttpURLConnection mUrlConnection;

        /**
         * Constructor.
         *
         * @param reference The reference to the checker class.
         */
        private TimerTaskListener(final RadioStationChecker reference,
                                  final HttpURLConnection urlConnection) {
            super();
            mReference = new WeakReference<>(reference);
            mUrlConnection = urlConnection;
        }

        @Override
        public void run() {
            AppLogger.e(CLASS_NAME + " Stream Url check failed by timeout");
            final RadioStationChecker reference = mReference.get();
            if (reference == null) {
                return;
            }
            reference.clear(mUrlConnection);
        }
    }
}
