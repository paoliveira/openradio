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

package com.yuriy.openradio.business;

import android.support.v4.media.MediaBrowserCompat;

import com.yuriy.openradio.utils.MediaItemHelper;

import java.util.Comparator;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/05/17
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * This class designed in a way to provide sort functionality for the
 * {@link android.support.v4.media.MediaBrowserCompat.MediaItem}s.
 */
public final class MediaItemsComparator implements Comparator<MediaBrowserCompat.MediaItem> {

    /**
     * Default constructor.
     */
    public MediaItemsComparator() {
        super();
    }

    @Override
    public final int compare(final MediaBrowserCompat.MediaItem o1,
                             final MediaBrowserCompat.MediaItem o2) {
        final int sortId1 = MediaItemHelper.getSortIdField(o1);
        final int sortId2 = MediaItemHelper.getSortIdField(o2);
        if (sortId2 > sortId1) {
            return -1;
        }
        if (sortId2 < sortId1) {
            return 1;
        }
        return 0;
    }
}
