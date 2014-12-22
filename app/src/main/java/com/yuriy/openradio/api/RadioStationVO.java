/*
 * Copyright 2014 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.api;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/16/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

/**
 * {@link com.yuriy.openradio.api.RadioStationVO} is a value object that holds information
 * about concrete Radio Station.
 */
public class RadioStationVO {

    private int mId;

    // TODO: Convert to enum
    private int mStatus;

    private String mName = "";

    private String mStreamURL = "";

    private String mWebSite = "";

    // TODO: Convert to enum
    private String mCountry = "";

    // TODO: Convert to enum
    private String mBitRate = "";

    private String mGenre = "";

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private RadioStationVO() { }

    public int getId() {
        return mId;
    }

    public void setId(final int value) {
        mId = value;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(final int value) {
        mStatus = value;
    }

    public String getName() {
        return mName;
    }

    public void setName(final String value) {
        mName = value;
    }

    public String getStreamURL() {
        return mStreamURL;
    }

    public void setStreamURL(final String value) {
        mStreamURL = value;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(final String value) {
        mCountry = value;
    }

    public String getBitRate() {
        return mBitRate;
    }

    public void setBitRate(final String value) {
        mBitRate = value;
    }

    public String getWebSite() {
        return mWebSite;
    }

    public void setWebSite(final String value) {
        mWebSite = value;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(final String value) {
        mGenre = value;
    }

    @Override
    public String toString() {
        return "RadioStation{" +
                "id=" + mId +
                ", status=" + mStatus +
                ", name='" + mName + '\'' +
                ", streamURL='" + mStreamURL + '\'' +
                ", webSite='" + mWebSite + '\'' +
                ", country='" + mCountry + '\'' +
                ", bitRate='" + mBitRate + '\'' +
                ", genre='" + mGenre + '\'' +
                '}';
    }

    /**
     * Factory method to create instance of the {@link com.yuriy.openradio.api.RadioStationVO}.
     *
     * @return Instance of the {@link com.yuriy.openradio.api.RadioStationVO}.
     */
    public static RadioStationVO makeDefaultInstance() {
        return new RadioStationVO();
    }
}
