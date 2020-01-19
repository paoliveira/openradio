/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.service;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.leanback.media.PlayerAdapter;

import com.yuriy.openradio.shared.model.storage.ServiceLifecyclePreferencesManager;
import com.yuriy.openradio.shared.service.OpenRadioService;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;

public final class TvServicePlayerAdapter extends PlayerAdapter {

    private final Context mContext;
    private boolean mIsPlaying;

    public TvServicePlayerAdapter(final Context context) {
        super();
        mContext = context;
        mIsPlaying = false;
    }

    @Override
    public void play() {
        if (!ServiceLifecyclePreferencesManager.isServiceActive(mContext)) {
            return;
        }
        if (mIsPlaying) {
            return;
        }
        mIsPlaying = true;
        AnalyticsUtils.logMessage("TvServicePayerAdapter[" + this.hashCode() + "]->play:startForegroundService");
        ContextCompat.startForegroundService(
                mContext, OpenRadioService.makePlayLastPlayedItemIntent(mContext)
        );
    }

    @Override
    public void pause() {
        if (!ServiceLifecyclePreferencesManager.isServiceActive(mContext)) {
            return;
        }
        if (!mIsPlaying) {
            return;
        }
        mIsPlaying = false;
        AnalyticsUtils.logMessage("TvServicePayerAdapter[" + this.hashCode() + "]->pause:startForegroundService");
        ContextCompat.startForegroundService(
                mContext, OpenRadioService.makeStopLastPlayedItemIntent(mContext)
        );
    }

    @Override
    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public boolean isPrepared() {
        return true;
    }
}
