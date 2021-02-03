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

import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.yuriy.openradio.gabor.shared.utils.AppUtils;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 05/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public abstract class NotificationData {

    // Standard notification values:
    private String mContentTitle;
    private String mContentText;
    private int mPriority;

    // Notification channel values (O and above):
    private String mChannelId;
    private CharSequence mChannelName;
    private String mChannelDescription;
    private int mChannelImportance;
    private boolean mChannelEnableVibrate;
    private int mChannelLockScreenVisibility;

    NotificationData() {
        super();
        mPriority = NotificationCompat.PRIORITY_DEFAULT;
        if (AppUtils.hasVersionN()) {
            mChannelImportance = NotificationManager.IMPORTANCE_DEFAULT;
        }
        mChannelEnableVibrate = false;
        mChannelLockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC;
    }

    public String getContentTitle() {
        return mContentTitle;
    }

    public void setContentTitle(final String value) {
        mContentTitle = value;
    }

    public String getContentText() {
        return mContentText;
    }

    public void setContentText(final String value) {
        mContentText = value;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(final int value) {
        mPriority = value;
    }

    String getChannelId() {
        return mChannelId;
    }

    public void setChannelId(final String value) {
        mChannelId = value;
    }

    CharSequence getChannelName() {
        return mChannelName;
    }

    public void setChannelName(final CharSequence value) {
        mChannelName = value;
    }

    String getChannelDescription() {
        return mChannelDescription;
    }

    public void setChannelDescription(final String value) {
        mChannelDescription = value;
    }

    int getChannelImportance() {
        return mChannelImportance;
    }

    public void setChannelImportance(final int value) {
        mChannelImportance = value;
    }

    boolean getChannelEnableVibrate() {
        return mChannelEnableVibrate;
    }

    public void setChannelEnableVibrate(final boolean value) {
        mChannelEnableVibrate = value;
    }

    int getChannelLockScreenVisibility() {
        return mChannelLockScreenVisibility;
    }

    public void setChannelLockScreenVisibility(final int value) {
        mChannelLockScreenVisibility = value;
    }

    @NonNull
    @Override
    public String toString() {
        return "NotificationData{" +
                "id='" + mChannelId + '\'' +
                ", title='" + mContentTitle + '\'' +
                ", text='" + mContentText + '\'' +
                ", priority=" + mPriority +
                ", name=" + mChannelName +
                ", description='" + mChannelDescription + '\'' +
                ", importance=" + mChannelImportance +
                ", enableVibrate=" + mChannelEnableVibrate +
                ", lockScreenVisibility=" + mChannelLockScreenVisibility +
                '}';
    }
}
