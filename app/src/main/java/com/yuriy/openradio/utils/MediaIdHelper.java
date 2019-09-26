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

package com.yuriy.openradio.utils;

import android.text.TextUtils;

import com.yuriy.openradio.model.media.item.MediaItemCommand;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Utility class to help on queue related tasks.
 * Media IDs used on browseable items of {@link android.media.browse.MediaBrowser}.
 */
public final class MediaIdHelper {

    public static final String MEDIA_ID_ROOT = "__ROOT__";

    public static final String MEDIA_ID_ALL_CATEGORIES = "__ALL_CATEGORIES__";

    public static final String MEDIA_ID_COUNTRY_STATIONS = "__COUNTRY_STATIONS__";

    public static final String MEDIA_ID_COUNTRIES_LIST = "__COUNTRIES_LIST__";

    public static final String MEDIA_ID_FAVORITES_LIST = "__FAVORITES_LIST__";

    public static final String MEDIA_ID_LOCAL_RADIO_STATIONS_LIST
            = "__MEDIA_ID_LOCAL_RADIO_STATIONS_LIST__";

    public static final String MEDIA_ID_CHILD_CATEGORIES = "__CHILD_CATEGORIES__";

    public static final String MEDIA_ID_RADIO_STATIONS_IN_CATEGORY
            = "__RADIO_STATIONS_IN_CATEGORY__";

    public static final String MEDIA_ID_SEARCH_FROM_APP = "__SEARCH_FROM_APP__";

    public static final String MEDIA_ID_POPULAR_STATIONS = "__POPULAR_STATIONS__";

    public static final String MEDIA_ID_ALL_STATIONS = "__ALL_STATIONS__";

    public static final String MEDIA_ID_RECENT_PLAYED_SONGS = "__RECENT_PLAYED_SONGS__";

    public static final String MEDIA_ID_RECENT_ADDED_STATIONS = "__RECENT_ADDED_STATIONS__";

    public static final String MEDIA_ID_LIST_ENDED = "MEDIA_ID_LIST_ENDED";

    /**
     *
     */
    private static final String[] IDS = {
            MEDIA_ID_ALL_CATEGORIES,
            MEDIA_ID_COUNTRIES_LIST,
            MEDIA_ID_COUNTRY_STATIONS,
            MEDIA_ID_FAVORITES_LIST,
            MEDIA_ID_LOCAL_RADIO_STATIONS_LIST,
            MEDIA_ID_CHILD_CATEGORIES,
            MEDIA_ID_RADIO_STATIONS_IN_CATEGORY,
            MEDIA_ID_ROOT,
            MEDIA_ID_SEARCH_FROM_APP,
            MEDIA_ID_ALL_STATIONS,
            MEDIA_ID_POPULAR_STATIONS,
            MEDIA_ID_RECENT_PLAYED_SONGS,
            MEDIA_ID_RECENT_ADDED_STATIONS
    };

    /**
     * Default constructor.
     */
    private MediaIdHelper() {
        super();
    }

    /**
     * Gets Id that is use to extract correct command implementation of the
     * {@link MediaItemCommand}.
     *
     * @param value String pattern that represents loaded menu item.
     *
     * @return Extracted Id.
     */
    public static String getId(final String value) {
        if (value == null) {
            return null;
        }
        for (final String id : IDS) {
            if (value.startsWith(id) || value.equals(id)) {
                if (!TextUtils.isEmpty(getCountryCode(value))) {
                    return MEDIA_ID_COUNTRY_STATIONS;
                }
                return id;
            }
        }
        return null;
    }

    /**
     * Checks whether provided category Id is belongs to the "Stations in the Country" Id.
     * If {@code true} - extract Country Code and return it, in case of {@code false} - return null.
     *
     * @param value Category Id.
     *
     * @return The value of the Country Code, {@code null} - otherwise.
     */
    public static String getCountryCode(final String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        if (!value.startsWith(MEDIA_ID_COUNTRIES_LIST) || value.equals(MEDIA_ID_COUNTRIES_LIST)) {
            return null;
        }
        final String result = value.substring(value.length() - 2);
        if (!TextUtils.isEmpty(value) && result.length() == 2) {
            return result.toUpperCase();
        }
        return null;
    }

    /**
     * Returns whether or not current Catalog is sortable, for instance Favorites or
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return {@code true} in case of Catalog is sortable, {@code false} otherwise.
     */
    public static boolean isMediaIdSortable(final String categoryMediaId) {
        return MEDIA_ID_FAVORITES_LIST.equals(categoryMediaId)
                || MEDIA_ID_LOCAL_RADIO_STATIONS_LIST.equals(categoryMediaId);
    }

    /**
     * Returns whether or not current Catalog is refreshable once end of list reached, for instance
     * Local Radio Stations.
     *
     * @param categoryMediaId Category Media Id.
     * @return {@code true} in case of Catalog is refreshable, {@code false} otherwise.
     */
    public static boolean isMediaIdRefreshable(final String categoryMediaId) {
        return !TextUtils.isEmpty(categoryMediaId)
                && (MEDIA_ID_ALL_STATIONS.equals(categoryMediaId)
                || MEDIA_ID_COUNTRY_STATIONS.equals(categoryMediaId)
                || MEDIA_ID_RADIO_STATIONS_IN_CATEGORY.equals(categoryMediaId)
                || categoryMediaId.contains(MEDIA_ID_COUNTRIES_LIST)
                || categoryMediaId.contains(MEDIA_ID_CHILD_CATEGORIES));
    }
}
