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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rsimages")
data class Image(
    @PrimaryKey
    @ColumnInfo(name = "rsId")
    val mRsId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var mData: ByteArray? = null
) {

    override fun toString() = mRsId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (mRsId != other.mRsId) return false
        if (mData != null) {
            if (other.mData == null) return false
            if (!mData.contentEquals(other.mData)) return false
        } else if (other.mData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mRsId.hashCode()
        result = 31 * result + (mData?.contentHashCode() ?: 0)
        return result
    }
}
