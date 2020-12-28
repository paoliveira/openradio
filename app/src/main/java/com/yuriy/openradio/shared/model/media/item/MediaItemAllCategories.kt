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
package com.yuriy.openradio.shared.model.media.item

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.media.item.MediaItemCommandImpl.Companion.getCacheType
import com.yuriy.openradio.shared.model.net.UrlBuilder.allCategoriesUrl
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper.setDrawableId

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [MediaItemAllCategories] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display radio stations of All Categories.
 */
class MediaItemAllCategories : MediaItemCommand {
    override fun execute(playbackStateListener: IUpdatePlaybackState?,
                         dependencies: MediaItemCommandDependencies) {
        d("$LOG_TAG invoked")
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        val executorService = dependencies.executorService
        if (executorService.isShutdown) {
            e("Can not handle MediaItemAllCategories, executor is shut down")
            dependencies.result.sendError(Bundle())
            return
        }
        executorService.submit {
            // Load all categories into menu
            loadAllCategories(playbackStateListener, dependencies)
        }
    }

    /**
     * Load All Categories into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param dependencies          Instance of the [MediaItemCommandDependencies] which holds various
     * references needed to execute command.
     */
    private fun loadAllCategories(playbackStateListener: IUpdatePlaybackState?,
                                  dependencies: MediaItemCommandDependencies) {
        val list = dependencies.serviceProvider.getCategories(
                dependencies.downloader,
                allCategoriesUrl,
                getCacheType(dependencies)
        )
        if (list.isEmpty() && playbackStateListener != null) {
            playbackStateListener.updatePlaybackState(
                    dependencies.context.getString(R.string.no_data_message)
            )
            return
        }

        // Counter of max number of categories. It is matter for Android Auto since binder can not transfer a huge
        // amount of data.
        var counter = 0
        for (category in list) {
            if (dependencies.isAndroidAuto && counter++ > MAX_COUNTER) {
                break
            }
            val bundle = Bundle()
            setDrawableId(bundle, R.drawable.ic_child_categories)
            dependencies.addMediaItem(
                    MediaBrowserCompat.MediaItem(
                            MediaDescriptionCompat.Builder()
                                    .setMediaId(MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES + category.id)
                                    .setTitle(category.title)
                                    .setExtras(bundle)
                                    .setSubtitle(category.getDescription(dependencies.context))
                                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    )
            )
        }
        dependencies.result.sendResult(dependencies.mediaItems)
        dependencies.resultListener.onResult()
    }

    companion object {
        private val LOG_TAG = MediaItemAllCategories::class.java.simpleName
        private const val MAX_COUNTER = 200
    }
}
