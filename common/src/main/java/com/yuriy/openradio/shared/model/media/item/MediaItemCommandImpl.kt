/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.cache.CacheType
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.vo.RadioStation

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class MediaItemCommandImpl internal constructor() : MediaItemCommand {

    override fun execute(playbackStateListener: IUpdatePlaybackState?, dependencies: MediaItemCommandDependencies) {
        AppLogger.d("$CLASS_NAME invoked")
        if (!dependencies.isSameCatalogue) {
            AppLogger.d("$CLASS_NAME not the same catalogue, clear list")
            dependencies.radioStationsStorage.clear()
        }
    }

    abstract fun doLoadNoDataReceived(): Boolean

    fun handleDataLoaded(playbackStateListener: IUpdatePlaybackState?,
                         dependencies: MediaItemCommandDependencies,
                         list: List<RadioStation>) {
        AppLogger.d(CLASS_NAME + " loaded " + list.size + " items")
        if (list.isEmpty()) {
            if (doLoadNoDataReceived()) {
                val track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                    dependencies.context,
                    MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES
                )
                val mediaDescription = track.description
                val mediaItem = MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                MediaItemHelper.setDrawableId(mediaItem.description.extras, R.drawable.ic_radio_station_empty)
                dependencies.addMediaItem(mediaItem)
                dependencies.result.sendResult(dependencies.mediaItems)
                dependencies.resultListener.onResult()
                playbackStateListener?.updatePlaybackState(dependencies.context.getString(R.string.no_data_message))
            } else {
                dependencies.result.sendResult(MediaItemHelper.createListEndedResult())
                dependencies.resultListener.onResult()
            }
            return
        }
        dependencies.radioStationsStorage.addAll(list)
        deliverResult(dependencies)
    }

    fun deliverResult(dependencies: MediaItemCommandDependencies) {
        val radioStations = dependencies.radioStationsStorage.all
        for (radioStation in radioStations) {
            val mediaDescription = MediaItemHelper.buildMediaDescriptionFromRadioStation(
                radioStation,
                isFavorite = FavoritesStorage.isFavorite(radioStation, dependencies.context)
            )
            val mediaItem = MediaBrowserCompat.MediaItem(
                mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            dependencies.addMediaItem(mediaItem)
        }
        AppLogger.d(CLASS_NAME + " deliver " + dependencies.mediaItems.size + " items")
        dependencies.result.sendResult(dependencies.mediaItems)
        dependencies.resultListener.onResult()
    }

    companion object {
        private val CLASS_NAME = MediaItemCommandImpl::class.java.simpleName

        @JvmStatic
        fun getCacheType(dependencies: MediaItemCommandDependencies): CacheType {
            return if (dependencies.isSavedInstance) {
                CacheType.IN_MEMORY
            } else CacheType.PERSISTENT
        }
    }
}
