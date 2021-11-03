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

package com.yuriy.openradio.shared.model.storage.cache.api

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = PersistentApiContract.TABLE_NAME)
data class PersistentApiEntry(
    /**
     * Primary Key
     */
    @PrimaryKey(autoGenerate = true) val id: Long? = null,

    val name: String,

    val data: String,

    val timestamp: Long
)
