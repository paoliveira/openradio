/*
 * Copyright 2021 The "Open Radio" Project. Author: Chernyshov Yuriy
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

package com.yuriy.openradio.shared.utils

import com.yuriy.openradio.shared.model.media.MediaId
import com.yuriy.openradio.shared.model.storage.DeviceLocalsStorage
import com.yuriy.openradio.shared.model.storage.FavoritesStorage
import com.yuriy.openradio.shared.vo.RadioStation
import java.util.Collections

object SortUtils {

    /**
     * Updates Radio Stations of a particular category with the Sort Id by the given Media Id.
     *
     * @param mediaId Media Id of the Radio Station.
     * @param sortId  Sort Id to update to.
     * @param categoryMediaId Category.
     */
    fun updateSortIds(
        comparator: Comparator<RadioStation>,
        mediaId: String, sortId: Int, categoryMediaId: String,
        favoritesStorage: FavoritesStorage, deviceLocalsStorage: DeviceLocalsStorage
    ) {
        when (categoryMediaId) {
            MediaId.MEDIA_ID_FAVORITES_LIST -> {
                val all = favoritesStorage.getAll()
                resortIds(comparator, all, sortId, mediaId)
                favoritesStorage.addAll(all)
            }
            MediaId.MEDIA_ID_LOCAL_RADIO_STATIONS_LIST -> {
                val all = deviceLocalsStorage.getAll()
                resortIds(comparator, all, sortId, mediaId)
                deviceLocalsStorage.addAll(all)
            }
        }
    }

    private fun resortIds(
        comparator: Comparator<RadioStation>,
        all: List<RadioStation>, sortId: Int, mediaId: String
    ) {
        Collections.sort(all, comparator)
        var counter = 0
        var value: Int
        for (item in all) {
            value = if (mediaId == item.id) {
                sortId
            } else {
                if (item.sortId == sortId) {
                    counter++
                }
                counter++
            }
            item.sortId = value
        }
    }
}