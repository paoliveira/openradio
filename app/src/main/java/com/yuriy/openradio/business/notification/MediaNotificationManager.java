package com.yuriy.openradio.business.notification;

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

    public static String createNotificationChannel(
            final Context context,
            MediaNotificationData notificationData) {

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
