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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImageDao {

    @Query("SELECT * FROM rsimages WHERE rsId = :rsId")
    fun getImage(rsId: String): Image?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertImage(rsImage: Image)

    @Query("DELETE FROM rsimages WHERE rsId = :rsId")
    fun delete(rsId: String)

    @Query("SELECT COUNT(DISTINCT rsId) FROM rsimages")
    fun getCount(): Int

    @Query("DELETE FROM rsimages")
    fun deleteAll()
}
