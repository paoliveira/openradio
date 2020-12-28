/*
 * Copyright 2017-2020 The "Open Radio" Project. Author: Chernyshov Yuriy
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
package com.yuriy.openradio.shared.vo

import android.content.Context
import com.yuriy.openradio.R

/**
 * Created by Yuriy Chernyshov
 * At Android Studio
 * On 12/15/14
 * E-Mail: chernyshov.yuriy@gmail.com
 *
 * [Category] is a value object that holds data related to category of Radio Stations.
 */
class Category
/**
 * Private constructor.
 * Disallow instantiation of this helper class.
 */
private constructor() {

    var id: String? = null
    var stationsCount = 0
    var title = ""

    fun getDescription(context: Context): String {
        var desc = stationsCount.toString()
        desc += if (stationsCount == 0 || stationsCount > 1) {
            " " + context.getString(R.string.radio_stations)
        } else {
            " " + context.getString(R.string.radio_station)
        }
        return desc
    }

    companion object {
        /**
         * Factory method to create instance of the [Category].
         *
         * @return Instance of the [Category].
         */
        @JvmStatic
        fun makeDefaultInstance(): Category {
            return Category()
        }
    }
}
