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

package com.yuriy.openradio.business;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.BitmapHelper;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/2/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class BitmapsOverlay {

    private static final String CLASS_NAME = BitmapsOverlay.class.getSimpleName();

    /**
     * Private constructor.
     */
    private BitmapsOverlay() { }

    /**
     * Factory method to make default instance of the {@link BitmapsOverlay}.
     * @return Default instance of the {@link BitmapsOverlay}.
     */
    public static BitmapsOverlay getInstance() {
        return new BitmapsOverlay();
    }

    /**
     * Overlay the Bitmap with the Android's drawable resource that associated with the provided Id.
     *
     * @param context    Callee context.
     * @param resourceId Id of the drawable resource that will be placed over the provided Bitmap.
     * @param baseBitmap Bitmap that need to be overlay by the drawable resource.
     * @return Bitmap as the result of the overlay.
     */
    public final Bitmap execute(final Context context, final int resourceId,
                                final Bitmap baseBitmap) {
        if (resourceId == 0) {
            AppLogger.w(CLASS_NAME + " Invalid resource Id");
            return baseBitmap;
        }

        final Bitmap overlayBitmap = BitmapFactory.decodeResource(
                context.getResources(),
                resourceId
        );

        return BitmapHelper.overlayWithBitmap(baseBitmap, overlayBitmap);
    }
}
