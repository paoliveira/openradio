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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
interface MediaItemCommand {

    companion object {
        const val CMD_TIMEOUT_MS = 5000L
    }

    /**
     * Interface to provide callback when an event occur during command execution.
     */
    interface IUpdatePlaybackState {

        /**
         * Callback when an event occur during command execution.
         *
         * @param error Description of an error.
         */
        fun updatePlaybackState(error: String?)
    }

    /**
     * Common method to execute single, specific command.
     *
     * @param playbackStateListener Implementation of the [IUpdatePlaybackState] interface.
     * @param dependencies Instance of the [MediaItemCommandDependencies] which holds various references needed to
     * execute command.
     */
    fun execute(playbackStateListener: IUpdatePlaybackState?, dependencies: MediaItemCommandDependencies)
}
