/*
 * Copyright 2017-2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.model.storage.drive

import android.content.Context
import com.yuriy.openradio.shared.dependencies.DependencyRegistry
import com.yuriy.openradio.shared.dependencies.FavoritesStorageDependency
import com.yuriy.openradio.shared.dependencies.LocalRadioStationsStorageDependency
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.model.storage.LocalRadioStationsStorage
import com.yuriy.openradio.shared.model.translation.RadioStationJsonDeserializer
import com.yuriy.openradio.shared.utils.AppLogger
import com.yuriy.openradio.shared.utils.JsonUtils
import com.yuriy.openradio.shared.view.SafeToast
import com.yuriy.openradio.shared.vo.RadioStation
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileOutputStream


/**
 * @param mContext  Context of the application.
 */
class ExportImportManager(private val mContext: Context) : FavoritesStorageDependency,
    LocalRadioStationsStorageDependency {

    private lateinit var mFavoritesStorage: FavoritesStorage
    private lateinit var mLocalRadioStationsStorage: LocalRadioStationsStorage

    init {
        DependencyRegistry.injectFavoritesStorage(this)
        DependencyRegistry.injectLocalRadioStationsStorage(this)
    }

    override fun configureWith(storage: FavoritesStorage) {
        mFavoritesStorage = storage
    }

    override fun configureWith(storage: LocalRadioStationsStorage) {
        mLocalRadioStationsStorage = storage
    }

    /**
     * Export Radio Stations to file.
     */
    fun exportRadioStations() {
        try {
            FileOutputStream(getFile()).use {
                it.write(getExportData().encodeToByteArray())
            }
            SafeToast.showAnyThread(mContext, "Radio stations were exported")
        } catch (e: JSONException) {
            AppLogger.e("exportRadioStations", e)
            SafeToast.showAnyThread(mContext, "Radio station export failed")
        }
    }

    /**
     * Import Radio Stations from file.
     */
    fun importRadioStations() {
        try {
            FileInputStream(getFile()).use {
                handleImportedData(String(it.readBytes()))
            }
            SafeToast.showAnyThread(mContext, "Radio stations were imported")
        } catch (e: JSONException) {
            AppLogger.e("importRadioStations", e)
            SafeToast.showAnyThread(mContext, "Radio station import failed")
        } catch (e: IllegalArgumentException) {
            SafeToast.showAnyThread(mContext, "Radio station import failed")
        }
    }

    private fun getFile() = mContext.getExternalFilesDir(null)?.resolve(FILE_NAME)

    /**
     * Get data of all Radio Stations which are intended for export.
     */
    private fun getExportData(): String {
        val jsonObject = JSONObject()
        jsonObject.put(VERSION_KEY, EXPORT_VERSION)
        jsonObject.put(RADIO_STATION_CATEGORY_FAVORITES, JSONObject(mFavoritesStorage.getAllAsJson(mContext)))
        jsonObject.put(RADIO_STATION_CATEGORY_LOCALS, JSONObject(mLocalRadioStationsStorage.getAllAsJson(mContext)))
        return jsonObject.toString(2)
    }

    /**
     * Demarshall String into lists of Radio Stations and update storage of the application.
     *
     * @param data     imported data.
     */
    private fun handleImportedData(data: String) {
        val radioStationCategories = JSONObject(data)
        //todo: add version check and add marshalling logic here when incrementing #EXPORT_VERSION
        for (radioStation in deserialize(radioStationCategories.getJSONObject(RADIO_STATION_CATEGORY_FAVORITES))) {
            mFavoritesStorage.add(radioStation, mContext)
        }
        for (radioStation in deserialize(radioStationCategories.getJSONObject(RADIO_STATION_CATEGORY_LOCALS))) {
            mLocalRadioStationsStorage.add(radioStation, mContext)
        }
    }

    private fun deserialize(category: JSONObject): List<RadioStation> =
            JsonUtils.toMap<JSONObject>(category).values.map {
                entry -> val radioStation = RadioStationJsonDeserializer().deserialize(entry)
                if (! radioStation.isValid()) {
                    AppLogger.e("deserialize: radio station %s failed to import".format(radioStation.name))
                    throw IllegalArgumentException()
                }
                radioStation
            }

    companion object {
        private const val FILE_NAME = "radio_Stations.json"
        private const val VERSION_KEY = "version"
        private const val EXPORT_VERSION = 1
        private const val RADIO_STATION_CATEGORY_FAVORITES = "favorites"
        private const val RADIO_STATION_CATEGORY_LOCALS = "locals"
        private const val FILE_NAME_RADIO_STATIONS = "RadioStations.txt"
    }
}
