/*
 * Copyright 2015-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.support.v4.media.MediaBrowserCompat
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaItemHelper.buildMediaDescriptionFromRadioStation
import com.yuriy.openradio.shared.utils.MediaItemHelper.updateFavoriteField
import com.yuriy.openradio.shared.utils.MediaItemHelper.updateSortIdField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemFavoritesList] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display radio stations from Favorites list.
 */
class MediaItemFavoritesList : MediaItemCommand {

    override fun execute(playbackStateListener: IUpdatePlaybackState?,
                         dependencies: MediaItemCommandDependencies) {
        AppLogger.d("$LOG_TAG invoked")
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()

        GlobalScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                val context = dependencies.context
                val list = FavoritesStorage.getAll(context)
                dependencies.radioStationsStorage.clearAndCopy(list)
                for (radioStation in list) {
                    val mediaDescription = buildMediaDescriptionFromRadioStation(
                            context, radioStation
                    )
                    val mediaItem = MediaBrowserCompat.MediaItem(
                            mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    )
                    updateFavoriteField(mediaItem, true)
                    updateSortIdField(mediaItem, radioStation.sortId)
                    dependencies.addMediaItem(mediaItem)
                    AppLogger.d("$LOG_TAG sort id:${radioStation.sortId}")
                }
                dependencies.result.sendResult(dependencies.mediaItems)
                dependencies.resultListener.onResult()
            } ?: dependencies.result.sendResult(null)
        }
    }

    companion object {
        private val LOG_TAG = MediaItemFavoritesList::class.java.simpleName
    }
}
