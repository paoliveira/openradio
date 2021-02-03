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

package com.yuriy.openradio.gabor.shared.notification;

import android.content.Context;

import androidx.annotation.NonNull;

import com.yuriy.openradio.gabor.R;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class ServiceStartedNotificationData extends NotificationData {

    public static final String CHANNEL_ID = "channel_id_2";

    ServiceStartedNotificationData(@NonNull final Context context) {
        super();
        setContentTitle(context.getString(R.string.notification_str));
        setContentText(context.getString(R.string.notification_str));
        setChannelId(CHANNEL_ID);
        setChannelName(context.getString(R.string.radio_station_str));
        setChannelDescription("Radio Station just started");
    }
}
