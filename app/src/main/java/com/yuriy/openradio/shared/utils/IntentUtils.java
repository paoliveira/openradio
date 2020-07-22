/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/20/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class IntentUtils {

    public static final int REQUEST_CODE_LOCATION_SETTINGS = 100;

    public static final int REQUEST_CODE_FILE_SELECTED = 101;

    /**
     * Private constructor
     */
    private IntentUtils() { }

    /**
     * Make intent to navigate to provided url.
     *
     * @param url Url to navigate to.
     * @return {@link Intent}.
     */
    public static Intent makeUrlBrowsableIntent(final String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    /**
     * Make Intent to open Location Service settings,
     *
     * @return {@link Intent}.
     */
    public static Intent makeOpenLocationSettingsIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    /**
     * Dump content of Intent's bundle into string.
     *
     * @param intent Intent to process.
     * @return String representation of bundles.
     */
    public static String intentBundleToString(@Nullable final Intent intent) {
        if (intent == null) {
            return "Intent[null]";
        }
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return "Bundle[null]";
        }
        final StringBuilder builder = new StringBuilder("Bundle[");
        try {
            for (final String key : bundle.keySet()) {
                builder.append(key).append(":").append((bundle.get(key) != null ? bundle.get(key) : "NULL"));
                builder.append("|");
            }
            builder.delete(builder.length() - 1, builder.length());
        } catch (final Exception e) {
            AppLogger.e("Intent's bundle to string exception:" + e);
        }
        builder.append("]");
        return builder.toString();
    }
}
