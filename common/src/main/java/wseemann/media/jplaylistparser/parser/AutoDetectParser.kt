/*
 * Copyright 2014 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wseemann.media.jplaylistparser.parser

import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.AppUtils
import okhttp3.*
import wseemann.media.jplaylistparser.exception.JPlaylistParserException
import wseemann.media.jplaylistparser.mime.MediaType.Companion.parse
import wseemann.media.jplaylistparser.parser.asx.ASXPlaylistParser
import wseemann.media.jplaylistparser.parser.m3u.M3UPlaylistParser
import wseemann.media.jplaylistparser.parser.m3u8.M3U8PlaylistParser
import wseemann.media.jplaylistparser.parser.pls.PLSPlaylistParser
import wseemann.media.jplaylistparser.parser.xspf.XSPFPlaylistParser
import wseemann.media.jplaylistparser.playlist.Playlist
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AutoDetectParser(private val mTimeout: Int) {

    @Throws(IOException::class, JPlaylistParserException::class)
    fun parse(url: String, mimeType: String?, stream: InputStream, playlist: Playlist) {
        var mimeTypeCpy = mimeType
        if (mimeTypeCpy == null) {
            mimeTypeCpy = AppUtils.EMPTY_STRING
        }
        if (mimeTypeCpy.split(";".toRegex()).toTypedArray().isNotEmpty()) {
            mimeTypeCpy = mimeTypeCpy.split(";".toRegex()).toTypedArray()[0]
        }
        val m3uPlaylistParser = M3UPlaylistParser(mTimeout)
        val m3u8PlaylistParser = M3U8PlaylistParser(mTimeout)
        val plsPlaylistParser = PLSPlaylistParser(mTimeout)
        val xspfPlaylistParser = XSPFPlaylistParser(mTimeout)
        val asxPlaylistParser = ASXPlaylistParser(mTimeout)
        var extension: String = getFileExtension(url)
        val parser: Parser
        if (extension.equals(M3UPlaylistParser.EXTENSION, ignoreCase = true)
                || m3uPlaylistParser.supportedTypes.contains(parse(mimeTypeCpy)) &&
                !extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = m3uPlaylistParser
        } else if (extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)
                || m3uPlaylistParser.supportedTypes.contains(parse(mimeTypeCpy))) {
            parser = m3u8PlaylistParser
        } else if (extension.equals(PLSPlaylistParser.EXTENSION, ignoreCase = true)
                || plsPlaylistParser.supportedTypes.contains(parse(mimeTypeCpy))) {
            parser = plsPlaylistParser
        } else if (extension.equals(XSPFPlaylistParser.EXTENSION, ignoreCase = true)
                || xspfPlaylistParser.supportedTypes.contains(parse(mimeTypeCpy))) {
            parser = xspfPlaylistParser
        } else if (extension.equals(ASXPlaylistParser.EXTENSION, ignoreCase = true)
                || asxPlaylistParser.supportedTypes.contains(parse(mimeTypeCpy))) {
            parser = asxPlaylistParser
        } else {
            extension = getStreamExtension(url)
            parser = if (extension.equals(M3UPlaylistParser.EXTENSION, ignoreCase = true)
                    && !extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
                m3uPlaylistParser
            } else if (extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
                m3u8PlaylistParser
            } else if (extension.equals(PLSPlaylistParser.EXTENSION, ignoreCase = true)) {
                plsPlaylistParser
            } else if (extension.equals(XSPFPlaylistParser.EXTENSION, ignoreCase = true)) {
                xspfPlaylistParser
            } else if (extension.equals(ASXPlaylistParser.EXTENSION, ignoreCase = true)) {
                asxPlaylistParser
            } else {
                throw JPlaylistParserException("Unsupported format:$url")
            }
        }
        parser.parse(url, stream, playlist)
    }

    @Throws(IOException::class, JPlaylistParserException::class)
    fun parse(url: String?, playlist: Playlist) {
        requireNotNull(url) { "URI cannot be NULL" }
        val m3uPlaylistParser = M3UPlaylistParser(mTimeout)
        val m3u8PlaylistParser = M3U8PlaylistParser(mTimeout)
        val plsPlaylistParser = PLSPlaylistParser(mTimeout)
        val xspfPlaylistParser = XSPFPlaylistParser(mTimeout)
        val asxPlaylistParser = ASXPlaylistParser(mTimeout)
        var extension: String = getFileExtension(url)
        val parser: Parser
        if (extension.equals(M3UPlaylistParser.EXTENSION, ignoreCase = true)
                && !extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = m3uPlaylistParser
        } else if (extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = m3u8PlaylistParser
        } else if (extension.equals(PLSPlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = plsPlaylistParser
        } else if (extension.equals(XSPFPlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = xspfPlaylistParser
        } else if (extension.equals(ASXPlaylistParser.EXTENSION, ignoreCase = true)) {
            parser = asxPlaylistParser
        } else {
            extension = getStreamExtension(url)
            parser = if (extension.equals(M3UPlaylistParser.EXTENSION, ignoreCase = true)
                    && !extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
                m3uPlaylistParser
            } else if (extension.equals(M3U8PlaylistParser.EXTENSION, ignoreCase = true)) {
                m3u8PlaylistParser
            } else if (extension.equals(PLSPlaylistParser.EXTENSION, ignoreCase = true)) {
                plsPlaylistParser
            } else if (extension.equals(XSPFPlaylistParser.EXTENSION, ignoreCase = true)) {
                xspfPlaylistParser
            } else if (extension.equals(ASXPlaylistParser.EXTENSION, ignoreCase = true)) {
                asxPlaylistParser
            } else {
                throw JPlaylistParserException("Unsupported format:$url")
            }
        }
        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        try {
            val urlRefetch = URL(URLDecoder.decode(url, "UTF-8"))
            conn = urlRefetch.openConnection() as HttpURLConnection
            conn.connectTimeout = mTimeout
            conn.readTimeout = mTimeout
            conn.requestMethod = "GET"
            inputStream = conn.inputStream
            parser.parse(urlRefetch.toString(), inputStream, playlist)
        } catch (e: SocketTimeoutException) {
            AppLogger.e("Can not parse uri:$url", e)
        } catch (e: IOException) {
            AppLogger.e("Can not parse uri:$url", e)
        } finally {
            conn?.disconnect()
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    /**/
                }
            }
        }
    }

    fun getFileExtension(uri: String): String {
        var fileExtension = AppUtils.EMPTY_STRING
        var beginIndex = uri.lastIndexOf(".")
        if (beginIndex != -1) {
            var endIndex = uri.length
            fileExtension = uri.substring(beginIndex, endIndex)
            // Keep this order the same!
            endIndex = when {
                fileExtension.startsWith(PLSPlaylistParser.EXTENSION) -> {
                    PLSPlaylistParser.EXTENSION.length
                }
                fileExtension.startsWith(M3U8PlaylistParser.EXTENSION) -> {
                    M3U8PlaylistParser.EXTENSION.length
                }
                fileExtension.startsWith(M3UPlaylistParser.EXTENSION) -> {
                    M3UPlaylistParser.EXTENSION.length
                }
                fileExtension.startsWith(XSPFPlaylistParser.EXTENSION) -> {
                    XSPFPlaylistParser.EXTENSION.length
                }
                fileExtension.startsWith(ASXPlaylistParser.EXTENSION) -> {
                    ASXPlaylistParser.EXTENSION.length
                }
                else -> {
                    fileExtension.length
                }
            }
            beginIndex = 0
            fileExtension = fileExtension.substring(beginIndex, endIndex)
        }
        return fileExtension
    }

    fun getStreamExtension(url: String, withAnalytics: Boolean = true): String {
        if (withAnalytics) {
            AnalyticsUtils.logMessage("UnsupportedPlaylist:$url")
        }
        var result = AppUtils.EMPTY_STRING
        val httpUrl = HttpUrl.parse(url)
        if (httpUrl == null) {
            if (withAnalytics) {
                AnalyticsUtils.logUnsupportedInvalidPlaylist(url)
            }
            return result
        }
        if (withAnalytics) {
            AnalyticsUtils.logUnsupportedPlaylist(url)
        }
        val client = OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(mTimeout.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(mTimeout.toLong(), TimeUnit.MILLISECONDS)
                .build()

        val request = Request.Builder().url(url).build()
        val latch = CountDownLatch(1)
        AppLogger.d("StreamExtension:$url")
        client.newCall(request).enqueue(
                object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        // Ignore
                        latch.countDown()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        AppLogger.d("StreamExtension:response:${response.headers()}")
                        val content = response.header("content-disposition", AppUtils.EMPTY_STRING)
                        result = getFileExtension(getFileExtFromHeaderParam(content))
//                        if (result.isEmpty()) {
//                            content = response.header("Content-Type", "")
//                            if (content.isNullOrEmpty()) {
//                                latch.countDown()
//                                return
//                            }
//                            if (content.toLowerCase(Locale.ROOT) == "audio/mpeg") {
//                                result = M3UPlaylistParser.EXTENSION
//                            }
//                        }
                        latch.countDown()
                    }
                }
        )
        latch.await((mTimeout + 1000).toLong(), TimeUnit.SECONDS)
        AppLogger.d("Stream ext:$result")
        return result
    }

    companion object {

        fun getFileExtFromHeaderParam(headerParam: String?): String {
            if (headerParam.isNullOrEmpty()) {
                return AppUtils.EMPTY_STRING
            }
            val data = headerParam.split("filename=")
            if (data.size == 2) {
                return data[1]
            }
            return AppUtils.EMPTY_STRING
        }
    }
}
