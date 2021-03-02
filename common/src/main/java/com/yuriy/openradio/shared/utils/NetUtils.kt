/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import androidx.core.util.Pair
import com.yuriy.openradio.R
import com.yuriy.openradio.shared.model.net.DownloaderException
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppUtils.getUserAgent
import com.yuriy.openradio.shared.view.SafeToast
import okhttp3.internal.Util
import wseemann.media.jplaylistparser.parser.AutoDetectParser
import wseemann.media.jplaylistparser.playlist.Playlist
import wseemann.media.jplaylistparser.playlist.PlaylistEntry
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

object NetUtils {

    private val CLASS_NAME = NetUtils::class.java.simpleName
    private const val USER_AGENT_PARAMETER_KEY = "User-Agent"
    private const val HEADER_FIELD_LOCATION = "Location"

    @JvmStatic
    fun getHttpURLConnection(context: Context,
                             urlString: String, requestMethod: String?): HttpURLConnection? {
        return try {
            getHttpURLConnection(context, URL(urlString), requestMethod, null)
        } catch (exception: MalformedURLException) {
            logException(
                    RuntimeException("Can not get http connection from $urlString", exception)
            )
            null
        }
    }

    @JvmStatic
    fun getHttpURLConnection(context: Context,
                             url: URL,
                             requestMethod: String?,
                             parameters: List<Pair<String, String>>?): HttpURLConnection? {
        var connection: HttpURLConnection? = null
        var isRedirect = false
        var connectUrl = url
        var maxAttempt = 3

        do {
            try {
                isRedirect = false
                connection = connectUrl.openConnection() as HttpURLConnection
                connection.readTimeout = AppUtils.TIME_OUT
                connection.connectTimeout = AppUtils.TIME_OUT
                connection.instanceFollowRedirects = true
                connection.useCaches = false
                connection.defaultUseCaches = false
                connection.requestMethod = requestMethod
                val userAgent = getUserAgent(context)
                try {
                    connection.setRequestProperty(USER_AGENT_PARAMETER_KEY, userAgent)
                } catch (e: Exception) {
                    SafeToast.showAnyThread(context, context.getString(R.string.user_agent_can_not_apply))
                }
                AppLogger.d("$CLASS_NAME UserAgent:$userAgent")

                // If there are http request parameters:
                if (parameters != null && parameters.isNotEmpty()) {
                    connection.setRequestProperty("enctype", "application/x-www-form-urlencoded")
                    try {
                        connection.outputStream.use { outputStream ->
                            BufferedWriter(OutputStreamWriter(outputStream, Util.UTF_8)).use { writer ->
                                writer.write(getPostParametersQuery(parameters))
                                writer.flush()
                            }
                        }
                    } catch (exception: IOException) {
                        logException(
                                DownloaderException(
                                        DownloaderException.createExceptionMessage(url.toString(), parameters),
                                        exception
                                )
                        )
                    }
                }
                connection.connect()
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    if (maxAttempt-- <= 0) {
                        AppLogger.e("$CLASS_NAME redirect reached max attempts number")
                        break
                    }
                    val newUrl = connection.getHeaderField(HEADER_FIELD_LOCATION)
                    connection.disconnect()
                    AppLogger.i("$CLASS_NAME redirect from $connectUrl to $newUrl")
                    connectUrl = URL(newUrl)
                    isRedirect = true
                }
            } catch (exception: Exception) {
                logException(RuntimeException("Can not get http connection from $url", exception))
            }
        } while (isRedirect)

        return connection
    }

    @JvmStatic
    fun closeHttpURLConnection(connection: HttpURLConnection?) {
        if (connection == null) {
            return
        }
        connection.disconnect()
    }

    @JvmStatic
    fun checkResource(context: Context, url: String): Boolean {
        val connection = getHttpURLConnection(context, url, "GET") ?: return false
        val responseCode: Int = try {
            connection.responseCode
        } catch (exception: IOException) {
            closeHttpURLConnection(connection)
            return false
        }
        if (responseCode < HttpURLConnection.HTTP_OK || responseCode > HttpURLConnection.HTTP_MULT_CHOICE - 1) {
            closeHttpURLConnection(connection)
            return false
        }
        closeHttpURLConnection(connection)
        return true
    }

    /**
     * Creates and returns a query of htpp connection parameters.
     *
     * @param params List of the parameters (keys and values).
     * @return String representation of query.
     * @throws UnsupportedEncodingException
     */
    @JvmStatic
    @Throws(UnsupportedEncodingException::class)
    fun getPostParametersQuery(params: List<Pair<String, String>>): String {
        val result = StringBuilder()
        var first = true
        for (pair in params) {
            if (first) {
                first = false
            } else {
                result.append("&")
            }
            result.append(URLEncoder.encode(pair.first, AppUtils.UTF8))
            result.append("=")
            result.append(URLEncoder.encode(pair.second, AppUtils.UTF8))
        }
        AppLogger.i("$CLASS_NAME post query:$result")
        return result.toString()
    }

    @JvmStatic
    fun extractUrlsFromPlaylist(context: Context, playlistUrl: String): Array<String?> {
        val connection = getHttpURLConnection(context, playlistUrl, "GET") ?: return arrayOfNulls(0)
        var inputStream: InputStream? = null
        var result: Array<String?>? = null
        try {
            val contentType = connection.contentType
            inputStream = connection.inputStream
            val parser = AutoDetectParser(AppUtils.TIME_OUT)
            val playlist = Playlist()
            parser.parse(playlistUrl, contentType, inputStream, playlist)
            val length = playlist.playlistEntries.size
            result = arrayOfNulls(length)
            AppLogger.d("$CLASS_NAME Found $length streams associated with $playlistUrl")
            for (i in 0 until length) {
                val entry = playlist.playlistEntries[i]
                result[i] = entry[PlaylistEntry.URI]
                AppLogger.d("$CLASS_NAME - ${result[i]}")
                // TODO: Improve - get the first URL currently.
                break
            }
        } catch (e: Exception) {
            val errorMessage = "Can not get urls from playlist at $playlistUrl"
            logException(Exception(errorMessage, e))
        } finally {
            closeHttpURLConnection(connection)
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    /**/
                }
            }
        }
        return result ?: arrayOfNulls(0)
    }
}
