package com.yuriy.openradio.shared.model.storage

class StorageManagerLayerImpl(
    private val mFavoritesStorage: FavoritesStorage,
    private val mDeviceLocalsStorage: DeviceLocalsStorage
) : StorageManagerLayer {

    override fun mergeFavorites(value: String) {
        val list = mFavoritesStorage.getAll()
        val rxList = mFavoritesStorage.getAllFromString(value)
        RadioStationsStorage.merge(list, rxList)
        for (station in list) {
            mFavoritesStorage.add(station)
        }
    }

    override fun mergeDeviceLocals(value: String) {
        val list = mDeviceLocalsStorage.getAll()
        val rxList = mDeviceLocalsStorage.getAllFromString(value)
        RadioStationsStorage.merge(list, rxList)
        for (station in list) {
            mDeviceLocalsStorage.add(station)
        }
    }

    override fun getAllFavoritesAsString(): String {
        return mFavoritesStorage.getAllFavoritesAsString()
    }

    override fun getAllDeviceLocalsAsString(): String {
        return mDeviceLocalsStorage.getAllAsString()
    }
}
