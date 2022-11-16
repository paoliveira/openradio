package com.yuriy.openradio.automotive.ui

import com.yuriy.openradio.shared.model.storage.LocationStorage

class AutomotiveSettingsActivityPresenterImpl(
    private val mLocationStorage: LocationStorage
) : AutomotiveSettingsActivityPresenter {

    override fun getCountryCode(): String {
        return mLocationStorage.getCountryCode()
    }
}
