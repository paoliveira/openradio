/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Factory to build Notification Channels.
 */
public final class NotificationChannelFactory {

    private final NotificationManager mManager;
    private NotificationChannel mChannel;
    private NotificationChannel mChannelNoStream;

    public NotificationChannelFactory(final Context context) {
        super();
        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    void createChannel(final NotificationData data) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (mChannel != null) {
            return;
        }
        // The id of the channel.
        final String id = data.getChannelId();
        // The user-visible name of the channel.
        final CharSequence name = data.getChannelName();
        // The user-visible description of the channel.
        final String description = data.getChannelDescription();
        final int importance = data.getChannelImportance();
        final boolean enableVibrate = data.getChannelEnableVibrate();
        final int lockScreenVisibility = data.getChannelLockScreenVisibility();
        // Initializes NotificationChannel.
        mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        mChannel.enableVibration(enableVibrate);
        mChannel.setLockscreenVisibility(lockScreenVisibility);
        // Keep this nulls to suspend bug in Android O when each notification provides with a sound
        mChannel.setSound(null, null);
        // Adds NotificationChannel to system. Attempting to create an existing notification
        // channel with its original values performs no operation, so it's safe to perform the
        // below sequence.
        if (mManager != null) {
            mManager.createNotificationChannel(mChannel);
        }
    }

    void createChannelNoStream(final NoMediaNotificationData data) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        if (mChannelNoStream != null) {
            return;
        }
        // The id of the channel.
        final String id = data.getChannelId();
        // The user-visible name of the channel.
        final CharSequence name = data.getChannelName();
        // The user-visible description of the channel.
        final String description = data.getChannelDescription();
        final int importance = data.getChannelImportance();
        final boolean enableVibrate = data.getChannelEnableVibrate();
        final int lockScreenVisibility = data.getChannelLockScreenVisibility();
        // Initializes NotificationChannel.
        mChannelNoStream = new NotificationChannel(id, name, importance);
        mChannelNoStream.setDescription(description);
        mChannelNoStream.enableVibration(enableVibrate);
        mChannelNoStream.setLockscreenVisibility(lockScreenVisibility);
        // Keep this nulls to suspend bug in Android O when each notification provides with a sound
        mChannelNoStream.setSound(null, null);
        // Adds NotificationChannel to system. Attempting to create an existing notification
        // channel with its original values performs no operation, so it's safe to perform the
        // below sequence.
        if (mManager != null) {
            mManager.createNotificationChannel(mChannelNoStream);
        }
    }
}
