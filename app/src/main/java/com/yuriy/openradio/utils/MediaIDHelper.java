/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.utils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.text.TextUtils;

/**
 * Utility class to help on queue related tasks.
 * Media IDs used on browseable items of {@link android.media.browse.MediaBrowser}.
 */
public final class MediaIDHelper {

    public static final String MEDIA_ID_ROOT = "__ROOT__";

    public static final String MEDIA_ID_ALL_CATEGORIES = "__ALL_CATEGORIES__";

    public static final String MEDIA_ID_COUNTRY_STATIONS = "__COUNTRY_STATIONS__";

    public static final String MEDIA_ID_COUNTRIES_LIST = "__COUNTRIES_LIST__";

    public static final String MEDIA_ID_FAVORITES_LIST = "__FAVORITES_LIST__";

    public static final String MEDIA_ID_LOCAL_RADIO_STATIONS_LIST
            = "__MEDIA_ID_LOCAL_RADIO_STATIONS_LIST__";

    public static final String MEDIA_ID_PARENT_CATEGORIES = "__PARENT_CATEGORIES__";

    public static final String MEDIA_ID_CHILD_CATEGORIES = "__CHILD_CATEGORIES__";

    public static final String MEDIA_ID_RADIO_STATIONS_IN_CATEGORY
            = "__RADIO_STATIONS_IN_CATEGORY__";

    public static final String MEDIA_ID_SEARCH_FROM_APP = "__SEARCH_FROM_APP__";

    public static final String MEDIA_ID_ALL_STATIONS = "__ALL_STATIONS__";

    /**
     *
     */
    private static final String[] IDS = {
            MEDIA_ID_ALL_CATEGORIES,
            MEDIA_ID_CHILD_CATEGORIES,
            MEDIA_ID_COUNTRIES_LIST,
            MEDIA_ID_COUNTRY_STATIONS,
            MEDIA_ID_FAVORITES_LIST,
            MEDIA_ID_LOCAL_RADIO_STATIONS_LIST,
            MEDIA_ID_PARENT_CATEGORIES,
            MEDIA_ID_RADIO_STATIONS_IN_CATEGORY,
            MEDIA_ID_ROOT,
            MEDIA_ID_SEARCH_FROM_APP,
            MEDIA_ID_ALL_STATIONS
    };

    /**
     *
     * @param startsWith
     * @return
     */
    public static String getId(final String startsWith) {
        for (final String id : IDS) {
            if (startsWith.startsWith(id) || startsWith.equals(id)) {
                return id;
            }
        }
        return null;
    }
}
