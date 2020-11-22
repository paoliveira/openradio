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

package com.yuriy.openradio.shared.model.api;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 */

import android.net.Uri;

import androidx.core.util.Pair;

import com.yuriy.openradio.shared.model.net.Downloader;
import com.yuriy.openradio.shared.model.storage.cache.CacheType;
import com.yuriy.openradio.shared.vo.Category;
import com.yuriy.openradio.shared.vo.Country;
import com.yuriy.openradio.shared.vo.RadioStation;

import java.util.List;

/**
 * {@link ApiServiceProvider} is an interface which provide various methods of API.
 */
public interface ApiServiceProvider {

    /**
     * Get a list of all categories.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @return Collection of the {@link Category}s
     */
    List<Category> getCategories(final Downloader downloader, final Uri uri, final CacheType cacheType);

    /**
     * Get a list of all countries.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @return Collection of the Countries
     */
    List<Country> getCountries(final Downloader downloader, final Uri uri, final CacheType cacheType);

    /**
     * Get a list of Radio Stations by provided Uri.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @return collection of the Radio Stations.
     */
    List<RadioStation> getStations(final Downloader downloader, final Uri uri, final CacheType cacheType);

    /**
     * Get a list of Radio Stations by provided Uri.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @param parameters List of parameters to attach to url connection.
     * @return collection of the Radio Stations.
     */
    List<RadioStation> getStations(final Downloader downloader,
                                   final Uri uri,
                                   final List<Pair<String, String>> parameters,
                                   final CacheType cacheType);

    /**
     * Get a Radio Station.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @return Radio Station.
     */
    RadioStation getStation(final Downloader downloader, final Uri uri, final CacheType cacheType);

    /**
     * Add Radio Station to server.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     * @param parameters List of parameters to attach to url connection.
     */
    boolean addStation(final Downloader downloader,
                       final Uri uri,
                       final List<Pair<String, String>> parameters,
                       final CacheType cacheType);

    /**
     * Close resources related to service provider, such as connections, streams, etc ...
     */
    void close();

    /**
     * Clear resources related to service provider, such as persistent or in memory storage, etc ...
     */
    void clear();
}
