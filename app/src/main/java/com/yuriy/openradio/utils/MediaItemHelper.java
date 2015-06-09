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

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Bundle;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/4/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class MediaItemHelper {

    private static final String KEY_IS_FAVORITE = "KEY_IS_FAVORITE";

    /**
     *
     * @param mediaItem  {@link android.media.browse.MediaBrowser.MediaItem}.
     * @param isFavorite Whether Item is in Favorites.
     */
    public static void updateFavoriteField(final MediaBrowser.MediaItem mediaItem,
                                           final boolean isFavorite) {
        if (mediaItem == null) {
            return;
        }
        final MediaDescription mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        if (bundle == null) {
            return;
        }
        bundle.putBoolean(KEY_IS_FAVORITE, isFavorite);
    }

    /**
     *
     * @param mediaItem {@link android.media.browse.MediaBrowser.MediaItem}.
     * @return True is Item is Favorite, False - otherwise.
     */
    public static boolean isFavoriteField(final MediaBrowser.MediaItem mediaItem) {
        if (mediaItem == null) {
            return false;
        }
        final MediaDescription mediaDescription = mediaItem.getDescription();
        final Bundle bundle = mediaDescription.getExtras();
        return bundle != null && bundle.getBoolean(KEY_IS_FAVORITE, false);
    }
}
