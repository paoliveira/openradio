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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
class PersistentAPIDbHelper internal constructor(context: Context?, dbName: String?) :
        SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    companion object {
        private const val DATABASE_VERSION = 2
        const val DATABASE_NAME = "APICache.db"
        private const val SQL_CREATE_ENTRIES = "CREATE TABLE IF NOT EXISTS " + PersistentAPIContract.APIEntry.TABLE_NAME + " (" +
                PersistentAPIContract.APIEntry.ID + " INTEGER PRIMARY KEY," +
                PersistentAPIContract.APIEntry.COLUMN_NAME_KEY + " TEXT," +
                PersistentAPIContract.APIEntry.COLUMN_NAME_DATA + " TEXT, " +
                PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP + " INTEGER)"
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + PersistentAPIContract.APIEntry.TABLE_NAME
    }
}
