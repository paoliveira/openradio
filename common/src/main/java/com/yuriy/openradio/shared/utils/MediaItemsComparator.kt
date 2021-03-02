/*
 * Copyright 2017 The "Open Radio" Project. Author:
 * Chernyshov Yuriy
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
package com.yuriy.openradio.shared.utils

import android.support.v4.media.MediaBrowserCompat
import com.yuriy.openradio.shared.utils.MediaItemHelper.getSortIdField
import java.util.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/05/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * This class designed in a way to provide sort functionality for the
 * [MediaBrowserCompat.MediaItem]s.
 */
class MediaItemsComparator : Comparator<MediaBrowserCompat.MediaItem> {
    override fun compare(mediaItem1: MediaBrowserCompat.MediaItem?,
                         mediaItem2: MediaBrowserCompat.MediaItem?): Int {
        val sortId1 = getSortIdField(mediaItem1)
        val sortId2 = getSortIdField(mediaItem2)
        return sortId1.compareTo(sortId2)
    }
}
