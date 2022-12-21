/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.model.net

import android.content.Context
import android.net.Uri
import androidx.core.util.Pair
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.NetUtils
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 *
 * [HTTPDownloaderImpl] allows to download data from the
 * resource over HTTP protocol.
 */
class HTTPDownloaderImpl : DownloaderLayer {

    private var mUrlsSet: Array<String>? = null
    private val mRandom = Random()
    private val mExecutor = Executors.newFixedThreadPool(8)

    override fun downloadDataFromUri(
        context: Context, uri: Uri,
        parameters: List<Pair<String, String>>,
        contentTypeFilter: String
    ): ByteArray {
        val task = BytesDownloader(mUrlsSet, mRandom, context, uri, parameters, contentTypeFilter)
        return mExecutor.submit(task).get()
    }

    class BytesDownloader(
        private var mUrlsSet: Array<String>?,
        private val mRandom: Random,
        private val context: Context, private val uri: Uri,
        private val parameters: List<Pair<String, String>>,
        private val contentTypeFilter: String
    ) : Callable<ByteArray> {

        override fun call(): ByteArray {
            var response = ByteArray(0)
            val url = getConnectionUrl(uri, parameters) ?: return response
            AppLogger.i("$CLASS_NAME Request URL:$url")
            val connection = NetUtils.getHttpURLConnection(
                context,
                url,
                if (parameters.isEmpty()) NetUtils.HTTP_METHOD_GET else NetUtils.HTTP_METHOD_POST,
                parameters
            ) ?: return response
            var responseCode = 0
            try {
                responseCode = connection.responseCode
            } catch (exception: IOException) {
                AppLogger.e(
                    "$CLASS_NAME getResponse ${
                        NetUtils.createExceptionMessage(
                            url.toString(),
                            parameters
                        )
                    }", exception
                )
            }
            AppLogger.d("$CLASS_NAME response code:$responseCode for $url")
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode > HttpURLConnection.HTTP_MULT_CHOICE - 1) {
                NetUtils.closeHttpURLConnection(connection)
                AppLogger.e(
                    "$CLASS_NAME ${NetUtils.createExceptionMessage(url.toString(), parameters)}",
                    Exception("Response code is $responseCode")
                )
                return response
            }

            val contentType = connection.getHeaderField("Content-Type")
            AppLogger.d("$CLASS_NAME content type:$contentType for $url")

            if (contentTypeFilter != AppUtils.EMPTY_STRING && !contentType.startsWith(contentTypeFilter)) {
                NetUtils.closeHttpURLConnection(connection)
                AppLogger.w("$CLASS_NAME filtered out $contentType for $url")
                return response
            }

