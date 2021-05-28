/*
 * Copyright 2015-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.yuriy.openradio.shared.model.api.ApiServiceProvider
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.storage.RadioStationsStorage
import com.yuriy.openradio.shared.service.OpenRadioService.ResultListener
import com.yuriy.openradio.shared.utils.MediaItemsComparator
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 10/13/15
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [MediaItemCommandDependencies] is a class that designed to keep all necessary references that are
 * shared between implementations of the [MediaItemCommand] interface, includes application
 * context, name of the current category, list of all categories, etc ...
 */
class MediaItemCommandDependencies(
    /**
     * Context of the application.
     */
    val context: Context,
    val downloader: Downloader,
    val result: MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>>,
    val radioStationsStorage: RadioStationsStorage,
    val serviceProvider: ApiServiceProvider,
    val countryCode: String,
    val parentId: String,
    val isAndroidAuto: Boolean,
    val isSameCatalogue: Boolean,
    val isSavedInstance: Boolean,
    val resultListener: ResultListener,
    val options: Bundle,
    val radioStationsComparator: Comparator<RadioStation>) {

    private val mMediaItems: MutableList<MediaBrowserCompat.MediaItem>
    private val mMediaItemsComparator: Comparator<MediaBrowserCompat.MediaItem>

    fun addMediaItem(item: MediaBrowserCompat.MediaItem) {
        mMediaItems.add(item)
    }

    /**
     *
     * @return
     */
    val mediaItems: List<MediaBrowserCompat.MediaItem>
        get() {
            Collections.sort(mMediaItems, mMediaItemsComparator)
            radioStationsStorage.sort(radioStationsComparator)
            return mMediaItems
        }

    /**
     * Main constructor.
     */
    init {
        mMediaItemsComparator = MediaItemsComparator()
        mMediaItems = ArrayList()
    }
}
