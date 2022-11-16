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
import com.yuriy.openradio.shared.utils.MediaItemBuilder

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemRootCar] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display root menu items for Car display.
 */
class MediaItemRootCar : MediaItemCommand {

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        dependencies.result.detach()
        val context = dependencies.context
        // Show Favorites if they are exists.
        val favorites = dependencies.presenter.getAllFavorites()
        if (favorites.isNotEmpty()) {
            dependencies.addMediaItem(MediaItemBuilder.buildFavoritesMenuItem(context))
        }
        // Recently added Radio Stations.
        dependencies.addMediaItem(MediaItemBuilder.buildRecentMenuItem(context))
        // Browse category to provide the rest of categories.
        dependencies.addMediaItem(MediaItemBuilder.buildBrowseMenuItem(context))
        dependencies.result.sendResult(dependencies.getMediaItems())
        dependencies.resultListener.onResult()
    }
}
