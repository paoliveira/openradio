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
import com.yuriy.openradio.shared.utils.MediaItemBuilder

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemBrowseCar] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display Browse menu items for Car display.
 */
class MediaItemBrowseCar : MediaItemCommand {

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        val context = dependencies.context
        dependencies.result.detach()
        // Popular Radio Stations
        dependencies.addMediaItem(MediaItemBuilder.buildPopularMenuItem(context))
        // Worldwide Stations
        dependencies.addMediaItem(MediaItemBuilder.buildCategoriesMenuItem(context))
        // All countries list
        dependencies.addMediaItem(MediaItemBuilder.buildCountriesMenuItem(context))
        // If the Country code is known:
        val locationStr = context.getString(R.string.default_country_use_location)
        val countryCode = dependencies.countryCode
        if (countryCode.isNotEmpty() && countryCode != locationStr) {
            dependencies.addMediaItem(MediaItemBuilder.buildCountryMenuItem(context, countryCode))
        }
        // Show Local Radio Stations if they are exists
        val deviceLocal = dependencies.presenter.getAllDeviceLocal()
        if (deviceLocal.isNotEmpty()) {
            // Locals list
            dependencies.addMediaItem(MediaItemBuilder.buildDeviceLocalsMenuItem(context))
        }
        dependencies.result.sendResult(dependencies.getMediaItems())
        dependencies.resultListener.onResult()
    }
}
