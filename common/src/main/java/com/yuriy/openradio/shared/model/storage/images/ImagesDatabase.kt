/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.storage.images

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database for images.
 */
@Database(entities = [Image::class], version = 1, exportSchema = false)
abstract class ImagesDatabase : RoomDatabase() {

    abstract fun rsImageDao(): ImageDao

    companion object {

        private const val DATABASE_NAME = "rsimages-db"

        // For Singleton instantiation
        @Volatile
        private var instance: ImagesDatabase? = null

        fun getInstance(context: Context): ImagesDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create the database.
        private fun buildDatabase(context: Context): ImagesDatabase {
            return Room.databaseBuilder(context, ImagesDatabase::class.java, DATABASE_NAME)
                .build()
        }
    }
}
