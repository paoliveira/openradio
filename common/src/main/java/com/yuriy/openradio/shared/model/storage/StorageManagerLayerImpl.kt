/*
 * Copyright 2022 The "Open Radio" Project. Author: Chernyshov Yuriy
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
        return mFavoritesStorage.getAllAsString()
    }

    override fun getAllDeviceLocalsAsString(): String {
        return mDeviceLocalsStorage.getAllAsString()
    }
}
