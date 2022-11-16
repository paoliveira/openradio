/*
 * Copyright 2019-2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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

import android.content.Context
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class PersistentApiCache(context: Context, dbName: String) : ApiCache {

    private val mDb = PersistentApiDb.getInstance(context, dbName)

    override fun get(key: String): String {
        var data = AppUtils.EMPTY_STRING
        val record = mDb.persistentApiCacheDao().getRecord(key)
        if (record == null) {
            AppLogger.d("$CLASS_NAME cache is empty for $key")
            return data
        }
        if (System.currentTimeMillis() - record.timestamp <= SEC_IN_DAY) {
            data = record.data
        }
        AppLogger.d("$CLASS_NAME cached response for $key is $data")
        return data
    }

    override fun put(key: String, data: String) {
        val recordId = mDb.persistentApiCacheDao().insert(
            PersistentApiEntry(name = key, data = data, timestamp = System.currentTimeMillis())
        )
        val count = mDb.persistentApiCacheDao().getCount()
        AppLogger.d("$CLASS_NAME put record id:$recordId, count is $count")
    }

    override fun clear() {
        mDb.persistentApiCacheDao().clear()
        val count = mDb.persistentApiCacheDao().getCount()
        AppLogger.d("$CLASS_NAME clear, count is $count")
    }

    override fun remove(key: String) {
        mDb.persistentApiCacheDao().delete(key)
        val count = mDb.persistentApiCacheDao().getCount()
        AppLogger.d("$CLASS_NAME delete record, count is $count")
    }

    companion object {
        private val CLASS_NAME = PersistentApiCache::class.java.simpleName
        private const val SEC_IN_DAY = 86400
    }
}
