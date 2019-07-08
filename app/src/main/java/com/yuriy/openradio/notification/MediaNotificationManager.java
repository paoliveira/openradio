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

package com.yuriy.openradio.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Simplifies common {@link Notification} tasks.
 */
public final class MediaNotificationManager {

    public MediaNotificationManager() {
        super();
    }

    static String createNotificationChannel(
            final Context context,
            final MediaNotificationData notificationData) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the channel.
            final String channelId = notificationData.getChannelId();
            // The user-visible name of the channel.
            final CharSequence channelName = notificationData.getChannelName();
            // The user-visible description of the channel.
            final String channelDescription = notificationData.getChannelDescription();
            final int channelImportance = notificationData.getChannelImportance();
            final boolean channelEnableVibrate = notificationData.getChannelEnableVibrate();
            final int channelLocksScreenVisibility =
                    notificationData.getChannelLockScreenVisibility();
            // Initializes NotificationChannel.
            final NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, channelName, channelImportance);
            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableVibration(channelEnableVibrate);
            notificationChannel.setLockscreenVisibility(channelLocksScreenVisibility);
            // Keep this nulls to suspend bug in Android O when each notification provides with a sound
            notificationChannel.setSound(null, null);
            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            final NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
            return channelId;
        } else {
            // Returns null for pre-O (26) devices.
            return null;
        }
    }

    static String createNotificationChannelNoStream(
            final Context context,
            final NoMediaNotificationData notificationData) {

        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The id of the channel.
            final String channelId = notificationData.getChannelId();
            // The user-visible name of the channel.
            final CharSequence channelName = notificationData.getChannelName();
            // The user-visible description of the channel.
            final String channelDescription = notificationData.getChannelDescription();
            final int channelImportance = notificationData.getChannelImportance();
            final boolean channelEnableVibrate = notificationData.getChannelEnableVibrate();
            final int channelLocksScreenVisibility =
                    notificationData.getChannelLockScreenVisibility();
            // Initializes NotificationChannel.
            final NotificationChannel notificationChannel =
                    new NotificationChannel(channelId, channelName, channelImportance);
            notificationChannel.setDescription(channelDescription);
            notificationChannel.enableVibration(channelEnableVibrate);
            notificationChannel.setLockscreenVisibility(channelLocksScreenVisibility);
            // Keep this nulls to suspend bug in Android O when each notification provides with a sound
            notificationChannel.setSound(null, null);
            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            final NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
            return channelId;
        } else {
            // Returns null for pre-O (26) devices.
            return null;
        }
    }
}