            try {
                val inputStream = BufferedInputStream(connection.inputStream)
                response = toByteArray(inputStream)
            } catch (exception: IOException) {
                AppLogger.e(
                    "$CLASS_NAME getStream ${
                        NetUtils.createExceptionMessage(
                            url.toString(),
                            parameters
                        )
                    }", exception
                )
            } finally {
                NetUtils.closeHttpURLConnection(connection)
            }
            AppLogger.d("$CLASS_NAME return ${response.size} bytes for $url")
            return response
        }

        /**
         * Do DNS look up in order to get available url for service connection.
         * Addresses, if found, are cached and next time is used from the cache.
         *
         * @param uri        Initial (with dummy and predefined prefix) url to perform look up on.
         * @param parameters Parameters associated with request.
         * @return URL object to do connection with.
         */
        private fun getConnectionUrl(
            uri: Uri,
            parameters: List<Pair<String, String>>
        ): URL? {
            // If there is no predefined prefix - return original URL.
            val uriStr = uri.toString()
            if (!uriStr.startsWith(UrlBuilder.BASE_URL_PREFIX)) {
                return getUrl(uriStr, parameters)
            }

            // Return cached URL if available.
            synchronized(mRandom) {
                if (mUrlsSet != null && mUrlsSet!!.isNotEmpty()) {
                    val i = mRandom.nextInt(mUrlsSet!!.size)
                    return getUrlModified(uriStr, mUrlsSet!![i], parameters)
                }
            }

            // Perform look up and cache results.
            try {
                val list = InetAddress.getAllByName(UrlBuilder.LOOK_UP_DNS)
                synchronized(mRandom) {
                    mUrlsSet = Array(list.size) { AppUtils.EMPTY_STRING }
                    var i = 0
                    for (item in list) {
                        mUrlsSet!![i++] = "https://" + item.canonicalHostName
                        AppLogger.i("$CLASS_NAME look up host:" + mUrlsSet!![i - 1])
                    }
                }
            } catch (exception: UnknownHostException) {
                AppLogger.e(
                    "$CLASS_NAME do lookup ${NetUtils.createExceptionMessage(uri, parameters)}",
                    exception
                )
            }

            // Do random selection from available addresses.
            var url: URL? = null
            synchronized(mRandom) {
                if (mUrlsSet != null && mUrlsSet!!.isNotEmpty()) {
                    val i = mRandom.nextInt(mUrlsSet!!.size)
                    url = getUrlModified(uriStr, mUrlsSet!![i], parameters)
                }
            }

            // Uri to URL parse might fail.
            if (url != null) {
                return url
            }

            // Use predefined addresses, these are needs to be verified time after time in order to be up to date.
            val i = mRandom.nextInt(UrlBuilder.RESERVED_URLS.size)
            return getUrlModified(uriStr, UrlBuilder.RESERVED_URLS[i], parameters)
        }

        private fun getUrlModified(
            uriOrigin: String,
            uri: String?, parameters: List<Pair<String, String>>
        ): URL? {
            val uriModified = uriOrigin.replaceFirst(UrlBuilder.BASE_URL_PREFIX.toRegex(), uri!!)
            return getUrl(uriModified, parameters)
        }

        private fun getUrl(uri: String, parameters: List<Pair<String, String>>): URL? {
            return try {
                URL(uri)
            } catch (exception: MalformedURLException) {
                AppLogger.e(
                    "$CLASS_NAME getUrl ${
                        NetUtils.createExceptionMessage(
                            uri,
                            parameters
                        )
                    }", exception
                )
                null
            }
        }
    }

    companion object {
        /**
         * Tag to use in logging message.
         */
        private const val CLASS_NAME = "HTTPDI"

        /**
         * The default buffer size ({@value}) to use for
         * [.copyLarge]
         */
        private const val DEFAULT_BUFFER_SIZE = 1024 * 4

        /**
         * Represents the end-of-file (or stream).
         */
        private const val EOF = -1

        const val CONTENT_TYPE_IMG = "image/"

        /**
         * Gets the contents of an `InputStream` as a `byte[]`.
         *
         *
         * This method buffers the input internally, so there is no need to use a
         * `BufferedInputStream`.
         *
         * @param input the `InputStream` to read from
         * @return the requested byte array
         * @throws NullPointerException if the input is null
         * @throws IOException          if an I/O error occurs
         */
        @Throws(IOException::class)
        private fun toByteArray(input: InputStream): ByteArray {
            val output = ByteArrayOutputStream()
            copy(input, output)
            return output.toByteArray()
        }

        /**
         * Copies bytes from an `InputStream` to an
         * `OutputStream`.
         *
         *
         * This method buffers the input internally, so there is no need to use a
         * `BufferedInputStream`.
         *
         *
         * Large streams (over 2GB) will return a bytes copied value of
         * `-1` after the copy has completed since the correct
         * number of bytes cannot be returned as an int. For large streams
         * use the `copyLarge(InputStream, OutputStream)` method.
         *
         * @param input  the `InputStream` to read from
         * @param output the `OutputStream` to write to
         * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
         * @throws NullPointerException if the input or output is null
         * @throws IOException          if an I/O error occurs
         * @since 1.1
         */
        @Throws(IOException::class)
        private fun copy(input: InputStream, output: OutputStream): Int {
            val count = copyLarge(input, output)
            return if (count > Int.MAX_VALUE) {
                -1
            } else count.toInt()
        }

        /**
         * Copies bytes from a large (over 2GB) `InputStream` to an
         * `OutputStream`.
         *
         *
         * This method buffers the input internally, so there is no need to use a
         * `BufferedInputStream`.
         *
         *
         * The buffer size is given by [.DEFAULT_BUFFER_SIZE].
         *
         * @param input  the `InputStream` to read from
         * @param output the `OutputStream` to write to
         * @return the number of bytes copied
         * @throws NullPointerException if the input or output is null
         * @throws IOException          if an I/O error occurs
         * @since 1.3
         */
        @Throws(IOException::class)
        private fun copyLarge(input: InputStream, output: OutputStream): Long {
            return copy(input, output, DEFAULT_BUFFER_SIZE)
        }

        /**
         * Copies bytes from an `InputStream` to an `OutputStream`
         * using an internal buffer of the given size.
         *
         *
         * This method buffers the input internally, so there is no need to use
         * a `BufferedInputStream`.
         *
         *
         *
         * @param input      the `InputStream` to read from
         * @param output     the `OutputStream` to write to
         * @param bufferSize the bufferSize used to copy from the input to the output
         * @return the number of bytes copied
         * @throws NullPointerException if the input or output is null
         * @throws IOException          if an I/O error occurs
         * @since 2.5
         */
        @Throws(IOException::class)
        fun copy(
            input: InputStream, output: OutputStream,
            bufferSize: Int
        ): Long {
            return copyLarge(input, output, ByteArray(bufferSize))
        }

        /**
         * Copies bytes from a large (over 2GB) `InputStream` to an
         * `OutputStream`.
         *
         *
         * This method uses the provided buffer, so there is no need to use a
         * `BufferedInputStream`.
         *
         *
         *
         * @param input  the `InputStream` to read from
         * @param output the `OutputStream` to write to
         * @param buffer the buffer to use for the copy
         * @return the number of bytes copied
         * @throws NullPointerException if the input or output is null
         * @throws IOException          if an I/O error occurs
         * @since 2.2
         */
        @Throws(IOException::class)
        private fun copyLarge(
            input: InputStream, output: OutputStream,
            buffer: ByteArray
        ): Long {
            var count: Long = 0
            var n: Int
            while (EOF != input.read(buffer).also { n = it }) {
                output.write(buffer, 0, n)
                count += n.toLong()
            }
            return count
        }
    }
}