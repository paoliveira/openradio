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

import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.toMediaItemPlayable
import com.yuriy.openradio.shared.vo.isInvalid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemLocalsList] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display radio stations from Locals list.
 */
class MediaItemLocalsList : MediaItemCommand {

    private var mJob: Job? = null

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        mJob?.cancel()
        mJob = dependencies.mScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                val list = dependencies.presenter.getAllDeviceLocal()
                Collections.sort(list, dependencies.presenter.getRadioStationsComparator())
                for (radioStation in list) {
                    if (radioStation.isInvalid()) {
                        AppLogger.w("Skip invalid local $radioStation")
                        continue
                    }
                    dependencies.addMediaItem(
                        radioStation.toMediaItemPlayable(
                            isFavorite = dependencies.presenter.isRadioStationFavorite(radioStation),
                            isLocal = true
                        )
                    )
                }
                dependencies.result.sendResult(dependencies.getMediaItems())
                dependencies.resultListener.onResult(list)
            } ?: dependencies.result.sendResult(null)
        }
    }
}
