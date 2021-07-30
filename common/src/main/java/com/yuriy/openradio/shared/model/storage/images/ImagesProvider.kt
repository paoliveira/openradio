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
import android.os.Environment
import android.os.ParcelFileDescriptor
import com.yuriy.openradio.shared.coroutines.launch
import com.yuriy.openradio.shared.dependencies.DependencyRegistry
import com.yuriy.openradio.shared.dependencies.DownloaderDependency
import com.yuriy.openradio.shared.dependencies.ImagesDatabaseDependency
import com.yuriy.openradio.shared.dependencies.NetworkMonitorDependency
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.NetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class ImagesProvider : ContentProvider(), NetworkMonitorDependency, DownloaderDependency, ImagesDatabaseDependency {

    private lateinit var mImagesDatabase: ImagesDatabase
    private lateinit var mNetworkMonitor: NetworkMonitor
    private lateinit var mDownloader: Downloader
    private val mExecutor = Executors.newFixedThreadPool(8)

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
            AppLogger.w("$TAG insert '$uri' with invalid auth")
            return Uri.EMPTY
        }
        if (values == null) {
            AppLogger.w("$TAG insert with no values")
            return Uri.EMPTY
        }
        val rsId = ImagesStore.getRsId(values)
        if (rsId.isEmpty()) {
            AppLogger.w("$TAG insert with empty id")
            return Uri.EMPTY
        }
        val imageUrl = ImagesStore.getImageUrl(values)
        if (imageUrl.isEmpty()) {
            AppLogger.w("$TAG insert with empty image url for $rsId")
            return Uri.EMPTY
        }
        val task = ImageDownloader(rsId, imageUrl)
        mExecutor.submit(task)
        return Uri.EMPTY
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        AppLogger.d("$TAG open file for $uri")
        val context = this.context ?: return null
        val rsId = uri.lastPathSegment
        if (rsId.isNullOrEmpty()) {
            AppLogger.w("$TAG open file for $uri has no valid id")
            return null
        }

        var bytes: ByteArray
        runBlocking(Dispatchers.IO) {
            bytes = getFileBytes(rsId)
        }
        if (bytes.isEmpty()) {
            AppLogger.w("$TAG no bytes available for $uri")
            return null
        }

        val path = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File.createTempFile(TMP_FILE_NAME, TMP_FILE_EXT, path)
        val os = FileOutputStream(file)
        os.write(bytes)
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

    private fun getFileBytes(rsId: String): ByteArray {
        val image = mImagesDatabase.rsImageDao().getImage(rsId) ?: return ByteArray(0)
        return image.mData ?: return ByteArray(0)
    }

    inner class ImageDownloader(private val mRsId: String, private val mImageUrl: String) : Runnable {

        init {
            AppLogger.d("$TAG new task created for $mRsId $mImageUrl")
        }

        override fun run() {
            val existedBytes = getFileBytes(mRsId)
            if (existedBytes.isNotEmpty()) {
                AppLogger.d("$TAG bytes for $mRsId already exists")
                return
            }
            AppLogger.d("$TAG execute task for $mRsId $mImageUrl")
            var orientation = ExifInterface.ORIENTATION_NORMAL
            val bytes = if (NetUtils.isWebUrl(mImageUrl)) {
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
            mImagesDatabase.rsImageDao().insertImage(Image(mRsId, scaleBytes(bytes, orientation)))
            AppLogger.d("$TAG db contains ${mImagesDatabase.rsImageDao().getCount()} images")
        }

        private fun scaleBytes(bytes: ByteArray, orientation: Int): ByteArray {
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            var width = bmp.width
            var height = bmp.height
            when {
                width > height -> {
                    // landscape
                    val ratio = width / MAX_WIDTH
                    width = MAX_WIDTH
                    height /= ratio
                }
                height > width -> {
                    // portrait
                    val ratio = height / MAX_HEIGHT
                    height = MAX_HEIGHT
                    width /= ratio
                }
                else -> {
                    // square
                    height = MAX_HEIGHT
                    width = MAX_WIDTH
                }
            }
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            }
            bmp = Bitmap.createScaledBitmap(bmp, width, height, true)
            bmp = Bitmap.createBitmap(bmp, 0,0 , width, height, matrix, true)
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 0, stream)
            bmp.recycle()
            stream.close()
            return stream.toByteArray()
        }
    }

    companion object {

        private val TAG = ImagesProvider::class.java.simpleName
        private const val TMP_FILE_NAME = "rs_img_tmp"
        private const val TMP_FILE_EXT = ".jpg"
        private const val MAX_WIDTH = 500
        private const val MAX_HEIGHT = 500
    }
}
