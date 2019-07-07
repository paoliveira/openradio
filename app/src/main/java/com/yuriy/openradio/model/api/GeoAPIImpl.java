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

package com.yuriy.openradio.model.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.yuriy.openradio.model.parser.DataParser;
import com.yuriy.openradio.model.parser.GeoDataParser;
import com.yuriy.openradio.model.net.Downloader;
import com.yuriy.openradio.utils.AppLogger;
import com.yuriy.openradio.vo.Country;
import com.yuriy.openradio.vo.GeoLocation;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link GeoAPIImpl} is the implementation of the {@link GeoAPI} interface.
 */
public final class GeoAPIImpl implements GeoAPI {

    /**
     * Tag string to use in logging messages.
     */
    private static final String CLASS_NAME = GeoAPIImpl.class.getSimpleName();

    /**
     * Implementation of the {@link DataParser} allows to
     * parse raw response of the data into different formats.
     */
    private GeoDataParser mDataParser;

    /**
     * Constructor.
     *
     * @param dataParser Implementation of the {@link DataParser}
     */
    public GeoAPIImpl(final GeoDataParser dataParser) {
        mDataParser = dataParser;
    }

    @Override
    @NonNull
    public Country getCountry(final Downloader downloader, final Uri uri) {
        if (mDataParser == null) {
            AppLogger.w(CLASS_NAME + " Can not parse data, parser is null");
            return Country.getDefaultCountry();
        }

        final byte[] data = downloader.downloadDataFromUri(uri);
        final GeoLocation location = mDataParser.getLocation(data);

        return location.getCountry();
    }
}
