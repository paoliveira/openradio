package com.yuriy.openradio.business;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/6/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.graphics.Bitmap;

/**
 * {@link FlagLoaderListener} is an interface designed to provide callbacks of the Load Flag
 * functionality.
 */
public interface FlagLoaderListener {

    /**
     * Dispatching when Flag's bitmap is ready.
     *
     * @param bitmap Bitmap of the Flag.
     */
    void onComplete(final Bitmap bitmap);

    /**
     * Dispatching when there is an error while loading Flag's bitmap.
     *
     * @param message Error message.
     */
    void onError(final String message);
}
