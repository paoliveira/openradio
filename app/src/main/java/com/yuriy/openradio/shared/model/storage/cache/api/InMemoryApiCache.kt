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
package com.yuriy.openradio.shared.model.storage.cache.api

import android.text.TextUtils
import com.yuriy.openradio.shared.utils.AppLogger.d
import org.json.JSONArray
import java.util.concurrent.*

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class InMemoryApiCache : ApiCache {
    override fun get(key: String?): JSONArray? {
        if (!TextUtils.isEmpty(key) && RESPONSES_MAP.containsKey(key)) {
            val data = RESPONSES_MAP[key]
            d(CLASS_NAME + "Cached response from RAM for " + key + " is " + data)
            return data
        }
        return null
    }

    override fun put(key: String?, data: JSONArray?) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        RESPONSES_MAP[key] = data
    }

    override fun remove(key: String?) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        RESPONSES_MAP.remove(key)
    }

    override fun clear() {
        RESPONSES_MAP.clear()
    }

    companion object {
        private val CLASS_NAME = InMemoryApiCache::class.java.simpleName

        /**
         * Data structure to cache API responses.
         */
        private val RESPONSES_MAP: MutableMap<String?, JSONArray?> = ConcurrentHashMap()
    }
}
