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

package com.yuriy.openradio.shared.utils;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yuriy.openradio.BuildConfig;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/26/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *  A helper class designed to assist with Analytics APIs.
 */
public final class AnalyticsUtils {

    /**
     * Default constructor.
     */
    private AnalyticsUtils() {
        super();
    }

    public static void init() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }

    /**
     * All logged exceptions will appear as “non-fatal” issues in the crash report dashboard.
     *
     * @param exception Exception.
     */
    public static void logException(final Exception exception) {
        AppLogger.e(Log.getStackTraceString(exception));
        FirebaseCrashlytics.getInstance().recordException(exception);
    }

    public static void logMessage(final String message) {
        FirebaseCrashlytics.getInstance().log(message);
    }
}
