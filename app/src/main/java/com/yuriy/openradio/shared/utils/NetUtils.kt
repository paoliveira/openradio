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
import com.yuriy.openradio.shared.model.net.DownloaderException
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logException
import com.yuriy.openradio.shared.utils.AppLogger.d
import com.yuriy.openradio.shared.utils.AppLogger.i
import com.yuriy.openradio.shared.utils.AppUtils.getUserAgent
import okhttp3.internal.Util
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder

object NetUtils {

    private const val USER_AGENT_PARAMETER_KEY = "User-Agent"

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
        try {
            connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = AppUtils.TIME_OUT
            connection.connectTimeout = AppUtils.TIME_OUT
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.defaultUseCaches = false
            connection.requestMethod = requestMethod
            val userAgent = getUserAgent(context)
            connection.setRequestProperty(USER_AGENT_PARAMETER_KEY, userAgent)
            d("NetUtils UserAgent:$userAgent")

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
                                    DownloaderException.createExceptionMessage(url.toString(), parameters), exception
                            )
                    )
                }
            }
            connection.connect()
        } catch (exception: IOException) {
            logException(
                    RuntimeException("Can not get http connection from $url", exception)
            )
        }
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
        val responseCode: Int
        responseCode = try {
            connection.responseCode
        } catch (exception: IOException) {
            closeHttpURLConnection(connection)
            return false
        }
        if (responseCode < 200 || responseCode > 299) {
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
        i("POST query:$result")
        return result.toString()
    }
}
