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

package com.yuriy.openradio.shared.dependencies

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import androidx.multidex.MultiDexApplication
import com.yuriy.openradio.shared.model.api.ApiServiceProvider
import com.yuriy.openradio.shared.model.api.ApiServiceProviderImpl
import com.yuriy.openradio.shared.model.net.Downloader
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.NetworkMonitor
import com.yuriy.openradio.shared.model.parser.DataParser
import com.yuriy.openradio.shared.model.parser.JsonDataParserImpl
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.model.storage.images.ImagesDatabase
import com.yuriy.openradio.shared.utils.AppLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

object DependencyRegistry {

    private lateinit var mNetMonitor: NetworkMonitor
    private lateinit var mDownloader: Downloader
    private lateinit var mParser: DataParser
    private lateinit var mProvider: ApiServiceProvider
    private lateinit var mFavoritesStorage: FavoritesStorage
    private lateinit var mLocalRadioStationsStorage: LocalRadioStationsStorage
    private lateinit var mLatestRadioStationStorage: LatestRadioStationStorage
    private lateinit var mImagesDatabase: ImagesDatabase
    /**
     * Flag that indicates whether application runs over normal Android or Android TV.
     */
    private var mIsTv by Delegates.notNull<Boolean>()

    @Volatile
    private var mInit = AtomicBoolean(false)

    fun init(context: Context) {
        if (mInit.get()) {
            return
        }

        val orientationStr: String
        val orientation = context.resources.configuration.orientation
        orientationStr = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            "Landscape"
        } else {
            "Portrait"
        }
        val uiModeManager = context.getSystemService(MultiDexApplication.UI_MODE_SERVICE) as UiModeManager
        mIsTv = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            AppLogger.d("Running on a TV Device in $orientationStr")
            true
        } else {
            AppLogger.d("Running on a non-TV Device")
            false
        }

        mImagesDatabase = ImagesDatabase.getInstance(context)
        mNetMonitor = NetworkMonitor(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
        mNetMonitor.init()
        mDownloader = HTTPDownloaderImpl()
        mParser = JsonDataParserImpl()
        mProvider = ApiServiceProviderImpl(context, mParser, mNetMonitor)
        mFavoritesStorage = FavoritesStorage()
        mLatestRadioStationStorage = LatestRadioStationStorage()
        mLocalRadioStationsStorage = LocalRadioStationsStorage(
            mFavoritesStorage, mLatestRadioStationStorage
        )

        mInit.set(true)
    }

    fun isTv(): Boolean {
        return mIsTv
    }

    fun injectNetworkMonitor(dependency: NetworkMonitorDependency) {
        dependency.configureWith(mNetMonitor)
    }

    fun injectDownloader(dependency: DownloaderDependency) {
        dependency.configureWith(mDownloader)
    }

    fun injectParser(dependency: ParserDependency) {
        dependency.configureWith(mParser)
    }

    fun injectProvider(dependency: ApiServiceProviderDependency) {
        dependency.configureWith(mProvider)
    }

    fun injectFavoritesStorage(dependency: FavoritesStorageDependency) {
        dependency.configureWith(mFavoritesStorage)
    }

    fun injectLocalRadioStationsStorage(dependency: LocalRadioStationsStorageDependency) {
        dependency.configureWith(mLocalRadioStationsStorage)
    }

    fun injectLatestRadioStationStorage(dependency: LatestRadioStationStorageDependency) {
        dependency.configureWith(mLatestRadioStationStorage)
    }

    fun injectImagesDatabase(dependency: ImagesDatabaseDependency) {
        dependency.configureWith(mImagesDatabase)
    }
}
