/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import java.io.Serializable;

/**
 * This class contains data to validate prior to add to data set (either local or remote server).
 */
public final class RadioStationToAdd implements Serializable {

    private final String mName;
    private final String mUrl;
    private final String mImageLocalUrl;
    private final String mImageWebUrl;
    private final String mHomePage;
    private final String mGenre;
    private final String mCountry;
    private final boolean mAddToFav;
    private final boolean mAddToServer;

    /**
     * @param name          Name of the Radio Station.
     * @param url           Url of the Stream associated with Radio Station.
     * @param imageLocalUrl Local Url of the Image associated with Radio Station.
     * @param imageWebUrl   Web Url of the Image associated with Radio Station.
     * @param homePage      Web Url of Radio Station's home page.
     * @param genre         Genre of the Radio Station.
     * @param country       Country of the Radio Station.
     * @param addToFav      Whether or not add radio station to favorites.
     * @param addToServer   Whether or not add radio station to the server.
     */
    public RadioStationToAdd(final String name, final String url, final String imageLocalUrl,
                             final String imageWebUrl, final String homePage, final String genre,
                             final String country, final boolean addToFav, final boolean addToServer) {
        super();
        mName = name;
        mUrl = url;
        mImageLocalUrl = imageLocalUrl;
        mImageWebUrl = imageWebUrl;
        mHomePage = homePage;
        mGenre = genre;
        mCountry = country;
        mAddToFav = addToFav;
        mAddToServer = addToServer;
    }

    public String getName() {
        return mName;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getImageLocalUrl() {
        return mImageLocalUrl;
    }

    public String getImageWebUrl() {
        return mImageWebUrl;
    }

    public String getHomePage() {
        return mHomePage;
    }

    public String getGenre() {
        return mGenre;
    }

    public String getCountry() {
        return mCountry;
    }

    public boolean isAddToFav() {
        return mAddToFav;
    }

    public boolean isAddToServer() {
        return mAddToServer;
    }

    @Override
    public String toString() {
        return "RadioStationToAdd{" +
                "name='" + mName + '\'' +
                ", url='" + mUrl + '\'' +
                ", imageLocalUrl='" + mImageLocalUrl + '\'' +
                ", imageWebUrl='" + mImageWebUrl + '\'' +
                ", homePage='" + mHomePage + '\'' +
                ", genre='" + mGenre + '\'' +
                ", country='" + mCountry + '\'' +
                ", addToFav=" + mAddToFav +
                ", addToServer=" + mAddToServer +
                '}';
    }
}
