/*
 * Copyright 2016 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public class AppLocalBroadcastReceiver extends BroadcastReceiver {

    /**
     * Tag name to use in the logging.
     */
    private static final String CLASS_NAME = AppLocalBroadcastReceiver.class.getSimpleName();

    /**
     * Action name for the "Disable Location service" on the mobile device.
     */
    private static final String ACTION_LOCATION_DISABLED = "ACTION_LOCATION_DISABLED";

    /**
     * Action name for the "Location receive country code" on the mobile device.
     */
    private static final String ACTION_LOCATION_COUNTRY_CODE = "ACTION_LOCATION_COUNTRY_CODE";

    /**
     *
     */
    private static final String ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED
            = "ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED";

    /**
     * Key value for the Country Code in the Intent's bundles.
     */
    private static final String KEY_COUNTRY_CODE = "KEY_COUNTRY_CODE";

    /**
     *
     */
    private static final String KEY_CURRENT_INDEX_ON_QUEUE = "KEY_CURRENT_INDEX_ON_QUEUE";

    private static final String KEY_CURRENT_MEDIA_ID_ON_QUEUE = "KEY_CURRENT_MEDIA_ID_ON_QUEUE";

    /**
     * Callback listener of the various events.
     */
    private AppLocalBroadcastReceiverCallback mCallback;

    /**
     * Private constructor.
     */
    private AppLocalBroadcastReceiver() { }

    /**
     * Register listener for the Local broadcast receiver actions. This listener pass events to
     * Activity class.
     *
     * @param callback Implementation of the {@link AppLocalBroadcastReceiverCallback}
     */
    public void registerListener(final AppLocalBroadcastReceiverCallback callback) {
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
        Log.i(CLASS_NAME, "On receive:" + intent);

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

        if (action.equals(ACTION_LOCATION_DISABLED)) {
            if (mCallback != null) {
                mCallback.onLocationDisabled();
            }
        }

        if (action.equals(ACTION_LOCATION_COUNTRY_CODE)) {
            final String countryCode = intent.getStringExtra(KEY_COUNTRY_CODE);
            if (countryCode != null && mCallback != null) {
                mCallback.onLocationCountryCode(countryCode);
            }
        }

        if (action.equals(ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED)) {
            final int currentIndex = intent.getIntExtra(KEY_CURRENT_INDEX_ON_QUEUE, 0);
            final String currentMediaId = intent.getStringExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE);
            if (mCallback != null) {
                mCallback.onCurrentIndexOnQueueChanged(currentIndex, currentMediaId);
            }
        }
    }

    /**
     * @return Name for the Location Disabled action.
     */
    public static String getActionLocationDisabled() {
        return ACTION_LOCATION_DISABLED;
    }

    /**
     * @return Name for the Location Country code action.
     */
    public static String getActionLocationCountryCode() {
        return ACTION_LOCATION_COUNTRY_CODE;
    }

    /**
     * @return Name for the Current Index on Queue Changed action.
     */
    public static String getActionCurrentIndexOnQueueChanged() {
        return ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED;
    }

    /**
     * @return Instance of the {@link Intent} that indicates about disabled Location Service.
     */
    public static Intent createIntentLocationDisabled() {
        return new Intent(ACTION_LOCATION_DISABLED);
    }

    /**
     * @return Instance of the {@link Intent} that indicates about country code received from
     * the Location Service.
     *
     * @param countryCode Country code.
     */
    public static Intent createIntentLocationCountryCode(final String countryCode) {
        final Intent intent = new Intent(ACTION_LOCATION_COUNTRY_CODE);
        intent.putExtra(KEY_COUNTRY_CODE, countryCode);
        return intent;
    }

    /**
     * @return Instance of the {@link Intent} that indicates Current Index of the queue item.
     *
     * @param currentIndex Index of the current selected item in the queue.
     * @param mediaId
     */
    public static Intent createIntentCurrentIndexOnQueue(final int currentIndex,
                                                         final String mediaId) {
        final Intent intent = new Intent(ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED);
        intent.putExtra(KEY_CURRENT_INDEX_ON_QUEUE, currentIndex);
        intent.putExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE, mediaId);
        return intent;
    }

    /**
     * Factory method to create default instance.
     *
     * @return Instance of the {@link AppLocalBroadcastReceiver}
     */
    public static AppLocalBroadcastReceiver getInstance() {
        return new AppLocalBroadcastReceiver();
    }
}
