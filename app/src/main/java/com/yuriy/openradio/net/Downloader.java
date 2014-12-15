package com.yuriy.openradio.net;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.net.Uri;

/**
 * {@link com.yuriy.openradio.net.Downloader} is an interface provides method which allows to
 * perform download operations. Different implementations will allows to perform downloading via
 * different protocols: HTTP, FTP, etc ...
 */
public interface Downloader {

    /**
     * Method to download data from provided {@link android.net.Uri}.
     *
     * @param uri Provided {@link android.net.Uri}.
     * @return Downloaded data/
     */
    public String downloadDataFromUri(final Uri uri);
}
