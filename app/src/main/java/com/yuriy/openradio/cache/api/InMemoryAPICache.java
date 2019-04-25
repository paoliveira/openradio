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

import android.text.TextUtils;

import com.yuriy.openradio.utils.AppLogger;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONArray;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class InMemoryAPICache implements ApiCache {

    private static final String CLASS_NAME = InMemoryAPICache.class.getSimpleName();

    /**
     * Data structure to cache API responses.
     */
    private static final Map<String, JSONArray> RESPONSES_MAP = new ConcurrentHashMap<>();

    /**
     *
     */
    public InMemoryAPICache() {
        super();
    }

    @Override
    @Nullable
    public JSONArray get(final String key) {
        if (!TextUtils.isEmpty(key) && RESPONSES_MAP.containsKey(key)) {
            AppLogger.i(CLASS_NAME + " Get response from the cache");
            return RESPONSES_MAP.get(key);
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
