/*
 * Copyright 2015-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.utils.MediaItemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class is implementation of the [MediaItemCommand] that
 * designed to prepare data to display radio stations of All Categories.
 */
class MediaItemAllCategories : MediaItemCommand {

    private var mJob: Job? = null

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        if (dependencies.isSavedInstance) {
            dependencies.result.sendResult(null)
            return
        }
        mJob?.cancel()
        mJob = dependencies.mScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                // Load all categories into menu
                loadAllCategories(playbackStateListener, dependencies)
            } ?: dependencies.result.sendResult(null)
        }
    }

    /**
     * Load All Categories into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param dependencies Instance of the [MediaItemCommandDependencies] which holds various references needed to
     * execute command.
     */
    private fun loadAllCategories(
        playbackStateListener: IUpdatePlaybackState,
        dependencies: MediaItemCommandDependencies
    ) {
        val set = dependencies.presenter.getAllCategories()
        if (set.isEmpty()) {
            playbackStateListener.updatePlaybackState(
                dependencies.context.getString(R.string.no_data_message)
            )
            return
        }

        // Counter of max number of categories. It is matter for Car since binder can not transfer a huge
        // amount of data.
        var counter = 0
        for (category in set) {
            if (dependencies.isCar && counter++ > MAX_COUNTER) {
                break
            }
            val bundle = Bundle()
            MediaItemHelper.setDrawableId(bundle, R.drawable.ic_child_categories)
            dependencies.addMediaItem(
                MediaBrowserCompat.MediaItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(MediaId.MEDIA_ID_CHILD_CATEGORIES + category.id)
                        .setTitle(category.title)
                        .setExtras(bundle)
                        .setSubtitle(category.getDescription(dependencies.context))
                        .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
            )
        }
        dependencies.result.sendResult(dependencies.getMediaItems())
        dependencies.resultListener.onResult()
    }

    companion object {
        private const val MAX_COUNTER = 200
    }
}
