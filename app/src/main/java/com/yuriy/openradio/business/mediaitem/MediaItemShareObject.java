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

package com.yuriy.openradio.business.mediaitem;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;

import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.business.service.OpenRadioService;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.vo.Category;
import com.yuriy.openradio.vo.RadioStation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemShareObject} is a class that designed to keep all necessary references that are
 * shared between implementations of the {@link MediaItemCommand} interface, includes application
 * context, name of the current category, list of all categories, etc ...
 */
public final class MediaItemShareObject {

    /**
     * Collection of the Child Categories.
     */
    private final List<Category> mChildCategories = new ArrayList<>();

    /**
     * Id of the current Category. It is used for example when back from an empty Category.
     */
    private String mCurrentCategory = "";

    /**
     * Context of the application.
     * TODO : must be final mandatory
     */
    @NonNull
    private Context mContext;

    /**
     * String value of the Country Code.
     */
    private String mCountryCode;

    /**
     * TODO : must be final mandatory
     */
    @NonNull
    private Downloader mDownloader;

    /**
     * TODO : must be final mandatory
     */
    @NonNull
    private APIServiceProvider mServiceProvider;

    /**
     * TODO : must be final mandatory
     */
    @NonNull
    private MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> mResult;

    /**
     * TODO : must be final mandatory
     */
    @NonNull
    private List<MediaBrowserCompat.MediaItem> mMediaItems;

    /**
     *
     */
    private String mParentId;

    /**
     *
     */
    private List<RadioStation> mRadioStations;

    /**
     * Flag that indicates whether application runs over normal Android or Auto version.
     */
    private boolean mIsAndroidAuto = false;

    private OpenRadioService.RemotePlay mRemotePlay;

    private volatile boolean mIsSameCatalogue;

    private volatile boolean mIsUseCache;

    /**
     * Private constructor.
     */
    private MediaItemShareObject() {
        super();
    }

    public OpenRadioService.RemotePlay getRemotePlay() {
        return mRemotePlay;
    }

    public void setRemotePlay(final OpenRadioService.RemotePlay value) {
        this.mRemotePlay = value;
    }

    /**
     * @return Collection of the Child Categories.
     */
    public List<Category> getChildCategories() {
        return mChildCategories;
    }

    /**
     * @return Id of the current Category.
     */
    public String getCurrentCategory() {
        return mCurrentCategory;
    }

    /**
     * Sets the Id of the current Category.
     * @param value Id of the current Category.
     */
    public void setCurrentCategory(final String value) {
        mCurrentCategory = value;
    }

    /**
     * Gets the application's context.
     * @return the Application's context.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Sets the application's context.
     * @param value The application's context.
     */
    public void setContext(@NonNull final Context value) {
        mContext = value;
    }

    /**
     *
     * @return
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     *
     * @param value
     */
    public void setCountryCode(final String value) {
        mCountryCode = value;
    }

    /**
     *
     * @return
     */
    @NonNull
    public Downloader getDownloader() {
        return mDownloader;
    }

    /**
     *
     * @param value
     */
    public void setDownloader(@NonNull final Downloader value) {
        this.mDownloader = value;
    }

    /**
     *
     * @return
     */
    @NonNull
    public APIServiceProvider getServiceProvider() {
        return mServiceProvider;
    }

    /**
     *
     * @param value
     */
    public void setServiceProvider(@NonNull APIServiceProvider value) {
        mServiceProvider = value;
    }

    /**
     *
     * @return
     */
    @NonNull
    public MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> getResult() {
        return mResult;
    }

    /**
     *
     * @param value
     */
    public void setResult(@NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> value) {
        mResult = value;
    }

    /**
     *
     * @return
     */
    @NonNull
    public List<MediaBrowserCompat.MediaItem> getMediaItems() {
        return mMediaItems;
    }

    /**
     *
     * @param value
     */
    public void setMediaItems(@NonNull List<MediaBrowserCompat.MediaItem> value) {
        mMediaItems = value;
    }

    /**
     *
     * @return
     */
    public String getParentId() {
        return mParentId;
    }

    /**
     *
     * @param value
     */
    public void setParentId(final String value) {
        mParentId = value;
    }

    /**
     *
     * @return
     */
    public List<RadioStation> getRadioStations() {
        return mRadioStations;
    }

    /**
     *
     * @param value
     */
    public void setRadioStations(final List<RadioStation> value) {
        mRadioStations = value;
    }

    /**
     *
     * @return
     */
    public boolean isAndroidAuto() {
        return mIsAndroidAuto;
    }

    /**
     *
     * @param value
     */
    public void setIsAndroidAuto(boolean value) {
        mIsAndroidAuto = value;
    }

    public boolean isSameCatalogue() {
        return mIsSameCatalogue;
    }

    public void isSameCatalogue(final boolean value) {
        mIsSameCatalogue = value;
    }

    public boolean isUseCache() {
        return mIsUseCache;
    }

    public void setUseCache(final boolean value) {
        mIsUseCache = value;
    }

    /**
     * Factory method to create default instance.
     *
     * @return Default instance of the {@link MediaItemShareObject}.
     */
    public static MediaItemShareObject getDefaultInstance() {
        return new MediaItemShareObject();
    }
}
