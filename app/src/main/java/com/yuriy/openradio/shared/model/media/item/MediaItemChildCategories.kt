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

import android.os.Bundle
import com.yuriy.openradio.shared.model.media.item.MediaItemCommand.IUpdatePlaybackState
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.model.net.UrlBuilder.getStationsInCategory
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.MediaIdHelper
import com.yuriy.openradio.shared.vo.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class is implementation of the [MediaItemCommand] that designed to prepare data to display radio stations of
 * child categories of category.
 */
class MediaItemChildCategories : IndexableMediaItemCommand() {

    override fun execute(playbackStateListener: IUpdatePlaybackState?, dependencies: MediaItemCommandDependencies) {
        super.execute(playbackStateListener, dependencies)
        AppLogger.d("$LOG_TAG invoked")
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        if (dependencies.isSavedInstance) {
            deliverResult(dependencies)
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            withTimeoutOrNull(MediaItemCommand.CMD_TIMEOUT_MS) {
                val childMenuId = dependencies.parentId
                        .replace(MediaIdHelper.MEDIA_ID_CHILD_CATEGORIES, "")
                val list: List<RadioStation> = ArrayList(
                        dependencies.serviceProvider.getStations(
                                dependencies.downloader,
                                getStationsInCategory(
                                        childMenuId,
                                        pageNumber * (UrlBuilder.ITEMS_PER_PAGE + 1),
                                        UrlBuilder.ITEMS_PER_PAGE
                                ),
                                getCacheType(dependencies)
                        )
                )
                handleDataLoaded(playbackStateListener, dependencies, list)
            } ?: dependencies.result.sendError(Bundle())
        }
    }

    companion object {
        private val LOG_TAG = MediaItemChildCategories::class.java.simpleName
    }
}
