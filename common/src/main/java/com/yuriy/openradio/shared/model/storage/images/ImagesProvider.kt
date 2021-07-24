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
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.yuriy.openradio.shared.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ImagesProvider : ContentProvider() {

    private lateinit var mImagesDatabase: ImagesDatabase

    override fun onCreate(): Boolean {
        AppLogger.d("$TAG created")
        mImagesDatabase = ImagesDatabase.getInstance(context!!)
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String {
        if (uri.authority != ImagesStore.AUTHORITY) {
            return ImagesStore.CONTENT_TYPE_UNKNOWN
        }
        return ImagesStore.CONTENT_TYPE_IMAGE
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (uri.authority != ImagesStore.AUTHORITY) {
            return Uri.EMPTY
        }
        if (values == null) {
            return Uri.EMPTY
        }
        val rsId = ImagesStore.getRsId(values)
        if (rsId.isEmpty()) {
            return Uri.EMPTY
        }
        val imageUrl = ImagesStore.getImageUrl(values)
        if (imageUrl.isEmpty()) {
            return Uri.EMPTY
        }
        AppLogger.d("$TAG insert $rsId $imageUrl")
        downloadImage(rsId, imageUrl)
        return Uri.EMPTY
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        AppLogger.d("$TAG open file :$uri")
        val context = this.context ?: return null
        val rsId = uri.lastPathSegment
        if (rsId.isNullOrEmpty()) {
            AppLogger.w("$TAG open file for $uri has no valid id")
            return null
        }
        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File.createTempFile(TMP_FILE_NAME, TMP_FILE_EXT, path)
        val os = FileOutputStream(file)
        os.write(getFileBytes(rsId))
        os.close()
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
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

    private fun getImageBytes(imageUrl: String): ByteArray {
        val output = ByteArrayOutputStream()
        try {
            URL(imageUrl).openStream().use { stream ->
                val buffer = ByteArray(TMP_FILE_BUFFER)
                while (true) {
                    val bytesRead = stream.read(buffer)
                    if (bytesRead < 0) {
                        break
                    }
                    output.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: Exception) {
            output.reset()
        }
        return output.toByteArray()
    }

    private fun getFileBytes(rsId: String): ByteArray {
        val image = mImagesDatabase.rsImageDao().getImage(rsId) ?: return ByteArray(0)
        return image.mData ?: return ByteArray(0)
    }

    private fun downloadImage(rsId: String, imageUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val existedBytes = getFileBytes(rsId)
            if (existedBytes.isNotEmpty()) {
                return@launch
            }
            val bytes = getImageBytes(imageUrl)
            if (bytes.isEmpty()) {
                return@launch
            }
            mImagesDatabase.rsImageDao().insertImage(Image(rsId, bytes))
            AppLogger.d(
                "$TAG image $imageUrl inserted, num of images:${mImagesDatabase.rsImageDao().getCount()}"
            )
        }
    }

    companion object {

        private val TAG = ImagesProvider::class.java.simpleName
        private const val TMP_FILE_NAME = "rs_img_tmp"
        private const val TMP_FILE_EXT = ".jpg"
        private const val TMP_FILE_BUFFER = 1024
    }
}
