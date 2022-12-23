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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.yuriy.openradio.shared.dependencies.DependencyRegistryCommon

class ImagesProvider : ContentProvider() {

    private lateinit var mImagesPersistenceLayer: ImagesPersistenceLayer

    fun configureWith(imagesPersistenceLayer: ImagesPersistenceLayer) {
        mImagesPersistenceLayer = imagesPersistenceLayer
    }

    override fun onCreate(): Boolean {
        DependencyRegistryCommon.init(context!!)
        DependencyRegistryCommon.inject(this)
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        mImagesPersistenceLayer.delete(uri)
        return 1
    }

    override fun getType(uri: Uri): String {
        if (!ImagesStore.isAuthorised(uri)) {
            return ImagesStore.CONTENT_TYPE_UNKNOWN
        }
        return ImagesStore.CONTENT_TYPE_IMAGE
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        return Uri.EMPTY
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return mImagesPersistenceLayer.open(uri)
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }
}
