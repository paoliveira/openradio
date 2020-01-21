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

package com.yuriy.openradio.shared.model.media.item;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import com.yuriy.openradio.shared.model.api.ApiServiceProvider;
import com.yuriy.openradio.shared.model.net.Downloader;
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.MediaItemsComparator;
import com.yuriy.openradio.shared.utils.RadioStationsComparator;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemCommandDependencies} is a class that designed to keep all necessary references that are
 * shared between implementations of the {@link MediaItemCommand} interface, includes application
 * context, name of the current category, list of all categories, etc ...
 */
public final class MediaItemCommandDependencies {

    /**
     * Context of the application.
     */
    @NonNull
    private final Context mContext;

    /**
     * String value of the Country Code.
     */
    private final String mCountryCode;

    /**
     *
     */
    @NonNull
    private final Downloader mDownloader;

    /**
     *
     */
    @NonNull
    private final ApiServiceProvider mServiceProvider;

    /**
     *
     */
    @NonNull
    private final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> mResult;

    @NonNull
    private final List<MediaBrowserCompat.MediaItem> mMediaItems;

    /**
     *
     */
    private final String mParentId;

    /**
     *
     */
    @NonNull
    private final List<RadioStation> mRadioStations;

    /**
     * Flag that indicates whether application runs over normal Android or Auto version.
     */
    private final boolean mIsAndroidAuto;

    private final OpenRadioService.RemotePlay mRemotePlay;

    private final OpenRadioService.ResultListener mResultListener;

    private final boolean mIsSameCatalogue;

    private final Comparator<MediaBrowserCompat.MediaItem> mMediaItemsComparator;

    private final Comparator<RadioStation> mRadioStationsComparator;

    /**
     * Main constructor.
     */
    public MediaItemCommandDependencies(@NonNull final Context context,
                                        @NonNull final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result,
                                        @NonNull final List<RadioStation> radioStations,
                                        @NonNull final ApiServiceProvider serviceProvider,
                                        @NonNull final String countryCode,
                                        @NonNull final String parentId,
                                        final boolean isAndroidAuto,
                                        final boolean isSameCatalogue,
                                        @NonNull final OpenRadioService.RemotePlay remotePlay,
                                        @NonNull final OpenRadioService.ResultListener resultListener) {
        super();
        mContext = context;
        mMediaItemsComparator = new MediaItemsComparator();
        mRadioStationsComparator = new RadioStationsComparator();
        mDownloader = new HTTPDownloaderImpl();
        mMediaItems = new ArrayList<>();
        mResult = result;
        mRadioStations = radioStations;
        mServiceProvider = serviceProvider;
        mCountryCode = countryCode;
        mParentId = parentId;
        mIsAndroidAuto = isAndroidAuto;
        mIsSameCatalogue = isSameCatalogue;
        mRemotePlay = remotePlay;
        mResultListener = resultListener;
    }

    public OpenRadioService.RemotePlay getRemotePlay() {
        return mRemotePlay;
    }

    public OpenRadioService.ResultListener getResultListener() {
        return mResultListener;
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
     *
     * @return
     */
    public String getCountryCode() {
        return mCountryCode;
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
     * @return
     */
    @NonNull
    public ApiServiceProvider getServiceProvider() {
        return mServiceProvider;
    }

    /**
     *
     * @return
     */
    @NonNull
    public MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> getResult() {
        return mResult;
    }

    public void addMediaItem(@NonNull final MediaBrowserCompat.MediaItem item) {
        mMediaItems.add(item);
    }

    /**
     *
     * @return
     */
    @NonNull
    public List<MediaBrowserCompat.MediaItem> getMediaItems() {
        Collections.sort(mMediaItems, mMediaItemsComparator);
        Collections.sort(mRadioStations, mRadioStationsComparator);
        return mMediaItems;
    }

    /**
     *
     * @return
     */
    public String getParentId() {
        return mParentId;
    }

    public void addAllRadioStations(@NonNull final List<RadioStation> list) {
        mRadioStations.addAll(list);
    }

    /**
     *
     */
    public void clearRadioStations() {
        mRadioStations.clear();
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
     * @return
     */
    public boolean isAndroidAuto() {
        return mIsAndroidAuto;
    }

    public boolean isSameCatalogue() {
        return mIsSameCatalogue;
    }
}
