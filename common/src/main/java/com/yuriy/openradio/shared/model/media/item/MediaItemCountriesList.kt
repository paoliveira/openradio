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

import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.service.LocationService
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaItemBuilder
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
 * [MediaItemCountriesList] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display list of all Countries.
 */
class MediaItemCountriesList : MediaItemCommand {

    private var mJob: Job? = null

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        mJob?.cancel()
        mJob = dependencies.mScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                // Load all countries into menu
                loadAllCountries(playbackStateListener, dependencies)
            } ?: dependencies.result.sendResult(null)
        }
    }

    /**
     * Load All Countries into Menu.
     *
     * @param playbackStateListener Listener of the Playback State changes.
     * @param dependencies           Instance of the [MediaItemCommandDependencies] which holds various
     * references needed to execute command.
     */
    private fun loadAllCountries(
        playbackStateListener: IUpdatePlaybackState,
        dependencies: MediaItemCommandDependencies
    ) {
        val list = dependencies.presenter.getAllCountries()
        if (list.isEmpty()) {
            playbackStateListener.updatePlaybackState(
                dependencies.context.getString(R.string.no_data_message)
            )
            return
        }
        for (country in list) {
            if (!LocationService.COUNTRY_CODE_TO_NAME.containsKey(country.code)) {
                // Add missing country to the Map of the existing ones.
                AppLogger.w("$CLASS_NAME Missing country:$country")
                continue
            }
            dependencies.addMediaItem(MediaItemBuilder.buildCountryMenuItem(dependencies.context, country))
        }
        dependencies.result.sendResult(dependencies.getMediaItems())
        dependencies.resultListener.onResult()
    }

    companion object {
        /**
         * String tag to use in the log message.
         */
        private val CLASS_NAME = MediaItemCountriesList::class.java.simpleName
    }
}
