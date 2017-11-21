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

package com.yuriy.openradio.business;

import com.yuriy.openradio.vo.GoogleGeoLocation;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 19/11/17
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * Data parser for the response obtained from Google Geo API.
 */
public interface GoogleGeoDataParser {

    /**
     * Get location from the raw response.
     *
     * @param data Bytes array represented server's response.
     * @return Location object created from the response.
     */
    GoogleGeoLocation getLocation(final byte[] data);
}
