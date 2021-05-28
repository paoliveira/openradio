/*
 * Copyright 2018-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.utils.AppLogger
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 14/01/18
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class IndexableMediaItemCommand internal constructor() : MediaItemCommandImpl() {

    /**
     * Index of the current page of the Radio Stations List.
     */
    private val mPageIndex: AtomicInteger = AtomicInteger(UrlBuilder.FIRST_PAGE_INDEX)

    override fun execute(playbackStateListener: IUpdatePlaybackState, dependencies: MediaItemCommandDependencies) {
        super.execute(playbackStateListener, dependencies)
        AppLogger.d("$CLASS_NAME invoked")
        if (!dependencies.isSameCatalogue) {
            mPageIndex.set(UrlBuilder.FIRST_PAGE_INDEX)
        }
    }

    override fun doLoadNoDataReceived(): Boolean {
        return mPageIndex.get() == UrlBuilder.FIRST_PAGE_INDEX + 1
    }

    val pageNumber: Int
        get() = mPageIndex.getAndIncrement()

    companion object {
        private val CLASS_NAME = IndexableMediaItemCommand::class.java.simpleName
    }
}
