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

package com.yuriy.openradio.shared.service;

import android.content.Context;

import androidx.leanback.media.PlayerAdapter;

public final class ServicePlayerTvAdapter extends PlayerAdapter {

    private final Context mContext;
    private boolean mIsPlaying;

    public ServicePlayerTvAdapter(final Context context) {
        super();
        mContext = context;
        mIsPlaying = false;
    }

    @Override
    public void play() {
        if (mIsPlaying) {
            return;
        }
        mIsPlaying = true;
        //TODO:FIXME
        //mContext.startService(OpenRadioService.makePlayLastPlayedItemIntent(mContext));
    }

    @Override
    public void pause() {
        if (!mIsPlaying) {
            return;
        }
        mIsPlaying = false;
        //TODO:FIXME
        //mContext.startService(OpenRadioService.makeStopLastPlayedItemIntent(mContext));
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
