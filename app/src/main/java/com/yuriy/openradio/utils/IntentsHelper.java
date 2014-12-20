package com.yuriy.openradio.utils;

import android.content.Intent;
import android.net.Uri;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class IntentsHelper {

    /**
     * Private constructor
     */
    private IntentsHelper() { }

    /**
     * Make intent to navigate to provided url.
     *
     * @param url Url to navigate to.
     * @return {@link android.content.Intent}
     */
    public static Intent makeUrlBrowsableIntent(final String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }
}
