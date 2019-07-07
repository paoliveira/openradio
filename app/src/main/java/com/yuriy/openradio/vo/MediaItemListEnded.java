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

package com.yuriy.openradio.vo;

import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;

import com.yuriy.openradio.utils.MediaIdHelper;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/08/18
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * This class is a value object to indicate end of the indexed list.
 */
public class MediaItemListEnded extends MediaBrowserCompat.MediaItem {

    /**
     * Default constructor.
     */
    public MediaItemListEnded() {
        this(new MediaDescriptionCompat.Builder()
                .setMediaId(MediaIdHelper.MEDIA_ID_LIST_ENDED)
                .build(), 1);
    }

    /**
     *
     * @param description
     * @param flags
     */
    private MediaItemListEnded(@NonNull final MediaDescriptionCompat description, final int flags) {
        super(description, flags);
    }
}
