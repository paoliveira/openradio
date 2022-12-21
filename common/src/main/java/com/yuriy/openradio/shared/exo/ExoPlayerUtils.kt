/*
 * Copyright 2020-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.yuriy.openradio.shared.exo.extentions.album
import com.yuriy.openradio.shared.exo.extentions.albumArtUri
import com.yuriy.openradio.shared.exo.extentions.artist
import com.yuriy.openradio.shared.exo.extentions.id
import com.yuriy.openradio.shared.exo.extentions.mediaUri
import com.yuriy.openradio.shared.exo.extentions.title
import com.yuriy.openradio.shared.utils.AnalyticsUtils
import com.yuriy.openradio.shared.utils.AppUtils
import com.yuriy.openradio.shared.utils.AppUtils.getUserAgent
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

object ExoPlayerUtils {

    const val METADATA_ID_TT2 = "TT2"
    const val METADATA_ID_TIT2 = "TIT2"

    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"
    private var sDataSourceFactory: DataSource.Factory? = null
    private var sHttpDataSourceFactory: HttpDataSource.Factory? = null
    private var sDownloadCache: Cache? = null
    private var sDownloadDirectory: File? = null
    private var sDatabaseProvider: DatabaseProvider? = null
    private var sUserAgent = AppUtils.EMPTY_STRING

    fun buildRenderersFactory(context: Context): RenderersFactory {
        return DefaultRenderersFactory(context.applicationContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }

    /**
     * Returns a [DataSource.Factory].
     */
    @Synchronized
    fun getDataSourceFactory(context: Context): DataSource.Factory? {
        val userAgent = getUserAgent(context)
        if (sUserAgent == userAgent) {
            sDataSourceFactory = null
            sHttpDataSourceFactory = null
        }
        sUserAgent = userAgent
        if (sDataSourceFactory == null) {
            val factory = getHttpDataSourceFactory(sUserAgent)
            val upstreamFactory = DefaultDataSource.Factory(context, factory!!)
            sDataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context)!!)
        }
        return sDataSourceFactory
    }

    @Synchronized
    fun getHttpDataSourceFactory(userAgent: String): HttpDataSource.Factory? {
        if (sHttpDataSourceFactory == null) {
            AnalyticsUtils.logMessage("ExoPlayer UserAgent '$userAgent'")
            val cookieManager = CookieManager()
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
            CookieHandler.setDefault(cookieManager)
            sHttpDataSourceFactory =
                DefaultHttpDataSource.Factory().setUserAgent(userAgent).setAllowCrossProtocolRedirects(true)
        }
        return sHttpDataSourceFactory
    }

    fun getMediaMetadataCompat(mediaItem: MediaItem?, streamMetadata: String): MediaMetadataCompat {
        val item = MediaMetadataCompat.Builder()
        if (mediaItem == null) {
            item.id = AppUtils.EMPTY_STRING
            return item.build()
        }
        val requestMetadata = mediaItem.requestMetadata
        val mediaMetadata = mediaItem.mediaMetadata
        item.id = mediaItem.mediaId
        item.title = mediaMetadata.title.toString()
        item.album = mediaMetadata.albumTitle.toString()
        item.artist = streamMetadata
        item.mediaUri = requestMetadata.mediaUri.toString()
        item.albumArtUri = mediaMetadata.artworkUri.toString()
        return item.build()
    }

    private fun buildReadOnlyCacheDataSource(
        upstreamFactory: DataSource.Factory,
        cache: Cache
    ): CacheDataSource.Factory {
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
                downloadContentDirectory, NoOpCacheEvictor(), getDatabaseProvider(context)!!
            )
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
            sDatabaseProvider = StandaloneDatabaseProvider(context)
        }
        return sDatabaseProvider
    }
}
