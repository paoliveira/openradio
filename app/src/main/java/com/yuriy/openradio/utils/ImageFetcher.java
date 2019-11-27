/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.yuriy.openradio.utils;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.yuriy.openradio.BuildConfig;
import com.yuriy.openradio.broadcast.ConnectivityReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images fetched from a URL.
 */
public class ImageFetcher extends ImageResizer {
    private static final String TAG = "ImageFetcher";
    private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String HTTP_CACHE_DIR = "http";
    private static final int IO_BUFFER_SIZE = 8 * 1024;

    private DiskLruCache mHttpDiskCache;
    private File mHttpCacheDir;
    private boolean mHttpDiskCacheStarting = true;
    private final Object mHttpDiskCacheLock = new Object();
    private static final int DISK_CACHE_INDEX = 0;
    private Context mContext;

    /**
     * Initialize providing a single target image size (used for both width and height);
     *
     * @param context
     * @param imageSize
     */
    public ImageFetcher(final Context context, final int imageSize) {
        super(context, imageSize);
        init(context);
    }

    private void init(@NonNull final Context context) {
        mContext = context;
        mHttpCacheDir = ImageCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
    }

    @Override
    protected void initDiskCacheInternal() {
        super.initDiskCacheInternal();
        initHttpDiskCache();
    }

    private void initHttpDiskCache() {
        if (!mHttpCacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            mHttpCacheDir.mkdirs();
        }
        synchronized (mHttpDiskCacheLock) {
            if (ImageCache.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE) {
                try {
                    mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1, HTTP_CACHE_SIZE);
                    if (BuildConfig.DEBUG) {
                        AppLogger.d(TAG + " HTTP cache initialized");
                    }
                } catch (IOException e) {
                    mHttpDiskCache = null;
                }
            }
            mHttpDiskCacheStarting = false;
            mHttpDiskCacheLock.notifyAll();
        }
    }

    @Override
    protected void clearCacheInternal() {
        super.clearCacheInternal();
        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null && !mHttpDiskCache.isClosed()) {
                try {
                    mHttpDiskCache.delete();
                    if (BuildConfig.DEBUG) {
                        AppLogger.d(TAG + " HTTP cache cleared");
                    }
                } catch (IOException e) {
                    FabricUtils.logException(e);
                }
                mHttpDiskCache = null;
                mHttpDiskCacheStarting = true;
                initHttpDiskCache();
            }
        }
    }

    @Override
    protected void flushCacheInternal() {
        super.flushCacheInternal();
        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    mHttpDiskCache.flush();
                    if (BuildConfig.DEBUG) {
                        AppLogger.d(TAG + " HTTP cache flushed");
                    }
                } catch (IOException e) {
                    FabricUtils.logException(e);
                }
            }
        }
    }

    @Override
    protected void closeCacheInternal() {
        super.closeCacheInternal();
        synchronized (mHttpDiskCacheLock) {
            if (mHttpDiskCache != null) {
                try {
                    if (!mHttpDiskCache.isClosed()) {
                        mHttpDiskCache.close();
                        mHttpDiskCache = null;
                        if (BuildConfig.DEBUG) {
                            AppLogger.d(TAG + " HTTP cache closed");
                        }
                    }
                } catch (IOException e) {
                    FabricUtils.logException(e);
                }
            }
        }
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background
     * thread.
     *
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(final Context context, final String data) {
        if (BuildConfig.DEBUG) {
            AppLogger.d(TAG + " processBitmap - " + data);
        }

        final String key = ImageCache.hashKeyForDisk(data);
        FileDescriptor fileDescriptor = null;
        FileInputStream fileInputStream = null;
        DiskLruCache.Snapshot snapshot;
        synchronized (mHttpDiskCacheLock) {
            // Wait for disk cache to initialize
            while (mHttpDiskCacheStarting) {
                try {
                    mHttpDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    /* Ignore */
                }
            }

            if (mHttpDiskCache != null) {
                try {
                    snapshot = mHttpDiskCache.get(key);
                    if (snapshot == null) {
                        if (BuildConfig.DEBUG) {
                            AppLogger.d(TAG + " processBitmap, not found in http cache, downloading...");
                        }
                        DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
                        if (editor != null) {
                            if (downloadUrlToStream(context, data, editor.newOutputStream(DISK_CACHE_INDEX))) {
                                editor.commit();
                            } else {
                                editor.abort();
                            }
                        }
                        snapshot = mHttpDiskCache.get(key);
                    }
                    if (snapshot != null) {
                        fileInputStream =
                                (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
                        fileDescriptor = fileInputStream.getFD();
                    }
                } catch (IOException | IllegalStateException e) {
                    FabricUtils.logException(e);
                } finally {
                    if (fileDescriptor == null && fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }

        Bitmap bitmap = null;
        if (fileDescriptor != null) {
            bitmap = decodeSampledBitmapFromDescriptor(
                    fileDescriptor, mImageWidth, mImageHeight, getImageCache()
            );

            // This is somehow workaround for TV. In TV layout for player there is no way to set
            // art drawable parameters, so landscape images are occupy whole player
            if (bitmap != null && mIsTvPlayer) {
                final int maxSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
                bitmap = overlayBitmapToCenter(
                        Bitmap.createBitmap(maxSide, maxSide, bitmap.getConfig()), bitmap
                );
            }
        }

        return bitmap;
    }

    @Override
    protected Bitmap processBitmap(final Object data) {
        return processBitmap(mContext, String.valueOf(data));
    }

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     *
     * @param context      Context of a callee.
     * @param urlString    The URL to fetch.
     * @param outputStream
     * @return true if successful, false otherwise
     */
    public static boolean downloadUrlToStream(final Context context,
                                              final String urlString,
                                              final OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            if (AppUtils.isWebUrl(urlString)) {
                if (ConnectivityReceiver.checkConnectivityAndNotify(context)) {
                    final URL url = new URL(urlString);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(1000);
                    in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
                }
            } else {
                in = new BufferedInputStream(new FileInputStream(new File(urlString)), IO_BUFFER_SIZE);
            }

            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);

            if (in == null) {
                return false;
            }

            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            FabricUtils.logException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                /* Ignore */
            }
        }
        return false;
    }
}
