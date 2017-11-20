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

import com.yuriy.openradio.business.DataParser;
import com.yuriy.openradio.business.GoogleGeoDataParser;
import com.yuriy.openradio.net.Downloader;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.CountryVO;
import com.yuriy.openradio.vo.GoogleGeoLocation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link GoogleGeoAPIImpl} is the implementation of the
 * {@link APIServiceProvider} interface.
 */
public final class GoogleGeoAPIImpl implements GoogleGeoAPI {

    /**
     * Tag string to use in logging messages.
     */
    @SuppressWarnings("unused")
    private static final String CLASS_NAME = GoogleGeoAPIImpl.class.getSimpleName();

    /**
     * Implementation of the {@link DataParser} allows to
     * parse raw response of the data into different formats.
     */
    private GoogleGeoDataParser mDataParser;

    /**
     * Constructor.
     *
     * @param dataParser Implementation of the {@link DataParser}
     */
    public GoogleGeoAPIImpl(final GoogleGeoDataParser dataParser) {
        mDataParser = dataParser;
    }

    @Override
    public CountryVO getCountry(final Downloader downloader, final Uri uri) {
        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + " Can not parse data, parser is null");
            return null;
        }

        final byte[] data = downloader.downloadDataFromUri(uri);
        final GoogleGeoLocation location = mDataParser.getLocation(data);

        return location.getCountry();
    }
}
