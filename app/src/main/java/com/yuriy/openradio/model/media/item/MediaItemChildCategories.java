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

import android.support.annotation.NonNull;

import com.yuriy.openradio.model.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIdHelper;
import com.yuriy.openradio.vo.RadioStation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemChildCategories} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of child categories of category.
 */
public final class MediaItemChildCategories extends IndexableMediaItemCommand {

    private static final String LOG_TAG = MediaItemChildCategories.class.getSimpleName();

    public MediaItemChildCategories() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject) {
        super.execute(playbackStateListener, shareObject);
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    final List<RadioStation> list = new ArrayList<>();
                    if (!shareObject.isRestoreInstance()) {
                        final String childMenuId = shareObject.getParentId()
                                .replace(MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES, "");
                        list.addAll(
                                shareObject.getServiceProvider().getStations(
                                        shareObject.getDownloader(),
                                        UrlBuilder.getStationsInCategory(
                                                childMenuId,
                                                getPageNumber(),
                                                UrlBuilder.ITEMS_PER_PAGE
                                        )
                                )
                        );
                    }
                    handleDataLoaded(playbackStateListener, shareObject, list);
                }
        );
    }
}
