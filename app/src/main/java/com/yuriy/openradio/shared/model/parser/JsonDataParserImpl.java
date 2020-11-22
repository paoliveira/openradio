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

package com.yuriy.openradio.shared.model.parser;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * {@link JsonDataParserImpl} is the implementation of {@link DataParser} that designed to parse
 * input into JSON format.
 *
 * TODO : Currently holds only constants. Need to crete interface and use this impl in API data
 *        provider impl.
 */
public class JsonDataParserImpl implements DataParser {

    public static final String KEY_STATION_UUID = "stationuuid";
    public static final String KEY_NAME = "name";
    public static final String KEY_COUNTRY = "country";
    /**
     * 2 letters, uppercase.
     */
    public static final String KEY_COUNTRY_CODE = "countrycode";
    public static final String KEY_BIT_RATE = "bitrate";
    public static final String KEY_STATUS = "status";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_THUMB = "thumb";
    public static final String KEY_URL = "url";
    public static final String KEY_FAV_ICON = "favicon";
    public static final String KEY_STATIONS_COUNT = "stationcount";
    public static final String KEY_HOME_PAGE = "homepage";
    /**
     * string, multi value, split by comma.
     */
    public static final String KEY_TAGS = "tags";
    /**
     * string, multi value, split by comma.
     */
    public static final String KEY_LANGUAGE = "language";
    /**
     * number, integer
     */
    public static final String KEY_VOTES = "votes";
    /**
     * datetime, YYYY-MM-DD HH:mm:ss
     * The time of the last click recorded for this stream.
     */
    public static final String KEY_CLICK_TIMESTAMP = "clicktimestamp";
    /**
     * number, integer.
     * Clicks within the last 24 hours.
     */
    public static final String KEY_CLICK_COUNT = "clickcount";
    /**
     * number, integer.
     * The difference of the click counts within the last 2 days.
     * Positive values mean an increase, negative a decrease of clicks.
     */
    public static final String KEY_CLICK_TREND = "clicktrend";
}
