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

package com.yuriy.openradio.shared.model.parser

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [JsonDataParserImpl] is the implementation of [DataParser] that designed to parse
 * input into JSON format.
 *
 * TODO : Currently holds only constants. Need to crete interface and use this impl in API data
 * provider impl.
 */
class JsonDataParserImpl : DataParser {

    companion object {
        const val KEY_STATION_UUID = "stationuuid"
        const val KEY_NAME = "name"
        const val KEY_COUNTRY = "country"

        /**
         * 2 letters, uppercase.
         */
        const val KEY_COUNTRY_CODE = "countrycode"
        const val KEY_BIT_RATE = "bitrate"
        const val KEY_STATUS = "status"
        const val KEY_URL = "url"
        const val KEY_URL_RESOLVED = "url_resolved"
        const val KEY_FAV_ICON = "favicon"
        const val KEY_STATIONS_COUNT = "stationcount"
        const val KEY_HOME_PAGE = "homepage"

        const val KEY_LAST_CHECK_OK_TIME = "lastcheckoktime"

        const val KEY_LAST_CHECK_OK = "lastcheckok"
    }
}
