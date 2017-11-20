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

package com.yuriy.openradio.vo;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 4/25/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link StreamVO} is a value object that holds Stream's data.
 */
public final class StreamVO {

    private int mId;

    private String mUrl = "";

    private int mBitrate = 128;

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(final String value) {
        mUrl = value;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public void setBitrate(final int value) {
        mBitrate = value;
    }

    public int getId() {
        return mId;
    }

    public void setId(int value) {
        mId = value;
    }

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private StreamVO() { }

    /**
     * Factory method to create instance of the {@link StreamVO}.
     *
     * @return Instance of the {@link StreamVO}.
     */
    public static StreamVO makeDefaultInstance() {
        return new StreamVO();
    }
}
