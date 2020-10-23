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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory to build Notification Channels.
 */
public final class NotificationChannelFactory {

    private final NotificationManager mManager;
    private final Map<String, NotificationChannel> mNotificationChannelMap;

    public NotificationChannelFactory(final Context context) {
        super();
        mNotificationChannelMap = new ConcurrentHashMap<>();
        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    void updateChannel(final int id, final Notification notification) {
        if (mManager == null) {
            return;
        }
        // Address NPE inside ApplicationPackageManager when build notification
        try {
            mManager.notify(id, notification);
        } catch (final Exception e) {
            AppLogger.e("Can not do notification:" + e);
        }
    }

    void createChannel(@NonNull final NotificationData data) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (!AppUtils.hasVersionO()) {
            return;
        }
        // The id of the channel.
        final String id = data.getChannelId();
        if (mNotificationChannelMap.containsKey(id)) {
            return;
        }
        // The user-visible name of the channel.
        final CharSequence name = data.getChannelName();
        // The user-visible description of the channel.
        final String description = data.getChannelDescription();
        final int importance = data.getChannelImportance();
        final boolean enableVibrate = data.getChannelEnableVibrate();
        final int lockScreenVisibility = data.getChannelLockScreenVisibility();
        // Initializes NotificationChannel.
        final NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        channel.enableVibration(enableVibrate);
        channel.setLockscreenVisibility(lockScreenVisibility);
        // Keep this nulls to suspend bug in Android O when each notification provides with a sound
        channel.setSound(null, null);
        // Adds NotificationChannel to system. Attempting to create an existing notification
        // channel with its original values performs no operation, so it's safe to perform the
        // below sequence.
        mNotificationChannelMap.put(id, channel);
        if (mManager != null) {
            mManager.createNotificationChannel(channel);
        }
    }
}
