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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.api.APIServiceProvider;
import com.yuriy.openradio.api.CategoryVO;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.net.UrlBuilder;
import com.yuriy.openradio.utils.AppUtils;
import com.yuriy.openradio.utils.MediaIDHelper;
import com.yuriy.openradio.utils.QueueHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class MediaItemParentCategories implements MediaItemCommand {

    @Override
    public void create(final Context context, final String countryCode,
                       final Downloader downloader, final APIServiceProvider serviceProvider,
                       @NonNull final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                       final List<MediaBrowser.MediaItem> mediaItems,
                       final IUpdatePlaybackState playbackStateListener) {

        // Use result.detach to allow calling result.sendResult from another thread:
        result.detach();

        final String primaryMenuId
                = parentId.replace(MediaIDHelper.MEDIA_ID_PARENT_CATEGORIES, "");

        mCurrentCategory = primaryMenuId;

        AppUtils.API_CALL_EXECUTOR.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // Load child categories into menu
                        loadChildCategories(
                                context,
                                serviceProvider,
                                downloader,
                                primaryMenuId,
                                mediaItems,
                                result,
                                playbackStateListener
                        );
                    }
                }
        );
    }

    /**
     * Load Child Categories into Menu.
     *
     * @param serviceProvider {@link com.yuriy.openradio.api.APIServiceProvider}
     * @param downloader      {@link com.yuriy.openradio.net.Downloader}
     * @param primaryItemId   Id of the primary menu item.
     * @param mediaItems      Collections of {@link android.media.browse.MediaBrowser.MediaItem}s
     * @param result          Result of the loading.
     */
    private void loadChildCategories(final Context context,
                                     final APIServiceProvider serviceProvider,
                                     final Downloader downloader,
                                     final String primaryItemId,
                                     final List<MediaBrowser.MediaItem> mediaItems,
                                     final MediaBrowserService.Result<List<MediaBrowser.MediaItem>> result,
                                     final IUpdatePlaybackState playbackStateListener) {
        final List<CategoryVO> list = serviceProvider.getCategories(downloader,
                UrlBuilder.getChildCategoriesUrl(context, primaryItemId));

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(context.getString(R.string.no_data_message));
            return;
        }

        Collections.sort(list, new Comparator<CategoryVO>() {

            @Override
            public int compare(CategoryVO lhs, CategoryVO rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        QueueHelper.copyCollection(mChildCategories, list);

        final String iconUrl = "android.resource://" +
                context.getPackageName() + "/drawable/ic_child_categories";

        for (CategoryVO category : mChildCategories) {
            mediaItems.add(new MediaBrowser.MediaItem(
                    new MediaDescription.Builder()
                            .setMediaId(
                                    MediaIDHelper.MEDIA_ID_CHILD_CATEGORIES
                                            + String.valueOf(category.getId())
                            )
                            .setTitle(category.getTitle())
                            .setIconUri(Uri.parse(iconUrl))
                            .setSubtitle(category.getDescription())
                            .build(), MediaBrowser.MediaItem.FLAG_BROWSABLE
            ));
        }

        result.sendResult(mediaItems);
    }
}
