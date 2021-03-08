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

package com.yuriy.openradio.shared.model.storage

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.*

class ImagesProvider : ContentProvider() {

    private lateinit var mExecutor: ExecutorService

    override fun onCreate(): Boolean {
        mExecutor = Executors.newCachedThreadPool()
        AppLogger.d("$TAG created")
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
        if (context == null) {
            return Uri.EMPTY
        }
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
        val file = getImage(context!!, rsId)
        if (file == null) {
            AppLogger.d("$TAG insert $file is null")
            return Uri.EMPTY
        }
        AppLogger.d("$TAG file (${file.exists()}) $file")
        if (!file.exists()) {
            mExecutor.submit {
                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(file)
                    FileUtils.downloadUrlToStream(context!!, imageUrl, outputStream)
                    outputStream.flush()
                    AppLogger.d("$TAG file $file downloaded ${file.exists()}")

                    notifyReady(context!!, uri, file.absolutePath)
                } catch (e: IOException) {
                    AppLogger.e("$TAG can't download RS art:$e")
                } finally {
                    try {
                        outputStream!!.close()
                    } catch (e: IOException) {
                        AppLogger.e("$TAG can't close stream:$e")
                    }
                }
            }
        } else {
            notifyReady(context!!, uri, file.absolutePath)
        }

        return Uri.EMPTY
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        AppLogger.d("$TAG open file:$uri")
        val context = this.context ?: return null
        val file = File(uri.path ?: "")
        if (!file.exists()) {
            throw FileNotFoundException(uri.path)
        }
        // Only allow access to files under cache path
        val cachePath = context.cacheDir.path
        if (!file.path.startsWith(cachePath)) {
            throw FileNotFoundException()
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    companion object {

        private val TAG = ImagesProvider::class.java.simpleName

        private fun getImage(context: Context, id: String): File? {
            val contextWrapper = ContextWrapper(context)
            val directory: File = contextWrapper.cacheDir
            return try {
                File(directory, "$id.jpeg")
            } catch (e: Exception) {
                null
            }
        }

        private fun notifyReady(context: Context, uri: Uri, path: String) {
            context.contentResolver.notifyChange(ImagesStore.buildNotifyInsertedUri(uri, path), null)
        }
    }
}
