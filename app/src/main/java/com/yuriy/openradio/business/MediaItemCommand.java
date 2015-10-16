/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.media.browse.MediaBrowser;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.RadioStationVO;
import com.yuriy.openradio.net.Downloader;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public interface MediaItemCommand {

    /**
     *
     */
    interface IUpdatePlaybackState {

        /**
         *
         * @param error
         */
        void updatePlaybackState(final String error);
    }

    /**
     *
     * @param countryCode
     * @param downloader
     * @param serviceProvider
     * @param result
     * @param mediaItems
     * @param playbackStateListener
     * @param parentId
     * @param radioStations
     * @param shareObject
     */
    void create(final String countryCode,
                final Downloader downloader, final APIServiceProvider serviceProvider,
                @NonNull final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                final List<MediaBrowser.MediaItem> mediaItems,
                final IUpdatePlaybackState playbackStateListener,
                final String parentId, final List<RadioStationVO> radioStations,
                @NonNull final MediaItemShareObject shareObject);
}
