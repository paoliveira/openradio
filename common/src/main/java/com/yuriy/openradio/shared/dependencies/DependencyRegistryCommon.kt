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
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.GoogleApiAvailability
import com.yuriy.openradio.shared.model.ModelLayerImpl
import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.model.media.EqualizerLayerImpl
import com.yuriy.openradio.shared.model.media.RadioStationManagerLayer
import com.yuriy.openradio.shared.model.media.RadioStationManagerLayerImpl
import com.yuriy.openradio.shared.model.net.HTTPDownloaderImpl
import com.yuriy.openradio.shared.model.net.NetworkLayer
import com.yuriy.openradio.shared.model.net.NetworkLayerImpl
import com.yuriy.openradio.shared.model.parser.ParserLayerJsonImpl
import com.yuriy.openradio.shared.model.storage.*
import com.yuriy.openradio.shared.model.storage.cache.api.InMemoryApiCache
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentApiCache
import com.yuriy.openradio.shared.model.storage.cache.api.PersistentApiDb
import com.yuriy.openradio.shared.model.storage.images.ImagesDatabase
import com.yuriy.openradio.shared.model.storage.images.ImagesPersistenceLayer
import com.yuriy.openradio.shared.model.storage.images.ImagesPersistenceLayerImpl
import com.yuriy.openradio.shared.model.storage.images.ImagesProvider
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.model.timer.SleepTimerModelImpl
import com.yuriy.openradio.shared.service.OpenRadioService
import com.yuriy.openradio.shared.service.OpenRadioServicePresenterImpl
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.RadioStationsComparator
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object DependencyRegistryCommon {

    private lateinit var sFavoritesStorage: FavoritesStorage
    private lateinit var sDeviceLocalsStorage: DeviceLocalsStorage
    private lateinit var sLatestRadioStationStorage: LatestRadioStationStorage
    private lateinit var sLocationStorage :LocationStorage
    private lateinit var sNetworkSettingsStorage: NetworkSettingsStorage
    private lateinit var sEqualizerLayer: EqualizerLayer
    private lateinit var sNetworkLayer: NetworkLayer
    private lateinit var sRadioStationManagerLayer: RadioStationManagerLayer
    private lateinit var sImagesPersistenceLayer: ImagesPersistenceLayer
    private lateinit var sOpenRadioServicePresenter: OpenRadioServicePresenterImpl
    private lateinit var sSleepTimerModel: SleepTimerModel
    /**
     * Flag that indicates whether application runs over normal Android or Android TV.
     */
    private var sIsTv = AtomicBoolean(false)
    private var sIsCar = AtomicBoolean(false)
    private var sIsCastAvailable = AtomicBoolean(false)

    @Volatile
    private var sInit = AtomicBoolean(false)

    fun init(context: Context ) {
        if (sInit.get()) {
            return
        }
        AppLogger.i("DI common inited")
        val orientationStr: String
        val orientation = context.resources.configuration.orientation
        orientationStr = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            "Landscape"
        } else {
            "Portrait"
        }
        val uiModeManager = context.getSystemService(MultiDexApplication.UI_MODE_SERVICE) as UiModeManager
        AppLogger.d("CurrentModeType:${uiModeManager.currentModeType}")
        isTv = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            AppLogger.d("Running on TV Device in $orientationStr")
            true
        } else {
            AppLogger.d("Running on non-TV Device")
            false
        }
        isCar = if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_CAR) {
            AppLogger.d("Running on Car Device in $orientationStr")
            true
        } else {
            AppLogger.d("Running on non-Car Device")
            false
        }

        val isGoogleApiAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        isCastAvailable = try {
            CastContext.getSharedInstance(context)
            true
        } catch (e: Exception) {
            AppLogger.e("Cast API availability exception '${e.message}'")
            false
        }
        AppLogger.i("Google API:$isGoogleApiAvailable")
        AppLogger.i("Cast API:$isCastAvailable")

        val parser = ParserLayerJsonImpl()
        sNetworkLayer = NetworkLayerImpl(
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
        val downloader = HTTPDownloaderImpl()
        val apiCachePersistent = PersistentApiCache(context, PersistentApiDb.DATABASE_DEFAULT_FILE_NAME)
        val apiCacheInMemory = InMemoryApiCache()
        val modelLayer = ModelLayerImpl(
            context, parser, sNetworkLayer, downloader, apiCachePersistent, apiCacheInMemory
        )
        val contextRef = WeakReference(context)
        sFavoritesStorage = FavoritesStorage(contextRef)
        sLatestRadioStationStorage = LatestRadioStationStorage(contextRef)
        sDeviceLocalsStorage = DeviceLocalsStorage(
            contextRef, sFavoritesStorage, sLatestRadioStationStorage
        )
        val equalizerStorage = EqualizerStorage(contextRef)
        sEqualizerLayer = EqualizerLayerImpl(equalizerStorage)
        val imagesDatabase = ImagesDatabase.getInstance(context)
        sImagesPersistenceLayer = ImagesPersistenceLayerImpl(context, downloader, imagesDatabase)
        sRadioStationManagerLayer = RadioStationManagerLayerImpl(
            modelLayer, sDeviceLocalsStorage, sFavoritesStorage, sImagesPersistenceLayer
        )
        sLocationStorage = LocationStorage(contextRef)
        val radioStationsComparator = RadioStationsComparator()
        sNetworkSettingsStorage = NetworkSettingsStorage(contextRef)
        sSleepTimerModel = SleepTimerModelImpl(contextRef)
        sOpenRadioServicePresenter = OpenRadioServicePresenterImpl(
            sNetworkLayer, modelLayer,
            sFavoritesStorage, sDeviceLocalsStorage, sLatestRadioStationStorage, sNetworkSettingsStorage, sLocationStorage,
            sImagesPersistenceLayer, radioStationsComparator, sEqualizerLayer,
            apiCachePersistent, apiCacheInMemory, sSleepTimerModel
        )

        sInit.set(true)
    }

    var isTv: Boolean
        get() = sIsTv.get()
        set(value) {
            sIsTv.set(value)
        }

    var isCar: Boolean
        get() = sIsCar.get()
        set(value) {
            sIsCar.set(value)
        }

    var isCastAvailable: Boolean
        get() = sIsCastAvailable.get()
        private set(value) {
            sIsCastAvailable.set(value)
        }

    fun inject(dependency: ImagesProvider) {
        dependency.configureWith(sImagesPersistenceLayer)
    }

    fun inject(service: OpenRadioService) {
        service.configureWith(sOpenRadioServicePresenter)
    }

    fun inject(dependency: SleepTimerModelDependency) {
        dependency.configureWith(sSleepTimerModel)
    }

    fun inject(dependency: RemoteControlListenerDependency) {
        dependency.configureWith(sOpenRadioServicePresenter.getRemoteControlListenerProxy())
    }

    /**
     * TODO: Find a better way to cooperate below dependencies:
     */

    fun getNetworkLayer(): NetworkLayer {
        return sNetworkLayer
    }

    fun getFavoriteStorage(): FavoritesStorage {
        return sFavoritesStorage
    }

    fun getLocalRadioStationsStorage(): DeviceLocalsStorage {
        return sDeviceLocalsStorage
    }

    fun getLatestRadioStationStorage(): LatestRadioStationStorage {
        return sLatestRadioStationStorage
    }

    fun getEqualizerLayer(): EqualizerLayer {
        return sEqualizerLayer
    }

    fun getRadioStationManagerLayer(): RadioStationManagerLayer {
        return sRadioStationManagerLayer
    }

    fun getLocationStorage(): LocationStorage {
        return sLocationStorage
    }

    fun getNetworkSettingsStorage(): NetworkSettingsStorage {
        return sNetworkSettingsStorage
    }

    fun getSleepTimerModel(): SleepTimerModel {
        return sSleepTimerModel
    }
}
