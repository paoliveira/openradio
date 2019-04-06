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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.yuriy.openradio.utils.AppLogger;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 02/04/19
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class PersistentAPICache implements APICache {

    private static final String CLASS_NAME = PersistentAPICache.class.getSimpleName() + " ";
    private static final String[] PROJECTION = {
            BaseColumns._ID,
            PersistentAPIContract.APIEntry.COLUMN_NAME_KEY,
            PersistentAPIContract.APIEntry.COLUMN_NAME_DATA,
            PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP
    };
    private static final String SELECTION = PersistentAPIContract.APIEntry.COLUMN_NAME_KEY + " = ?";
    private static final int SEC_IN_DAY = 86400;

    private final PersistentAPIDbHelper mDbHelper;

    public PersistentAPICache(final Context context, final String dbName) {
        super();
        mDbHelper = new PersistentAPIDbHelper(context, dbName);
    }

    @Override
    @Nullable
    public JSONArray get(final String key) {
        final SQLiteDatabase db = mDbHelper.getReadableDatabase();

        final String[] selectionArgs = { key };

        final Cursor cursor = db.query(
                PersistentAPIContract.APIEntry.TABLE_NAME,   // The table to query
                PROJECTION,            // The array of columns to return (pass null to get all)
                SELECTION,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,           // don't group the rows
                null,            // don't filter by row groups
                null            // The sort order
        );

        JSONArray jsonArray = null;
        while (cursor.moveToNext()) {
            final long cId = cursor.getLong(
                    cursor.getColumnIndexOrThrow(BaseColumns._ID)
            );
            final String cKey = cursor.getString(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_KEY)
            );
            final String cData = cursor.getString(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_DATA)
            );
            final int cTime = cursor.getInt(
                    cursor.getColumnIndexOrThrow(PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP)
            );
            AppLogger.d(CLASS_NAME + "Get id:" + cId + ", key:" + cKey + ", data:" + cData + ", time:" + cTime);

            if (getTime() - cTime > SEC_IN_DAY) {
                AppLogger.d(CLASS_NAME + "Remove cached response");
                delete(key);
                break;
            }

            try {
                jsonArray = new JSONArray(cData);
            } catch (JSONException e) {
                AppLogger.e(CLASS_NAME + "Can not re-create JSON Array:" + e);
            }
        }
        cursor.close();

        return jsonArray;
    }

    @Override
    public void put(final String key, final JSONArray data) {
        // Gets the data repository in write mode
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        final ContentValues values = new ContentValues();
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_KEY, key);
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_DATA, data.toString());
        values.put(PersistentAPIContract.APIEntry.COLUMN_NAME_TIMESTAMP, getTime());

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.replace(PersistentAPIContract.APIEntry.TABLE_NAME, null, values);
        AppLogger.d(CLASS_NAME + "Put::new row::" + newRowId);
    }

    @Override
    public void clear() {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int deletedRows = db.delete(PersistentAPIContract.APIEntry.TABLE_NAME, null, null);
        AppLogger.d(CLASS_NAME + "Put::clear rows::" + deletedRows);
    }

    public void close() {
        mDbHelper.close();
    }

    protected void delete(final String key) {
        final SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final String[] selectionArgs = { key };
        final int deletedRows = db.delete(PersistentAPIContract.APIEntry.TABLE_NAME, SELECTION, selectionArgs);
        AppLogger.d(CLASS_NAME + "Put::del row::" + deletedRows + ", key:" + key);
    }

    private static int getTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}
