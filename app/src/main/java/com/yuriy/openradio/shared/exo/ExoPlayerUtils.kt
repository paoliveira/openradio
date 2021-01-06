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
package com.yuriy.openradio.shared.exo

import android.content.Context
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.cronet.CronetDataSourceFactory
import com.google.android.exoplayer2.ext.cronet.CronetEngineWrapper
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.yuriy.openradio.shared.utils.AnalyticsUtils.logMessage
import com.yuriy.openradio.shared.utils.AppUtils.getUserAgent
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import java.io.File
import java.util.concurrent.*

object ExoPlayerUtils {

    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
    private var sDataSourceFactory: @MonotonicNonNull DataSource.Factory? = null
    private var sHttpDataSourceFactory: @MonotonicNonNull HttpDataSource.Factory? = null
    private var sDownloadCache: @MonotonicNonNull Cache? = null
    private var sDownloadDirectory: @MonotonicNonNull File? = null
    private var sDatabaseProvider: @MonotonicNonNull DatabaseProvider? = null
    private var sUserAgent: String? = null

    @JvmStatic
    fun buildRenderersFactory(context: Context): RenderersFactory {
        return DefaultRenderersFactory(context.applicationContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }

    /**
     * Returns a [DataSource.Factory].
     */
    @JvmStatic
    @Synchronized
    fun getDataSourceFactory(context: Context): DataSource.Factory? {
        val userAgent = getUserAgent(context)
        if (sUserAgent == userAgent) {
            sDataSourceFactory = null
            sHttpDataSourceFactory = null
        }
        sUserAgent = userAgent
        if (sDataSourceFactory == null) {
            val factory = getHttpDataSourceFactory(context, sUserAgent!!)
            val upstreamFactory = DefaultDataSourceFactory(context, factory!!)
            sDataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context)!!)
        }
        return sDataSourceFactory
    }

    @Synchronized
    fun getHttpDataSourceFactory(
            context: Context, userAgent: String): HttpDataSource.Factory? {
        if (sHttpDataSourceFactory == null) {
            val cronetEngineWrapper = CronetEngineWrapper(context)
            logMessage("ExoPlayer UserAgent '$userAgent'")
            sHttpDataSourceFactory = CronetDataSourceFactory(
                    cronetEngineWrapper, Executors.newSingleThreadExecutor(), userAgent
            )
        }
        return sHttpDataSourceFactory
    }

    private fun buildReadOnlyCacheDataSource(
            upstreamFactory: DataSource.Factory, cache: Cache): CacheDataSource.Factory {
        return CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    private fun getDownloadCache(context: Context): Cache? {
        if (sDownloadCache == null) {
            val downloadContentDirectory = File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY)
            sDownloadCache = SimpleCache(
                    downloadContentDirectory, NoOpCacheEvictor(), getDatabaseProvider(context)!!)
        }
        return sDownloadCache
    }

    @Synchronized
    private fun getDownloadDirectory(context: Context): File? {
        if (sDownloadDirectory == null) {
            sDownloadDirectory = context.getExternalFilesDir(null)
            if (sDownloadDirectory == null) {
                sDownloadDirectory = context.filesDir
            }
        }
        return sDownloadDirectory
    }

    @Synchronized
    private fun getDatabaseProvider(context: Context): DatabaseProvider? {
        if (sDatabaseProvider == null) {
            sDatabaseProvider = ExoDatabaseProvider(context)
        }
        return sDatabaseProvider
    }
}
