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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemPopularStations] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare the top popular stations all time based on unique views and station detail
 * API call.
 */
class MediaItemPopularStations : MediaItemCommandImpl() {

    override fun execute(
        playbackStateListener: IUpdatePlaybackState,
        dependencies: MediaItemCommandDependencies
    ) {
        super.execute(playbackStateListener, dependencies)
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        if (dependencies.isSavedInstance) {
            deliverResult(dependencies)
            return
        }
        mJob?.cancel()
        mJob = dependencies.mScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                // Load all categories into menu
                handleDataLoaded(
                    playbackStateListener,
                    dependencies,
                    dependencies.presenter.getPopularStations()
                )
            } ?: dependencies.result.sendResult(null)
        }
    }

    override fun doLoadNoDataReceived(): Boolean {
        return true
    }
}
