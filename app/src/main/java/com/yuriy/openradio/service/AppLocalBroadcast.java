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

package com.yuriy.openradio.service;

import android.content.Intent;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class AppLocalBroadcast {

    /**
     * Tag name to use in the logging.
     */
    private static final String CLASS_NAME = AppLocalBroadcast.class.getSimpleName();

    /**
     * Action name for the "Disable Location service" on the mobile device.
     */
    private static final String ACTION_LOCATION_DISABLED = "ACTION_LOCATION_DISABLED";

    /**
     * Action name for the "Location receive country code" on the mobile device.
     */
    private static final String ACTION_LOCATION_COUNTRY_CODE = "ACTION_LOCATION_COUNTRY_CODE";

    /**
     * Action name for the "Current index on queue" changed,
     * when currently selected Radio Station was changed.
     */
    private static final String ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED
            = "ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED";

    /**
     * Action name for the "Master Volume Changed" event,
     * when volume of application's player was changed.
     */
    private static final String ACTION_MASTER_VOLUME_CHANGED = "ACTION_MASTER_VOLUME_CHANGED";

    /**
     * Key value for the Country Code in the Intent's bundles.
     */
    private static final String KEY_COUNTRY_CODE = "KEY_COUNTRY_CODE";

    /**
     * Key value for the Currently selected index in the Intent's bundles.
     */
    private static final String KEY_CURRENT_INDEX_ON_QUEUE = "KEY_CURRENT_INDEX_ON_QUEUE";

    /**
     * Key value for the Currently selected Media Id in the Intent's bundles.
     */
    private static final String KEY_CURRENT_MEDIA_ID_ON_QUEUE = "KEY_CURRENT_MEDIA_ID_ON_QUEUE";

    /**
     * Private constructor.
     */
    private AppLocalBroadcast() { }

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
     * @return Name for the Master Volume Changed action.
     */
    public static String getActionMasterVolumeChanged() {
        return ACTION_MASTER_VOLUME_CHANGED;
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
     * @param mediaId      Id of the Media Item.
     */
    public static Intent createIntentCurrentIndexOnQueue(final int currentIndex,
                                                         final String mediaId) {
        final Intent intent = new Intent(ACTION_CURRENT_INDEX_ON_QUEUE_CHANGED);
        intent.putExtra(KEY_CURRENT_INDEX_ON_QUEUE, currentIndex);
        intent.putExtra(KEY_CURRENT_MEDIA_ID_ON_QUEUE, mediaId);
        return intent;
    }

    /**
     * @return Intent to associate with master volume changed event.
     */
    public static Intent createIntentMasterVolumeChanged() {
        return new Intent(ACTION_MASTER_VOLUME_CHANGED);
    }

    /**
     * Factory method to create default instance.
     *
     * @return Instance of the {@link AppLocalBroadcast}
     */
    public static AppLocalBroadcast getInstance() {
        return new AppLocalBroadcast();
    }
}
