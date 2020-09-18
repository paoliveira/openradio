/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy [chernyshov.yuriy@gmail.com]
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

package com.yuriy.openradio.shared.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import com.yuriy.openradio.shared.model.LifecycleModel;
import com.yuriy.openradio.shared.utils.AppLogger;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public final class BackgroundService extends JobIntentService {

    private static final String CLASS_NAME = BackgroundService.class.getSimpleName() + " ";
    private static final int JOB_ID = 2000;
    private static final String KEY_CMD = "KEY_COMMAND";
    private static final String KEY_CMD_NAME_STOP_ORS_FROM_DESTROY = "KEY_CMD_NAME_STOP_ORS";
    @Inject
    LifecycleModel mLifecycleModel;

    public BackgroundService() {
        super();
    }

    /**
     * Factory method to make the desired Intent.
     */
    private static Intent makeIntent(final Context context) {
        // Create an intent associated with the Background Service class.
        return new Intent(context, BackgroundService.class);
    }

    /**
     * Factory method to create intent to stop {@link OpenRadioService} when UI is destroyed.
     *
     * @param context Context of callee.
     */
    public static void makeIntentStopServiceFromDestroy(final Context context) {
        // Create an Intent to start job in the background via a Service.
        final Intent intent = makeIntent(context);
        intent.putExtra(KEY_CMD, KEY_CMD_NAME_STOP_ORS_FROM_DESTROY);
        enqueueWork(context, BackgroundService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        AppLogger.d(CLASS_NAME + "on handle work");
        if (!intent.hasExtra(KEY_CMD)) {
            return;
        }
        switch (intent.getStringExtra(KEY_CMD)) {
            case KEY_CMD_NAME_STOP_ORS_FROM_DESTROY:
                final Lifecycle.Event event = mLifecycleModel.getEvent();
                AppLogger.d("App state " + event);
                // This means stop was invoked when destroy was invoked from very background state.
                // For instance, when application consumed too much resources and system decided to free resources.
                if (mLifecycleModel.isAppInBg()) {
                    AppLogger.e("Kill app process.");
                    android.os.Process.killProcess(android.os.Process.myPid());
                    break;
                }
                ContextCompat.startForegroundService(
                        getApplicationContext(),
                        OpenRadioService.makeStopServiceIntent(getApplicationContext())
                );
                break;
        }
    }
}
