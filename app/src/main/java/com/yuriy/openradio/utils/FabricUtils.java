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
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/26/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *  A helper class designed to assist with API Crashlytics provided.
 */
public final class FabricUtils {

    public static final String EVENT_NAME_API_EXEC = "ApiExecutor";
    public static final String EVENT_NAME_GOOGLE_DRIVE = "GoogleDrive";
    public static final String EVENT_NAME_REMOTE_CONTROL_RECEIVER_RECEIVED = "RemoteControlReceiver received";

    /**
     * Default constructor.
     */
    private FabricUtils() {
        super();
    }

    /**
     * All logged exceptions will appear as “non-fatal” issues in the Fabric dashboard.
     *
     * @param exception Exception.
     */
    public static void logException(final Exception exception) {
        AppLogger.e(Log.getStackTraceString(exception));
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.logException(exception);
    }

    /**
     * Logs custom event to the Fabric dashboard.
     *
     * @param name Event name.
     */
    public static void logCustomEvent(final String name, final String key, final String value) {
        final boolean isFabricInit = Fabric.isInitialized();
        AppLogger.d("Ev:" + name + ", key:" + key + ", val:" + value + ", FabricInit:" + isFabricInit);
        if (!isFabricInit) {
            return;
        }
        Answers.getInstance().logCustom(new CustomEvent(name).putCustomAttribute(key, value));
    }

    public static void logCustomEvent(final String name, final String key, final Number value) {
        final boolean isFabricInit = Fabric.isInitialized();
        AppLogger.d("Ev:" + name + ", key:" + key + ", val:" + value + ", FabricInit:" + isFabricInit);
        if (!isFabricInit) {
            return;
        }
        Answers.getInstance().logCustom(new CustomEvent(name).putCustomAttribute(key, value));
    }
}
