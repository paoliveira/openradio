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

package com.yuriy.openradio.shared.vo;

import android.content.Context;

import com.yuriy.openradio.R;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link Category} is a value object that holds data related to category of Radio Stations.
 */
public final class Category {

    private String mId;

    private int mStationsCount;

    private String mDescription = "";

    private String mTitle = "";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private Category() { }

    public String getId() {
        return mId;
    }

    public void setId(final String value) {
        mId = value;
    }

    public String getDescription(final Context context) {
        if (mDescription != null && !mDescription.isEmpty()) {
            return mDescription;
        }
        String desc = String.valueOf(mStationsCount);
        final int count = getStationsCount();
        if (count == 0 || count > 1) {
            desc += " " + context.getString(R.string.radio_stations);
        } else {
            desc += " " + context.getString(R.string.radio_station);
        }
        return desc;
    }

    public void setDescription(final String value) {
        mDescription = value;
    }

    public int getStationsCount() {
        return mStationsCount;
    }

    public void setStationsCount(final int value) {
        mStationsCount = value;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String value) {
        mTitle = value;
    }

    /**
     * Factory method to create instance of the {@link Category}.
     *
     * @return Instance of the {@link Category}.
     */
    public static Category makeDefaultInstance() {
        return new Category();
    }
}
