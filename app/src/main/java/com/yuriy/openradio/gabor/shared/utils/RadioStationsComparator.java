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

package com.yuriy.openradio.gabor.shared.utils;

import android.support.v4.media.MediaBrowserCompat;

import com.yuriy.openradio.gabor.shared.vo.RadioStation;

import java.util.Comparator;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 06/05/17
 * E-Mail: chernyshov.yuriy@gmail.com
 * <p>
 * This class designed in a way to provide sort functionality for the
 * {@link MediaBrowserCompat.MediaItem}s.
 */
public final class RadioStationsComparator implements Comparator<RadioStation> {

    /**
     * Default constructor.
     */
    public RadioStationsComparator() {
        super();
    }

    @Override
    public final int compare(final RadioStation radioStation1,
                             final RadioStation radioStation2) {
        final int sortId1 = radioStation1 == null ? -1 : radioStation1.getSortId();
        final int sortId2 = radioStation1 == null ? -1 : radioStation2.getSortId();
        return Integer.compare(sortId1, sortId2);
    }
}
