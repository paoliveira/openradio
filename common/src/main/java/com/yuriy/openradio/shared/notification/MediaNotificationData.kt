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

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import com.yuriy.openradio.R

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
internal class MediaNotificationData(context: Context,
                                     metadata: MediaMetadataCompat) : NotificationData() {
    companion object {
        const val CHANNEL_ID = "channel_id_1"
    }

    init {
        val description = metadata.description
        contentTitle = if (description.title != null) {
            description.title.toString()
        } else {
            context.getString(R.string.notification_str)
        }
        contentText = if (description.subtitle != null) {
            description.subtitle.toString()
        } else {
            context.getString(R.string.notification_str)
        }
        channelId = CHANNEL_ID
        channelName = context.getString(R.string.radio_station_str)
        channelDescription = "Updates about current Radio Station"
    }
}
