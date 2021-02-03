/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.gabor.shared;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.yuriy.openradio.gabor.R;
import com.yuriy.openradio.gabor.shared.model.storage.AppPreferencesManager;
import com.yuriy.openradio.gabor.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.gabor.shared.utils.AnalyticsUtils;
import com.yuriy.openradio.gabor.shared.utils.AppLogger;
import com.yuriy.openradio.gabor.shared.utils.AppUtils;
import com.yuriy.openradio.gabor.shared.utils.FileUtils;
import com.yuriy.openradio.gabor.shared.vo.RadioStation;

import java.util.List;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Created with Android Studio.
 * User: Yuriy Chernyshov
 * Date: 12/21/13
 * Time: 6:29 PM
 */
@HiltAndroidApp
public final class MainApp extends MultiDexApplication {

    /**
     * Tag string to use in logging message.
     */
    private static final String CLASS_NAME = MainApp.class.getSimpleName() + " ";

    /**
     * Constructor.
     */
    public MainApp() {
        super();
    }

    @Override
    protected void attachBaseContext(@NonNull final Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        AnalyticsUtils.init();
        AppLogger.d(CLASS_NAME + "OnCreate");
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        final Context context = this;
        final Thread thread = new Thread(
                () -> {
                    final boolean isLoggingEnabled = AppPreferencesManager.areLogsEnabled(
                            context
                    );
                    AppLogger.initLogger(context);
                    AppLogger.setLoggingEnabled(isLoggingEnabled);
                    printFirstLogMessage(context);
                    correctBufferSettings(context);
                    migrateImagesToIntStorage(context);
                }
        );
        thread.setName("Init-thread");
        thread.start();
    }

    /**
     * Print first log message with summary information about device and application.
     */
    @SuppressWarnings("all")
    private static void printFirstLogMessage(final Context context) {
        final String[] densities = AppUtils.getDensity(context);
        final StringBuilder firstLogMessage = new StringBuilder();
        firstLogMessage.append("\n");
        firstLogMessage.append("########### Create '");
        firstLogMessage.append(context.getString(R.string.app_name));
        firstLogMessage.append("' Application ###########\n");
        firstLogMessage.append("- processors: ");
        firstLogMessage.append(Runtime.getRuntime().availableProcessors());
        firstLogMessage.append("\n");
        firstLogMessage.append("- version: ");
        firstLogMessage.append(AppUtils.getApplicationVersionName(context));
        firstLogMessage.append(".");
        firstLogMessage.append(AppUtils.getApplicationVersionCode(context));
        firstLogMessage.append("\n");
        firstLogMessage.append("- OS ver: ");
        firstLogMessage.append(Build.VERSION.RELEASE);
        firstLogMessage.append("\n");
        firstLogMessage.append("- API level: ");
        firstLogMessage.append(Build.VERSION.SDK_INT);
        firstLogMessage.append("\n");
        firstLogMessage.append("- Density: ");
        firstLogMessage.append(densities[0]).append(" ").append(densities[1]);
        firstLogMessage.append("\n");
        firstLogMessage.append("- Country: ");
        firstLogMessage.append(AppUtils.getUserCountry(context));

        AppLogger.i(firstLogMessage.toString());
    }

    /**
     * Correct mal formatted values entered by user.
     *
     * @param context Context of a callee.
     */
    private static void correctBufferSettings(final Context context) {
        final int maxBufferMs = AppPreferencesManager.getMaxBuffer(context);
        final int minBufferMs = AppPreferencesManager.getMinBuffer(context);
        final int playBufferMs = AppPreferencesManager.getPlayBuffer(context);
        final int playBufferRebufferMs = AppPreferencesManager.getPlayBufferRebuffer(context);

        if (maxBufferMs < minBufferMs) {
            AppPreferencesManager.setMaxBuffer(context, DefaultLoadControl.DEFAULT_MAX_BUFFER_MS);
            AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS);
        }
        if (minBufferMs < playBufferMs) {
            AppPreferencesManager.setPlayBuffer(
                    context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS
            );
            AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS);
        }
        if (minBufferMs < playBufferRebufferMs) {
            AppPreferencesManager.setPlayBufferRebuffer(
                    context, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            );
            AppPreferencesManager.setMinBuffer(context, DefaultLoadControl.DEFAULT_MIN_BUFFER_MS);
        }
    }

    /**
     * @param context Context of a callee.
     */
    private static void migrateImagesToIntStorage(final Context context) {
        AppLogger.d(CLASS_NAME + "Migrate image to int. storage started");
        final List<RadioStation> list = LocalRadioStationsStorage.getAllLocals(context);
        String imageUrl;
        String imageUrlLocal;
        final String filesDir = FileUtils.getFilesDir(context).getAbsolutePath();
        for (final RadioStation radioStation : list) {
            imageUrl = radioStation.getImageUrl();
            if (TextUtils.isEmpty(imageUrl)) {
                continue;
            }
            if (imageUrl.contains(filesDir)) {
                continue;
            }
            imageUrlLocal = FileUtils.copyExtFileToIntDir(context, imageUrl);
            if (imageUrlLocal == null) {
                imageUrlLocal = imageUrl;
            }
            radioStation.setImageUrl(imageUrlLocal);
            radioStation.setThumbUrl(imageUrlLocal);

            LocalRadioStationsStorage.add(radioStation, context);
        }
        AppLogger.d(CLASS_NAME + "Migrate image to int. storage completed");
    }
}
