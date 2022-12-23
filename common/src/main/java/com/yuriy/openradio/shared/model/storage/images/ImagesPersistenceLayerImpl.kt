/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.yuriy.openradio.shared.model.net.DownloaderLayer
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.NetUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Implementation fo the [ImagesPersistenceLayer] interface to work with room database.
 */
class ImagesPersistenceLayerImpl(
    private val mContext: Context,
    private val mDownloader: DownloaderLayer,
    private val mImagesDatabase: ImagesDatabase
) : ImagesPersistenceLayer {

    /**
     * Coroutine scope to handle non UI operations.
     */
    private val mIoScope = CoroutineScope(Job() + Dispatchers.IO)
    private val mUiScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun open(uri: Uri): ParcelFileDescriptor? {
        AppLogger.d("$TAG open file for '$uri'")
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
                    ImageDownloader(id, url, uri).run()
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
                    //val uriToReport = uri.buildUpon().appendQueryParameter("load", "success").build()
                    //mContext.contentResolver?.notifyChange(uriToReport, null)
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

    override fun delete(uri: Uri) {
        if (!ImagesStore.isAuthorised(uri)) {
            AppLogger.e("$TAG delete '$uri' with invalid auth")
            return
        }
        AppLogger.d("$TAG delete with '$uri'")
        val id = ImagesStore.getId(uri)
        if (id.isNotEmpty()) {
            delete(id)
        }
    }

    override fun delete(mediaId: String) {
        mIoScope.launch(Dispatchers.IO) {
            synchronized(mImagesDatabase) {
                mImagesDatabase.rsImageDao().delete(mediaId)
            }
        }
    }

    override fun deleteAll() {
        synchronized(mImagesDatabase) {
            mImagesDatabase.rsImageDao().deleteAll()
        }
    }

    private fun getFileBytes(rsId: String): ByteArray {
        synchronized(mImagesDatabase) {
            val image = mImagesDatabase.rsImageDao().getImage(rsId) ?: return ByteArray(0)
            return image.mData ?: return ByteArray(0)
        }
    }

    private inner class ImageDownloader(
        private val mId: String,
        private val mImageUrl: String,
        private val mUri: Uri
    ) :
        Runnable {

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
                    mContext, Uri.parse(mImageUrl), contentTypeFilter = HTTPDownloaderImpl.CONTENT_TYPE_IMG
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
            synchronized(mImagesDatabase) {
                mImagesDatabase.rsImageDao().insertImage(Image(mId, bytes))
            }
            val uriToReport = mUri.buildUpon().appendQueryParameter("load", "success").build()
            AppLogger.d("$TAG image downloaded for $mUri, notify $uriToReport")
            mContext.contentResolver?.notifyChange(uriToReport, null)
        }

        private fun scaleBytes(bytes: ByteArray, orientation: Int): ByteArray {
            var bmp = try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ByteArray(0)
            } catch (exception: OutOfMemoryError) {
                return handleOOM(bytes.size, exception)
            }
            var width = bmp.width
            var height = bmp.height
            AppLogger.d("$TAG origin image [${width}x${height}]")
            when {
                width > height -> {
                    // landscape
                    val ratio = width / MAX_WIDTH
                    width = MAX_WIDTH.toInt()
                    height = (height / ratio).toInt()
                }

                height > width -> {
                    // portrait
                    val ratio = height / MAX_HEIGHT
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
            try {
                bmp = Bitmap.createScaledBitmap(bmp, width, height, true)
                bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true)
            } catch (exception: OutOfMemoryError) {
                return handleOOM(bytes.size, exception)
            }
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 0, stream)
            bmp.recycle()
            stream.close()
            return stream.toByteArray()
        }

        private fun handleOOM(bytesSize: Int, exception: OutOfMemoryError): ByteArray {
            System.gc()
            AppLogger.e("$TAG can't decode $bytesSize bytes for $mImageUrl", exception)
            AnalyticsUtils.logBitmapDecode(mImageUrl, bytesSize)
            return ByteArray(0)
        }
    }

    companion object {
        private const val TAG = "IPL"
        private const val MAX_WIDTH = 500.0
        private const val MAX_HEIGHT = 500.0

        /**
         * Maximum bytes for an image.
         */
        private const val MAX_IMG_SIZE = 1000000
    }
}