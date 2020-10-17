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

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.R;
import com.yuriy.openradio.shared.model.net.UrlBuilder;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.ConcurrentUtils;
import com.yuriy.openradio.shared.utils.MediaIdHelper;
import com.yuriy.openradio.shared.utils.MediaItemHelper;
import com.yuriy.openradio.shared.vo.Category;

import java.util.List;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
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
                        @NonNull final MediaItemCommandDependencies dependencies) {
        AppLogger.d(LOG_TAG + " invoked");
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.getResult().detach();

        ConcurrentUtils.API_CALL_EXECUTOR.submit(
                () -> {
                    // Load all categories into menu
                    loadAllCategories(playbackStateListener, dependencies);
                }
        );
    }

    /**
     * Load All Categories into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param dependencies          Instance of the {@link MediaItemCommandDependencies} which holds various
     *                              references needed to execute command.
     */
    private void loadAllCategories(final IUpdatePlaybackState playbackStateListener,
                                   @NonNull final MediaItemCommandDependencies dependencies) {
        final List<Category> list = dependencies.getServiceProvider().getCategories(
                dependencies.getDownloader(),
                UrlBuilder.getAllCategoriesUrl(),
                MediaItemCommandImpl.getCacheType(dependencies)
        );

        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(
                    dependencies.getContext().getString(R.string.no_data_message)
            );
            return;
        }

        for (final Category category : list) {
            final Bundle bundle = new Bundle();
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_child_categories);
            dependencies.addMediaItem(
                    new MediaBrowserCompat.MediaItem(
                            new MediaDescriptionCompat.Builder()
                                    .setMediaId(MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES + category.getId())
                                    .setTitle(category.getTitle())
                                    .setExtras(bundle)
                                    .setSubtitle(category.getDescription())
                                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            );
        }

        dependencies.getResult().sendResult(dependencies.getMediaItems());
        dependencies.getResultListener().onResult();
    }
}
