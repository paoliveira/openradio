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
package com.yuriy.openradio.shared.notification

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.yuriy.openradio.shared.utils.AppUtils.hasVersionN

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
abstract class NotificationData internal constructor() {
    // Standard notification values:
    var contentTitle: String? = null
    var contentText: String? = null
    var priority: Int = NotificationCompat.PRIORITY_DEFAULT

    // Notification channel values (O and above):
    var channelId: String? = null
    var channelName: CharSequence? = null
    var channelDescription: String? = null
    var channelImportance = 0
    var channelEnableVibrate: Boolean
    var channelLockScreenVisibility: Int

    override fun toString(): String {
        return "NotificationData{" +
                "id='" + channelId + '\'' +
                ", title='" + contentTitle + '\'' +
                ", text='" + contentText + '\'' +
                ", priority=" + priority +
                ", name=" + channelName +
                ", description='" + channelDescription + '\'' +
                ", importance=" + channelImportance +
                ", enableVibrate=" + channelEnableVibrate +
                ", lockScreenVisibility=" + channelLockScreenVisibility +
                '}'
    }

    init {
        if (hasVersionN()) {
            channelImportance = NotificationManager.IMPORTANCE_DEFAULT
        }
        channelEnableVibrate = false
        channelLockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
    }
}
