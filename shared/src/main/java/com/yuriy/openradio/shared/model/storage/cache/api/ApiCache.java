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

package com.yuriy.openradio.shared.model.storage.cache.api;

import org.json.JSONArray;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public interface ApiCache {

    JSONArray get(final String key);

    void put(final String key, final JSONArray data);

    void remove(final String key);

    void clear();
}