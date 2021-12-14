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

package com.yuriy.openradio.shared.model.storage.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.yuriy.openradio.shared.R
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

/**
 * Manager for export and import of favorite and local radio stations to/from a file.
 *
 * @author Eran Leshem
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
    @OptIn(DelicateCoroutinesApi::class)
    fun exportRadioStations(intent: Intent?) {
        val uri = getUri(intent)
        if (uri != Uri.EMPTY) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    mContext.contentResolver.openOutputStream(uri)?.use {
                        it.write(getExportData().encodeToByteArray())
                        SafeToast.showAnyThread(mContext, "Radio stations were exported")
                    }
                } catch (e: JSONException) {
                    AppLogger.e("exportRadioStations", e)
                    SafeToast.showAnyThread(mContext, "Radio station export failed")
                }
            }
        }
    }

    /**
     * Import Radio Stations from file.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun importRadioStations(intent: Intent?) {
        val uri = getUri(intent)
        if (uri != Uri.EMPTY) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    mContext.contentResolver.openInputStream(uri)?.use {
                        handleImportedData(String(it.readBytes()))
                        SafeToast.showAnyThread(mContext, "Radio stations were imported")
                    }
                } catch (e: JSONException) {
                    AppLogger.e("importRadioStations", e)
                    SafeToast.showAnyThread(mContext, "Radio station import failed")
                } catch (e: IllegalArgumentException) {
                    SafeToast.showAnyThread(mContext, "Radio station import failed")
                }
            }
        }
    }

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
            JsonUtils.toMap<JSONObject>(category).values.map { entry ->
                val radioStation = RadioStationJsonDeserializer().deserialize(entry)
                if (!radioStation.isValid()) {
                    AppLogger.e("deserialize: radio station %s failed to import".format(radioStation.name))
                    throw IllegalArgumentException()
                }
                radioStation
            }

    private fun getUri(intent: Intent?): Uri {
        val selectedFile = intent?.data
        if (selectedFile == null) {
            AppLogger.e("Can not process export/import - file uri is null")
            SafeToast.showAnyThread(mContext, mContext.getString(R.string.can_not_open_file))
            return Uri.EMPTY
        }

        return selectedFile
    }

    companion object {
        private const val VERSION_KEY = "version"
        private const val EXPORT_VERSION = 1
        private const val RADIO_STATION_CATEGORY_FAVORITES = "favorites"
        private const val RADIO_STATION_CATEGORY_LOCALS = "locals"
    }
}
