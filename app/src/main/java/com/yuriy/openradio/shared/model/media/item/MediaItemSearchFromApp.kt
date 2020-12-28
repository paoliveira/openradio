/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.model.net.UrlBuilder.getSearchUrl
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import com.yuriy.openradio.shared.utils.AppUtils.searchQuery
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 8/31/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [MediaItemSearchFromApp] is concrete implementation of the [MediaItemCommand] that
 * designed to prepare data to display radio stations from the search collection.
 */
class MediaItemSearchFromApp : IndexableMediaItemCommand() {
    override fun execute(playbackStateListener: IUpdatePlaybackState?,
                         dependencies: MediaItemCommandDependencies) {
        super.execute(playbackStateListener, dependencies)
        d("$LOG_TAG invoked")
        // Use result.detach to allow calling result.sendResult from another thread:
        dependencies.result.detach()
        if (dependencies.isSavedInstance) {
            deliverResult(dependencies)
            return
        }
        val executorService = dependencies.executorService
        if (executorService.isShutdown) {
            e("Can not handle MediaItemSearchFromApp, executor is shut down")
            dependencies.result.sendError(Bundle())
            return
        }
        executorService.submit {
            val list: List<RadioStation> = ArrayList(
                    dependencies.serviceProvider.getStations(
                            dependencies.downloader,
                            getSearchUrl(searchQuery!!),
                            getCacheType(dependencies)
                    )
            )
            handleDataLoaded(playbackStateListener, dependencies, list)
        }
    }

    companion object {
        private val LOG_TAG = MediaItemSearchFromApp::class.java.simpleName
    }
}
