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

package com.yuriy.openradio.shared.exo;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.ext.cronet.CronetDataSourceFactory;
import com.google.android.exoplayer2.ext.cronet.CronetEngineWrapper;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.yuriy.openradio.shared.utils.AppLogger;
import com.yuriy.openradio.shared.utils.AppUtils;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.File;
import java.util.concurrent.Executors;

public final class ExoPlayerUtils {

    private static final String DOWNLOAD_CONTENT_DIRECTORY = "downloads";
    private static DataSource.@MonotonicNonNull Factory sDataSourceFactory;
    private static HttpDataSource.@MonotonicNonNull Factory sHttpDataSourceFactory;
    private static @MonotonicNonNull Cache sDownloadCache;
    private static @MonotonicNonNull File sDownloadDirectory;
    private static @MonotonicNonNull DatabaseProvider sDatabaseProvider;
    private static String sUserAgent;

    private ExoPlayerUtils() {
        super();
    }

    public static RenderersFactory buildRenderersFactory(@NonNull final Context context) {
        return new DefaultRenderersFactory(context.getApplicationContext())
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
    }

    /**
     * Returns a {@link DataSource.Factory}.
     */
    public static synchronized DataSource.Factory getDataSourceFactory(@NonNull final Context context) {
        final String userAgent = AppUtils.getCustomUserAgent();
        if (!TextUtils.equals(sUserAgent, userAgent)) {
            sDataSourceFactory = null;
            sHttpDataSourceFactory = null;
        }
        sUserAgent = userAgent;
        if (sDataSourceFactory == null) {
            final HttpDataSource.Factory factory = getHttpDataSourceFactory(context, sUserAgent);
            DefaultDataSourceFactory upstreamFactory = new DefaultDataSourceFactory(context, factory);
            sDataSourceFactory = buildReadOnlyCacheDataSource(upstreamFactory, getDownloadCache(context));
        }
        return sDataSourceFactory;
    }

    public static synchronized HttpDataSource.Factory getHttpDataSourceFactory(
            @NonNull final Context context, @NonNull final String userAgent) {
        if (sHttpDataSourceFactory == null) {
            final CronetEngineWrapper cronetEngineWrapper = new CronetEngineWrapper(context);
            AppLogger.d("ExoPlayer UserAgent:" + userAgent);
            sHttpDataSourceFactory =
                    new CronetDataSourceFactory(cronetEngineWrapper, Executors.newSingleThreadExecutor(), userAgent);
        }
        return sHttpDataSourceFactory;
    }

    private static CacheDataSource.Factory buildReadOnlyCacheDataSource(
            @NonNull final DataSource.Factory upstreamFactory, @NonNull final Cache cache) {
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private static synchronized Cache getDownloadCache(@NonNull final Context context) {
        if (sDownloadCache == null) {
            File downloadContentDirectory =
                    new File(getDownloadDirectory(context), DOWNLOAD_CONTENT_DIRECTORY);
            sDownloadCache =
                    new SimpleCache(
                            downloadContentDirectory, new NoOpCacheEvictor(), getDatabaseProvider(context));
        }
        return sDownloadCache;
    }

    private static synchronized File getDownloadDirectory(@NonNull final Context context) {
        if (sDownloadDirectory == null) {
            sDownloadDirectory = context.getExternalFilesDir(null);
            if (sDownloadDirectory == null) {
                sDownloadDirectory = context.getFilesDir();
            }
        }
        return sDownloadDirectory;
    }

    private static synchronized DatabaseProvider getDatabaseProvider(@NonNull final Context context) {
        if (sDatabaseProvider == null) {
            sDatabaseProvider = new ExoDatabaseProvider(context);
        }
        return sDatabaseProvider;
    }
}
