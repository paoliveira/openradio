/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import java.io.Serializable;

/**
 * {@link com.yuriy.openradio.api.RadioStationVO} is a value object that holds information
 * about concrete Radio Station.
 */
public final class RadioStationVO implements Serializable {

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

    private String mImageUrl = "";

    private String mThumbUrl = "";

    /**
     * Flag indicates that Station's data has been downloaded and updates.
     * In version Dirble v2 when list of the stations received they comes without stream url
     * and bitrate, upon selecting one - it is necessary to load additional data.
     */
    private boolean mIsUpdated;

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private RadioStationVO() { }

    public final int getId() {
        return mId;
    }

    public final void setId(final int value) {
        mId = value;
    }

    public final int getStatus() {
        return mStatus;
    }

    public final void setStatus(final int value) {
        mStatus = value;
    }

    public final String getName() {
        return mName;
    }

    public final void setName(final String value) {
        mName = value;
    }

    public final String getStreamURL() {
        return mStreamURL;
    }

    public final void setStreamURL(final String value) {
        mStreamURL = value;
    }

    public final String getCountry() {
        return mCountry;
    }

    public final void setCountry(final String value) {
        mCountry = value;
    }

    public final String getBitRate() {
        return mBitRate;
    }

    public final void setBitRate(final String value) {
        mBitRate = value;
    }

    public final String getWebSite() {
        return mWebSite;
    }

    public final void setWebSite(final String value) {
        mWebSite = value;
    }

    public final String getGenre() {
        return mGenre;
    }

    public final void setGenre(final String value) {
        mGenre = value;
    }

    public final boolean getIsUpdated() {
        return mIsUpdated;
    }

    public final void setIsUpdated(final boolean value) {
        mIsUpdated = value;
    }

    public final String getImageUrl() {
        return mImageUrl;
    }

    public final void setImageUrl(final String value) {
        mImageUrl = value;
    }

    public final String getThumbUrl() {
        return mThumbUrl;
    }

    public final void setThumbUrl(final String value) {
        mThumbUrl = value;
    }

    @Override
    public final boolean equals(final Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        final RadioStationVO that = (RadioStationVO) object;

        if (mId != that.mId) return false;
        if (mStatus != that.mStatus) return false;
        if (mIsUpdated != that.mIsUpdated) return false;
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) return false;
        if (mStreamURL != null ? !mStreamURL.equals(that.mStreamURL) : that.mStreamURL != null)
            return false;
        if (mWebSite != null ? !mWebSite.equals(that.mWebSite) : that.mWebSite != null)
            return false;
        if (mCountry != null ? !mCountry.equals(that.mCountry) : that.mCountry != null)
            return false;
        if (mBitRate != null ? !mBitRate.equals(that.mBitRate) : that.mBitRate != null)
            return false;
        if (mGenre != null ? !mGenre.equals(that.mGenre) : that.mGenre != null) return false;
        if (mImageUrl != null ? !mImageUrl.equals(that.mImageUrl) : that.mImageUrl != null)
            return false;
        return !(mThumbUrl != null ? !mThumbUrl.equals(that.mThumbUrl) : that.mThumbUrl != null);

    }

    @Override
    public final int hashCode() {
        int result = mId;
        result = 31 * result + mStatus;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mStreamURL != null ? mStreamURL.hashCode() : 0);
        result = 31 * result + (mWebSite != null ? mWebSite.hashCode() : 0);
        result = 31 * result + (mCountry != null ? mCountry.hashCode() : 0);
        result = 31 * result + (mBitRate != null ? mBitRate.hashCode() : 0);
        result = 31 * result + (mGenre != null ? mGenre.hashCode() : 0);
        result = 31 * result + (mImageUrl != null ? mImageUrl.hashCode() : 0);
        result = 31 * result + (mThumbUrl != null ? mThumbUrl.hashCode() : 0);
        result = 31 * result + (mIsUpdated ? 1 : 0);
        return result;
    }

    @Override
    public final String toString() {
        return "RadioStationVO{" +
                "mId=" + mId +
                ", mStatus=" + mStatus +
                ", mName='" + mName + '\'' +
                ", mStreamURL='" + mStreamURL + '\'' +
                ", mWebSite='" + mWebSite + '\'' +
                ", mCountry='" + mCountry + '\'' +
                ", mBitRate='" + mBitRate + '\'' +
                ", mGenre='" + mGenre + '\'' +
                ", mImageUrl='" + mImageUrl + '\'' +
                ", mThumbUrl='" + mThumbUrl + '\'' +
                ", mIsUpdated=" + mIsUpdated +
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
