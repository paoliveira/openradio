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

import android.support.annotation.NonNull;

import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.vo.RadioStation;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemCountryStations} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of the single Country.
 */
public final class MediaItemCountryStations extends IndexableMediaItemCommand {

    /**
     * Default constructor.
     */
    public MediaItemCountryStations() {
        super();
    }

    @Override
    public void create(final IUpdatePlaybackState playbackStateListener,
                       @NonNull final MediaItemShareObject shareObject) {
        super.create(playbackStateListener, shareObject);

        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load all categories into menu
                    final List<RadioStation> list = shareObject.getServiceProvider().getStations(
                            shareObject.getDownloader(),
                            UrlBuilder.getStationsInCountry(
                                    shareObject.getContext(),
                                    shareObject.getCountryCode(),
                                    incrementAndGetPageIndex(),
                                    UrlBuilder.ITEMS_PER_PAGE
                            )
                    );
                    handleDataLoaded(playbackStateListener, shareObject, list);
                }
        );
    }
}
