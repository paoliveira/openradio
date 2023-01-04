package com.yuriy.openradio.shared.service

import android.content.Context
import com.yuriy.openradio.shared.model.ModelLayer
import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.model.media.RemoteControlListener
import com.yuriy.openradio.shared.model.net.NetworkLayer
import com.yuriy.openradio.shared.model.net.NetworkMonitorListener
import com.yuriy.openradio.shared.model.net.UrlLayer
import com.yuriy.openradio.shared.model.storage.DeviceLocalsStorage
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LatestRadioStationStorage
import com.yuriy.openradio.shared.model.storage.LocationStorage
import com.yuriy.openradio.shared.model.storage.NetworkSettingsStorage
import com.yuriy.openradio.shared.model.storage.cache.api.ApiCache
import com.yuriy.openradio.shared.model.storage.images.ImagesPersistenceLayer
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.model.translation.MediaIdBuilderDefault
import com.yuriy.openradio.shared.utils.SortUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.RadioStation

class OpenRadioServicePresenterImpl(
    private val mUrlLayer: UrlLayer,
    private val mNetworkLayer: NetworkLayer,
    private val mModelLayer: ModelLayer,
    private var mFavoritesStorage: FavoritesStorage,
    private val mDeviceLocalsStorage: DeviceLocalsStorage,
    private val mLatestRadioStationStorage: LatestRadioStationStorage,
    private val mNetworkSettingsStorage: NetworkSettingsStorage,
    private val mLocationStorage: LocationStorage,
    private var mImagesPersistenceLayer: ImagesPersistenceLayer,
    private val mEqualizerLayer: EqualizerLayer,
    private val mApiCachePersistent: ApiCache,
    private val mApiCacheInMemory: ApiCache,
    private val mSleepTimerModel: SleepTimerModel
) : OpenRadioServicePresenter {

    private var mRemoteControlListener: RemoteControlListener? = null
    private val mRemoteControlListenerProxy = RemoteControlListenerProxy()

    fun getRemoteControlListenerProxy(): RemoteControlListener {
        return mRemoteControlListenerProxy
    }

    override fun startNetworkMonitor(context: Context, listener: NetworkMonitorListener) {
        mNetworkLayer.startMonitor(context, listener)
    }

    override fun stopNetworkMonitor(context: Context) {
        mNetworkLayer.stopMonitor(context)
    }

    override fun isMobileNetwork(): Boolean {
        return mNetworkLayer.isMobileNetwork()
    }

    override fun getUseMobile(): Boolean {
        return mNetworkSettingsStorage.getUseMobile()
    }

    override fun getStationsInCategory(categoryId: String, pageNumber: Int): Set<RadioStation> {
        return mModelLayer.getStations(
            mUrlLayer.getStationsInCategory(
                categoryId,
                pageNumber
            ),
            MediaIdBuilderDefault()
        )
    }

    override fun getStationsByCountry(countryCode: String, pageNumber: Int): Set<RadioStation> {
        return mModelLayer.getStations(
            mUrlLayer.getStationsByCountry(
                countryCode,
                pageNumber
            ),
            MediaIdBuilderDefault()
        )
    }

    override fun getRecentlyAddedStations(): Set<RadioStation> {
        return mModelLayer.getStations(
            mUrlLayer.getRecentlyAddedStations(),
            MediaIdBuilderDefault()
        )
    }

    override fun getPopularStations(): Set<RadioStation> {
        return mModelLayer.getStations(
            mUrlLayer.getPopularStations(),
            MediaIdBuilderDefault()
        )
    }

    override fun getSearchStations(query: String, mediaIdBuilder: MediaIdBuilder): Set<RadioStation> {
        return mModelLayer.getStations(
            mUrlLayer.getSearchUrl(query), mediaIdBuilder
        )
    }

    override fun getAllCategories(): Set<Category> {
        return mModelLayer.getCategories(mUrlLayer.getAllCategoriesUrl())
    }

    override fun getAllCountries(): Set<Country> {
        return mModelLayer.getCountries()
    }

    override fun getAllFavorites(): Set<RadioStation> {
        val list = mFavoritesStorage.getAll()
        // TODO: Investigate this algorithm.
        var counter = 0
        for (radioStation in list) {
            radioStation.sortId = counter++
        }
        return list
    }

    override fun getAllDeviceLocal(): Set<RadioStation> {
        return mDeviceLocalsStorage.getAll()
    }

    override fun getLastRadioStation(): RadioStation {
        return mLatestRadioStationStorage.get()
    }

    override fun setLastRadioStation(radioStation: RadioStation) {
        mLatestRadioStationStorage.add(radioStation)
    }

    override fun getCountryCode(): String {
        return mLocationStorage.getCountryCode()
    }

    override fun isRadioStationFavorite(radioStation: RadioStation): Boolean {
        return mFavoritesStorage.isFavorite(radioStation)
    }

    override fun updateRadioStationFavorite(radioStation: RadioStation) {
        updateRadioStationFavorite(radioStation, isRadioStationFavorite(radioStation).not())
    }

    override fun updateRadioStationFavorite(radioStation: RadioStation, isFavorite: Boolean) {
        if (isFavorite) {
            mFavoritesStorage.add(radioStation)
        } else {
            mFavoritesStorage.remove(radioStation)
        }
    }

    override fun updateSortIds(
        mediaId: String,
        sortId: Int,
        categoryMediaId: String
    ) {
        SortUtils.updateSortIds(
            mediaId, sortId, categoryMediaId,
            mFavoritesStorage, mDeviceLocalsStorage
        )
    }

    override fun getEqualizerLayer(): EqualizerLayer {
        return mEqualizerLayer
    }

    override fun clear() {
        mApiCachePersistent.clear()
        mApiCacheInMemory.clear()
        mImagesPersistenceLayer.deleteAll()
        mLatestRadioStationStorage.clear()
    }

    override fun close() {
        mApiCacheInMemory.clear()
    }

    override fun getSleepTimerModel(): SleepTimerModel {
        return mSleepTimerModel
    }

    override fun setRemoteControlListener(value: RemoteControlListener) {
        mRemoteControlListener = value
    }

    override fun removeRemoteControlListener(value: RemoteControlListener) {
        mRemoteControlListener = null
    }

    private inner class RemoteControlListenerProxy : RemoteControlListener {

        override fun onMediaPlay() {
            mRemoteControlListener?.onMediaPlay()
        }

        override fun onMediaPlayPause() {
            mRemoteControlListener?.onMediaPlayPause()
        }

        override fun onMediaPauseStop() {
            mRemoteControlListener?.onMediaPauseStop()
        }
    }
}
