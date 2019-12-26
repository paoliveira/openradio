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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.yuriy.openradio.shared.R;
import com.yuriy.openradio.shared.model.net.UrlBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * {@link BitmapUtils} is a helper class that provides different methods to operate over Bitmap.
 */
public final class BitmapUtils {

    // Bitmap size for album art in media notifications when there are more than 3 playback actions
    public static final int MEDIA_ART_SMALL_WIDTH = 64;
    public static final int MEDIA_ART_SMALL_HEIGHT = 64;

    // Bitmap size for album art in media notifications when there are no more than 3 playback actions
    public static final int MEDIA_ART_BIG_WIDTH = 128;
    public static final int MEDIA_ART_BIG_HEIGHT = 128;

    private static final String IMAGE_URL_EXP = "([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp))$)";

    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(IMAGE_URL_EXP);

    /**
     * Scale Bitmap.
     *
     * @param scaleFactor Scale factor.
     * @param data        Bytes which represents a Bitmap.
     * @return Scaled Bitmap.
     */
    public static Bitmap scaleBitmap(final int scaleFactor, final byte[] data) {
        // Get the dimensions of the bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // Decode the image file into a Bitmap sized to fill the View
        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Method to help find scale factor for the Bitmap vased on the desired Width and Height.
     *
     * @param targetW desired width.
     * @param targetH Desired height.
     * @param bytes   Bytes which represents a Bitmap.
     * @return Scale factor.
     */
    private static int findScaleFactor(final int targetW, final int targetH, final byte[] bytes) {
        // Get the dimensions of the bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        int actualW = options.outWidth;
        int actualH = options.outHeight;

        // Determine how much to scale down the image
        return Math.min(actualW / targetW, actualH / targetH);
    }

    /**
     * Download and scale Bitmap.
     *
     * @param uri    Uri to fetch from.
     * @param width  Width of the Bitmap.
     * @param height Height of the Bitmap.
     * @return Downloaded and scaled Bitmap.
     * @throws IOException
     */
    public static Bitmap fetchAndRescaleBitmap(String uri, final int width, final int height)
            throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream;
        int scaleFactor;

        if (AppUtils.isWebUrl(uri)) {
            connection = (HttpURLConnection) new URL(uri).openConnection();
            doConnection(connection);

            boolean redirect = false;
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    redirect = true;
                    connection.disconnect();
                }
            }
            if (redirect) {
                // get redirect url from "Location" header field
                uri = connection.getHeaderField("Location");
                connection = (HttpURLConnection) new URL(uri).openConnection();
                doConnection(connection);
            }
            inputStream = connection.getInputStream();
        } else {
            inputStream = new FileInputStream(new File(uri));
        }

        // Good old way to get data and use it whatever way I need to.
        int n;
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while ((n = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, n);
        }

        scaleFactor = findScaleFactor(width, height, outputStream.toByteArray());
        Bitmap bitmap;
        try {
            bitmap = scaleBitmap(scaleFactor, outputStream.toByteArray());
        } finally {
            inputStream.close();
            outputStream.close();
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (bitmap != null) {
            AppLogger.d("FetchedAndRescaled bmp:" + bitmap.getWidth() + "x" + bitmap.getHeight());
        } else {
            final String message = "FetchedAndRescaled bmp for url '" + uri + "' is null";
            final Exception exception = new Exception(message);
            FabricUtils.logException(exception);
        }

        return bitmap;
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        float ratio = Math.min(
                maxImageSize / realImage.getWidth(),
                maxImageSize / realImage.getHeight()
        );
        int width = Math.round(ratio * realImage.getWidth());
        int height = Math.round(ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width, height, filter);
    }

    private static void doConnection(final HttpURLConnection connection) throws IOException {
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0...");
        connection.connect();
    }

    /**
     * Overlay two Bitmaps.
     *
     * @param bitmap_A Bitmap to overlay on.
     * @param bitmap_B Bitmap overlay.
     * @return Bitmap.
     */
    private static Bitmap overlay(final Bitmap bitmap_A, final Bitmap bitmap_B) {
        final Bitmap bitmap = Bitmap.createBitmap(
                bitmap_A.getWidth(), bitmap_A.getHeight(), Bitmap.Config.ARGB_8888
        );
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap_A, new Matrix(), null);
        canvas.drawBitmap(bitmap_B, 0, 0, null);
        return bitmap;
    }

    /**
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(final BitmapFactory.Options options,
                                            final int reqWidth, final int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Overlay one Bitmap over other Bitmap.
     *
     * @param baseBitmap Base Bitmap.
     * @param topBitmap  Bitmap that is use to overlay.
     * @return Overlay Bitmap.
     */
    static Bitmap overlayWithBitmap(final Bitmap baseBitmap, final Bitmap topBitmap) {
        final double scaleToUse = 60.0;
        final double scaledW = baseBitmap.getWidth() * scaleToUse / 100;
        final double scaledH = topBitmap.getHeight() * (Math.abs(scaledW / topBitmap.getWidth()));
        final Bitmap scaledFlag = Bitmap.createScaledBitmap(
                topBitmap,
                (int) scaledW,
                (int) scaledH,
                false
        );

        return overlay(baseBitmap, scaledFlag);
    }

    /**
     * @param url
     * @return
     */
    public static boolean isUrlLocalResource(final String url) {
        return !TextUtils.isEmpty(url) && url.startsWith("android.resource");
    }

    /**
     * Validate path to image with regular expression.
     *
     * @param image Path to validate.
     * @return true if path is valid, false otherwise
     */
    public static boolean isImageUrl(final String image) {
        return IMAGE_URL_PATTERN.matcher(image).matches();

    }

    /**
     * @param context
     * @param uri
     * @return
     */
    public static Drawable drawableFromUri(final Context context, final Uri uri) {
        Drawable drawable;
        try (final InputStream inputStream = context.getContentResolver().openInputStream(
                UrlBuilder.preProcessIconUri(uri))
        ) {
            drawable = Drawable.createFromStream(inputStream, uri.toString());
        } catch (final Exception e) {
            drawable = context.getResources().getDrawable(R.drawable.ic_favorites_off);
        }
        return drawable;
    }
}
