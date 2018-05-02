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

package com.yuriy.openradio;

import android.app.Application;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.yuriy.openradio.business.storage.AppPreferencesManager;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.utils.AppUtils;

import io.fabric.sdk.android.Fabric;

/**
 * Created with Android Studio.
 * User: Yuriy Chernyshov
 * Date: 12/21/13
 * Time: 6:29 PM
 */
public final class MainApp extends Application {

    /**
     * Constructor.
     */
    public MainApp() {
        super();
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        final Thread thread = new Thread(
                () -> {
                    final boolean isLoggingEnabled = AppPreferencesManager.areLogsEnabled(
                            getApplicationContext()
                    );
                    AppLogger.initLogger(getApplicationContext());
                    AppLogger.setIsLoggingEnabled(isLoggingEnabled);
                    printFirstLogMessage();

                    Fabric.with(getApplicationContext(), new Crashlytics());
                }
        );
        thread.start();
    }

    /**
     * Print first log message with summary information about device and application.
     */
    @SuppressWarnings("all")
    private void printFirstLogMessage() {
        final StringBuilder firstLogMessage = new StringBuilder();
        firstLogMessage.append("\n");
        firstLogMessage.append("########### Create '");
        firstLogMessage.append(getString(R.string.app_name));
        firstLogMessage.append("' Application ###########\n");
        firstLogMessage.append("- processors: ");
        firstLogMessage.append(Runtime.getRuntime().availableProcessors());
        firstLogMessage.append("\n");
        firstLogMessage.append("- version: ");
        firstLogMessage.append(AppUtils.getApplicationVersionCode(this));
        firstLogMessage.append(".");
        firstLogMessage.append(AppUtils.getApplicationVersionName(this));
        firstLogMessage.append("\n");
        firstLogMessage.append("- OS ver: ");
        firstLogMessage.append(Build.VERSION.RELEASE);
        firstLogMessage.append("\n");
        firstLogMessage.append("- API level: ");
        firstLogMessage.append(Build.VERSION.SDK_INT);
        firstLogMessage.append("\n");
        firstLogMessage.append("- Country: ");
        firstLogMessage.append(AppUtils.getUserCountry(this));

        AppLogger.i(firstLogMessage.toString());
    }
}
