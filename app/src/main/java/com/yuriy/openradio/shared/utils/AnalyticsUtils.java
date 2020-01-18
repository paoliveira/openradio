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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.yuriy.openradio.BuildConfig;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 7/26/16
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *  A helper class designed to assist with Analytics APIs.
 */
public final class AnalyticsUtils {

    public static final String EVENT_NAME_GOOGLE_DRIVE = "GoogleDrive";
    private static FirebaseAnalytics sFirebaseAnalytics;

    /**
     * Default constructor.
     */
    private AnalyticsUtils() {
        super();
    }

    public static void init(final Context context) {
        sFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        final Bundle bundle = new Bundle();
        sFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle);

        final CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build();
        Fabric.with(context, new Crashlytics.Builder().core(crashlyticsCore).build());
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

    public static void logMessage(final String message) {
        if (!Fabric.isInitialized()) {
            return;
        }
        Crashlytics.log(message);
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
