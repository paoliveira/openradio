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

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 6/9/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class RadioStationJSONHelper {

    private RadioStationJSONHelper() { super(); }

    /**
     * JSON's keys
     */

    static final String KEY_ID = "Id";

    static final String KEY_STATUS = "Status";

    static final String KEY_NAME = "Name";

    static final String KEY_STREAM_URL = "StreamUrl";

    static final String KEY_WEB_SITE = "Website";

    static final String KEY_COUNTRY = "Country";

    static final String KEY_BITRATE = "Bitrate";

    static final String KEY_GENRE = "Genre";

    static final String KEY_IMG_URL = "ImgUrl";

    static final String KEY_THUMB_URL = "ThumbUrl";

    static final String KEY_IS_LOCAL = "IsLocal";

    static final String KEY_SORT_ID = "SortId";
}
