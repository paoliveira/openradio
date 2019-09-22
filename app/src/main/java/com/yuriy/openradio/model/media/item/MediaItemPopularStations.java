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

package com.yuriy.openradio.model.media.item;

import androidx.annotation.NonNull;

import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.ConcurrentUtils;
import com.yuriy.openradio.vo.RadioStation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * {@link MediaItemPopularStations} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare the top popular stations all time based on unique views and station detail
 * API call.
 */
public final class MediaItemPopularStations extends MediaItemCommandImpl {

    private static final String LOG_TAG = MediaItemPopularStations.class.getSimpleName();

    /**
     * Default constructor.
     */
    public MediaItemPopularStations() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject) {
        super.execute(playbackStateListener, shareObject);
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load all categories into menu
                    final List<RadioStation> list = new ArrayList<>();
                    if (!shareObject.isRestoreInstance()) {
                        list.addAll(
                                shareObject.getServiceProvider().getStations(
                                        shareObject.getDownloader(),
                                        UrlBuilder.getPopularStations()
                                )
                        );
                    }
                    handleDataLoaded(playbackStateListener, shareObject, list);
                }
        );
    }

    @Override
    public boolean doLoadNoDataReceived() {
        return true;
    }
}
