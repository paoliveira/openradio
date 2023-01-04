package com.yuriy.openradio.shared.service

import android.content.Context
import com.yuriy.openradio.shared.model.media.EqualizerLayer
import com.yuriy.openradio.shared.model.media.RemoteControlListener
import com.yuriy.openradio.shared.model.net.NetworkMonitorListener
import com.yuriy.openradio.shared.model.timer.SleepTimerModel
import com.yuriy.openradio.shared.model.translation.MediaIdBuilder
import com.yuriy.openradio.shared.model.translation.MediaIdBuilderDefault
import com.yuriy.openradio.shared.vo.Category
import com.yuriy.openradio.shared.vo.Country
import com.yuriy.openradio.shared.vo.RadioStation

interface OpenRadioServicePresenter {

    fun startNetworkMonitor(context: Context, listener: NetworkMonitorListener)

    fun stopNetworkMonitor(context: Context)

    fun isMobileNetwork(): Boolean

    fun getUseMobile(): Boolean

    fun getStationsInCategory(categoryId: String, pageNumber: Int): Set<RadioStation>

    fun getStationsByCountry(countryCode: String, pageNumber: Int): Set<RadioStation>

    fun getRecentlyAddedStations(): Set<RadioStation>

    fun getPopularStations(): Set<RadioStation>

    fun getSearchStations(query: String, mediaIdBuilder: MediaIdBuilder = MediaIdBuilderDefault()): Set<RadioStation>

    fun getAllCategories(): Set<Category>

    fun getAllCountries(): Set<Country>

    fun getAllFavorites(): Set<RadioStation>

    fun getAllDeviceLocal(): Set<RadioStation>

    fun getLastRadioStation(): RadioStation

    fun getEqualizerLayer(): EqualizerLayer

    fun setLastRadioStation(radioStation: RadioStation)

    fun getCountryCode(): String

    fun isRadioStationFavorite(radioStation: RadioStation): Boolean

    fun updateRadioStationFavorite(radioStation: RadioStation)

    fun updateRadioStationFavorite(radioStation: RadioStation, isFavorite: Boolean)

    fun updateSortIds(mediaId: String, sortId: Int, categoryMediaId: String)

    fun getSleepTimerModel(): SleepTimerModel

    /**
     * Clear resources related to service provider, such as persistent or in memory storage, etc ...
     */
    fun clear()

    /**
     * Close resources related to service provider, such as connections, streams, etc ...
     */
    fun close()

    fun setRemoteControlListener(value: RemoteControlListener)

    fun removeRemoteControlListener(value: RemoteControlListener)
}
