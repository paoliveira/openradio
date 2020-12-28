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
package com.yuriy.openradio.shared.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.yuriy.openradio.shared.utils.AppUtils.hasVersionO
import java.util.concurrent.*

/**
 * Factory to build Notification Channels.
 */
class NotificationChannelFactory(context: Context) {
    private val mManager: NotificationManager?
    private val mNotificationChannelMap: MutableMap<String, NotificationChannel>

    fun createChannel(data: NotificationData) {
        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (!hasVersionO()) {
            return
        }
        // The id of the channel.
        val id = data.channelId ?: return
        if (mNotificationChannelMap.containsKey(id)) {
            return
        }
        // The user-visible name of the channel.
        val name = data.channelName
        // The user-visible description of the channel.
        val description = data.channelDescription
        val importance = data.channelImportance
        val enableVibrate = data.channelEnableVibrate
        val lockScreenVisibility = data.channelLockScreenVisibility
        // Initializes NotificationChannel.
        val channel = NotificationChannel(id, name, importance)
        channel.description = description
        channel.enableVibration(enableVibrate)
        channel.lockscreenVisibility = lockScreenVisibility
        // Keep this nulls to suspend bug in Android O when each notification provides with a sound
        channel.setSound(null, null)
        // Adds NotificationChannel to system. Attempting to create an existing notification
        // channel with its original values performs no operation, so it's safe to perform the
        // below sequence.
        mNotificationChannelMap[id] = channel
        mManager?.createNotificationChannel(channel)
    }

    init {
        mNotificationChannelMap = ConcurrentHashMap()
        mManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
