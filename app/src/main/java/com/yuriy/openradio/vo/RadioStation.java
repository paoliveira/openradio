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

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/16/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link RadioStation} is a value object that holds information
 * about concrete Radio Station.
 */
public final class RadioStation implements Serializable {

    public static final int SORT_ID_UNSET = -1;

    private int mId;

    // TODO: Convert to enum
    private int mStatus;

    private String mName = "";

    private String mWebSite = "";

    // TODO: Convert to enum
    private String mCountry = "";

    private String mGenre = "";

    private String mImageUrl = "";

    private String mThumbUrl = "";

    @NonNull
    private final MediaStream mMediaStream;

    /**
     * Flag indicate that Radio Station has been added locally to the phone storage.
     */
    private boolean mIsLocal;

    private boolean mIsLastKnown;

    private int mSortId = SORT_ID_UNSET;

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private RadioStation() {
        super();
        mMediaStream = MediaStream.makeDefaultInstance();
    }

    public final int getId() {
        return mId;
    }

    public final String getIdAsString() {
        return String.valueOf(mId);
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

    public final String getCountry() {
        return mCountry;
    }

    public final void setCountry(final String value) {
        mCountry = value;
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

    public boolean isLocal() {
        return mIsLocal;
    }

    public void setIsLocal(final boolean value) {
        mIsLocal = value;
    }

    public int getSortId() {
        return mSortId;
    }

    public void setSortId(final int value) {
        mSortId = value;
    }

    public boolean isLastKnown() {
        return mIsLastKnown;
    }

    public void setLastKnown(final boolean value) {
        mIsLastKnown = value;
    }

    @NonNull
    public final MediaStream getMediaStream() {
        return mMediaStream;
    }

    public final boolean isMediaStreamEmpty() {
        return mMediaStream.isEmpty();
    }

    public void setMediaStream(@NonNull final MediaStream value) {
        //TODO: Dangerous! MediaItem may be reference from the same RadioStation object!
        mMediaStream.clear();
        final int size = value.getVariantsNumber();
        for (int i = 0; i < size; i++) {
            mMediaStream.setVariant(value.getVariant(i).getBitrate(), value.getVariant(i).getUrl());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RadioStation that = (RadioStation) o;

        if (mId != that.mId) return false;
        return mMediaStream.equals(that.mMediaStream);
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mMediaStream.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RadioStation " + hashCode() + " {" +
                "id=" + mId +
                ", status=" + mStatus +
                ", name='" + mName + '\'' +
                ", stream='" + mMediaStream + '\'' +
                ", webSite='" + mWebSite + '\'' +
                ", country='" + mCountry + '\'' +
                ", genre='" + mGenre + '\'' +
                ", imageUrl='" + mImageUrl + '\'' +
                ", thumbUrl='" + mThumbUrl + '\'' +
                ", isLocal=" + mIsLocal + '\'' +
                ", isLastKnown=" + mIsLastKnown + '\'' +
                ", sortId=" + mSortId +
                '}';
    }

    /**
     * Factory method to create instance of the {@link RadioStation}.
     *
     * @return Instance of the {@link RadioStation}.
     */
    public static RadioStation makeDefaultInstance() {
        return new RadioStation();
    }
}
