/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/3/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link AppLocalBroadcastReceiverCallback} is an interface that provides various callback methods
 * of the Local Broadcast Receiver.
 */
public interface AppLocalBroadcastReceiverCallback {

    /**
     * Dispatches when Location Services detects as disabled.
     */
    void onLocationDisabled();

    /**
     * Dispatches when Location Services detects country code.
     *
     * @param countryCode Country code.
     */
    void onLocationCountryCode(final String countryCode);

    /**
     *
     * @param index
     * @param mediaId
     */
    void onCurrentIndexOnQueueChanged(final int index, final String mediaId);
}
