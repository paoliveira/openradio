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
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.net.Uri;

import com.yuriy.openradio.net.Downloader;

import java.util.List;

/**
 * {@link com.yuriy.openradio.api.APIServiceProvider} is an interface which provide
 * various methods of the Dirble's API.
 */
public interface APIServiceProvider {

    /**
     * Get a list of all categories.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     *
     * @return Collection of the {@link com.yuriy.openradio.api.CategoryVO}s
     */
    public List<CategoryVO> getAllCategories(final Downloader downloader, final Uri uri);

    /**
     * Get a list of child in category.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return Collection of the {@link com.yuriy.openradio.api.CategoryVO}s
     */
    public List<CategoryVO> getChildCategories(final Downloader downloader, final Uri uri);

    /**
     * Get a list of Radio Stations in the Category.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return collection of the Radio Stations.
     */
    public List<RadioStationVO> getStationsInCategory(final Downloader downloader, final Uri uri);

    /**
     * Get a Radio Station.
     *
     * @param downloader Implementation of the {@link com.yuriy.openradio.net.Downloader} interface.
     * @param uri        {@link android.net.Uri} of the request.
     * @return Radio Station.
     */
    public RadioStationVO getStation(final Downloader downloader, final Uri uri);

    public List<RadioStationVO> getStationsInCountry(final Downloader downloader, final Uri uri);
}
