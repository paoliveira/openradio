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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.yuriy.openradio.R;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.vo.Category;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link MediaItemAllCategories} is concrete implementation of the {@link MediaItemCommand} that
 * designed to prepare data to display radio stations of All Categories.
 */
public final class MediaItemAllCategories implements MediaItemCommand {

    private static final String LOG_TAG = MediaItemAllCategories.class.getSimpleName();

    public MediaItemAllCategories() {
        super();
    }

    @Override
    public void execute(final IUpdatePlaybackState playbackStateListener,
                        @NonNull final MediaItemShareObject shareObject) {

        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        shareObject.getResult().detach();

        AppUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load all categories into menu
                    loadAllCategories(playbackStateListener, shareObject);
                }
        );
    }

    /**
     * Load All Categories into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command.
     */
    private void loadAllCategories(final IUpdatePlaybackState playbackStateListener,
                                   @NonNull final MediaItemShareObject shareObject) {
        final List<Category> list = shareObject.getServiceProvider().getCategories(
                shareObject.getDownloader(),
                UrlBuilder.getAllCategoriesUrl(shareObject.getContext()));

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(
                    shareObject.getContext().getString(R.string.no_data_message)
            );
            return;
        }

        Collections.sort(list, (lhs, rhs) -> lhs.getTitle().compareTo(rhs.getTitle()));

        final String iconUrl = "android.resource://" +
                shareObject.getContext().getPackageName() + "/drawable/ic_child_categories";

        final Set<String> predefinedCategories = AppUtils.predefinedCategories();
        for (final Category category : list) {

            if (!predefinedCategories.contains(category.getTitle())) {
                continue;
            }

            shareObject.getMediaItems().add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getTitle())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(category.getDescription())
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }

        shareObject.getResult().sendResult(shareObject.getMediaItems());
    }
}
