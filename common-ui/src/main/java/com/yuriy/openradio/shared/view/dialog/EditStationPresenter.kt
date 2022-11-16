package com.yuriy.openradio.shared.view.dialog

import com.yuriy.openradio.shared.vo.RadioStation

/**
 * // TODO : Move business logic from Edit Dialog here.
 */
interface EditStationPresenter {

    fun getDeviceLocalRadioStation(mediaId: String): RadioStation

    fun isRadioStationFavorite(radioStation: RadioStation): Boolean
}
