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

package com.yuriy.openradio.shared.vo;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage;
import com.yuriy.openradio.shared.utils.AnalyticsUtils;

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

    private String mId;

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

    private int mSortId = SORT_ID_UNSET;

    /**
     * Private constructor.
     * Disallow instantiation of this helper class.
     */
    private RadioStation(final Context context, final String id) {
        super();
        setId(context, id);
        mMediaStream = MediaStream.makeDefaultInstance();
    }

    /**
     * Copy constructor.
     *
     * @param radioStation Object to be copied.
     */
    private RadioStation(final Context context, @NonNull final RadioStation radioStation) {
        super();
        mCountry = radioStation.mCountry;
        mGenre = radioStation.mGenre;
        setId(context, radioStation.mId);
        mImageUrl = radioStation.mImageUrl;
        mIsLocal = radioStation.mIsLocal;
        mMediaStream = MediaStream.makeCopyInstance(radioStation.mMediaStream);
        mName = radioStation.mName;
        mSortId = radioStation.mSortId;
        mStatus = radioStation.mStatus;
        mThumbUrl = radioStation.mThumbUrl;
        mWebSite = radioStation.mWebSite;
    }

    @NonNull
    public final String getId() {
        return mId;
    }

    private void setId(final Context context, String value) {
        if (TextUtils.isEmpty(value)) {
            AnalyticsUtils.logException(new IllegalArgumentException("Radio Station ID is invalid"));
            value = LocalRadioStationsStorage.getId(context);
        }
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

        RadioStation station = (RadioStation) o;

        if (!mId.equals(station.mId)) return false;
        return mMediaStream.equals(station.mMediaStream);
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + mMediaStream.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "RS " + hashCode() + " {" +
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
                ", sortId=" + mSortId +
                '}';
    }

    /**
     * Factory method to create instance of the {@link RadioStation}.
     *
     * @return Instance of the {@link RadioStation}.
     */
    public static RadioStation makeDefaultInstance(final Context context, final String id) {
        return new RadioStation(context, id);
    }

    /**
     * Factory method to create copy-instance of the {@link RadioStation}.
     *
     * @param radioStation Object to be copied.
     * @return Copied instance of {@link RadioStation}.
     */
    public static RadioStation makeCopyInstance(final Context context, final RadioStation radioStation) {
        return new RadioStation(context, radioStation);
    }
}
