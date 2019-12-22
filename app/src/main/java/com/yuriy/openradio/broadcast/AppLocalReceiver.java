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

package com.yuriy.openradio.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yuriy.openradio.shared.broadcast.AppLocalBroadcast;
import com.yuriy.openradio.shared.utils.AppLogger;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class AppLocalReceiver extends BroadcastReceiver {

    /**
     * Tag name to use in the logging.
     */
    private static final String CLASS_NAME = AppLocalReceiver.class.getSimpleName();

    /**
     * Key value for the Currently selected index in the Intent's bundles.
     */
    private static final String KEY_CURRENT_INDEX_ON_QUEUE = "KEY_CURRENT_INDEX_ON_QUEUE";

    /**
     * Key value for the Currently selected Media Id in the Intent's bundles.
     */
    private static final String KEY_CURRENT_MEDIA_ID_ON_QUEUE = "KEY_CURRENT_MEDIA_ID_ON_QUEUE";

    /**
     * Callback listener of the various events.
     */
    private AppLocalReceiverCallback mCallback;

    /**
     * Private constructor.
     */
    private AppLocalReceiver() { }

    /**
     * Register listener for the Local broadcast receiver actions. This listener pass events to
     * Activity class.
     *
     * @param callback Implementation of the {@link AppLocalReceiverCallback}
     */
    public void registerListener(final AppLocalReceiverCallback callback) {
        mCallback = callback;
    }

    /**
     * Unregister listener for the Local broadcast receiver actions.
     */
    public void unregisterListener() {
        mCallback = null;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        AppLogger.i(CLASS_NAME + " On receive:" + intent);

        if (intent == null) {
            return;
        }
        if (context == null) {
            return;
        }

        final String action = intent.getAction();
        if (action == null || action.isEmpty()) {
            return;
        }

        if (action.equals(AppLocalBroadcast.getActionLocationDisabled())) {
            if (mCallback != null) {
                mCallback.onLocationDisabled();
            }
        }

        if (action.equals(AppLocalBroadcast.getActionCurrentIndexOnQueueChanged())) {
            final int currentIndex = intent.getIntExtra(KEY_CURRENT_INDEX_ON_QUEUE, 0);
            final String currentMediaId = intent.getStringExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE);
            if (mCallback != null) {
                mCallback.onCurrentIndexOnQueueChanged(currentIndex, currentMediaId);
            }
        }
    }

    /**
     * Factory method to create default instance.
     *
     * @return Instance of the {@link AppLocalReceiver}
     */
    public static AppLocalReceiver getInstance() {
        return new AppLocalReceiver();
    }
}
