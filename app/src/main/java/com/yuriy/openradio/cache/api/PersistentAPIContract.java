/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.cache.api;

import android.provider.BaseColumns;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
final class PersistentAPIContract {

    private PersistentAPIContract() {
        super();
    }

    /**
     * Inner class that defines the table contents
     */
    static final class APIEntry implements BaseColumns {

        static final String TABLE_NAME = "apicache";
        static final String COLUMN_NAME_KEY = "key";
        static final String COLUMN_NAME_DATA = "data";
        static final String COLUMN_NAME_TIMESTAMP = "timestamp";

    }
}
