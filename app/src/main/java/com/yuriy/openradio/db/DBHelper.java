/*
 * Copyright 2015 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.yuriy.openradio.utils.AppUtils;

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 5/23/15
 * E-Mail: chernyshov.yuriy@gmail.com
 */
public final class DBHelper extends SQLiteOpenHelper {

    private static final String CLASS_NAME = DBHelper.class.getSimpleName();

    /**
     * Name of the Database of the Application.
     */
    private static final String DB_NAME = "OpenRadioDB";

    /**
     * Name of the Table for the Favorites Radio Stations.
     */
    public static final String FAVORITES_TABLE_NAME = "FavoritesTable";

    public DBHelper(final Context context) {
        super(context, DB_NAME, null, AppUtils.getApplicationCode(context));
    }

    /**
     * This method will be called only for CLEAN database.
     * It should create all needed tables in database, no ALTERs.
     * If you need to add new table at some point of already published project, you need to
     * add this new table in onUpgrade() method of this class.
     */
    @Override
    public final void onCreate(final SQLiteDatabase db) {
        Log.d(CLASS_NAME, "OnCreate");

        String cmd = "CREATE TABLE " + FAVORITES_TABLE_NAME + "(" +
                "id VARCHAR PRIMARY KEY ASC, " +
                "name VARCHAR, " +
                "streamUrl VARCHAR, " +
                "webSite VARCHAR, " +
                "country VARCHAR, " +
                "genre VARCHAR, " +
                "imageUrl VARCHAR, " +
                "thumbUrl VARCHAR " +
                ")";
        db.execSQL(cmd);
        Log.i(CLASS_NAME, "Table created: " + FAVORITES_TABLE_NAME);
    }

    @Override
    public final void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        Log.d(CLASS_NAME, "OnUpgrade from " + oldVersion + " to " + newVersion);
    }
}
