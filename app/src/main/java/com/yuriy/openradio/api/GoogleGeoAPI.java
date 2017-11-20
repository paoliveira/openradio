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

package com.yuriy.openradio.api;

import android.net.Uri;

import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.vo.CategoryVO;
import com.yuriy.openradio.vo.CountryVO;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/14/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link GoogleGeoAPI} is an interface which provides
 * various methods of the Google Geo's API.
 */
public interface GoogleGeoAPI {

    /**
     * Get a country name.
     *
     * @param downloader Implementation of the {@link Downloader} interface.
     * @param uri        {@link Uri} of the request.
     *
     * @return Collection of the {@link CategoryVO}s
     */
    CountryVO getCountry(final Downloader downloader, final Uri uri);

}
