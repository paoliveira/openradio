package com.yuriy.openradio.shared.model.storage

interface StorageManagerLayer {

    fun mergeFavorites(value: String)

    fun mergeDeviceLocals(value: String)

    fun getAllFavoritesAsString(): String

    fun getAllDeviceLocalsAsString(): String
}
