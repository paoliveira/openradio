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

/**
 * Created by Chernyshov Yurii
 * At Android Studio
 * On 01/11/16
 * E-Mail: chernyshov.yuriy@gmail.com
 */
data class Country(val name: String, val code: String) : Comparable<Country> {

    override fun compareTo(other: Country): Int {
        return code.compareTo(other.code)
    }

    override fun toString(): String {
        return "Country{name='$name', code='$code'}"
    }

    companion object {
        /**
         * Default value of the Country Code.
         */
        const val COUNTRY_CODE_DEFAULT = "CA"

        /**
         * Ottawa longitude.
         */
        const val LONG_DEFAULT = 45.4215

        /**
         * Ottawa latitude.
         */
        const val LAT_DEFAULT = 75.6972
    }
}
