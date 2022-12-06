package com.yuriy.openradio.shared.service

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import com.yuriy.openradio.shared.model.ModelLayer
import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.model.media.RemoteControlListener
import com.yuriy.openradio.shared.model.net.NetworkLayer
import com.yuriy.openradio.shared.model.net.NetworkMonitorListener
import com.yuriy.openradio.shared.model.net.UrlBuilder
import com.yuriy.openradio.shared.model.storage.*
import com.yuriy.openradio.shared.model.storage.cache.api.ApiCache
import com.yuriy.openradio.shared.model.storage.images.ImagesPersistenceLayer
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.model.translation.MediaIdBuilderDefault
import com.yuriy.openradio.shared.utils.MediaItemsComparator
import com.yuriy.openradio.shared.utils.SortUtils
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.*

class OpenRadioServicePresenterImpl(
    private val mNetworkLayer: NetworkLayer,
    private val mModelLayer: ModelLayer,
    private var mFavoritesStorage: FavoritesStorage,
    private val mDeviceLocalsStorage: DeviceLocalsStorage,
    private val mLatestRadioStationStorage: LatestRadioStationStorage,
    private val mNetworkSettingsStorage: NetworkSettingsStorage,
    private val mLocationStorage: LocationStorage,
    private var mImagesPersistenceLayer: ImagesPersistenceLayer,
    private val mRadioStationsComparator: Comparator<RadioStation>,
    private val mEqualizerLayer: EqualizerLayer,
    private val mApiCachePersistent: ApiCache,
    private val mApiCacheInMemory: ApiCache,
    private val mSleepTimerModel: SleepTimerModel
) : OpenRadioServicePresenter {

    private val mMediaItemsComparator = MediaItemsComparator()
    private var mRemoteControlListener: RemoteControlListener ?= null
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

    override fun getStationById(id: String): RadioStation {
        return mModelLayer.getStation(UrlBuilder.getStation(id), MediaIdBuilderDefault())
    }

    override fun getStationsInCategory(categoryId: String, pageNumber: Int): List<RadioStation> {
        return ArrayList(
            mModelLayer.getStations(
                UrlBuilder.getStationsInCategory(
                    categoryId,
                    pageNumber * (UrlBuilder.ITEMS_PER_PAGE + 1),
                    UrlBuilder.ITEMS_PER_PAGE
                ),
                MediaIdBuilderDefault()
            )
        )
    }

    override fun getStationsByCountry(countryCode: String, pageNumber: Int): List<RadioStation> {
        return ArrayList(
            mModelLayer.getStations(
                UrlBuilder.getStationsByCountry(
                    countryCode,
                    pageNumber * (UrlBuilder.ITEMS_PER_PAGE + 1),
                    UrlBuilder.ITEMS_PER_PAGE
                ),
                MediaIdBuilderDefault()
            )
        )
    }

    override fun getRecentlyAddedStations(): List<RadioStation> {
        return ArrayList(
            mModelLayer.getStations(
                UrlBuilder.getRecentlyAddedStations(),
                MediaIdBuilderDefault()
            )
        )
    }

    override fun getPopularStations(): List<RadioStation> {
        return ArrayList(
            mModelLayer.getStations(
                UrlBuilder.getPopularStations(),
                MediaIdBuilderDefault()
            )
        )
    }

    override fun getSearchStations(query: String, mediaIdBuilder: MediaIdBuilder): List<RadioStation> {
        return ArrayList(
            mModelLayer.getStations(
                UrlBuilder.getSearchUrl(query), mediaIdBuilder
            )
        )
    }

    override fun getAllCategories(): List<Category> {
        return ArrayList(mModelLayer.getCategories(UrlBuilder.allCategoriesUrl))
    }

    override fun getAllCountries(): List<Country> {
        return ArrayList(mModelLayer.getCountries(UrlBuilder.allCountriesUrl))
    }

    override fun getAllFavorites(): List<RadioStation> {
        val list = mFavoritesStorage.getAll()
        Collections.sort(list, mRadioStationsComparator)
        // TODO: Investigate this algorithm.
        var counter = 0
        for (radioStation in list) {
            radioStation.sortId = counter++
        }
        return list
    }

    override fun getAllDeviceLocal(): List<RadioStation> {
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
        updateRadioStationFavorite(radioStation, isRadioStationFavorite(radioStation))
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
            mRadioStationsComparator, mediaId, sortId, categoryMediaId,
            mFavoritesStorage, mDeviceLocalsStorage
        )
    }

    override fun getEqualizerLayer(): EqualizerLayer {
        return mEqualizerLayer
    }

    override fun getRadioStationsComparator(): Comparator<RadioStation> {
        return mRadioStationsComparator
    }

    override fun getMediaItemsComparator(): Comparator<MediaBrowserCompat.MediaItem> {
        return mMediaItemsComparator
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

    private inner class RemoteControlListenerProxy: RemoteControlListener {

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
