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

import android.content.ContentValues
import android.content.Context
import android.provider.BaseColumns
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.e
import org.json.JSONArray
import org.json.JSONException

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class PersistentApiCache(context: Context?, dbName: String?) : ApiCache {

    private val mDbHelper: PersistentAPIDbHelper = PersistentAPIDbHelper(context, dbName)

    override fun get(key: String): JSONArray {
        val db = mDbHelper.readableDatabase
        val selectionArgs = arrayOf(key)
        val cursor = db.query(
                PersistentAPIContract.APIEntry.TABLE_NAME,  // The table to query
                PROJECTION,  // The array of columns to return (pass null to get all)
                SELECTION,  // The columns for the WHERE clause
                selectionArgs,  // The values for the WHERE clause
                null,  // don't group the rows
                null,  // don't filter by row groups
                null // The sort order
        )
        var data: JSONArray? = null
        while (cursor.moveToNext()) {
            val cId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(BaseColumns._ID)
            )
            val cKey = cursor.getString(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_KEY)
            )
            val cData = cursor.getString(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_DATA)
            )
            val cTime = cursor.getInt(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP)
            )
            d(CLASS_NAME + "Get id:" + cId + ", key:" + cKey + ", data:" + cData + ", time:" + cTime)
            if (time - cTime > SEC_IN_DAY) {
                // Do not return data, return null if time is expired.
                break
            }
            try {
                data = JSONArray(cData)
            } catch (e: JSONException) {
                e(CLASS_NAME + "Can not re-create JSON Array:" + e)
            }
        }
        cursor.close()
        d(CLASS_NAME + "Cached response from DB for " + key + " is " + data)
        return data ?: JSONArray()
    }

    override fun put(key: String, data: JSONArray) {
        // Gets the data repository in write mode
        val db = mDbHelper.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_KEY, key)
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_DATA, data.toString())
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP, time)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.replace(PersistentAPIContract.APIEntry.TABLE_NAME, null, values)
        d(CLASS_NAME + "New row:" + newRowId)
    }

    override fun clear() {
        val db = mDbHelper.writableDatabase
        val deletedRows = db.delete(PersistentAPIContract.APIEntry.TABLE_NAME, null, null)
        d(CLASS_NAME + "Clear rows:" + deletedRows)
    }

    override fun remove(key: String) {
        val db = mDbHelper.writableDatabase
        val selectionArgs = arrayOf(key)
        val deletedRows = db.delete(PersistentAPIContract.APIEntry.TABLE_NAME, SELECTION, selectionArgs)
        d(CLASS_NAME + "Remove row:" + deletedRows + ", key:" + key)
    }

    fun close() {
        mDbHelper.close()
    }

    fun getCount(key: String): Int {
        val db = mDbHelper.readableDatabase
        val selectionArgs = arrayOf(key)
        val cursor = db.query(
                PersistentAPIContract.APIEntry.TABLE_NAME,  // The table to query
                PROJECTION,  // The array of columns to return (pass null to get all)
                SELECTION,  // The columns for the WHERE clause
                selectionArgs,  // The values for the WHERE clause
                null,  // don't group the rows
                null,  // don't filter by row groups
                null // The sort order
        )
        val count = cursor.count
        cursor.close()
        return count
    }

    companion object {
        private val CLASS_NAME = PersistentApiCache::class.java.simpleName + " "
        private val PROJECTION = arrayOf(
                BaseColumns._ID,
                PersistentAPIContract.APIEntry.COLUMN_NAME_KEY,
                PersistentAPIContract.APIEntry.COLUMN_NAME_DATA,
                PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP
        )
        private const val SELECTION = PersistentAPIContract.APIEntry.COLUMN_NAME_KEY + " = ?"
        private const val SEC_IN_DAY = 86400
        private val time: Int
            get() = (System.currentTimeMillis() / 1000).toInt()
    }
}
