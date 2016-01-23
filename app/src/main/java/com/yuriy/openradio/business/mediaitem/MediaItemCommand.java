/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.business.mediaitem;

import android.support.annotation.NonNull;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public interface MediaItemCommand {

    /**
     * {@link MediaItemCommand.IUpdatePlaybackState} is an interface
     * to provide callback when an error occur during command execution.
     */
    interface IUpdatePlaybackState {

        /**
         * Callback when an error occur during command execution.
         *
         * @param error Description of an error.
         */
        void updatePlaybackState(final String error);
    }

    /**
     * Common method to execute single, specific command.
     *
     * @param playbackStateListener Implementation of the
     *                              {@link MediaItemCommand.IUpdatePlaybackState}
     *                              interface.
     * @param shareObject           Instance of the {@link MediaItemShareObject} which holds various
     *                              references needed to execute command.
     */
    void create(final IUpdatePlaybackState playbackStateListener,
                @NonNull final MediaItemShareObject shareObject);
}
