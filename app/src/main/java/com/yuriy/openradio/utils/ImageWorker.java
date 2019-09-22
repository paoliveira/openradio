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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.yuriy.openradio.BuildConfig;

import java.lang.ref.WeakReference;

/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;

    private ImageCache mImageCache;
    private ImageCache.ImageCacheParams mImageCacheParams;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = true;
    private boolean mExitTasksEarly = false;
    private boolean mPauseWork = false;
    private final Object mPauseWorkLock = new Object();

    Resources mResources;

    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;

    ImageWorker(Context context) {
        mResources = context.getResources();
    }

    /**
     * Load an image specified by the data parameter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing logic). A memory and
     * disk cache will be used if an {@link ImageCache} has been added using
     * {@link ImageWorker#addImageCache(FragmentManager, ImageCache.ImageCacheParams)}. If the
     * image is found in the memory cache, it is set immediately, otherwise an {@link AsyncTask}
     * will be created to asynchronously load the bitmap.
     *
     * @param data      The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void loadImage(Object data, ImageView imageView) {
        if (data == null) {
            return;
        }

        BitmapDrawable value = null;

        if (mImageCache != null) {
            value = mImageCache.getBitmapFromMemCache(String.valueOf(data));
        }

        if (value != null) {
            // Bitmap found in memory cache
            imageView.setImageDrawable(value);
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(data, imageView, this);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mResources, mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);

            if (ConcurrentUtils.isThreadPoolFull()) {
                return;
            }
            try {
                task.executeOnExecutor(ConcurrentUtils.THREAD_POOL_EXECUTOR);
            } catch (final Exception e) {
                /* Ignore */
            }
        }
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    /**
     * Adds an {@link ImageCache} to this {@link ImageWorker} to handle disk and memory bitmap
     * caching.
     *
     * @param fragmentManager
     * @param cacheParams     The cache parameters to use for the image cache.
     */
    public void addImageCache(FragmentManager fragmentManager, ImageCache.ImageCacheParams cacheParams) {
        mImageCacheParams = cacheParams;
        mImageCache = ImageCache.getInstance(fragmentManager, mImageCacheParams);
        if (ConcurrentUtils.isThreadPoolFull()) {
            return;
        }
        new CacheAsyncTask(this).executeOnExecutor(
                ConcurrentUtils.THREAD_POOL_EXECUTOR,
                MESSAGE_INIT_DISK_CACHE
        );
    }

    /**
     * Adds an {@link ImageCache} to this {@link ImageWorker} to handle disk and memory bitmap
     * caching.
     *
     * @param activity
     * @param diskCacheDirectoryName See
     *                               {@link ImageCache.ImageCacheParams#ImageCacheParams(android.content.Context, String)}.
     */
    public void addImageCache(FragmentActivity activity, String diskCacheDirectoryName) {
        mImageCacheParams = new ImageCache.ImageCacheParams(activity, diskCacheDirectoryName);
        mImageCache = ImageCache.getInstance(activity.getSupportFragmentManager(), mImageCacheParams);
        if (ConcurrentUtils.isThreadPoolFull()) {
            return;
        }
        new CacheAsyncTask(this).execute(MESSAGE_INIT_DISK_CACHE);
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the background thread.
     */
    public void setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
    }

    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
        setPauseWork(false);
    }

    /**
     * Subclasses should override this to define any processing or work that must happen to produce
     * the final bitmap. This will be executed in a background thread and be long running. For
     * example, you could resize a large bitmap here, or pull down an image from the network.
     *
     * @param data The data to identify which image to process, as provided by
     *             {@link ImageWorker#loadImage(Object, android.widget.ImageView)}
     * @return The processed bitmap
     */
    protected abstract Bitmap processBitmap(Object data);

    /**
     * @return The {@link ImageCache} object currently being used by this ImageWorker.
     */
    ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * Cancels any pending work attached to the provided ImageView.
     *
     * @param imageView
     */
    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
            if (BuildConfig.DEBUG) {
                final Object bitmapData = bitmapWorkerTask.mData;
                AppLogger.d(TAG + " cancelWork - cancelled work for " + bitmapData);
            }
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no work in
     * progress on this image view.
     * Returns false if the work in progress deals with the same data. The work is not
     * stopped in that case.
     */
    private static boolean cancelPotentialWork(Object data, ImageView imageView) {
        //BEGIN_INCLUDE(cancel_potential_work)
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.mData;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                if (BuildConfig.DEBUG) {
                    AppLogger.d(TAG + " cancelPotentialWork - cancelled work for " + data);
                }
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
        //END_INCLUDE(cancel_potential_work)
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with this imageView.
     * null if there is no such task.
     */
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    private static final class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable> {

        private final Object mData;
        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<ImageWorker> mReference;

        private BitmapWorkerTask(final Object data,
                                 final ImageView imageView,
                                 final ImageWorker reference) {
            super();
            mData = data;
            imageViewReference = new WeakReference<>(imageView);
            mReference = new WeakReference<>(reference);
        }

        /**
         * Background processing.
         */
        @Override
        protected BitmapDrawable doInBackground(final Void... params) {
            //BEGIN_INCLUDE(load_bitmap_in_background)
            if (BuildConfig.DEBUG) {
                AppLogger.d(TAG + " doInBackground - starting work");
            }

            final String dataString = String.valueOf(mData);
            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            final ImageWorker reference = mReference.get();
            if (reference == null) {
                AppLogger.w(TAG + " doInBackground - reference is null");
                return drawable;
            }

            // Wait here if work is paused and the task is not cancelled
            synchronized (reference.mPauseWorkLock) {
                while (reference.mPauseWork && !isCancelled()) {
                    try {
                        reference.mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }

            // If the image cache is available and this task has not been cancelled by another
            // thread and the ImageView that was originally bound to this task is still bound back
            // to this task and our "exit early" flag is not set then try and fetch the bitmap from
            // the cache
            if (reference.mImageCache != null && !isCancelled() && getAttachedImageView() != null
                    && !reference.mExitTasksEarly) {
                bitmap = reference.mImageCache.getBitmapFromDiskCache(dataString);
            }

            // If the bitmap was not found in the cache and this task has not been cancelled by
            // another thread and the ImageView that was originally bound to this task is still
            // bound back to this task and our "exit early" flag is not set, then call the main
            // process method (as implemented by a subclass)
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null
                    && !reference.mExitTasksEarly) {
                bitmap = reference.processBitmap(mData);
            }

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the cache for future use. Note we don't check if the task was cancelled
            // here, if it was, and the thread is still running, we may as well add the processed
            // bitmap to our cache as it might be used again in the future
            if (bitmap != null) {
                drawable = new BitmapDrawable(reference.mResources, bitmap);

                if (reference.mImageCache != null) {
                    reference.mImageCache.addBitmapToCache(dataString, drawable);
                }
            }

            if (BuildConfig.DEBUG) {
                AppLogger.d(TAG + " doInBackground - finished work");
            }

            return drawable;
            //END_INCLUDE(load_bitmap_in_background)
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(BitmapDrawable value) {
            //BEGIN_INCLUDE(complete_background_work)
            final ImageWorker reference = mReference.get();
            if (reference == null) {
                AppLogger.w(TAG + " onPostExecute - reference is null");
                return;
            }
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled() || reference.mExitTasksEarly) {
                value = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (value != null && imageView != null) {
                if (BuildConfig.DEBUG) {
                    AppLogger.d(TAG + " onPostExecute - setting bitmap");
                }
                reference.setImageDrawable(imageView, value);
            }
            //END_INCLUDE(complete_background_work)
        }

        @Override
        protected void onCancelled(final BitmapDrawable value) {
            super.onCancelled(value);
            final ImageWorker reference = mReference.get();
            if (reference == null) {
                AppLogger.w(TAG + " onPostExecute - reference is null");
                return;
            }
            synchronized (reference.mPauseWorkLock) {
                reference.mPauseWorkLock.notifyAll();
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the ImageView's task still
         * points to this task as well. Returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work is in progress.
     * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can bind its result,
     * independently of the finish order.
     */
    private static class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        private AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<>(bitmapWorkerTask);
        }

        private BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * Called when the processing is complete and the final drawable should be
     * set on the ImageView.
     *
     * @param imageView
     * @param drawable
     */
    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        if (mFadeInBitmap) {
            // Transition drawable with a transparent drawable and the final drawable
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[]{
                            new ColorDrawable(mResources.getColor(android.R.color.transparent)),
                            drawable
                    });
            // Set background to loading bitmap
            imageView.setBackgroundDrawable(
                    new BitmapDrawable(mResources, mLoadingBitmap));

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageDrawable(drawable);
        }
    }

    /**
     * Pause any ongoing background work. This can be used as a temporary
     * measure to improve performance. For example background work could
     * be paused when a ListView or GridView is being scrolled using a
     * {@link android.widget.AbsListView.OnScrollListener} to keep
     * scrolling smooth.
     * <p>
     * If work is paused, be sure setPauseWork(false) is called again
     * before your fragment or activity is destroyed (for example during
     * {@link Activity#onPause()}), or there is a risk the
     * background thread will never finish.
     */
    private void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    private static class CacheAsyncTask extends AsyncTask<Object, Void, Void> {

        private final WeakReference<ImageWorker> mReference;

        private CacheAsyncTask(final ImageWorker reference) {
            mReference = new WeakReference<>(reference);
        }

        @Override
        protected Void doInBackground(final Object... params) {
            final ImageWorker reference = mReference.get();
            if (reference == null) {
                AppLogger.w(TAG + " CacheAsyncTask doInBackground - reference is null");
                return null;
            }
            switch ((Integer) params[0]) {
                case MESSAGE_CLEAR:
                    reference.clearCacheInternal();
                    break;
                case MESSAGE_INIT_DISK_CACHE:
                    reference.initDiskCacheInternal();
                    break;
                case MESSAGE_FLUSH:
                    reference.flushCacheInternal();
                    break;
                case MESSAGE_CLOSE:
                    reference.closeCacheInternal();
                    break;
            }
            return null;
        }
    }

    void initDiskCacheInternal() {
        if (mImageCache != null) {
            mImageCache.initDiskCache();
        }
    }

    void clearCacheInternal() {
        if (mImageCache != null) {
            mImageCache.clearCache();
        }
    }

    void flushCacheInternal() {
        if (mImageCache != null) {
            mImageCache.flush();
        }
    }

    void closeCacheInternal() {
        if (mImageCache != null) {
            mImageCache.close();
            mImageCache = null;
        }
    }

    public void clearCache() {
        if (ConcurrentUtils.isThreadPoolFull()) {
            return;
        }
        new CacheAsyncTask(this).execute(MESSAGE_CLEAR);
    }

    public void flushCache() {
        if (ConcurrentUtils.isThreadPoolFull()) {
            return;
        }
        new CacheAsyncTask(this).execute(MESSAGE_FLUSH);
    }

    public void closeCache() {
        if (ConcurrentUtils.isThreadPoolFull()) {
            return;
        }
        new CacheAsyncTask(this).execute(MESSAGE_CLOSE);
    }
}
