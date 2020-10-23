/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.yuriy.openradio.shared.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.yuriy.openradio.shared.model.net.UrlBuilder;

import java.io.InputStream;

/**
 * {@link BitmapUtils} is a helper class that provides different methods to operate over Bitmap.
 */
public final class BitmapUtils {

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight()
        );
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static Drawable drawableFromUri(final Context context, final Uri uri, int defaultId) {
        Drawable drawable;
        try (final InputStream inputStream = context.getContentResolver().openInputStream(
                UrlBuilder.preProcessIconUri(uri))
        ) {
            drawable = Drawable.createFromStream(inputStream, uri.toString());
        } catch (final Exception e) {
            drawable = context.getResources().getDrawable(defaultId);
        }
        return drawable;
    }
}
