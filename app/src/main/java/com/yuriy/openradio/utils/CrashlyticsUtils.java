/*
 * Copyright 2017 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/26/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *  A helper class designed to assist with API Crashlytics provided.
 */
public final class CrashlyticsUtils {

    /**
     * Default constructor.
     */
    private CrashlyticsUtils() {
        super();
    }

    /**
     * All logged exceptions will appear as “non-fatal” issues in the Fabric dashboard.
     *
     * @param exception Exception.
     */
    public static void logException(final Exception exception) {
        AppLogger.e("Ex:" + Log.getStackTraceString(exception));
        if (Crashlytics.getInstance() == null) {
            return;
        }
        Crashlytics.logException(exception);
    }

    /**
     * Logs error to the Fabric dashboard.
     *
     * @param errorMessage Message associated with error.
     */
    public static void logError(final String errorMessage) {
        AppLogger.w("Er:" + errorMessage);
        if (Crashlytics.getInstance() == null) {
            return;
        }
        Crashlytics.log(errorMessage);
    }
}
