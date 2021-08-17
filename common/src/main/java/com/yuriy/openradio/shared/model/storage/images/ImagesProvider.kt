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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.yuriy.openradio.shared.dependencies.DependencyRegistry
import com.yuriy.openradio.shared.dependencies.DownloaderDependency
import com.yuriy.openradio.shared.dependencies.ImagesDatabaseDependency
import com.yuriy.openradio.shared.dependencies.NetworkMonitorDependency
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.NetUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class ImagesProvider : ContentProvider(), NetworkMonitorDependency, DownloaderDependency, ImagesDatabaseDependency {

    private lateinit var mImagesDatabase: ImagesDatabase
    private lateinit var mNetworkMonitor: NetworkMonitor
    private lateinit var mDownloader: Downloader

    /**
     * Coroutine scope to handle non UI operations.
     */
    private val mIoScope = CoroutineScope(Job() + Dispatchers.IO)

    override fun configureWith(networkMonitor: NetworkMonitor) {
        mNetworkMonitor = networkMonitor
    }

    override fun configureWith(downloader: Downloader) {
        mDownloader = downloader
    }

    override fun configureWith(database: ImagesDatabase) {
        mImagesDatabase = database
    }

    override fun onCreate(): Boolean {
        DependencyRegistry.init(context!!)
        DependencyRegistry.injectNetworkMonitor(this)
        DependencyRegistry.injectDownloader(this)
        DependencyRegistry.injectImagesDatabase(this)
        return true
    }

    /**
     * Refer to https://kotlinlang.org/docs/reference/java-interop.html#finalize
     */
    protected fun finalize() {
        AppLogger.w("$TAG finalized")
        mIoScope.cancel()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if (!ImagesStore.isAuthorised(uri)) {
            AppLogger.e("$TAG delete '$uri' with invalid auth")
            return 0
        }
        AppLogger.d("$TAG delete with '$uri'")
        val id = ImagesStore.getId(uri)
        if (id.isNotEmpty()) {
            mIoScope.launch(Dispatchers.IO) {
                mImagesDatabase.rsImageDao().delete(id)
            }
            return 1
        }
        return 0
    }

    override fun getType(uri: Uri): String {
        if (!ImagesStore.isAuthorised(uri)) {
            return ImagesStore.CONTENT_TYPE_UNKNOWN
        }
        return ImagesStore.CONTENT_TYPE_IMAGE
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        AppLogger.d("$TAG insert with $uri and $values")
        return Uri.EMPTY
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        AppLogger.d("$TAG open file for $uri")
        val id = ImagesStore.getId(uri)
        if (id.isEmpty()) {
            AppLogger.e("$TAG open file for $uri has no valid id")
            return null
        }
        val url = ImagesStore.getImageUrl(uri)
        if (url.isEmpty()) {
            AppLogger.e("$TAG open file for $uri has no valid url")
            return null
        }

        var bytes: ByteArray
        runBlocking(Dispatchers.IO) {
            bytes = getFileBytes(id)
        }
        if (bytes.isEmpty()) {
            AppLogger.w("$TAG no bytes available for $uri")
            mIoScope.launch(Dispatchers.IO) {
                try {
                    ImageDownloader(id, url, ImagesStore.buildImageLoadedUri(id)).run()
                } catch (e: Exception) {
                    AppLogger.e("$TAG can't handle image", e)
                }
            }
            return null
        }

        val pipe: Array<ParcelFileDescriptor>
        return try {
            pipe = ParcelFileDescriptor.createPipe()
            mIoScope.launch(Dispatchers.IO) {
                val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
                try {
                    // Write data to pipe:
                    output.write(bytes)
                } catch (e: IOException) {
                    AppLogger.e("$TAG exception transferring file", e)
                } finally {
                    output.flush()
                    output.close()
                }
            }
            pipe[0]
        } catch (e: IOException) {
            throw FileNotFoundException("Could not open pipe")
        }
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

    private fun getFileBytes(rsId: String): ByteArray {
        val image = mImagesDatabase.rsImageDao().getImage(rsId) ?: return ByteArray(0)
        return image.mData ?: return ByteArray(0)
    }

    inner class ImageDownloader(private val mId: String, private val mImageUrl: String, private val mUri: Uri) :
        Runnable {

        init {
            AppLogger.d("$TAG new task created for $mId $mImageUrl")
        }

        override fun run() {
            val existedBytes = getFileBytes(mId)
            if (existedBytes.isNotEmpty()) {
                AppLogger.d("$TAG bytes for $mId already exists")
                return
            }
            AppLogger.d("$TAG execute task for $mId $mImageUrl")
            var orientation = ExifInterface.ORIENTATION_NORMAL
            var bytes = if (NetUtils.isWebUrl(mImageUrl)) {
                mDownloader.downloadDataFromUri(
                    context!!, Uri.parse(mImageUrl), contentTypeFilter = HTTPDownloaderImpl.CONTENT_TYPE_IMG
                )
            } else {
                val file = File(mImageUrl)
                val exif = ExifInterface(file.absoluteFile.toString())
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                file.readBytes()
            }
            AppLogger.d("$TAG downloaded ${bytes.size} bytes")
            if (bytes.isEmpty()) {
                return
            }
            if (bytes.size >= MAX_IMG_SIZE) {
                return
            }
            bytes = scaleBytes(bytes, orientation)
            AppLogger.d("$TAG scaled to ${bytes.size} bytes")
            if (bytes.isEmpty()) {
                return
            }
            mImagesDatabase.rsImageDao().insertImage(Image(mId, bytes))
            AppLogger.d("$TAG db contains ${mImagesDatabase.rsImageDao().getCount()} images")
            context?.contentResolver?.notifyChange(mUri, null)
        }

        private fun scaleBytes(bytes: ByteArray, orientation: Int): ByteArray {
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ByteArray(0)
            var width = bmp.width
            var height = bmp.height
            AppLogger.d("$TAG origin image [${width}x${height}]")
            when {
                width > height -> {
                    // landscape
                    val ratio: Double = width / MAX_WIDTH
                    width = MAX_WIDTH.toInt()
                    height = (height / ratio).toInt()
                }
                height > width -> {
                    // portrait
                    val ratio: Double = height / MAX_HEIGHT
                    height = MAX_HEIGHT.toInt()
                    width = (width / ratio).toInt()
                }
                else -> {
                    // square
                    height = MAX_HEIGHT.toInt()
                    width = MAX_WIDTH.toInt()
                }
            }
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            }
            bmp = Bitmap.createScaledBitmap(bmp, width, height, true)
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true)
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 0, stream)
            bmp.recycle()
            stream.close()
            return stream.toByteArray()
        }
    }

    companion object {

        private val TAG = ImagesProvider::class.java.simpleName
        private const val MAX_WIDTH: Double = 500.0
        private const val MAX_HEIGHT: Double = 500.0

        /**
         * Maximum bytes for an image.
         */
        private const val MAX_IMG_SIZE = 1000000
    }
}
