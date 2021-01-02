/*
 * Copyright 2019 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.yuriy.openradio.shared.broadcast.ConnectivityReceiver
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppUtils.generateRandomHexToken
import com.yuriy.openradio.shared.utils.AppUtils.isWebUrl
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException

/**
 * Utility class to handle operations over files.
 */
object FileUtils {
    private const val IO_BUFFER_SIZE = 8 * 1024

    /**
     * This method creates a directory with given name is such does not exists
     *
     * @param path a path to the directory
     */
    fun createDirIfNeeded(path: String) {
        val file = File(path)
        deleteFile(file)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    /**
     *
     * @param filePath
     * @return
     */
    @JvmStatic
    fun deleteFile(filePath: String): Boolean {
        return deleteFile(File(filePath))
    }

    /**
     *
     * @param file
     * @return
     */
    private fun deleteFile(file: File?): Boolean {
        if (file == null) {
            return false
        }
        return if (isFileExists(file)) {
            file.delete()
        } else false
    }

    /**
     *
     * @param context
     * @return
     */
    @JvmStatic
    fun getFilesDir(context: Context): File {
        return context.filesDir
    }

    /**
     *
     * @param context
     * @param filePath
     * @return
     */
    @JvmStatic
    fun copyExtFileToIntDir(context: Context, filePath: String): String? {
        if (TextUtils.isEmpty(filePath)) {
            return filePath
        }
        val directory = getFilesDir(context)
        val file = File(directory, generateRandomHexToken(16))
        val out: OutputStream
        out = try {
            FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            logException(e)
            return null
        }
        downloadUrlToStream(context, filePath, out)
        try {
            out.close()
        } catch (e: IOException) {
            /* Ignore */
        }
        return if (!isFileExists(file)) {
            null
        } else file.absolutePath
    }

    /**
     * Download a bitmap from a URL and write the content to an output stream.
     *
     * @param context      Context of a callee.
     * @param urlString    The URL to fetch.
     * @param outputStream
     * @return true if successful, false otherwise
     */
    private fun downloadUrlToStream(context: Context?,
                                    urlString: String,
                                    outputStream: OutputStream?): Boolean {
        var connection: HttpURLConnection? = null
        var out: BufferedOutputStream? = null
        var bufferedInputStream: BufferedInputStream? = null
        try {
            if (isWebUrl(urlString)) {
                if (ConnectivityReceiver.checkConnectivityAndNotify(context!!)) {
                    connection = NetUtils.getHttpURLConnection(context, urlString, "GET")
                    if (connection == null) {
                        return false
                    }
                    bufferedInputStream = BufferedInputStream(connection.inputStream, IO_BUFFER_SIZE)
                }
            } else {
                bufferedInputStream = BufferedInputStream(FileInputStream(File(urlString)), IO_BUFFER_SIZE)
            }
            out = BufferedOutputStream(outputStream, IO_BUFFER_SIZE)
            if (bufferedInputStream == null) {
                return false
            }
            var b: Int
            while (bufferedInputStream.read().also { b = it } != -1) {
                out.write(b)
            }
            return true
        } catch (e: SocketTimeoutException) {
            logException(Exception("url:$urlString", e))
        } catch (e: IOException) {
            logException(Exception("url:$urlString", e))
        } finally {
            NetUtils.closeHttpURLConnection(connection)
            try {
                out?.close()
                bufferedInputStream?.close()
            } catch (e: IOException) {
                /* Ignore */
            }
        }
        return false
    }

    /**
     *
     * @param path
     * @return
     */
    fun createFileIfNeeded(path: String): File {
        val file = File(path)
        try {
            val result = file.createNewFile()
        } catch (e: IOException) {
            logException(
                    FileNotFoundException("""File $path not created:
${Log.getStackTraceString(e)}""")
            )
        }
        return file
    }

    private fun isFileExists(file: File?): Boolean {
        return if (file == null) {
            false
        } else file.exists() && !file.isDirectory
    }
}
