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

package com.yuriy.openradio.shared.notification;

import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import androidx.annotation.NonNull;

import com.yuriy.openradio.R;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class MediaNotificationData extends NotificationData {

    public static final String CHANNEL_ID = "channel_id_1";
    
    MediaNotificationData(@NonNull Context context,
                          @NonNull final MediaMetadataCompat metadata) {
        super();

        final MediaDescriptionCompat description = metadata.getDescription();
        if (description.getTitle() != null) {
            mContentTitle = description.getTitle().toString();
        } else {
            mContentTitle = context.getString(R.string.notification_str);
        }
        if (description.getSubtitle() != null) {
            mContentText = description.getSubtitle().toString();
        } else {
            mContentText = context.getString(R.string.notification_str);
        }

        mChannelId = CHANNEL_ID;
        mChannelName = context.getString(R.string.radio_station_str);
        mChannelDescription = "Updates about current Radio Station";
    }
}
