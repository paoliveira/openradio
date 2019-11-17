/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.yuriy.openradio.model.media;

import android.content.Context;

import androidx.leanback.widget.MultiActionsProvider;

import com.yuriy.openradio.utils.AppLogger;

// TODO: Switch to RadioStation
public class Song implements MultiActionsProvider {

    private String mTitle = "";
    private String mDescription = "";
    private String mText = "";
    private String mImage = "";
    private String mFile = "";
    private String mDuration = "0";
    private int mNumber = 0;
    private boolean mFavorite = false;
    private MultiAction[] mMediaRowActions;

    public Song() {
        AppLogger.d("Song Selector");
    }

    public void setMediaRowActions(MultiAction[] mediaRowActions) {
        mMediaRowActions = mediaRowActions;
    }

    public MultiAction[] getMediaRowActions() {
        return mMediaRowActions;
    }

    public String getDuration() {
        return mDuration;
    }

    public void setDuration(String duration) {
        mDuration = duration;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getText() {
        return mText;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public boolean isFavorite() {
        return mFavorite;
    }

    public void setFavorite(boolean favorite) {
        mFavorite = favorite;
    }

    public int getFileResource(Context context) {
        return context.getResources()
                .getIdentifier(mFile, "raw", context.getPackageName());
    }

    public int getImageResource(Context context) {
        return context.getResources()
                .getIdentifier(mImage, "drawable", context.getPackageName());
    }

    @Override
    public MultiAction[] getActions() {
        return mMediaRowActions;
    }
}
