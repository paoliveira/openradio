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

package com.yuriy.openradio.gabor.shared.model.storage.cache.api;

import android.text.TextUtils;

import com.yuriy.openradio.gabor.shared.utils.AppLogger;

import org.json.JSONArray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class InMemoryApiCache implements ApiCache {

    private static final String CLASS_NAME = InMemoryApiCache.class.getSimpleName();

    /**
     * Data structure to cache API responses.
     */
    private static final Map<String, JSONArray> RESPONSES_MAP = new ConcurrentHashMap<>();

    /**
     *
     */
    public InMemoryApiCache() {
        super();
    }

    @Override
    public JSONArray get(final String key) {
        if (!TextUtils.isEmpty(key) && RESPONSES_MAP.containsKey(key)) {
            final JSONArray data = RESPONSES_MAP.get(key);
            AppLogger.d(CLASS_NAME + "Cached response from RAM for " + key + " is " + data);
            return data;
        }
        return null;
    }

    @Override
    public void put(final String key, final JSONArray data) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        RESPONSES_MAP.put(key, data);
    }

    @Override
    public void remove(final String key) {
        if (TextUtils.isEmpty(key)) {
            return;
        }
        RESPONSES_MAP.remove(key);
    }

    @Override
    public void clear() {
        RESPONSES_MAP.clear();
    }
}
