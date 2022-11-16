/*
 * Copyright 2017-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.utils.MediaItemHelper
import com.yuriy.openradio.shared.utils.PlayerUtils
import com.yuriy.openradio.shared.utils.toMediaItemPlayable
import com.yuriy.openradio.shared.vo.RadioStation
import kotlinx.coroutines.Job

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class MediaItemCommandImpl internal constructor() : MediaItemCommand {

    protected var mJob: Job? = null

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {

    }

    abstract fun doLoadNoDataReceived(): Boolean

    fun handleDataLoaded(
        playbackStateListener: IUpdatePlaybackState,
        dependencies: MediaItemCommandDependencies,
        list: List<RadioStation>
    ) {
        if (list.isEmpty()) {
            if (doLoadNoDataReceived()) {
                val track = MediaItemHelper.buildMediaMetadataForEmptyCategory(
                    dependencies.context,
                    MediaId.MEDIA_ID_CHILD_CATEGORIES
                )
                val mediaDescription = track.description
                val mediaItem = MediaBrowserCompat.MediaItem(
                    mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
                MediaItemHelper.setDrawableId(mediaItem.description.extras, R.drawable.ic_radio_station_empty)
                dependencies.addMediaItem(mediaItem)
                dependencies.result.sendResult(dependencies.getMediaItems())
                dependencies.resultListener.onResult()
                playbackStateListener.updatePlaybackState(dependencies.context.getString(R.string.no_data_message))
            } else {
                dependencies.result.sendResult(PlayerUtils.createListEndedResult())
                dependencies.resultListener.onResult()
            }
            return
        }
        deliverResult(dependencies, list)
    }

    fun deliverResult(dependencies: MediaItemCommandDependencies, list: List<RadioStation> = ArrayList()) {
        for (radioStation in list) {
            dependencies.addMediaItem(
                radioStation.toMediaItemPlayable(
                    isFavorite = dependencies.presenter.isRadioStationFavorite(radioStation)
                )
            )
        }
        dependencies.result.sendResult(dependencies.getMediaItems())
        dependencies.resultListener.onResult(list)
    }
}
