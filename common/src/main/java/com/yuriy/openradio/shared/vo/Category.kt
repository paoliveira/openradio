/*
 * Copyright 2017-2023 The "Open Radio" Project. Author: Chernyshov Yuriy
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
class Category(
    val id: String,
    val title: String,
    private val mStationsCount: Int
) : Comparable<Category> {

    fun getDescription(context: Context): String {
        var desc = mStationsCount.toString()
        desc += if (mStationsCount == 0 || mStationsCount > 1) {
            " " + context.getString(R.string.radio_stations)
        } else {
            " " + context.getString(R.string.radio_station)
        }
        return desc
    }

    override fun compareTo(other: Category): Int {
        return other.mStationsCount.compareTo(mStationsCount)
    }
}
